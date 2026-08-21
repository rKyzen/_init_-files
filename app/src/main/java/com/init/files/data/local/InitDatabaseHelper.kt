package com.init.files.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * SQLite Open Helper managing local tables for _init_ /files.
 */
class InitDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "init_files.db"
        const val DATABASE_VERSION = 2

        const val TABLE_PINNED = "pinned_folders"
        const val TABLE_RECENTS = "recent_files"
        const val TABLE_SEARCH = "search_history"
        const val TABLE_PREFERENCES = "preferences"
        const val TABLE_TRASH = "trash_items"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_PINNED (
                path TEXT PRIMARY KEY,
                name TEXT NOT NULL,
                pinned_at INTEGER NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE $TABLE_RECENTS (
                path TEXT PRIMARY KEY,
                name TEXT NOT NULL,
                size INTEGER NOT NULL,
                last_opened INTEGER NOT NULL,
                mime_type TEXT
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE $TABLE_SEARCH (
                query TEXT PRIMARY KEY,
                searched_at INTEGER NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE $TABLE_PREFERENCES (
                key TEXT PRIMARY KEY,
                value TEXT NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE $TABLE_TRASH (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                original_path TEXT UNIQUE,
                trash_path TEXT NOT NULL,
                name TEXT NOT NULL,
                size INTEGER NOT NULL,
                deleted_at INTEGER NOT NULL,
                is_directory INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )

        // Default preferences
        db.execSQL("INSERT INTO $TABLE_PREFERENCES VALUES ('theme_mode', 'DARK')")
        db.execSQL("INSERT INTO $TABLE_PREFERENCES VALUES ('view_mode', 'LIST')")
        db.execSQL("INSERT INTO $TABLE_PREFERENCES VALUES ('sort_field', 'NAME')")
        db.execSQL("INSERT INTO $TABLE_PREFERENCES VALUES ('sort_order', 'ASCENDING')")
        db.execSQL("INSERT INTO $TABLE_PREFERENCES VALUES ('show_hidden', 'false')")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS $TABLE_TRASH (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    original_path TEXT UNIQUE,
                    trash_path TEXT NOT NULL,
                    name TEXT NOT NULL,
                    size INTEGER NOT NULL,
                    deleted_at INTEGER NOT NULL,
                    is_directory INTEGER NOT NULL DEFAULT 0
                )
                """.trimIndent()
            )
        }
    }
}
