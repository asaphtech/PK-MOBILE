package com.pkmobile.keyboard.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity yang merepresentasikan data Auto-Text / Shortcut.
 * Contoh: shortcut = "omw", expansion = "On my way!"
 */
@Entity(
    tableName = "shortcuts",
    indices = [Index(value = ["shortcut"], unique = true)]
)
data class ShortcutEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "shortcut")
    val shortcut: String,

    @ColumnInfo(name = "expansion")
    val expansion: String,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
