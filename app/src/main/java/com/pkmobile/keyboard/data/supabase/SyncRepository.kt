package com.pkmobile.keyboard.data.supabase

import android.content.Context
import androidx.room.withTransaction
import com.pkmobile.keyboard.data.db.AppDatabase
import com.pkmobile.keyboard.data.db.PresetEntity
import com.pkmobile.keyboard.data.db.ShortcutEntity
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
 * Repository untuk sinkronisasi 2 arah antara Supabase Cloud Database (tabel 'shortcuts')
 * dan Room Database lokal di HP Android.
 * Mengisolasi data Cloud ke dalam preset "preset_cloud" ("☁️ Cloud Sync (Supabase)")
 * agar tidak pernah menimpa atau menghapus file preset XML lokal.
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
     * Mengunduh data resmi dari Supabase Cloud dan memperbarui preset "preset_cloud"
     * secara terisolasi tanpa menyentuh file preset lain.
     * Hanya mengambil shortcut dari preset yang sedang aktif (is_active = true) di Supabase.
     */
    suspend fun sync(): SyncResult = withContext(Dispatchers.IO) {
        try {
            val userId = client.auth.currentUserOrNull()?.id ?: authService.getCurrentUserId()

            // 1. Cari Preset yang sedang Aktif (is_active = true) di Supabase
            val activePresets = try {
                client.from("presets").select {
                    filter {
                        eq("is_active", true)
                        if (userId != null) {
                            eq("user_id", userId)
                        }
                    }
                }.decodeList<SupabasePresetDto>()
            } catch (e: Exception) {
                emptyList()
            }

            val targetPreset = activePresets.firstOrNull()
            val targetPresetId = targetPreset?.id
            val cloudPresetDisplayName = if (targetPreset != null && targetPreset.name.isNotBlank()) {
                "☁️ ${targetPreset.name}"
            } else {
                CLOUD_PRESET_NAME
            }

            // 2. Ambil data unik dari Supabase Cloud (filter preset aktif jika tersedia)
            val cloudShortcuts = client.from("shortcuts").select(
                columns = Columns.raw("id,preset_id,trigger_code,expansion_text,category,expansion_mode,user_id")
            ) {
                filter {
                    if (targetPresetId != null) {
                        eq("preset_id", targetPresetId)
                    }
                    if (userId != null) {
                        eq("user_id", userId)
                    }
                }
            }
            .decodeList<SupabaseShortcutDto>()

            // 3. Bersihkan & deduplikasi data yang diunduh dari cloud
            val downloadedEntities = cloudShortcuts
                .filter { !it.triggerCode.isNullOrBlank() && !it.expansionText.isNullOrBlank() }
                .distinctBy { (it.triggerCode ?: "").trim().lowercase() }
                .map {
                    val entity = it.toEntity()
                    entity.copy(presetId = CLOUD_PRESET_ID)
                }

            // 4. Simpan ke Preset Cloud khusus (Atomic Room Transaction)
            database.withTransaction {
                // Hanya hapus shortcut lama milik preset_cloud (file lokal XML/JSON aman!)
                shortcutDao.deleteByPreset(CLOUD_PRESET_ID)

                val uniqueShortcuts = downloadedEntities.distinctBy { it.triggerCode }
                shortcutDao.insertAll(uniqueShortcuts)

                val existingPreset = presetDao.getPresetById(CLOUD_PRESET_ID)
                val allPresets = presetDao.getAllPresets()
                val shouldBeActive = existingPreset?.isActive ?: (allPresets.isEmpty() || allPresets.none { it.isActive })

                val cloudPreset = PresetEntity(
                    id = CLOUD_PRESET_ID,
                    name = cloudPresetDisplayName,
                    sourceType = "CLOUD",
                    isActive = shouldBeActive,
                    shortcutCount = uniqueShortcuts.size,
                    createdAt = existingPreset?.createdAt ?: System.currentTimeMillis()
                )
                presetDao.insertOrUpdate(cloudPreset)

                if (shouldBeActive) {
                    presetDao.setActivePreset(CLOUD_PRESET_ID)
                }
            }

            val pulledCount = downloadedEntities.size

            // Beritahu engine keyboard agar langsung memuat cache terbaru secara real-time
            CustomKeyboardService.notifyShortcutsChanged(context)

            val presetInfo = if (targetPreset != null) " [Preset: ${targetPreset.name}]" else ""
            SyncResult(
                success = true,
                pushedCount = 0,
                pulledCount = pulledCount,
                message = "Sinkronisasi sukses: $pulledCount shortcut$presetInfo berhasil diunduh ke Preset Cloud."
            )
        } catch (e: Exception) {
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
            val remotePresetId = try {
                client.from("presets").select {
                    filter {
                        eq("is_active", true)
                        if (userId != null) eq("user_id", userId)
                    }
                }.decodeList<SupabasePresetDto>().firstOrNull()?.id ?: "default_preset"
            } catch (e: Exception) {
                "default_preset"
            }

            val shortcutWithPreset = if (shortcut.presetId == CLOUD_PRESET_ID) {
                shortcut.copy(presetId = remotePresetId)
            } else {
                shortcut
            }

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
    suspend fun deleteCloudShortcut(triggerCode: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val userId = client.auth.currentUserOrNull()?.id ?: authService.getCurrentUserId()
            val cleanKey = com.pkmobile.keyboard.data.importer.ShortcutImporter.cleanTrigger(triggerCode).lowercase()

            val remotePresetId = try {
                client.from("presets").select {
                    filter {
                        eq("is_active", true)
                        if (userId != null) eq("user_id", userId)
                    }
                }.decodeList<SupabasePresetDto>().firstOrNull()?.id
            } catch (e: Exception) {
                null
            }

            client.from("shortcuts").delete {
                filter {
                    eq("trigger_code", cleanKey)
                    if (remotePresetId != null) {
                        eq("preset_id", remotePresetId)
                    }
                    if (userId != null) {
                        eq("user_id", userId)
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

