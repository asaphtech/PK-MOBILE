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
                        ensureDefaultPreset(database.presetDao())
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
                            ensureDefaultPreset(database.presetDao())
                        } else if (allPresets.none { it.isActive }) {
                            database.presetDao().setActivePreset(allPresets.first().id)
                        }
                    }
                }
            }

            private suspend fun ensureDefaultPreset(presetDao: PresetDao) {
                val defaultPreset = PresetEntity(
                    id = "default_preset",
                    name = "Paket Utama",
                    sourceType = "LOCAL",
                    isActive = true,
                    shortcutCount = 0,
                    createdAt = System.currentTimeMillis()
                )
                presetDao.insertOrUpdate(defaultPreset)
            }
        }
    }
}

