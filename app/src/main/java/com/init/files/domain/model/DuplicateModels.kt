package com.init.files.domain.model

/**
 * Group of identical duplicate files identified by checksum and exact size.
 */
data class DuplicateGroup(
    val checksum: String,
    val sizeBytes: Long,
    val primaryFile: FileItem,
    val duplicateFiles: List<FileItem>
) {
    val allFiles: List<FileItem>
        get() = listOf(primaryFile) + duplicateFiles

    val totalFilesCount: Int
        get() = allFiles.size

    val wastedBytes: Long
        get() = sizeBytes * duplicateFiles.size

    val formattedWastedSize: String
        get() = formatByteSize(wastedBytes)

    val formattedSingleSize: String
        get() = formatByteSize(sizeBytes)
}

/**
 * State representation for Duplicate Finder scan lifecycle.
 */
sealed interface DuplicateScanState {
    data object Idle : DuplicateScanState
    data class Scanning(
        val filesScanned: Int = 0,
        val duplicatesFound: Int = 0,
        val currentFileName: String = ""
    ) : DuplicateScanState
    data class Completed(
        val groups: List<DuplicateGroup> = emptyList(),
        val totalWastedBytes: Long = 0L,
        val selectedPaths: Set<String> = emptySet()
    ) : DuplicateScanState
    data class Deleting(
        val deletedCount: Int = 0,
        val totalToDelete: Int = 0
    ) : DuplicateScanState
}

/**
 * Smart Quick-Select filter modes.
 */
enum class DuplicateSelectFilter {
    ALL_DUPLICATES,
    KEEP_OLDEST,
    KEEP_NEWEST,
    DESELECT_ALL
}
