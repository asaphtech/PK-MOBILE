package com.pkmobile.keyboard.data.supabase

import android.content.Context
import com.pkmobile.keyboard.data.repository.ShortcutRepository
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class SyncResult(
    val success: Boolean,
    val pushedCount: Int = 0,
    val pulledCount: Int = 0,
    val message: String
)

/**
 * Repository untuk sinkronisasi Offline-First antara Room Database lokal
 * dan Supabase Cloud Database (tabel 'shortcuts').
 */
class SyncRepository(
    private val context: Context,
    private val shortcutRepository: ShortcutRepository,
    private val authService: AuthService
) {
    private val client by lazy { SupabaseConfig.getClient(context) }

    suspend fun sync(): SyncResult = withContext(Dispatchers.IO) {
        try {
            val userId = authService.getCurrentUserId()

            // 1. Ambil semua shortcut dari Room lokal
            val localShortcuts = shortcutRepository.getAllList()
            var pushedCount = 0

            if (localShortcuts.isNotEmpty()) {
                val dtoList = localShortcuts.map { entity ->
                    SupabaseShortcutDto.fromEntity(entity, userId)
                }
                // Upsert data ke Supabase Cloud
                client.from("shortcuts").upsert(dtoList)
                pushedCount = dtoList.size
            }

            // 2. Tarik data terbaru dari Supabase Cloud
            val cloudQuery = client.from("shortcuts").select {
                if (userId != null) {
                    filter {
                        eq("user_id", userId)
                    }
                }
            }
            val cloudShortcuts = cloudQuery.decodeList<SupabaseShortcutDto>()
            var pulledCount = 0

            for (cloudItem in cloudShortcuts) {
                val entity = cloudItem.toEntity()
                shortcutRepository.insertOrUpdate(entity)
                pulledCount++
            }

            SyncResult(
                success = true,
                pushedCount = pushedCount,
                pulledCount = pulledCount,
                message = "Sinkronisasi sukses: $pushedCount diunggah, $pulledCount diperbarui dari cloud."
            )
        } catch (e: Exception) {
            SyncResult(
                success = false,
                message = "Gagal sinkronisasi: ${e.localizedMessage ?: e.message}"
            )
        }
    }

    suspend fun pushOnly(): SyncResult = withContext(Dispatchers.IO) {
        try {
            val userId = authService.getCurrentUserId()
            val localShortcuts = shortcutRepository.getAllList()
            if (localShortcuts.isEmpty()) {
                return@withContext SyncResult(true, 0, 0, "Tidak ada data lokal untuk diunggah.")
            }
            val dtoList = localShortcuts.map { SupabaseShortcutDto.fromEntity(it, userId) }
            client.from("shortcuts").upsert(dtoList)
            SyncResult(true, dtoList.size, 0, "Berhasil mengunggah ${dtoList.size} shortcut ke cloud.")
        } catch (e: Exception) {
            SyncResult(false, message = "Gagal mengunggah: ${e.localizedMessage ?: e.message}")
        }
    }

    suspend fun pullOnly(): SyncResult = withContext(Dispatchers.IO) {
        try {
            val userId = authService.getCurrentUserId()
            val cloudQuery = client.from("shortcuts").select {
                if (userId != null) {
                    filter {
                        eq("user_id", userId)
                    }
                }
            }
            val cloudShortcuts = cloudQuery.decodeList<SupabaseShortcutDto>()
            for (cloudItem in cloudShortcuts) {
                shortcutRepository.insertOrUpdate(cloudItem.toEntity())
            }
            SyncResult(true, 0, cloudShortcuts.size, "Berhasil mengunduh ${cloudShortcuts.size} shortcut dari cloud.")
        } catch (e: Exception) {
            SyncResult(false, message = "Gagal mengunduh: ${e.localizedMessage ?: e.message}")
        }
    }
}
