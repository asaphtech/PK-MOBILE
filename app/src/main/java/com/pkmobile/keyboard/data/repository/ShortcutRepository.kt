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

    suspend fun insertShortcut(shortcut: String, expansion: String): Long {
        val entity = ShortcutEntity(
            shortcut = shortcut.trim().lowercase(),
            expansion = expansion.trim()
        )
        return shortcutDao.insertOrUpdate(entity)
    }

    suspend fun importShortcuts(pairs: List<Pair<String, String>>): Int {
        if (pairs.isEmpty()) return 0
        val entities = pairs.map { (key, value) ->
            ShortcutEntity(
                shortcut = key.trim().lowercase(),
                expansion = value.trim()
            )
        }
        shortcutDao.insertAll(entities)
        return entities.size
    }

    suspend fun deleteShortcut(shortcut: ShortcutEntity) {
        shortcutDao.delete(shortcut)
    }

    suspend fun deleteById(id: Long) {
        shortcutDao.deleteById(id)
    }
}
