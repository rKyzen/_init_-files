package com.init.files.domain.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Core representation of a file or directory in _init_ /files.
 */
data class FileItem(
    val id: String,
    val name: String,
    val path: String,
    val uriString: String? = null,
    val sizeBytes: Long = 0L,
    val lastModified: Long = 0L,
    val isDirectory: Boolean = false,
    val mimeType: String? = null,
    val extension: String = "",
    val isHidden: Boolean = false,
    val isFavorite: Boolean = false,
    val childrenCount: Int? = null,
    val category: FileCategory = FileCategory.ALL
) {
    val formattedSize: String
        get() = formatByteSize(sizeBytes)

    val formattedDate: String
        get() = formatDate(lastModified)
}

/**
 * Item stored in the Trash / Recycle Bin.
 */
data class TrashItem(
    val id: Long = 0L,
    val originalPath: String,
    val trashPath: String,
    val name: String,
    val sizeBytes: Long = 0L,
    val deletedAt: Long = 0L,
    val isDirectory: Boolean = false
) {
    val formattedSize: String
        get() = formatByteSize(sizeBytes)

    val formattedDeletedDate: String
        get() = formatDate(deletedAt)

    val daysRemaining: Int
        get() {
            val ageMs = System.currentTimeMillis() - deletedAt
            val totalRetentionMs = 30L * 24L * 60L * 60L * 1000L
            val remainingMs = totalRetentionMs - ageMs
            val days = (remainingMs / (24L * 60L * 60L * 1000L)).toInt()
            return days.coerceAtLeast(0)
        }

    val expiryLabel: String
        get() {
            val days = daysRemaining
            return when {
                days <= 0 -> "expires today"
                days == 1 -> "1 day left"
                else -> "$days days left"
            }
        }
}

/**
 * Standard categorized shortcuts.
 */
enum class FileCategory(val label: String, val extensions: Set<String>) {
    IMAGES("images", setOf("jpg", "jpeg", "png", "webp", "gif", "bmp", "svg", "heic", "avif")),
    VIDEOS("videos", setOf("mp4", "mkv", "webm", "avi", "mov", "3gp", "flv", "wmv")),
    AUDIO("audio", setOf("mp3", "wav", "flac", "m4a", "ogg", "aac", "opus", "mid")),
    DOCUMENTS("docs", setOf("pdf", "doc", "docx", "txt", "md", "csv", "xls", "xlsx", "ppt", "pptx", "epub", "json", "xml", "kt", "java", "py", "c", "cpp", "js", "html")),
    APKS("apks", setOf("apk", "apks", "xapk", "aab")),
    ARCHIVES("archives", setOf("zip", "rar", "7z", "tar", "gz", "bz2", "xz")),
    DOWNLOADS("downloads", emptySet()),
    ALL("all files", emptySet()),
    JUNK("junk", setOf("tmp", "log", "cache", "bak", "dmp"))
}

/**
 * Represents physical/virtual storage volumes.
 */
data class StorageVolumeInfo(
    val id: String,
    val name: String,
    val path: String,
    val totalBytes: Long,
    val freeBytes: Long,
    val usedBytes: Long = (totalBytes - freeBytes).coerceAtLeast(0L),
    val isPrimary: Boolean = true,
    val isRemovable: Boolean = false,
    val isMounted: Boolean = true
) {
    val usagePercentage: Float
        get() = if (totalBytes > 0) (usedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f) else 0f

    val formattedTotal: String
        get() = formatByteSize(totalBytes)

    val formattedUsed: String
        get() = formatByteSize(usedBytes)

    val formattedFree: String
        get() = formatByteSize(freeBytes)
}

/**
 * Breakdown of storage usage across categories.
 */
data class StorageBreakdown(
    val volumeInfo: StorageVolumeInfo,
    val imagesBytes: Long = 0L,
    val videosBytes: Long = 0L,
    val audioBytes: Long = 0L,
    val documentsBytes: Long = 0L,
    val apksBytes: Long = 0L,
    val archivesBytes: Long = 0L,
    val downloadsBytes: Long = 0L,
    val otherBytes: Long = 0L,
    val systemBytes: Long = 0L,
    val largestFiles: List<FileItem> = emptyList(),
    val largestFolders: List<FileItem> = emptyList(),
    val junkFiles: List<FileItem> = emptyList(),
    val junkTotalBytes: Long = 0L
)

enum class SortField {
    NAME, SIZE, DATE, TYPE
}

enum class SortOrder {
    ASCENDING, DESCENDING
}

data class SortConfig(
    val field: SortField = SortField.NAME,
    val order: SortOrder = SortOrder.ASCENDING
)

enum class ViewMode {
    LIST, GRID
}

enum class FileOperation {
    COPY, MOVE, RENAME, DELETE, ZIP, UNZIP, CREATE_FOLDER, CREATE_FILE
}

data class OperationProgress(
    val operation: FileOperation,
    val currentFileName: String = "",
    val processedItems: Int = 0,
    val totalItems: Int = 0,
    val processedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val isDone: Boolean = false,
    val errorMessage: String? = null
)

/**
 * Utility functions for byte formatting and timestamps.
 */
fun formatByteSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB", "PB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt().coerceIn(0, units.size - 1)
    val value = bytes / Math.pow(1024.0, digitGroups.toDouble())
    return if (digitGroups == 0) {
        "$bytes B"
    } else {
        String.format(Locale.US, "%.1f %s", value, units[digitGroups])
    }
}

fun formatDate(timestamp: Long): String {
    if (timestamp <= 0) return "--"
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
    return sdf.format(Date(timestamp))
}

fun categorizeFile(name: String, isDirectory: Boolean): FileCategory {
    if (isDirectory) return FileCategory.ALL
    val ext = name.substringAfterLast('.', "").lowercase()
    return when {
        FileCategory.IMAGES.extensions.contains(ext) -> FileCategory.IMAGES
        FileCategory.VIDEOS.extensions.contains(ext) -> FileCategory.VIDEOS
        FileCategory.AUDIO.extensions.contains(ext) -> FileCategory.AUDIO
        FileCategory.DOCUMENTS.extensions.contains(ext) -> FileCategory.DOCUMENTS
        FileCategory.APKS.extensions.contains(ext) -> FileCategory.APKS
        FileCategory.ARCHIVES.extensions.contains(ext) -> FileCategory.ARCHIVES
        FileCategory.JUNK.extensions.contains(ext) -> FileCategory.JUNK
        else -> FileCategory.ALL
    }
}
