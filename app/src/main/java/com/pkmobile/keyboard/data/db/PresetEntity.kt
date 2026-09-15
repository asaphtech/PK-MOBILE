package com.pkmobile.keyboard.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entitas yang merepresentasikan berkas / paket preset shortcut.
 * Memungkinkan pemisahan isolasi data antara file XML lokal, sinkronisasi Cloud Supabase,
 * dan input manual sehingga tidak saling menimpa atau menumpuk secara berantakan.
 */
@Entity(tableName = "presets")
data class PresetEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String, // Contoh: "preset_cloud", "default_preset", atau "preset_<timestamp>"

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "source_type")
    val sourceType: String = "LOCAL", // "CLOUD", "FILE_XML", "FILE_JSON", "MANUAL"

    @ColumnInfo(name = "is_active")
    val isActive: Boolean = false,

    @ColumnInfo(name = "shortcut_count")
    val shortcutCount: Int = 0,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
