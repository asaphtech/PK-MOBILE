package com.pkmobile.keyboard.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.room.migration.Migration

@Database(entities = [PresetEntity::class, ShortcutEntity::class], version = 5, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun shortcutDao(): ShortcutDao
    abstract fun presetDao(): PresetDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE shortcuts ADD COLUMN expansion_mode TEXT NOT NULL DEFAULT 'INSTANT'")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE shortcuts ADD COLUMN package_name TEXT NOT NULL DEFAULT 'Paket Utama'")
                db.execSQL("ALTER TABLE shortcuts ADD COLUMN is_active INTEGER NOT NULL DEFAULT 1")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "pk_keyboard_shortcuts.db"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .fallbackToDestructiveMigration()
                .addCallback(DatabaseCallback())
                .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        populateDefaultData(database.presetDao(), database.shortcutDao())
                    }
                }
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        // Pastikan selalu ada minimal 1 preset aktif jika database kosong
                        val allPresets = database.presetDao().getAllPresets()
                        if (allPresets.isEmpty()) {
                            populateDefaultData(database.presetDao(), database.shortcutDao())
                        } else if (allPresets.none { it.isActive }) {
                            database.presetDao().setActivePreset(allPresets.first().id)
                        }
                    }
                }
            }

            private suspend fun populateDefaultData(presetDao: PresetDao, shortcutDao: ShortcutDao) {
                val defaultPreset = PresetEntity(
                    id = "default_preset",
                    name = "Paket Utama (Bawaan)",
                    sourceType = "LOCAL",
                    isActive = true,
                    shortcutCount = 6,
                    createdAt = System.currentTimeMillis()
                )
                presetDao.insertOrUpdate(defaultPreset)

                val defaultShortcuts = listOf(
                    ShortcutEntity(shortcut = "omw", expansion = "On my way!", presetId = "default_preset"),
                    ShortcutEntity(shortcut = "brb", expansion = "Be right back", presetId = "default_preset"),
                    ShortcutEntity(shortcut = "thx", expansion = "Thank you so much!", presetId = "default_preset"),
                    ShortcutEntity(shortcut = "btw", expansion = "By the way", presetId = "default_preset"),
                    ShortcutEntity(shortcut = "otw", expansion = "On the way", presetId = "default_preset"),
                    ShortcutEntity(shortcut = "info", expansion = "Informasi lebih lanjut dapat menghubungi layanan kami.", presetId = "default_preset")
                )
                defaultShortcuts.forEach { shortcutDao.insertOrUpdate(it) }
            }
        }
    }
}

