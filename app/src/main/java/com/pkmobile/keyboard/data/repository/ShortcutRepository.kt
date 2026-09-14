package com.pkmobile.keyboard.data.repository

import com.pkmobile.keyboard.data.db.ShortcutDao
import com.pkmobile.keyboard.data.db.ShortcutEntity
import kotlinx.coroutines.flow.Flow

class ShortcutRepository(private val shortcutDao: ShortcutDao) {

    val allShortcutsFlow: Flow<List<ShortcutEntity>> = shortcutDao.getAllFlow()

    suspend fun getAllList(): List<ShortcutEntity> {
        return shortcutDao.getAllList()
    }

    suspend fun findExpansion(shortcut: String): String? {
        return shortcutDao.findByShortcut(shortcut.trim().lowercase())?.expansion
    }

    suspend fun insertShortcut(shortcut: String, expansion: String, expansionMode: String = "INSTANT"): Long {
        val cleanKey = com.pkmobile.keyboard.data.importer.ShortcutImporter.cleanTrigger(shortcut).lowercase()
        val entity = ShortcutEntity(
            shortcut = cleanKey,
            expansion = expansion.trim(),
            expansionMode = expansionMode
        )
        return shortcutDao.insertOrUpdate(entity)
    }

    suspend fun insertOrUpdate(entity: ShortcutEntity): Long {
        val cleanKey = com.pkmobile.keyboard.data.importer.ShortcutImporter.cleanTrigger(entity.shortcut).lowercase()
        val cleanExp = entity.expansion.trim()
        val cleanEntity = entity.copy(shortcut = cleanKey, expansion = cleanExp)
        return shortcutDao.insertOrUpdate(cleanEntity)
    }

    suspend fun update(shortcut: ShortcutEntity): Int {
        val cleanKey = com.pkmobile.keyboard.data.importer.ShortcutImporter.cleanTrigger(shortcut.shortcut).lowercase()
        val cleanExp = shortcut.expansion.trim()

        // Jika trigger baru sudah dipakai oleh baris lain, hapus agar tidak melanggar indeks unik
        val existing = shortcutDao.findByShortcut(cleanKey)
        if (existing != null && existing.id != shortcut.id) {
            shortcutDao.delete(existing)
        }

        val updated = shortcut.copy(
            shortcut = cleanKey,
            expansion = cleanExp,
            expansionMode = shortcut.expansionMode
        )
        val rows = shortcutDao.update(updated)
        if (rows == 0) {
            shortcutDao.insertOrUpdate(updated)
        }
        return 1
    }

    suspend fun insertAll(entities: List<ShortcutEntity>) {
        if (entities.isNotEmpty()) {
            val cleanEntities = entities.mapNotNull { entity ->
                val cleanKey = com.pkmobile.keyboard.data.importer.ShortcutImporter.cleanTrigger(entity.shortcut).lowercase()
                val cleanExp = entity.expansion.trim()
                if (cleanKey.isNotBlank() && cleanExp.isNotBlank()) {
                    ShortcutEntity(
                        shortcut = cleanKey,
                        expansion = cleanExp,
                        expansionMode = entity.expansionMode
                    )
                } else null
            }
            shortcutDao.insertAll(cleanEntities)
        }
    }

    suspend fun importShortcuts(pairs: List<Pair<String, String>>, clearExisting: Boolean = false, defaultMode: String = "INSTANT"): Int {
        if (clearExisting) {
            shortcutDao.deleteAll()
        }
        if (pairs.isEmpty()) return 0
        val entities = pairs.mapNotNull { (key, value) ->
            val cleanKey = com.pkmobile.keyboard.data.importer.ShortcutImporter.cleanTrigger(key).lowercase()
            val cleanExp = value.trim()
            if (cleanKey.isNotBlank() && cleanExp.isNotBlank()) {
                ShortcutEntity(
                    shortcut = cleanKey,
                    expansion = cleanExp,
                    expansionMode = defaultMode
                )
            } else null
        }
        shortcutDao.insertAll(entities)
        return entities.size
    }

    /**
     * Memeriksa dan membersihkan database otomatis dari data lama yang masih memuat tag XML seperti <tscut>.
     */
    suspend fun sanitizeExistingDatabase(): Int {
        val all = shortcutDao.getAllList()
        var updatedCount = 0
        for (item in all) {
            val cleanKey = com.pkmobile.keyboard.data.importer.ShortcutImporter.cleanTrigger(item.shortcut).lowercase()
            val cleanExp = item.expansion.trim()
            if (cleanKey != item.shortcut) {
                shortcutDao.delete(item)
                if (cleanKey.isNotBlank()) {
                    shortcutDao.insertOrUpdate(ShortcutEntity(shortcut = cleanKey, expansion = cleanExp, expansionMode = item.expansionMode))
                    updatedCount++
                }
            }
        }
        return updatedCount
    }

    suspend fun deleteAll() {
        shortcutDao.deleteAll()
    }

    suspend fun deleteShortcut(shortcut: ShortcutEntity) {
        shortcutDao.delete(shortcut)
    }

    suspend fun deleteById(id: Long) {
        shortcutDao.deleteById(id)
    }
}
