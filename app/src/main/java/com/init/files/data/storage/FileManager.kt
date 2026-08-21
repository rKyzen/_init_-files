package com.init.files.data.storage

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import com.init.files.domain.model.FileCategory
import com.init.files.domain.model.FileItem
import com.init.files.domain.model.FileOperation
import com.init.files.domain.model.OperationProgress
import com.init.files.domain.model.SortConfig
import com.init.files.domain.model.SortField
import com.init.files.domain.model.SortOrder
import com.init.files.domain.model.StorageBreakdown
import com.init.files.domain.model.StorageVolumeInfo
import com.init.files.domain.model.TrashItem
import com.init.files.domain.model.categorizeFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class ApkMetadata(
    val appName: String,
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val minSdk: Int,
    val targetSdk: Int,
    val icon: Drawable? = null
)

class FileManager(private val context: Context) {

    /**
     * Enumerates internal storage, SD cards, and USB OTG drives.
     */
    suspend fun getStorageVolumes(): List<StorageVolumeInfo> = withContext(Dispatchers.IO) {
        val list = mutableListOf<StorageVolumeInfo>()
        val sm = context.getSystemService(Context.STORAGE_SERVICE) as? StorageManager

        if (sm != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val volumes = sm.storageVolumes
            for (v in volumes) {
                val isPrimary = v.isPrimary
                val isRemovable = v.isRemovable
                val name = when {
                    isPrimary -> "Internal Storage"
                    isRemovable -> "SD Card / USB"
                    else -> v.getDescription(context) ?: "Storage"
                }

                val dir = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    v.directory
                } else {
                    Environment.getExternalStorageDirectory()
                }

                val path = dir?.absolutePath ?: Environment.getExternalStorageDirectory().absolutePath
                val total = dir?.totalSpace ?: 0L
                val free = dir?.freeSpace ?: 0L

                list.add(
                    StorageVolumeInfo(
                        id = path,
                        name = name,
                        path = path,
                        totalBytes = total,
                        freeBytes = free,
                        isPrimary = isPrimary,
                        isRemovable = isRemovable,
                        isMounted = v.state == Environment.MEDIA_MOUNTED
                    )
                )
            }
        }

        if (list.isEmpty()) {
            val internalDir = Environment.getExternalStorageDirectory()
            list.add(
                StorageVolumeInfo(
                    id = internalDir.absolutePath,
                    name = "Internal Storage",
                    path = internalDir.absolutePath,
                    totalBytes = internalDir.totalSpace,
                    freeBytes = internalDir.freeSpace,
                    isPrimary = true,
                    isRemovable = false,
                    isMounted = true
                )
            )
        }

        list
    }

    /**
     * Lists directory files and folders with sorting and hidden filter.
     */
    suspend fun listFiles(
        path: String,
        sortConfig: SortConfig = SortConfig(),
        showHidden: Boolean = false
    ): List<FileItem> = withContext(Dispatchers.IO) {
        val dir = File(path)
        if (!dir.exists() || !dir.isDirectory) {
            return@withContext emptyList()
        }

        val rawFiles = dir.listFiles() ?: return@withContext emptyList()

        val items = rawFiles
            .filter { showHidden || !it.name.startsWith(".") }
            .map { file ->
                val isDir = file.isDirectory || (!file.isFile && file.exists() && file.canRead() && file.list() != null)
                val name = file.name
                val ext = if (isDir) "" else name.substringAfterLast('.', "").lowercase()
                val mime = if (isDir) null else getMimeType(file)
                val children = if (isDir) file.list()?.size else null

                FileItem(
                    id = file.absolutePath,
                    name = name,
                    path = file.absolutePath,
                    sizeBytes = if (isDir) 0L else file.length(),
                    lastModified = file.lastModified(),
                    isDirectory = isDir,
                    mimeType = mime,
                    extension = ext,
                    isHidden = name.startsWith("."),
                    childrenCount = children,
                    category = categorizeFile(name, isDir)
                )
            }

        sortFileList(items, sortConfig)
    }

    /**
     * Fast category indexing querying MediaStore for media, docs, downloads, and APKs.
     */
    suspend fun getCategoryFiles(
        category: FileCategory,
        sortConfig: SortConfig = SortConfig()
    ): List<FileItem> = withContext(Dispatchers.IO) {
        val results = mutableListOf<FileItem>()
        val resolver = context.contentResolver

        when (category) {
            FileCategory.IMAGES -> queryMediaStore(
                resolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                category,
                results
            )
            FileCategory.VIDEOS -> queryMediaStore(
                resolver,
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                category,
                results
            )
            FileCategory.AUDIO -> queryMediaStore(
                resolver,
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                category,
                results
            )
            FileCategory.DOWNLOADS -> {
                val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (downloadDir.exists()) {
                    val files = listFiles(downloadDir.absolutePath, sortConfig, true)
                    results.addAll(files)
                }
            }
            FileCategory.DOCUMENTS, FileCategory.APKS, FileCategory.ARCHIVES -> {
                // Query MediaStore Files table by extension or MIME type
                val uri = MediaStore.Files.getContentUri("external")
                val projection = arrayOf(
                    MediaStore.Files.FileColumns._ID,
                    MediaStore.Files.FileColumns.DISPLAY_NAME,
                    MediaStore.Files.FileColumns.DATA,
                    MediaStore.Files.FileColumns.SIZE,
                    MediaStore.Files.FileColumns.DATE_MODIFIED,
                    MediaStore.Files.FileColumns.MIME_TYPE
                )

                val selectionParts = category.extensions.map { "LOWER(${MediaStore.Files.FileColumns.DATA}) LIKE '%.${it}'" }
                val selection = selectionParts.joinToString(" OR ")

                try {
                    val cursor = resolver.query(
                        uri,
                        projection,
                        if (selection.isNotEmpty()) selection else null,
                        null,
                        "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC"
                    )
                    cursor?.use {
                        val nameCol = it.getColumnIndex(MediaStore.Files.FileColumns.DISPLAY_NAME)
                        val dataCol = it.getColumnIndex(MediaStore.Files.FileColumns.DATA)
                        val sizeCol = it.getColumnIndex(MediaStore.Files.FileColumns.SIZE)
                        val dateCol = it.getColumnIndex(MediaStore.Files.FileColumns.DATE_MODIFIED)
                        val mimeCol = it.getColumnIndex(MediaStore.Files.FileColumns.MIME_TYPE)

                        while (it.moveToNext()) {
                            val data = if (dataCol >= 0) it.getString(dataCol) else null
                            val name = if (nameCol >= 0) it.getString(nameCol) ?: data?.substringAfterLast('/') ?: "file" else "file"
                            val size = if (sizeCol >= 0) it.getLong(sizeCol) else 0L
                            val date = if (dateCol >= 0) it.getLong(dateCol) * 1000L else 0L
                            val mime = if (mimeCol >= 0) it.getString(mimeCol) else null

                            if (data != null && File(data).exists()) {
                                results.add(
                                    FileItem(
                                        id = data,
                                        name = name,
                                        path = data,
                                        sizeBytes = size,
                                        lastModified = date,
                                        isDirectory = false,
                                        mimeType = mime,
                                        extension = name.substringAfterLast('.', "").lowercase(),
                                        category = category
                                    )
                                )
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            FileCategory.ALL, FileCategory.JUNK -> {
                // Fallback to top volume
            }
        }

        sortFileList(results, sortConfig)
    }

    private fun queryMediaStore(
        resolver: ContentResolver,
        uri: Uri,
        category: FileCategory,
        results: MutableList<FileItem>
    ) {
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.DATA,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.DATE_MODIFIED,
            MediaStore.MediaColumns.MIME_TYPE
        )

        try {
            val cursor = resolver.query(
                uri,
                projection,
                null,
                null,
                "${MediaStore.MediaColumns.DATE_MODIFIED} DESC"
            )

            cursor?.use {
                val nameCol = it.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                val dataCol = it.getColumnIndex(MediaStore.MediaColumns.DATA)
                val sizeCol = it.getColumnIndex(MediaStore.MediaColumns.SIZE)
                val dateCol = it.getColumnIndex(MediaStore.MediaColumns.DATE_MODIFIED)
                val mimeCol = it.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE)

                while (it.moveToNext()) {
                    val data = if (dataCol >= 0) it.getString(dataCol) else null
                    val name = if (nameCol >= 0) it.getString(nameCol) ?: data?.substringAfterLast('/') ?: "file" else "file"
                    val size = if (sizeCol >= 0) it.getLong(sizeCol) else 0L
                    val date = if (dateCol >= 0) it.getLong(dateCol) * 1000L else 0L
                    val mime = if (mimeCol >= 0) it.getString(mimeCol) else null

                    if (data != null && File(data).exists()) {
                        results.add(
                            FileItem(
                                id = data,
                                name = name,
                                path = data,
                                sizeBytes = size,
                                lastModified = date,
                                isDirectory = false,
                                mimeType = mime,
                                extension = name.substringAfterLast('.', "").lowercase(),
                                category = category
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Search files by query name, optional path, category, size range, and date range.
     */
    suspend fun searchFiles(
        query: String,
        rootPath: String? = null,
        category: FileCategory? = null,
        minSize: Long? = null,
        maxSize: Long? = null,
        minDate: Long? = null
    ): List<FileItem> = withContext(Dispatchers.IO) {
        val root = if (rootPath != null) File(rootPath) else Environment.getExternalStorageDirectory()
        val results = mutableListOf<FileItem>()
        val lowercaseQuery = query.lowercase().trim()

        fun searchRecursive(dir: File, depth: Int) {
            if (depth > 6 || results.size >= 200) return
            val list = dir.listFiles() ?: return

            for (f in list) {
                if (f.name.startsWith(".")) continue

                val isDir = f.isDirectory
                val nameMatches = lowercaseQuery.isEmpty() || f.name.lowercase().contains(lowercaseQuery)

                if (nameMatches) {
                    val cat = categorizeFile(f.name, isDir)
                    val catMatches = category == null || category == FileCategory.ALL || cat == category
                    val sizeMatches = if (isDir) true else {
                        (minSize == null || f.length() >= minSize) && (maxSize == null || f.length() <= maxSize)
                    }
                    val dateMatches = minDate == null || f.lastModified() >= minDate

                    if (catMatches && sizeMatches && dateMatches) {
                        results.add(
                            FileItem(
                                id = f.absolutePath,
                                name = f.name,
                                path = f.absolutePath,
                                sizeBytes = if (isDir) 0L else f.length(),
                                lastModified = f.lastModified(),
                                isDirectory = isDir,
                                mimeType = if (isDir) null else getMimeType(f),
                                extension = if (isDir) "" else f.name.substringAfterLast('.', "").lowercase(),
                                category = cat
                            )
                        )
                    }
                }

                if (isDir) {
                    searchRecursive(f, depth + 1)
                }
            }
        }

        if (root.exists() && root.isDirectory) {
            searchRecursive(root, 0)
        }

        results
    }

    // --- File Operations ---

    suspend fun createFolder(parentPath: String, name: String): Result<FileItem> = withContext(Dispatchers.IO) {
        try {
            val dir = File(parentPath, name)
            if (dir.exists()) {
                return@withContext Result.failure(Exception("Directory already exists"))
            }
            if (dir.mkdirs()) {
                val item = FileItem(
                    id = dir.absolutePath,
                    name = dir.name,
                    path = dir.absolutePath,
                    isDirectory = true
                )
                Result.success(item)
            } else {
                Result.failure(Exception("Failed to create directory"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createFile(parentPath: String, name: String): Result<FileItem> = withContext(Dispatchers.IO) {
        try {
            val file = File(parentPath, name)
            if (file.exists()) {
                return@withContext Result.failure(Exception("File already exists"))
            }
            if (file.createNewFile()) {
                val item = FileItem(
                    id = file.absolutePath,
                    name = file.name,
                    path = file.absolutePath,
                    sizeBytes = 0L,
                    lastModified = file.lastModified(),
                    isDirectory = false,
                    mimeType = getMimeType(file),
                    extension = file.name.substringAfterLast('.', "").lowercase(),
                    category = categorizeFile(file.name, false)
                )
                Result.success(item)
            } else {
                Result.failure(Exception("Failed to create file"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun rename(fileItem: FileItem, newName: String): Result<FileItem> = withContext(Dispatchers.IO) {
        try {
            val source = File(fileItem.path)
            val parent = source.parentFile ?: return@withContext Result.failure(Exception("Parent directory not found"))
            val target = File(parent, newName)
            if (target.exists()) {
                return@withContext Result.failure(Exception("An item with name '$newName' already exists"))
            }
            if (source.renameTo(target)) {
                val isDir = target.isDirectory
                val updated = FileItem(
                    id = target.absolutePath,
                    name = target.name,
                    path = target.absolutePath,
                    sizeBytes = if (isDir) 0L else target.length(),
                    lastModified = target.lastModified(),
                    isDirectory = isDir,
                    mimeType = if (isDir) null else getMimeType(target),
                    extension = if (isDir) "" else target.name.substringAfterLast('.', "").lowercase(),
                    category = categorizeFile(target.name, isDir)
                )
                Result.success(updated)
            } else {
                Result.failure(Exception("Rename operation failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteFiles(
        files: List<FileItem>,
        onProgress: (Int, Int) -> Unit = { _, _ -> }
    ): Result<Int> = withContext(Dispatchers.IO) {
        var count = 0
        try {
            for ((index, item) in files.withIndex()) {
                val file = File(item.path)
                if (file.exists()) {
                    if (file.isDirectory) {
                        file.deleteRecursively()
                    } else {
                        file.delete()
                    }
                    count++
                }
                onProgress(index + 1, files.size)
            }
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Trash / Recycle Bin Operations ---

    fun getTrashDirectory(): File {
        val storageRoot = Environment.getExternalStorageDirectory()
        val trashDir = File(storageRoot, ".init_trash")
        if (!trashDir.exists()) {
            trashDir.mkdirs()
        }
        return if (trashDir.canWrite()) trashDir else File(context.filesDir, "trash").apply { mkdirs() }
    }

    suspend fun moveToTrash(item: FileItem): Result<TrashItem> = withContext(Dispatchers.IO) {
        try {
            val src = File(item.path)
            if (!src.exists()) {
                return@withContext Result.failure(Exception("Source file does not exist"))
            }

            val trashDir = getTrashDirectory()
            val isDir = item.isDirectory || src.isDirectory
            val uniqueName = "${System.currentTimeMillis()}_${item.name}"
            val target = File(trashDir, uniqueName)

            val moved = if (src.renameTo(target)) {
                true
            } else {
                if (isDir) {
                    src.copyRecursively(target, overwrite = true)
                    src.deleteRecursively()
                } else {
                    src.copyTo(target, overwrite = true)
                    src.delete()
                }
                target.exists()
            }

            if (moved) {
                val trashItem = TrashItem(
                    originalPath = item.path,
                    trashPath = target.absolutePath,
                    name = item.name,
                    sizeBytes = if (isDir) 0L else item.sizeBytes,
                    deletedAt = System.currentTimeMillis(),
                    isDirectory = isDir
                )
                Result.success(trashItem)
            } else {
                Result.failure(Exception("Failed to move file to trash"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun restoreTrashItem(item: TrashItem): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val trashFile = File(item.trashPath)
            if (!trashFile.exists()) {
                return@withContext Result.failure(Exception("Trash item no longer exists on disk"))
            }

            val originalFile = File(item.originalPath)
            originalFile.parentFile?.mkdirs()

            val restored = if (trashFile.renameTo(originalFile)) {
                true
            } else {
                if (item.isDirectory) {
                    trashFile.copyRecursively(originalFile, overwrite = true)
                    trashFile.deleteRecursively()
                } else {
                    trashFile.copyTo(originalFile, overwrite = true)
                    trashFile.delete()
                }
                originalFile.exists()
            }

            if (restored) {
                Result.success(true)
            } else {
                Result.failure(Exception("Failed to restore item"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun permanentlyDeleteTrashItem(item: TrashItem): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val trashFile = File(item.trashPath)
            val deleted = if (trashFile.exists()) {
                if (item.isDirectory) trashFile.deleteRecursively() else trashFile.delete()
            } else {
                true
            }
            Result.success(deleted)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun emptyTrash(items: List<TrashItem>): Result<Int> = withContext(Dispatchers.IO) {
        var count = 0
        try {
            for (item in items) {
                val trashFile = File(item.trashPath)
                if (trashFile.exists()) {
                    if (item.isDirectory) trashFile.deleteRecursively() else trashFile.delete()
                }
                count++
            }
            val trashDir = getTrashDirectory()
            trashDir.listFiles()?.forEach {
                if (it.isDirectory) it.deleteRecursively() else it.delete()
            }
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun copyFiles(
        sources: List<FileItem>,
        targetDirPath: String,
        onProgress: (OperationProgress) -> Unit
    ): Result<Int> = withContext(Dispatchers.IO) {
        var processed = 0
        var totalBytes = sources.sumOf { it.sizeBytes }
        var processedBytes = 0L

        try {
            for (item in sources) {
                val src = File(item.path)
                val dest = File(targetDirPath, item.name)

                onProgress(
                    OperationProgress(
                        operation = FileOperation.COPY,
                        currentFileName = item.name,
                        processedItems = processed,
                        totalItems = sources.size,
                        processedBytes = processedBytes,
                        totalBytes = totalBytes
                    )
                )

                if (src.isDirectory) {
                    src.copyRecursively(dest, overwrite = true)
                } else {
                    src.inputStream().use { input ->
                        dest.outputStream().use { output ->
                            val buffer = ByteArray(64 * 1024)
                            var read: Int
                            while (input.read(buffer).also { read = it } >= 0) {
                                output.write(buffer, 0, read)
                                processedBytes += read
                                onProgress(
                                    OperationProgress(
                                        operation = FileOperation.COPY,
                                        currentFileName = item.name,
                                        processedItems = processed,
                                        totalItems = sources.size,
                                        processedBytes = processedBytes,
                                        totalBytes = totalBytes
                                    )
                                )
                            }
                        }
                    }
                }
                processed++
            }

            onProgress(
                OperationProgress(
                    operation = FileOperation.COPY,
                    processedItems = processed,
                    totalItems = sources.size,
                    isDone = true
                )
            )
            Result.success(processed)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun moveFiles(
        sources: List<FileItem>,
        targetDirPath: String,
        onProgress: (OperationProgress) -> Unit
    ): Result<Int> = withContext(Dispatchers.IO) {
        var processed = 0
        try {
            for (item in sources) {
                val src = File(item.path)
                val dest = File(targetDirPath, item.name)

                onProgress(
                    OperationProgress(
                        operation = FileOperation.MOVE,
                        currentFileName = item.name,
                        processedItems = processed,
                        totalItems = sources.size
                    )
                )

                if (src.renameTo(dest)) {
                    processed++
                } else {
                    // Fallback copy + delete
                    if (src.isDirectory) {
                        src.copyRecursively(dest, overwrite = true)
                        src.deleteRecursively()
                    } else {
                        src.copyTo(dest, overwrite = true)
                        src.delete()
                    }
                    processed++
                }
            }

            onProgress(
                OperationProgress(
                    operation = FileOperation.MOVE,
                    processedItems = processed,
                    totalItems = sources.size,
                    isDone = true
                )
            )
            Result.success(processed)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun zipFiles(
        sources: List<FileItem>,
        targetZipPath: String,
        onProgress: (OperationProgress) -> Unit
    ): Result<FileItem> = withContext(Dispatchers.IO) {
        try {
            val zipFile = File(targetZipPath)
            val zipOut = ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile)))

            var processed = 0
            for (item in sources) {
                val src = File(item.path)
                onProgress(
                    OperationProgress(
                        operation = FileOperation.ZIP,
                        currentFileName = item.name,
                        processedItems = processed,
                        totalItems = sources.size
                    )
                )
                addFileToZip(src, src.name, zipOut)
                processed++
            }
            zipOut.close()

            val created = FileItem(
                id = zipFile.absolutePath,
                name = zipFile.name,
                path = zipFile.absolutePath,
                sizeBytes = zipFile.length(),
                lastModified = zipFile.lastModified(),
                isDirectory = false,
                mimeType = "application/zip",
                extension = "zip",
                category = FileCategory.ARCHIVES
            )

            onProgress(
                OperationProgress(
                    operation = FileOperation.ZIP,
                    processedItems = processed,
                    totalItems = sources.size,
                    isDone = true
                )
            )
            Result.success(created)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun addFileToZip(file: File, entryName: String, zipOut: ZipOutputStream) {
        if (file.isDirectory) {
            val children = file.listFiles() ?: return
            for (c in children) {
                addFileToZip(c, "$entryName/${c.name}", zipOut)
            }
        } else {
            val buffer = ByteArray(64 * 1024)
            val input = BufferedInputStream(FileInputStream(file))
            zipOut.putNextEntry(ZipEntry(entryName))
            var read: Int
            while (input.read(buffer).also { read = it } >= 0) {
                zipOut.write(buffer, 0, read)
            }
            zipOut.closeEntry()
            input.close()
        }
    }

    suspend fun extractZip(
        zipFile: FileItem,
        targetDirPath: String,
        onProgress: (OperationProgress) -> Unit
    ): Result<Int> = withContext(Dispatchers.IO) {
        var count = 0
        try {
            val targetDir = File(targetDirPath)
            targetDir.mkdirs()

            val zipIn = ZipInputStream(BufferedInputStream(FileInputStream(File(zipFile.path))))
            var entry = zipIn.nextEntry

            while (entry != null) {
                val filePath = File(targetDirPath, entry.name)
                onProgress(
                    OperationProgress(
                        operation = FileOperation.UNZIP,
                        currentFileName = entry.name,
                        processedItems = count
                    )
                )

                if (entry.isDirectory) {
                    filePath.mkdirs()
                } else {
                    filePath.parentFile?.mkdirs()
                    val output = BufferedOutputStream(FileOutputStream(filePath))
                    val buffer = ByteArray(64 * 1024)
                    var read: Int
                    while (zipIn.read(buffer).also { read = it } >= 0) {
                        output.write(buffer, 0, read)
                    }
                    output.close()
                    count++
                }
                zipIn.closeEntry()
                entry = zipIn.nextEntry
            }
            zipIn.close()

            onProgress(
                OperationProgress(
                    operation = FileOperation.UNZIP,
                    processedItems = count,
                    isDone = true
                )
            )
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Storage Analyzer Engine ---

    suspend fun analyzeStorage(
        volume: StorageVolumeInfo,
        onProgress: (Float) -> Unit = {}
    ): StorageBreakdown = withContext(Dispatchers.IO) {
        val root = File(volume.path)
        if (!root.exists()) {
            return@withContext StorageBreakdown(volumeInfo = volume)
        }

        var imgBytes = 0L
        var vidBytes = 0L
        var audBytes = 0L
        var docBytes = 0L
        var apkBytes = 0L
        var archBytes = 0L
        var downBytes = 0L
        var otherBytes = 0L

        val allFiles = mutableListOf<FileItem>()
        val folderSizes = mutableListOf<FileItem>()
        val junkFiles = mutableListOf<FileItem>()

        fun scanDir(dir: File, depth: Int) {
            val list = dir.listFiles() ?: return
            var thisDirSize = 0L

            for (f in list) {
                if (f.isDirectory) {
                    if (depth < 3) {
                        val subSize = calculateFolderSize(f)
                        folderSizes.add(
                            FileItem(
                                id = f.absolutePath,
                                name = f.name,
                                path = f.absolutePath,
                                sizeBytes = subSize,
                                lastModified = f.lastModified(),
                                isDirectory = true
                            )
                        )
                    }
                    // Detect empty folders as junk
                    if (f.list()?.isEmpty() == true) {
                        junkFiles.add(
                            FileItem(
                                id = f.absolutePath,
                                name = f.name,
                                path = f.absolutePath,
                                sizeBytes = 0L,
                                isDirectory = true,
                                category = FileCategory.JUNK
                            )
                        )
                    }
                    if (depth < 6) {
                        scanDir(f, depth + 1)
                    }
                } else {
                    val size = f.length()
                    thisDirSize += size
                    val ext = f.name.substringAfterLast('.', "").lowercase()
                    val cat = categorizeFile(f.name, false)

                    when (cat) {
                        FileCategory.IMAGES -> imgBytes += size
                        FileCategory.VIDEOS -> vidBytes += size
                        FileCategory.AUDIO -> audBytes += size
                        FileCategory.DOCUMENTS -> docBytes += size
                        FileCategory.APKS -> apkBytes += size
                        FileCategory.ARCHIVES -> archBytes += size
                        FileCategory.JUNK -> {
                            junkFiles.add(
                                FileItem(
                                    id = f.absolutePath,
                                    name = f.name,
                                    path = f.absolutePath,
                                    sizeBytes = size,
                                    lastModified = f.lastModified(),
                                    category = FileCategory.JUNK
                                )
                            )
                        }
                        else -> {
                            if (f.parentFile?.name?.equals("download", ignoreCase = true) == true) {
                                downBytes += size
                            } else {
                                otherBytes += size
                            }
                        }
                    }

                    if (size >= 5 * 1024 * 1024) { // Files >= 5MB
                        allFiles.add(
                            FileItem(
                                id = f.absolutePath,
                                name = f.name,
                                path = f.absolutePath,
                                sizeBytes = size,
                                lastModified = f.lastModified(),
                                isDirectory = false,
                                extension = ext,
                                category = cat
                            )
                        )
                    }
                }
            }
        }

        scanDir(root, 0)

        val topFiles = allFiles.sortedByDescending { it.sizeBytes }.take(50)
        val topFolders = folderSizes.sortedByDescending { it.sizeBytes }.take(20)
        val junkTotal = junkFiles.sumOf { it.sizeBytes }
        val systemBytes = (volume.usedBytes - (imgBytes + vidBytes + audBytes + docBytes + apkBytes + archBytes + otherBytes)).coerceAtLeast(0L)

        StorageBreakdown(
            volumeInfo = volume,
            imagesBytes = imgBytes,
            videosBytes = vidBytes,
            audioBytes = audBytes,
            documentsBytes = docBytes,
            apksBytes = apkBytes,
            archivesBytes = archBytes,
            downloadsBytes = downBytes,
            otherBytes = otherBytes,
            systemBytes = systemBytes,
            largestFiles = topFiles,
            largestFolders = topFolders,
            junkFiles = junkFiles.take(50),
            junkTotalBytes = junkTotal
        )
    }

    private fun calculateFolderSize(folder: File): Long {
        var length = 0L
        val files = folder.listFiles() ?: return 0L
        for (f in files) {
            length += if (f.isDirectory) calculateFolderSize(f) else f.length()
        }
        return length
    }

    // --- APK Metadata & Checksums ---

    fun getApkMetadata(path: String): ApkMetadata? {
        return try {
            val pm = context.packageManager
            val packageInfo: PackageInfo? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageArchiveInfo(path, PackageManager.PackageInfoFlags.of(0))
            } else {
                pm.getPackageArchiveInfo(path, 0)
            }

            if (packageInfo != null) {
                val appInfo = packageInfo.applicationInfo
                appInfo?.sourceDir = path
                appInfo?.publicSourceDir = path
                val appName = appInfo?.let { pm.getApplicationLabel(it).toString() } ?: packageInfo.packageName
                val icon = appInfo?.let { pm.getApplicationIcon(it) }

                val vCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    packageInfo.longVersionCode
                } else {
                    @Suppress("DEPRECATION")
                    packageInfo.versionCode.toLong()
                }

                ApkMetadata(
                    appName = appName,
                    packageName = packageInfo.packageName,
                    versionName = packageInfo.versionName ?: "1.0",
                    versionCode = vCode,
                    minSdk = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) appInfo?.minSdkVersion ?: 21 else 21,
                    targetSdk = appInfo?.targetSdkVersion ?: 34,
                    icon = icon
                )
            } else null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun calculateChecksum(path: String, algorithm: String = "SHA-256"): String = withContext(Dispatchers.IO) {
        try {
            val digest = MessageDigest.getInstance(algorithm)
            val file = File(path)
            if (!file.exists() || file.isDirectory) return@withContext "N/A"

            file.inputStream().use { stream ->
                val buffer = ByteArray(64 * 1024)
                var read: Int
                while (stream.read(buffer).also { read = it } >= 0) {
                    digest.update(buffer, 0, read)
                }
            }

            val hexChars = "0123456789ABCDEF"
            val bytes = digest.digest()
            val result = StringBuilder(bytes.size * 2)
            for (byte in bytes) {
                val i = byte.toInt()
                result.append(hexChars[i shr 4 and 0x0f])
                result.append(hexChars[i and 0x0f])
            }
            result.toString()
        } catch (e: Exception) {
            "ERROR: ${e.message}"
        }
    }

    private fun getMimeType(file: File): String {
        val ext = file.name.substringAfterLast('.', "").lowercase()
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "application/octet-stream"
    }

    private fun sortFileList(items: List<FileItem>, sortConfig: SortConfig): List<FileItem> {
        val folders = items.filter { it.isDirectory }
        val files = items.filter { !it.isDirectory }

        val comparator = when (sortConfig.field) {
            SortField.NAME -> compareBy<FileItem> { it.name.lowercase() }
            SortField.SIZE -> compareBy<FileItem> { it.sizeBytes }
            SortField.DATE -> compareBy<FileItem> { it.lastModified }
            SortField.TYPE -> compareBy<FileItem> { it.extension }
        }

        val sortedFolders = if (sortConfig.order == SortOrder.ASCENDING) {
            folders.sortedWith(comparator)
        } else {
            folders.sortedWith(comparator.reversed())
        }

        val sortedFiles = if (sortConfig.order == SortOrder.ASCENDING) {
            files.sortedWith(comparator)
        } else {
            files.sortedWith(comparator.reversed())
        }

        return sortedFolders + sortedFiles
    }
}
