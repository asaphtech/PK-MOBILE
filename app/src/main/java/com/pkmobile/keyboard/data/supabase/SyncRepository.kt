package com.pkmobile.keyboard.data.supabase

import android.content.Context
import androidx.room.withTransaction
import com.pkmobile.keyboard.data.db.AppDatabase
import com.pkmobile.keyboard.data.repository.ShortcutRepository
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
 * Repository untuk sinkronisasi PULL ONLY dari Supabase Cloud Database (tabel 'shortcuts')
 * ke Room Database lokal di HP Android.
 * Menggantikan seluruh data lokal dengan data resmi dari Supabase secara atomik via Room Transaction.
 * Mencegah error duplikasi / conflict constraint karena tidak ada lagi push/insert dari HP ke Cloud.
 */
class SyncRepository(
    private val context: Context,
    private val shortcutRepository: ShortcutRepository,
    private val authService: AuthService
) {
    private val database by lazy { AppDatabase.getInstance(context) }
    private val shortcutDao by lazy { database.shortcutDao() }
    private val client by lazy { SupabaseConfig.getClient(context) }

    /**
     * Mengunduh data resmi dari Supabase Cloud dan menimpa database Room lokal secara atomik.
     */
    suspend fun syncFromCloud(): Result<Unit> = runCatching {
        withContext(Dispatchers.IO) {
            val userId = client.auth.currentUserOrNull()?.id ?: authService.getCurrentUserId()

            // 1. Ambil data unik dari Supabase Cloud
            val cloudShortcuts = client.from("shortcuts")
                .select(columns = Columns.raw("trigger_code,expansion_text,category,expansion_mode,user_id")) {
                    if (userId != null) filter { eq("user_id", userId) }
                }
                .decodeList<SupabaseShortcutDto>()

            // 2. Timpa total database Room HP dengan data resmi Supabase
            database.withTransaction {
                shortcutDao.deleteAll()
                val entities = cloudShortcuts
                    .filter { !it.triggerCode.isNullOrBlank() && !it.expansionText.isNullOrBlank() }
                    .distinctBy { (it.triggerCode ?: "").trim().lowercase() }
                    .map { it.toEntity() }
                shortcutDao.insertAll(entities)
            }
        }
    }

    /**
     * Alur Sinkronisasi PULL ONLY untuk tombol "SINKRONKAN SEKARANG" dan UI.
     * Tidak lagi melakukan push/insert data lokal ke Cloud.
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
                .map { it.toEntity() }

            // 3. Timpa total database Room HP dengan data resmi Supabase (Atomic Room Transaction)
            database.withTransaction {
                shortcutDao.deleteAll()
                val uniqueShortcuts = downloadedEntities.distinctBy { it.triggerCode }
                shortcutDao.insertAll(uniqueShortcuts)
            }

            val pulledCount = downloadedEntities.size

            SyncResult(
                success = true,
                pushedCount = 0,
                pulledCount = pulledCount,
                message = "Sinkronisasi sukses: $pulledCount shortcut resmi berhasil diunduh dari Cloud."
            )
        } catch (e: Exception) {
            SyncResult(
                success = false,
                message = "Gagal sinkronisasi: ${e.localizedMessage ?: e.message}"
            )
        }
    }

    suspend fun pullOnly(): SyncResult = sync()
}
