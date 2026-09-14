package com.pkmobile.keyboard.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

/**
 * Entity yang merepresentasikan data Auto-Text / Shortcut.
 * Primary Key menggunakan trigger_code (bukan ID autoincrement) agar tidak pernah terduplikasi.
 */
@Entity(tableName = "shortcuts")
data class ShortcutEntity(
    @PrimaryKey
    @ColumnInfo(name = "trigger_code")
    val triggerCode: String,

    @ColumnInfo(name = "expansion_text")
    val expansionText: String,

    @ColumnInfo(name = "category")
    val category: String? = "General",

    @ColumnInfo(name = "expansion_mode")
    val expansionMode: String = "INSTANT",

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
        expansionMode: String = "INSTANT",
        packageName: String = "Paket Utama",
        isActive: Boolean = true,
        category: String? = "General"
    ) : this(
        triggerCode = shortcut,
        expansionText = expansion,
        category = category ?: "General",
        expansionMode = expansionMode,
        packageName = packageName,
        isActive = isActive
    )
}
