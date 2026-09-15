package com.pkmobile.keyboard.data.repository

import com.pkmobile.keyboard.data.db.PresetDao
import com.pkmobile.keyboard.data.db.PresetEntity
import com.pkmobile.keyboard.data.db.ShortcutDao
import com.pkmobile.keyboard.data.db.ShortcutEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

class ShortcutRepository(
    private val shortcutDao: ShortcutDao,
    private val presetDao: PresetDao? = null
) {

    val allShortcutsFlow: Flow<List<ShortcutEntity>> = shortcutDao.getAllFlow()
    val activeShortcutsFlow: Flow<List<ShortcutEntity>> = shortcutDao.getActiveFlow()
    val shortcutsByActivePresetFlow: Flow<List<ShortcutEntity>> = shortcutDao.getShortcutsByActivePresetFlow()
    val distinctPackagesFlow: Flow<List<String>> = shortcutDao.getDistinctPackagesFlow()

    val allPresetsFlow: Flow<List<PresetEntity>> = presetDao?.getAllPresetsFlow() ?: emptyFlow()
    val activePresetFlow: Flow<PresetEntity?> = presetDao?.getActivePresetFlow() ?: emptyFlow()

    suspend fun getAllList(): List<ShortcutEntity> {
        return shortcutDao.getAllList()
    }

    suspend fun getActivePreset(): PresetEntity? {
        return presetDao?.getActivePreset()
    }

    suspend fun setActivePreset(presetId: String) {
        presetDao?.setActivePreset(presetId)
    }

    suspend fun deletePreset(presetId: String) {
        shortcutDao.deleteByPreset(presetId)
        presetDao?.deleteById(presetId)

        val remaining = presetDao?.getAllPresets() ?: emptyList()
        if (remaining.isNotEmpty() && remaining.none { it.isActive }) {
            presetDao?.setActivePreset(remaining.first().id)
        }
    }

    suspend fun getShortcutsByPreset(presetId: String): List<ShortcutEntity> {
        return shortcutDao.getShortcutsByPresetList(presetId)
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

    suspend fun setShortcutActiveInPreset(presetId: String, triggerCode: String, isActive: Boolean): Int {
        return shortcutDao.setShortcutActiveInPreset(presetId, triggerCode, isActive)
    }

    suspend fun insertShortcut(
        shortcut: String,
        expansion: String,
        expansionMode: String = "INSTANT",
        packageName: String = "Manual",
        isActive: Boolean = true,
        targetPresetId: String? = null
    ) {
        val activePreset = targetPresetId ?: presetDao?.getActivePreset()?.id ?: "default_preset"
        val cleanKey = com.pkmobile.keyboard.data.importer.ShortcutImporter.cleanTrigger(shortcut).lowercase()
        val entity = ShortcutEntity(
            presetId = activePreset,
            triggerCode = cleanKey,
            expansionText = expansion.trim(),
            category = packageName,
            expansionMode = expansionMode,
            packageName = packageName,
            isActive = isActive
        )
        shortcutDao.insertOrUpdate(entity)
        presetDao?.updateShortcutCount(activePreset, shortcutDao.countByPreset(activePreset))
    }

    suspend fun insertOrUpdate(entity: ShortcutEntity) {
        val cleanKey = com.pkmobile.keyboard.data.importer.ShortcutImporter.cleanTrigger(entity.triggerCode).lowercase()
        val cleanExp = entity.expansionText.trim()
        val cleanEntity = entity.copy(
            triggerCode = cleanKey,
            expansionText = cleanExp
        )
        shortcutDao.insertOrUpdate(cleanEntity)
        presetDao?.updateShortcutCount(cleanEntity.presetId, shortcutDao.countByPreset(cleanEntity.presetId))
    }

    suspend fun deleteAllShortcuts() {
        shortcutDao.deleteAll()
        val allPresets = presetDao?.getAllPresets() ?: emptyList()
        for (p in allPresets) {
            presetDao?.updateShortcutCount(p.id, 0)
        }
    }

    suspend fun update(oldTrigger: String, shortcut: ShortcutEntity): Int {
        val cleanKey = com.pkmobile.keyboard.data.importer.ShortcutImporter.cleanTrigger(shortcut.triggerCode).lowercase()
        val cleanExp = shortcut.expansionText.trim()
        if (oldTrigger.lowercase() != cleanKey) {
            shortcutDao.deleteByPresetAndTrigger(shortcut.presetId, oldTrigger)
        }
        val updated = shortcut.copy(
            triggerCode = cleanKey,
            expansionText = cleanExp,
            expansionMode = shortcut.expansionMode
        )
        shortcutDao.insertOrUpdate(updated)
        presetDao?.updateShortcutCount(shortcut.presetId, shortcutDao.countByPreset(shortcut.presetId))
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
                        presetId = entity.presetId,
                        triggerCode = cleanKey,
                        expansionText = cleanExp,
                        category = entity.category ?: "General",
                        expansionMode = entity.expansionMode,
                        packageName = entity.packageName,
                        isActive = entity.isActive
                    )
                } else null
            }.distinctBy { Pair(it.presetId, it.triggerCode) }
            shortcutDao.insertAll(cleanEntities)
        }
    }

    suspend fun importShortcuts(pairs: List<Pair<String, String>>, clearExisting: Boolean = false, defaultMode: String = "INSTANT"): Int {
        return importXmlShortcuts(pairs, fileName = "Imported", clearExisting = clearExisting, defaultMode = defaultMode)
    }

    /**
     * Mengimpor berkas shortcut ke dalam entitas Preset baru di penyimpanan HP.
     * Tidak akan menimpa atau menumpuk data lama dari berkas lain.
     */
    suspend fun importXmlShortcuts(
        pairs: List<Pair<String, String>>,
        fileName: String,
        clearExisting: Boolean = false,
        defaultMode: String = "INSTANT"
    ): Int {
        if (pairs.isEmpty()) return 0

        val cleanName = fileName.removeSuffix(".xml").removeSuffix(".XML")
            .removeSuffix(".json").removeSuffix(".JSON")
            .removeSuffix(".4pk").removeSuffix(".4PK")
            .removeSuffix(".txt").removeSuffix(".TXT")
            .trim().ifBlank { "Paket Berkas" }

        val presetId = "preset_" + System.currentTimeMillis()

        if (clearExisting) {
            presetDao?.deleteAll()
            shortcutDao.deleteAll()
        }

        val entities = pairs.mapNotNull { (key, value) ->
            val cleanKey = com.pkmobile.keyboard.data.importer.ShortcutImporter.cleanTrigger(key).lowercase()
            val cleanExp = value.trim()
            if (cleanKey.isNotBlank() && cleanExp.isNotBlank()) {
                ShortcutEntity(
                    presetId = presetId,
                    triggerCode = cleanKey,
                    expansionText = cleanExp,
                    category = cleanName,
                    expansionMode = defaultMode,
                    packageName = cleanName,
                    isActive = true
                )
            } else null
        }.distinctBy { it.triggerCode }

        if (entities.isNotEmpty()) {
            val preset = PresetEntity(
                id = presetId,
                name = cleanName,
                sourceType = "FILE_XML",
                isActive = true,
                shortcutCount = entities.size,
                createdAt = System.currentTimeMillis()
            )
            presetDao?.insertOrUpdate(preset)
            presetDao?.setActivePreset(presetId)
            shortcutDao.insertAll(entities)
        }

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
        presetDao?.deleteAll()
    }

    suspend fun deleteShortcut(shortcut: ShortcutEntity) {
        shortcutDao.delete(shortcut)
        presetDao?.updateShortcutCount(shortcut.presetId, shortcutDao.countByPreset(shortcut.presetId))
    }

    suspend fun deleteByTriggerCode(triggerCode: String) {
        shortcutDao.deleteByTriggerCode(triggerCode)
    }
}

