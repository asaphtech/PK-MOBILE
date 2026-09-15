package com.pkmobile.keyboard.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PresetDao {

    @Query("SELECT * FROM presets ORDER BY created_at ASC")
    fun getAllPresetsFlow(): Flow<List<PresetEntity>>

    @Query("SELECT * FROM presets ORDER BY created_at ASC")
    suspend fun getAllPresets(): List<PresetEntity>

    @Query("SELECT * FROM presets WHERE is_active = 1 LIMIT 1")
    fun getActivePresetFlow(): Flow<PresetEntity?>

    @Query("SELECT * FROM presets WHERE is_active = 1 LIMIT 1")
    suspend fun getActivePreset(): PresetEntity?

    @Query("SELECT * FROM presets WHERE id = :id LIMIT 1")
    suspend fun getPresetById(id: String): PresetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(preset: PresetEntity)

    @Update
    suspend fun update(preset: PresetEntity): Int

    @Query("UPDATE presets SET is_active = CASE WHEN id = :activeId THEN 1 ELSE 0 END")
    suspend fun setActivePreset(activeId: String)

    @Query("UPDATE presets SET shortcut_count = :count WHERE id = :id")
    suspend fun updateShortcutCount(id: String, count: Int)

    @Query("DELETE FROM presets WHERE id = :id")
    suspend fun deleteById(id: String): Int

    @Query("DELETE FROM presets")
    suspend fun deleteAll()
}
