package com.pkmobile.keyboard.data.supabase

import android.content.Context
import androidx.room.withTransaction
import com.pkmobile.keyboard.data.db.AppDatabase
import com.pkmobile.keyboard.data.repository.ShortcutRepository
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
 * Repository untuk sinkronisasi Offline-First antara Room Database lokal
 * dan Supabase Cloud Database (tabel 'shortcuts').
 * Mencegah duplikasi data dengan pembersihan atomik (database.withTransaction).
 */
class SyncRepository(
    private val context: Context,
    private val shortcutRepository: ShortcutRepository,
    private val authService: AuthService
) {
    private val database by lazy { AppDatabase.getInstance(context) }
    private val shortcutDao by lazy { database.shortcutDao() }
    private val client by lazy { SupabaseConfig.getClient(context) }

    suspend fun sync(): SyncResult = withContext(Dispatchers.IO) {
        try {
            val userId = authService.getCurrentUserId()

            // 1. Ambil semua shortcut unik dari Room lokal untuk diunggah
            val localShortcuts = shortcutDao.getAllList()
            var pushedCount = 0

            if (localShortcuts.isNotEmpty()) {
                val dtoList = localShortcuts
                    .distinctBy { it.triggerCode.trim().lowercase() }
                    .map { entity ->
                        SupabaseShortcutDto.fromEntity(entity, userId)
                    }
                client.from("shortcuts").upsert(dtoList)
                pushedCount = dtoList.size
            }

            // 2. Tarik data terbaru dari Supabase Cloud (select=trigger_code,expansion_text,category,expansion_mode,user_id)
            val cloudQuery = client.from("shortcuts").select(
                columns = Columns.raw("trigger_code,expansion_text,category,expansion_mode,user_id")
            ) {
                if (userId != null) {
                    filter {
                        eq("user_id", userId)
                    }
                }
            }
            val cloudShortcuts = cloudQuery.decodeList<SupabaseShortcutDto>()

            // 3. Bersihkan & deduplikasi data yang diunduh dari cloud
            val downloadedEntities = cloudShortcuts
                .filter { !it.triggerCode.isNullOrBlank() && !it.expansionText.isNullOrBlank() }
                .distinctBy { (it.triggerCode ?: "").trim().lowercase() }
                .map { it.toEntity() }

            // 4. Eksekusi Atomic Transaction di Room Database:
            // Bersihkan data lama terlebih dahulu agar tidak terduplikasi, lalu masukkan data terbaru
            database.withTransaction {
                shortcutDao.deleteAll()
                val uniqueShortcuts = downloadedEntities.distinctBy { it.triggerCode }
                shortcutDao.insertAll(uniqueShortcuts)
            }

            val pulledCount = downloadedEntities.size

            SyncResult(
                success = true,
                pushedCount = pushedCount,
                pulledCount = pulledCount,
                message = "Sinkronisasi sukses: $pulledCount shortcut diperbarui dari cloud."
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
            val localShortcuts = shortcutDao.getAllList()
            if (localShortcuts.isEmpty()) {
                return@withContext SyncResult(true, 0, 0, "Tidak ada data lokal untuk diunggah.")
            }
            val dtoList = localShortcuts
                .distinctBy { it.triggerCode.trim().lowercase() }
                .map { SupabaseShortcutDto.fromEntity(it, userId) }
            client.from("shortcuts").upsert(dtoList)
            SyncResult(true, dtoList.size, 0, "Berhasil mengunggah ${dtoList.size} shortcut ke cloud.")
        } catch (e: Exception) {
            SyncResult(false, message = "Gagal mengunggah: ${e.localizedMessage ?: e.message}")
        }
    }

    suspend fun pullOnly(): SyncResult = withContext(Dispatchers.IO) {
        try {
            val userId = authService.getCurrentUserId()
            val cloudQuery = client.from("shortcuts").select(
                columns = Columns.raw("trigger_code,expansion_text,category,expansion_mode,user_id")
            ) {
                if (userId != null) {
                    filter {
                        eq("user_id", userId)
                    }
                }
            }
            val cloudShortcuts = cloudQuery.decodeList<SupabaseShortcutDto>()

            val downloadedEntities = cloudShortcuts
                .filter { !it.triggerCode.isNullOrBlank() && !it.expansionText.isNullOrBlank() }
                .distinctBy { (it.triggerCode ?: "").trim().lowercase() }
                .map { it.toEntity() }

            database.withTransaction {
                shortcutDao.deleteAll()
                val uniqueShortcuts = downloadedEntities.distinctBy { it.triggerCode }
                shortcutDao.insertAll(uniqueShortcuts)
            }

            SyncResult(true, 0, downloadedEntities.size, "Berhasil mengunduh ${downloadedEntities.size} shortcut dari cloud.")
        } catch (e: Exception) {
            SyncResult(false, message = "Gagal mengunduh: ${e.localizedMessage ?: e.message}")
        }
    }
}
