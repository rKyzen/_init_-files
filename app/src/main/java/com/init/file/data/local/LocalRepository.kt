package com.init.file.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.init.file.domain.model.FileCategory
import com.init.file.domain.model.FileItem
import com.init.file.domain.model.SortConfig
import com.init.file.domain.model.SortField
import com.init.file.domain.model.SortOrder
import com.init.file.domain.model.TrashItem
import com.init.file.domain.model.ViewMode
import com.init.file.domain.model.categorizeFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Repository interface and implementation providing reactive access to local tables.
 */
class LocalRepository(private val dbHelper: InitDatabaseHelper) {

    private val _pinnedFoldersFlow = MutableStateFlow<List<FileItem>>(emptyList())
    val pinnedFoldersFlow: Flow<List<FileItem>> = _pinnedFoldersFlow.asStateFlow()

    private val _recentFilesFlow = MutableStateFlow<List<FileItem>>(emptyList())
    val recentFilesFlow: Flow<List<FileItem>> = _recentFilesFlow.asStateFlow()

    private val _trashItemsFlow = MutableStateFlow<List<TrashItem>>(emptyList())
    val trashItemsFlow: Flow<List<TrashItem>> = _trashItemsFlow.asStateFlow()

    private val _themeModeFlow = MutableStateFlow("DARK")
    val themeModeFlow: Flow<String> = _themeModeFlow.asStateFlow()

    private val _viewModeFlow = MutableStateFlow(ViewMode.LIST)
    val viewModeFlow: Flow<ViewMode> = _viewModeFlow.asStateFlow()

    private val _sortConfigFlow = MutableStateFlow(SortConfig())
    val sortConfigFlow: Flow<SortConfig> = _sortConfigFlow.asStateFlow()

    private val _showHiddenFlow = MutableStateFlow(false)
    val showHiddenFlow: Flow<Boolean> = _showHiddenFlow.asStateFlow()

    suspend fun initialize() = withContext(Dispatchers.IO) {
        refreshPinnedFolders()
        refreshRecentFiles()
        refreshTrashItems()
        refreshPreferences()
    }

    // --- Pinned Folders ---

    suspend fun refreshPinnedFolders() = withContext(Dispatchers.IO) {
        val list = mutableListOf<FileItem>()
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            InitDatabaseHelper.TABLE_PINNED,
            arrayOf("path", "name", "pinned_at"),
            null, null, null, null,
            "pinned_at DESC"
        )
        cursor.use {
            val pathIndex = it.getColumnIndexOrThrow("path")
            val nameIndex = it.getColumnIndexOrThrow("name")
            while (it.moveToNext()) {
                val path = it.getString(pathIndex)
                val name = it.getString(nameIndex)
                val file = File(path)
                list.add(
                    FileItem(
                        id = path,
                        name = name,
                        path = path,
                        sizeBytes = if (file.exists()) file.length() else 0L,
                        lastModified = if (file.exists()) file.lastModified() else 0L,
                        isDirectory = true,
                        isFavorite = true
                    )
                )
            }
        }
        _pinnedFoldersFlow.value = list
    }

    suspend fun pinFolder(path: String, name: String) = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("path", path)
            put("name", name)
            put("pinned_at", System.currentTimeMillis())
        }
        db.insertWithOnConflict(
            InitDatabaseHelper.TABLE_PINNED,
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE
        )
        refreshPinnedFolders()
    }

    suspend fun unpinFolder(path: String) = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        db.delete(InitDatabaseHelper.TABLE_PINNED, "path = ?", arrayOf(path))
        refreshPinnedFolders()
    }

    suspend fun isFolderPinned(path: String): Boolean = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            InitDatabaseHelper.TABLE_PINNED,
            arrayOf("path"),
            "path = ?",
            arrayOf(path),
            null, null, null
        )
        cursor.use { it.count > 0 }
    }

    suspend fun isPinned(path: String): Boolean = isFolderPinned(path)

    // --- Recent Files ---

    suspend fun refreshRecentFiles() = withContext(Dispatchers.IO) {
        val list = mutableListOf<FileItem>()
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            InitDatabaseHelper.TABLE_RECENTS,
            arrayOf("path", "name", "size", "last_opened", "mime_type"),
            null, null, null, null,
            "last_opened DESC",
            "30"
        )
        cursor.use {
            val pathIndex = it.getColumnIndexOrThrow("path")
            val nameIndex = it.getColumnIndexOrThrow("name")
            val sizeIndex = it.getColumnIndexOrThrow("size")
            val lastOpenedIndex = it.getColumnIndexOrThrow("last_opened")
            val mimeIndex = it.getColumnIndexOrThrow("mime_type")

            while (it.moveToNext()) {
                val path = it.getString(pathIndex)
                val name = it.getString(nameIndex)
                val size = it.getLong(sizeIndex)
                val lastOpened = it.getLong(lastOpenedIndex)
                val mime = it.getString(mimeIndex)
                val file = File(path)
                if (file.exists()) {
                    val isDir = file.isDirectory
                    val ext = if (isDir) "" else name.substringAfterLast('.', "").lowercase()
                    list.add(
                        FileItem(
                            id = path,
                            name = name,
                            path = path,
                            sizeBytes = if (isDir) 0L else size,
                            lastModified = lastOpened,
                            isDirectory = isDir,
                            mimeType = mime,
                            extension = ext,
                            category = categorizeFile(name, isDir)
                        )
                    )
                }
            }
        }
        _recentFilesFlow.value = list
    }

    suspend fun recordFileOpened(item: FileItem) = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("path", item.path)
            put("name", item.name)
            put("size", item.sizeBytes)
            put("last_opened", System.currentTimeMillis())
            put("mime_type", item.mimeType)
        }
        db.insertWithOnConflict(
            InitDatabaseHelper.TABLE_RECENTS,
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE
        )
        refreshRecentFiles()
    }

    suspend fun recordFileAccess(item: FileItem) = recordFileOpened(item)

    suspend fun clearRecentFiles() = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        db.delete(InitDatabaseHelper.TABLE_RECENTS, null, null)
        _recentFilesFlow.value = emptyList()
    }

    companion object {
        const val TRASH_RETENTION_DAYS = 30
    }

    suspend fun purgeExpiredTrashItems(olderThanDays: Int = TRASH_RETENTION_DAYS) = withContext(Dispatchers.IO) {
        val cutoffMs = System.currentTimeMillis() - (olderThanDays.toLong() * 24L * 60L * 60L * 1000L)
        val db = dbHelper.writableDatabase
        val cursor = db.query(
            InitDatabaseHelper.TABLE_TRASH,
            arrayOf("id", "trash_path"),
            "deleted_at < ?",
            arrayOf(cutoffMs.toString()),
            null, null, null
        )
        val expiredIds = mutableListOf<Long>()
        cursor.use {
            val idIdx = it.getColumnIndexOrThrow("id")
            val pathIdx = it.getColumnIndexOrThrow("trash_path")
            while (it.moveToNext()) {
                val id = it.getLong(idIdx)
                val trashPath = it.getString(pathIdx)
                try {
                    val file = File(trashPath)
                    if (file.exists()) {
                        file.deleteRecursively()
                    }
                } catch (_: Exception) {}
                expiredIds.add(id)
            }
        }
        for (id in expiredIds) {
            db.delete(InitDatabaseHelper.TABLE_TRASH, "id = ?", arrayOf(id.toString()))
        }
    }

    suspend fun refreshTrashItems() = withContext(Dispatchers.IO) {
        purgeExpiredTrashItems()
        val list = mutableListOf<TrashItem>()
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            InitDatabaseHelper.TABLE_TRASH,
            arrayOf("id", "original_path", "trash_path", "name", "size", "deleted_at", "is_directory"),
            null, null, null, null,
            "deleted_at DESC"
        )
        cursor.use {
            val idIndex = it.getColumnIndexOrThrow("id")
            val origIndex = it.getColumnIndexOrThrow("original_path")
            val trashIndex = it.getColumnIndexOrThrow("trash_path")
            val nameIndex = it.getColumnIndexOrThrow("name")
            val sizeIndex = it.getColumnIndexOrThrow("size")
            val delIndex = it.getColumnIndexOrThrow("deleted_at")
            val isDirIndex = it.getColumnIndexOrThrow("is_directory")

            while (it.moveToNext()) {
                val id = it.getLong(idIndex)
                val originalPath = it.getString(origIndex)
                val trashPath = it.getString(trashIndex)
                val name = it.getString(nameIndex)
                val size = it.getLong(sizeIndex)
                val deletedAt = it.getLong(delIndex)
                val isDir = it.getInt(isDirIndex) == 1

                list.add(
                    TrashItem(
                        id = id,
                        originalPath = originalPath,
                        trashPath = trashPath,
                        name = name,
                        sizeBytes = size,
                        deletedAt = deletedAt,
                        isDirectory = isDir
                    )
                )
            }
        }
        _trashItemsFlow.value = list
    }

    suspend fun recordTrashItem(item: TrashItem) = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("original_path", item.originalPath)
            put("trash_path", item.trashPath)
            put("name", item.name)
            put("size", item.sizeBytes)
            put("deleted_at", item.deletedAt)
            put("is_directory", if (item.isDirectory) 1 else 0)
        }
        db.insertWithOnConflict(
            InitDatabaseHelper.TABLE_TRASH,
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE
        )
        refreshTrashItems()
    }

    suspend fun removeTrashRecord(id: Long) = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        db.delete(InitDatabaseHelper.TABLE_TRASH, "id = ?", arrayOf(id.toString()))
        refreshTrashItems()
    }

    suspend fun clearAllTrashRecords() = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        db.delete(InitDatabaseHelper.TABLE_TRASH, null, null)
        _trashItemsFlow.value = emptyList()
    }

    // --- Search History ---

    suspend fun getSearchHistory(): List<String> = withContext(Dispatchers.IO) {
        val list = mutableListOf<String>()
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            InitDatabaseHelper.TABLE_SEARCH,
            arrayOf("query"),
            null, null, null, null,
            "searched_at DESC",
            "10"
        )
        cursor.use {
            val queryIndex = it.getColumnIndexOrThrow("query")
            while (it.moveToNext()) {
                list.add(it.getString(queryIndex))
            }
        }
        list
    }

    suspend fun saveSearchQuery(query: String) = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("query", query.trim())
            put("searched_at", System.currentTimeMillis())
        }
        db.insertWithOnConflict(
            InitDatabaseHelper.TABLE_SEARCH,
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    suspend fun clearSearchHistory() = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        db.delete(InitDatabaseHelper.TABLE_SEARCH, null, null)
    }

    // --- Preferences ---

    suspend fun refreshPreferences() = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            InitDatabaseHelper.TABLE_PREFERENCES,
            arrayOf("key", "value"),
            null, null, null, null, null
        )
        var theme = "DARK"
        var viewMode = ViewMode.LIST
        var sortField = SortField.NAME
        var sortOrder = SortOrder.ASCENDING
        var showHidden = false

        cursor.use {
            val keyIndex = it.getColumnIndexOrThrow("key")
            val valIndex = it.getColumnIndexOrThrow("value")
            while (it.moveToNext()) {
                val key = it.getString(keyIndex)
                val value = it.getString(valIndex)
                when (key) {
                    "theme_mode" -> theme = value
                    "view_mode" -> viewMode = try { ViewMode.valueOf(value) } catch (_: Exception) { ViewMode.LIST }
                    "sort_field" -> sortField = try { SortField.valueOf(value) } catch (_: Exception) { SortField.NAME }
                    "sort_order" -> sortOrder = try { SortOrder.valueOf(value) } catch (_: Exception) { SortOrder.ASCENDING }
                    "show_hidden" -> showHidden = value.toBoolean()
                }
            }
        }

        _themeModeFlow.value = theme
        _viewModeFlow.value = viewMode
        _sortConfigFlow.value = SortConfig(sortField, sortOrder)
        _showHiddenFlow.value = showHidden
    }

    suspend fun setPreference(key: String, value: String) = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("key", key)
            put("value", value)
        }
        db.insertWithOnConflict(
            InitDatabaseHelper.TABLE_PREFERENCES,
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE
        )
        refreshPreferences()
    }

    suspend fun setThemeMode(mode: String) = setPreference("theme_mode", mode)
    suspend fun setViewMode(mode: ViewMode) = setPreference("view_mode", mode.name)
    suspend fun setSortConfig(config: SortConfig) {
        setPreference("sort_field", config.field.name)
        setPreference("sort_order", config.order.name)
    }
    suspend fun setShowHidden(show: Boolean) = setPreference("show_hidden", show.toString())
}
