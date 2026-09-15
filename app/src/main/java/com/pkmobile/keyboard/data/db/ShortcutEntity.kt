package com.pkmobile.keyboard.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index

/**
 * Entity yang merepresentasikan data Auto-Text / Shortcut.
 * Primary Key berupa compound key (preset_id, trigger_code) agar setiap berkas/preset
 * dapat memiliki trigger tersendiri tanpa bentrok atau menimpa berkas lain.
 */
@Entity(
    tableName = "shortcuts",
    primaryKeys = ["preset_id", "trigger_code"],
    indices = [
        Index(value = ["preset_id"]),
        Index(value = ["trigger_code"])
    ]
)
data class ShortcutEntity(
    @ColumnInfo(name = "preset_id")
    val presetId: String = "default_preset",

    @ColumnInfo(name = "trigger_code")
    val triggerCode: String,

    @ColumnInfo(name = "expansion_text")
    val expansionText: String,

    @ColumnInfo(name = "category")
    val category: String? = "General",

    @ColumnInfo(name = "expansion_mode")
    val expansionMode: String = "SPACE",

    @ColumnInfo(name = "package_name")
    val packageName: String = "Paket Utama",

    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
) {
    // Properti alias untuk kompatibilitas penuh dengan kode UI, Adapter, dan Engine
    @delegate:Ignore
    val shortcut: String by lazy { triggerCode }

    @delegate:Ignore
    val expansion: String by lazy { expansionText }

    // Secondary constructor untuk fleksibilitas pemanggilan kode lama
    @Ignore
    constructor(
        shortcut: String,
        expansion: String,
        expansionMode: String = "SPACE",
        packageName: String = "Paket Utama",
        isActive: Boolean = true,
        category: String? = "General",
        presetId: String = "default_preset"
    ) : this(
        presetId = presetId,
        triggerCode = shortcut,
        expansionText = expansion,
        category = category ?: "General",
        expansionMode = expansionMode,
        packageName = packageName,
        isActive = isActive
    )
}

