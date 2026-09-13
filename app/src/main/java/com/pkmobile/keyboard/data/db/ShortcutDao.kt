package com.pkmobile.keyboard.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ShortcutDao {

    @Query("SELECT * FROM shortcuts ORDER BY shortcut ASC")
    fun getAllFlow(): Flow<List<ShortcutEntity>>

    @Query("SELECT * FROM shortcuts ORDER BY shortcut ASC")
    suspend fun getAllList(): List<ShortcutEntity>

    @Query("SELECT * FROM shortcuts WHERE LOWER(shortcut) = LOWER(:key) LIMIT 1")
    suspend fun findByShortcut(key: String): ShortcutEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(shortcut: ShortcutEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(shortcuts: List<ShortcutEntity>): List<Long>

    @Delete
    suspend fun delete(shortcut: ShortcutEntity)

    @Query("DELETE FROM shortcuts WHERE id = :id")
    suspend fun deleteById(id: Long)
}
