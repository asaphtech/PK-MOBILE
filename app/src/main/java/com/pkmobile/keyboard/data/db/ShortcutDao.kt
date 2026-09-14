package com.pkmobile.keyboard.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ShortcutDao {

    @Query("SELECT * FROM shortcuts ORDER BY trigger_code ASC")
    fun getAllFlow(): Flow<List<ShortcutEntity>>

    @Query("SELECT * FROM shortcuts ORDER BY trigger_code ASC")
    suspend fun getAllList(): List<ShortcutEntity>

    @Query("SELECT * FROM shortcuts WHERE LOWER(trigger_code) = LOWER(:key) LIMIT 1")
    suspend fun findByShortcut(key: String): ShortcutEntity?

    @Query("SELECT * FROM shortcuts WHERE LOWER(trigger_code) = LOWER(:key) AND is_active = 1 LIMIT 1")
    suspend fun findActiveByShortcut(key: String): ShortcutEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(shortcut: ShortcutEntity)

    @Update
    suspend fun update(shortcut: ShortcutEntity): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(shortcuts: List<ShortcutEntity>)

    @Query("SELECT * FROM shortcuts WHERE is_active = 1 ORDER BY trigger_code ASC")
    fun getActiveFlow(): Flow<List<ShortcutEntity>>

    @Query("SELECT DISTINCT package_name FROM shortcuts ORDER BY package_name ASC")
    fun getDistinctPackagesFlow(): Flow<List<String>>

    @Query("UPDATE shortcuts SET is_active = :isActive WHERE package_name = :packageName")
    suspend fun setPackageActive(packageName: String, isActive: Boolean): Int

    @Query("UPDATE shortcuts SET is_active = :isActive WHERE trigger_code = :triggerCode")
    suspend fun setShortcutActive(triggerCode: String, isActive: Boolean): Int

    @Query("DELETE FROM shortcuts WHERE package_name = :packageName")
    suspend fun deleteByPackage(packageName: String): Int

    @Delete
    suspend fun delete(shortcut: ShortcutEntity)

    @Query("DELETE FROM shortcuts WHERE trigger_code = :triggerCode")
    suspend fun deleteByTriggerCode(triggerCode: String): Int

    @Query("DELETE FROM shortcuts")
    suspend fun deleteAll()

    @Query("DELETE FROM shortcuts")
    suspend fun deleteAllShortcuts()

    @Query("DELETE FROM shortcuts")
    suspend fun clearAll()
}
