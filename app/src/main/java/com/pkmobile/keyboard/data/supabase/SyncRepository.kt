package com.pkmobile.keyboard.data.supabase

import android.content.Context
import androidx.room.withTransaction
import com.pkmobile.keyboard.data.db.AppDatabase
import com.pkmobile.keyboard.data.db.PresetEntity
import com.pkmobile.keyboard.data.db.ShortcutEntity
import com.pkmobile.keyboard.data.importer.ShortcutImporter
import com.pkmobile.keyboard.data.repository.ShortcutRepository
import com.pkmobile.keyboard.service.CustomKeyboardService
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class SyncResult(
    val success: Boolean,
    val pushedCount: Int = 0,
    val pulledCount: Int = 0,
    val message: String
)

/**
 * Repository untuk sinkronisasi 2 arah antara Supabase Cloud Database
 * (tabel 'presets' & 'shortcuts') dan Room Database lokal di HP Android.
 * Mengelola multi-preset terisolasi sesuai ID dan nama dari Supabase Cloud
 * tanpa menggabungkan seluruh shortcut ke dalam satu preset dummy.
 */
class SyncRepository(
    private val context: Context,
    private val shortcutRepository: ShortcutRepository,
    private val authService: AuthService
) {
    companion object {
        const val CLOUD_PRESET_ID = "preset_cloud"
        const val CLOUD_PRESET_NAME = "☁️ Cloud Sync (Supabase)"
    }

    private val database by lazy { AppDatabase.getInstance(context) }
    private val shortcutDao by lazy { database.shortcutDao() }
    private val presetDao by lazy { database.presetDao() }
    private val client by lazy { SupabaseConfig.getClient(context) }

    /**
     * Mengunduh daftar presets dan shortcuts dari Supabase Cloud,
     * membuat/memperbarui setiap preset secara terisolasi di Room lokal,
     * dan menetapkan preset yang berstatus is_active = true di Cloud sebagai preset aktif di HP.
     */
    suspend fun sync(): SyncResult = withContext(Dispatchers.IO) {
        try {
            // 1. Ambil daftar tabel 'presets' dari Supabase
            val cloudPresets = try {
                client.from("presets")
                    .select()
                    .decodeList<SupabasePresetDto>()
            } catch (e: Exception) {
                android.util.Log.e("SyncRepository", "Gagal mengambil tabel presets dari Supabase: ${e.message}", e)
                emptyList()
            }

            // 2. Ambil daftar tabel 'shortcuts' dari Supabase
            val cloudShortcuts = try {
                client.from("shortcuts")
                    .select(columns = Columns.raw("id,preset_id,trigger_code,expansion_text,category,expansion_mode,user_id"))
                    .decodeList<SupabaseShortcutDto>()
            } catch (e: Exception) {
                // Fallback jika Columns.raw gagal
                client.from("shortcuts")
                    .select()
                    .decodeList<SupabaseShortcutDto>()
            }

            if (cloudPresets.isEmpty() && cloudShortcuts.isEmpty()) {
                return@withContext SyncResult(
                    success = false,
                    message = "Tidak ada data preset maupun shortcut ditemukan di Supabase Cloud."
                )
            }

            // Filter shortcut yang valid (memiliki trigger dan expansion)
            val validShortcuts = cloudShortcuts.filter {
                !it.triggerCode.isNullOrBlank() && !it.expansionText.isNullOrBlank()
            }

            // Kumpulkan pemetaan preset ID -> Nama Preset
            val presetMap = mutableMapOf<String, String>()
            cloudPresets.forEach { p ->
                val name = p.name.trim().ifBlank { p.id }
                presetMap[p.id] = name
            }

            // Kelompokkan shortcut berdasarkan preset_id
            val shortcutsByPreset = validShortcuts.groupBy { dto ->
                dto.presetId?.trim()?.ifBlank { "default_preset" } ?: "default_preset"
            }

            // Jika ada shortcut yang merujuk ke preset_id yang belum ada di tabel presets, daftarkan
            shortcutsByPreset.keys.forEach { pId ->
                if (!presetMap.containsKey(pId)) {
                    presetMap[pId] = if (pId == "default_preset") "Paket Utama" else "Preset $pId"
                }
            }

            // Tentukan preset mana yang berstatus is_active = true dari Cloud
            val activePresetFromCloud = cloudPresets.firstOrNull { it.isActive }
            val activePresetId = activePresetFromCloud?.id

            var totalInsertedShortcuts = 0

            // 3, 4, & 5. Simpan ke Room Database lokal secara atomik dalam transaksi
            database.withTransaction {
                // Bersihkan preset_cloud lama jika pernah ada, agar tidak meninggalkan duplikat usang
                shortcutDao.deleteByPreset(CLOUD_PRESET_ID)
                presetDao.deleteById(CLOUD_PRESET_ID)

                // Hapus shortcut lama hanya untuk preset-preset yang berasal dari Cloud
                for ((pId, _) in presetMap) {
                    shortcutDao.deleteByPreset(pId)
                }

                // Masukkan masing-masing shortcut ke preset_id yang sesuai
                for ((pId, shortcutsInPreset) in shortcutsByPreset) {
                    val entities = shortcutsInPreset
                        .distinctBy { (it.triggerCode ?: "").trim().lowercase() }
                        .map { dto ->
                            val cleanTrigger = ShortcutImporter.cleanTrigger(dto.triggerCode ?: "").lowercase()
                            val cleanExp = ShortcutImporter.cleanTextFormatting(dto.expansionText ?: "")
                            ShortcutEntity(
                                presetId = pId,
                                triggerCode = cleanTrigger,
                                expansionText = cleanExp,
                                category = dto.category?.ifBlank { "General" } ?: "General",
                                expansionMode = dto.expansionMode?.ifBlank { "SPACE" } ?: "SPACE",
                                packageName = dto.category?.ifBlank { "General" } ?: "General",
                                isActive = true
                            )
                        }
                    if (entities.isNotEmpty()) {
                        shortcutDao.insertAll(entities)
                        totalInsertedShortcuts += entities.size
                    }
                }

                // Buat / Update PresetEntity di tabel presets Room lokal
                for ((pId, pName) in presetMap) {
                    val count = shortcutDao.countByPreset(pId)
                    val existing = presetDao.getPresetById(pId)

                    val shouldBeActive = if (activePresetId != null) {
                        pId == activePresetId
                    } else {
                        existing?.isActive ?: false
                    }

                    val presetEntity = PresetEntity(
                        id = pId,
                        name = pName,
                        sourceType = "CLOUD",
                        isActive = shouldBeActive,
                        shortcutCount = count,
                        createdAt = existing?.createdAt ?: System.currentTimeMillis()
                    )
                    presetDao.insertOrUpdate(presetEntity)
                }

                // Set preset lokal yang berstatus is_active = true sesuai dengan Cloud
                if (activePresetId != null) {
                    presetDao.setActivePreset(activePresetId)
                } else {
                    val all = presetDao.getAllPresets()
                    if (all.isNotEmpty() && all.none { it.isActive }) {
                        presetDao.setActivePreset(all.first().id)
                    }
                }
            }

            // Beritahu keyboard service agar langsung memuat cache shortcut terbaru
            CustomKeyboardService.notifyShortcutsChanged(context)

            val activePresetName = if (activePresetId != null) {
                presetMap[activePresetId] ?: activePresetId
            } else {
                presetDao.getActivePreset()?.name ?: "-"
            }

            val presetCount = presetMap.size
            val msg = if (presetCount > 1) {
                "Sinkronisasi sukses: $totalInsertedShortcuts shortcut berhasil diunduh ke $presetCount Preset (${presetMap.values.joinToString(", ")}). Preset aktif: '$activePresetName'."
            } else {
                "Sinkronisasi sukses: $totalInsertedShortcuts shortcut berhasil diunduh ke Preset '$activePresetName'."
            }

            SyncResult(
                success = true,
                pushedCount = 0,
                pulledCount = totalInsertedShortcuts,
                message = msg
            )
        } catch (e: Exception) {
            android.util.Log.e("SyncRepository", "Sync failed", e)
            SyncResult(
                success = false,
                message = "Gagal sinkronisasi: ${e.localizedMessage ?: e.message}"
            )
        }
    }

    /**
     * Mendorong (Push) shortcut dari HP ke Supabase Cloud (HP ➔ Web).
     */
    suspend fun pushShortcut(shortcut: ShortcutEntity): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val userId = client.auth.currentUserOrNull()?.id ?: authService.getCurrentUserId()
            val targetPresetId = if (shortcut.presetId.isNotBlank() && shortcut.presetId != CLOUD_PRESET_ID) {
                shortcut.presetId
            } else {
                try {
                    client.from("presets").select {
                        filter {
                            eq("is_active", true)
                        }
                    }.decodeList<SupabasePresetDto>().firstOrNull()?.id ?: "default_preset"
                } catch (e: Exception) {
                    "default_preset"
                }
            }

            val shortcutWithPreset = shortcut.copy(presetId = targetPresetId)
            val dto = SupabaseShortcutDto.fromEntity(shortcutWithPreset, userId)
            client.from("shortcuts").upsert(dto)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Menghapus shortcut dari Supabase Cloud (HP ➔ Web).
     */
    suspend fun deleteCloudShortcut(triggerCode: String, presetId: String? = null): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val cleanKey = ShortcutImporter.cleanTrigger(triggerCode).lowercase()
            val targetPresetId = presetId ?: try {
                client.from("presets").select {
                    filter {
                        eq("is_active", true)
                    }
                }.decodeList<SupabasePresetDto>().firstOrNull()?.id
            } catch (e: Exception) {
                null
            }

            client.from("shortcuts").delete {
                filter {
                    eq("trigger_code", cleanKey)
                    if (targetPresetId != null) {
                        eq("preset_id", targetPresetId)
                    }
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun pullOnly(): SyncResult = sync()
}

