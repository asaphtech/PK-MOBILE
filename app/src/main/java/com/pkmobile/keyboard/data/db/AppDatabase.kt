package com.pkmobile.keyboard.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [ShortcutEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun shortcutDao(): ShortcutDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "pk_keyboard_shortcuts.db"
                )
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
                        populateDefaultShortcuts(database.shortcutDao())
                    }
                }
            }

            private suspend fun populateDefaultShortcuts(dao: ShortcutDao) {
                val defaultShortcuts = listOf(
                    ShortcutEntity(shortcut = "omw", expansion = "On my way!"),
                    ShortcutEntity(shortcut = "brb", expansion = "Be right back"),
                    ShortcutEntity(shortcut = "thx", expansion = "Thank you so much!"),
                    ShortcutEntity(shortcut = "btw", expansion = "By the way"),
                    ShortcutEntity(shortcut = "otw", expansion = "On the way"),
                    ShortcutEntity(shortcut = "info", expansion = "Informasi lebih lanjut dapat menghubungi layanan kami.")
                )
                defaultShortcuts.forEach { dao.insertOrUpdate(it) }
            }
        }
    }
}
