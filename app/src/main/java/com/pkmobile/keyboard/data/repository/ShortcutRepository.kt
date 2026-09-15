package com.pkmobile.keyboard.data.repository

import com.pkmobile.keyboard.data.db.ShortcutDao
import com.pkmobile.keyboard.data.db.ShortcutEntity
import kotlinx.coroutines.flow.Flow

class ShortcutRepository(private val shortcutDao: ShortcutDao) {

    val allShortcutsFlow: Flow<List<ShortcutEntity>> = shortcutDao.getAllFlow()
    val activeShortcutsFlow: Flow<List<ShortcutEntity>> = shortcutDao.getActiveFlow()
    val distinctPackagesFlow: Flow<List<String>> = shortcutDao.getDistinctPackagesFlow()

    suspend fun getAllList(): List<ShortcutEntity> {
        return shortcutDao.getAllList()
    }

    suspend fun findExpansion(shortcut: String): String? {
        return shortcutDao.findActiveByShortcut(shortcut.trim().lowercase())?.expansionText
    }

    suspend fun setPackageActive(packageName: String, isActive: Boolean): Int {
        return shortcutDao.setPackageActive(packageName, isActive)
    }

    suspend fun setShortcutActive(triggerCode: String, isActive: Boolean): Int {
        return shortcutDao.setShortcutActive(triggerCode, isActive)
    }

    suspend fun insertShortcut(
        shortcut: String,
        expansion: String,
        expansionMode: String = "INSTANT",
        packageName: String = "Manual",
        isActive: Boolean = true
    ) {
        val cleanKey = com.pkmobile.keyboard.data.importer.ShortcutImporter.cleanTrigger(shortcut).lowercase()
        val entity = ShortcutEntity(
            triggerCode = cleanKey,
            expansionText = expansion.trim(),
            category = packageName,
            expansionMode = expansionMode,
            packageName = packageName,
            isActive = isActive
        )
        shortcutDao.insertOrUpdate(entity)
    }

    suspend fun insertOrUpdate(entity: ShortcutEntity) {
        val cleanKey = com.pkmobile.keyboard.data.importer.ShortcutImporter.cleanTrigger(entity.triggerCode).lowercase()
        val cleanExp = entity.expansionText.trim()
        val cleanEntity = entity.copy(
            triggerCode = cleanKey,
            expansionText = cleanExp
        )
        shortcutDao.insertOrUpdate(cleanEntity)
    }

    suspend fun deleteAllShortcuts() {
        shortcutDao.deleteAll()
    }

    suspend fun update(oldTrigger: String, shortcut: ShortcutEntity): Int {
        val cleanKey = com.pkmobile.keyboard.data.importer.ShortcutImporter.cleanTrigger(shortcut.triggerCode).lowercase()
        val cleanExp = shortcut.expansionText.trim()
        if (oldTrigger.lowercase() != cleanKey) {
            shortcutDao.deleteByTriggerCode(oldTrigger)
        }
        val updated = shortcut.copy(
            triggerCode = cleanKey,
            expansionText = cleanExp,
            expansionMode = shortcut.expansionMode
        )
        shortcutDao.insertOrUpdate(updated)
        return 1
    }

    suspend fun update(shortcut: ShortcutEntity): Int {
        return update(shortcut.triggerCode, shortcut)
    }

    suspend fun insertAll(entities: List<ShortcutEntity>) {
        if (entities.isNotEmpty()) {
            val cleanEntities = entities.mapNotNull { entity ->
                val cleanKey = com.pkmobile.keyboard.data.importer.ShortcutImporter.cleanTrigger(entity.triggerCode).lowercase()
                val cleanExp = entity.expansionText.trim()
                if (cleanKey.isNotBlank() && cleanExp.isNotBlank()) {
                    ShortcutEntity(
                        triggerCode = cleanKey,
                        expansionText = cleanExp,
                        category = entity.category ?: "General",
                        expansionMode = entity.expansionMode,
                        packageName = entity.packageName,
                        isActive = entity.isActive
                    )
                } else null
            }.distinctBy { it.triggerCode }
            shortcutDao.insertAll(cleanEntities)
        }
    }

    suspend fun importShortcuts(pairs: List<Pair<String, String>>, clearExisting: Boolean = false, defaultMode: String = "INSTANT"): Int {
        return importXmlShortcuts(pairs, fileName = "Imported", clearExisting = clearExisting, defaultMode = defaultMode)
    }

    suspend fun importXmlShortcuts(
        pairs: List<Pair<String, String>>,
        fileName: String,
        clearExisting: Boolean = false,
        defaultMode: String = "INSTANT"
    ): Int {
        if (clearExisting) {
            shortcutDao.deleteAll()
        }
        if (pairs.isEmpty()) return 0
        val pkgName = fileName.ifBlank { "Paket XML" }
        val entities = pairs.mapNotNull { (key, value) ->
            val cleanKey = com.pkmobile.keyboard.data.importer.ShortcutImporter.cleanTrigger(key).lowercase()
            val cleanExp = value.trim()
            if (cleanKey.isNotBlank() && cleanExp.isNotBlank()) {
                ShortcutEntity(
                    triggerCode = cleanKey,
                    expansionText = cleanExp,
                    category = pkgName,
                    expansionMode = defaultMode,
                    packageName = pkgName,
                    isActive = true
                )
            } else null
        }.distinctBy { it.triggerCode }
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
            val cleanKey = com.pkmobile.keyboard.data.importer.ShortcutImporter.cleanTrigger(item.triggerCode).lowercase()
            val cleanExp = com.pkmobile.keyboard.data.importer.ShortcutImporter.cleanTextFormatting(item.expansionText)
            if (cleanKey != item.triggerCode || cleanExp != item.expansionText) {
                shortcutDao.delete(item)
                if (cleanKey.isNotBlank() && cleanExp.isNotBlank()) {
                    shortcutDao.insertOrUpdate(
                        item.copy(triggerCode = cleanKey, expansionText = cleanExp)
                    )
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

    suspend fun deleteByTriggerCode(triggerCode: String) {
        shortcutDao.deleteByTriggerCode(triggerCode)
    }
}
