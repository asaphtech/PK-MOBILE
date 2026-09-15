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
     */
    suspend fun sync(): SyncResult = withContext(Dispatchers.IO) {
        try {
            val userId = client.auth.currentUserOrNull()?.id ?: authService.getCurrentUserId()

            // 1. Ambil data unik dari Supabase Cloud
            val cloudShortcuts = client.from("shortcuts").select(
                columns = Columns.raw("trigger_code,expansion_text,category,expansion_mode,user_id")
            ) {
                if (userId != null) {
                    filter {
                        eq("user_id", userId)
                    }
                }
            }
            .decodeList<SupabaseShortcutDto>()

            // 2. Bersihkan & deduplikasi data yang diunduh dari cloud
            val downloadedEntities = cloudShortcuts
                .filter { !it.triggerCode.isNullOrBlank() && !it.expansionText.isNullOrBlank() }
                .distinctBy { (it.triggerCode ?: "").trim().lowercase() }
                .map {
                    val entity = it.toEntity()
                    entity.copy(presetId = CLOUD_PRESET_ID)
                }

            // 3. Simpan ke Preset Cloud khusus (Atomic Room Transaction)
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
                    name = CLOUD_PRESET_NAME,
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

            SyncResult(
                success = true,
                pushedCount = 0,
                pulledCount = pulledCount,
                message = "Sinkronisasi sukses: $pulledCount shortcut resmi berhasil diunduh ke Preset Cloud."
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
            val dto = SupabaseShortcutDto.fromEntity(shortcut, userId)
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
            client.from("shortcuts").delete {
                filter {
                    eq("trigger_code", cleanKey)
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

