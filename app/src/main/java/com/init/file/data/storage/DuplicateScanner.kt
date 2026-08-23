package com.init.file.data.storage

import android.content.Context
import android.os.Environment
import com.init.file.domain.model.DuplicateGroup
import com.init.file.domain.model.FileCategory
import com.init.file.domain.model.FileItem
import com.init.file.domain.model.categorizeFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.RandomAccessFile
import java.security.MessageDigest

sealed interface DuplicateScanProgress {
    data class Scanning(val filesIndexed: Int, val currentPath: String) : DuplicateScanProgress
    data class Analyzing(val collisionsCount: Int, val processedCollisions: Int) : DuplicateScanProgress
    data class Completed(val groups: List<DuplicateGroup>, val totalWastedBytes: Long) : DuplicateScanProgress
    data class Error(val message: String) : DuplicateScanProgress
}

class DuplicateScanner(
    private val context: Context,
    private val fileManager: FileManager
) {
    /**
     * Executes multi-stage duplicate scan across mounted storage directories.
     */
    fun scanForDuplicates(
        categoryFilter: FileCategory = FileCategory.ALL
    ): Flow<DuplicateScanProgress> = flow {
        try {
            val roots = mutableListOf<File>()
            val external = Environment.getExternalStorageDirectory()
            if (external.exists() && external.canRead()) {
                roots.add(external)
            }

            // Excluded paths like Android/data, Android/obb, .trash, .vault
            val ignoredSubstrings = setOf("/Android/data", "/Android/obb", "/.trash", "/.vault", "/vault_store", "/.thumbnails")

            val filesBySize = mutableMapOf<Long, MutableList<File>>()
            var filesIndexed = 0

            for (root in roots) {
                val queue = ArrayDeque<File>()
                queue.add(root)

                while (queue.isNotEmpty()) {
                    val dir = queue.removeFirst()
                    val files = dir.listFiles() ?: continue

                    for (file in files) {
                        val path = file.absolutePath
                        if (ignoredSubstrings.any { path.contains(it) }) continue

                        if (file.isDirectory) {
                            if (!file.name.startsWith(".")) {
                                queue.add(file)
                            }
                        } else if (file.isFile && file.length() >= 1024L) { // Min 1 KB
                            val cat = categorizeFile(file.name, isDirectory = false)
                            if (categoryFilter == FileCategory.ALL || cat == categoryFilter) {
                                val size = file.length()
                                filesBySize.getOrPut(size) { mutableListOf() }.add(file)
                                filesIndexed++
                                if (filesIndexed % 50 == 0) {
                                    emit(DuplicateScanProgress.Scanning(filesIndexed, file.name))
                                }
                            }
                        }
                    }
                }
            }

            emit(DuplicateScanProgress.Scanning(filesIndexed, "Analyzing file collisions..."))

            // Stage 1: Filter size collisions (size groups with >= 2 files)
            val sizeCandidateGroups = filesBySize.values.filter { it.size >= 2 }
            val totalCollisions = sizeCandidateGroups.sumOf { it.size }
            var processedCollisions = 0

            // Stage 2: Quick 4KB Sample Hash for candidate collisions
            val sampleHashGroups = mutableMapOf<String, MutableList<File>>()
            for (candidateGroup in sizeCandidateGroups) {
                for (file in candidateGroup) {
                    val sampleHash = computeSampleHash(file)
                    if (sampleHash != null) {
                        val key = "${file.length()}_$sampleHash"
                        sampleHashGroups.getOrPut(key) { mutableListOf() }.add(file)
                    }
                    processedCollisions++
                    if (processedCollisions % 25 == 0) {
                        emit(DuplicateScanProgress.Analyzing(totalCollisions, processedCollisions))
                    }
                }
            }

            // Stage 3: Full SHA-256 Digest on matching sample hashes
            val fullCandidateGroups = sampleHashGroups.values.filter { it.size >= 2 }
            val exactDuplicateGroups = mutableListOf<DuplicateGroup>()

            for (group in fullCandidateGroups) {
                val sha256Groups = mutableMapOf<String, MutableList<File>>()
                for (file in group) {
                    val fullDigest = computeFullSha256(file)
                    if (fullDigest != null) {
                        sha256Groups.getOrPut(fullDigest) { mutableListOf() }.add(file)
                    }
                }

                for ((checksum, matchingFiles) in sha256Groups) {
                    if (matchingFiles.size >= 2) {
                        // Sort by modification date ascending (oldest first as primary)
                        val sortedFiles = matchingFiles.sortedBy { it.lastModified() }
                        val fileItems = sortedFiles.map { toFileItem(it) }

                        val groupItem = DuplicateGroup(
                            checksum = checksum,
                            sizeBytes = fileItems[0].sizeBytes,
                            primaryFile = fileItems[0],
                            duplicateFiles = fileItems.drop(1)
                        )
                        exactDuplicateGroups.add(groupItem)
                    }
                }
            }

            // Sort groups by wasted space descending (largest duplicates first)
            val sortedDuplicateGroups = exactDuplicateGroups.sortedByDescending { it.wastedBytes }
            val totalWasted = sortedDuplicateGroups.sumOf { it.wastedBytes }

            emit(DuplicateScanProgress.Completed(sortedDuplicateGroups, totalWasted))
        } catch (e: Exception) {
            emit(DuplicateScanProgress.Error(e.message ?: "Unknown scan failure"))
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Deletes the specified list of duplicate file paths.
     */
    suspend fun deleteDuplicateFiles(
        paths: Set<String>,
        useTrash: Boolean = false
    ): Int = withContext(Dispatchers.IO) {
        var deleted = 0
        for (path in paths) {
            try {
                val file = File(path)
                if (file.exists() && file.isFile) {
                    if (useTrash) {
                        val item = toFileItem(file)
                        val trashRes = fileManager.moveToTrash(item)
                        if (trashRes.isSuccess) {
                            deleted++
                        } else if (file.delete()) {
                            deleted++
                        }
                    } else {
                        if (file.delete()) {
                            deleted++
                        }
                    }
                }
            } catch (_: Exception) {}
        }
        deleted
    }

    private fun computeSampleHash(file: File): String? {
        return try {
            val digest = MessageDigest.getInstance("MD5")
            val length = file.length()
            val sampleSize = 4096

            RandomAccessFile(file, "r").use { raf ->
                // Read start 4KB
                val startBuf = ByteArray(minOf(length, sampleSize.toLong()).toInt())
                raf.readFully(startBuf)
                digest.update(startBuf)

                // Read end 4KB
                if (length > sampleSize * 2) {
                    raf.seek(length - sampleSize)
                    val endBuf = ByteArray(sampleSize)
                    raf.readFully(endBuf)
                    digest.update(endBuf)
                }
            }

            bytesToHex(digest.digest())
        } catch (_: Exception) {
            null
        }
    }

    private fun computeFullSha256(file: File): String? {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            FileInputStream(file).use { fis ->
                val buffer = ByteArray(64 * 1024)
                var read: Int
                while (fis.read(buffer).also { read = it } != -1) {
                    digest.update(buffer, 0, read)
                }
            }
            bytesToHex(digest.digest())
        } catch (_: Exception) {
            null
        }
    }

    private fun toFileItem(file: File): FileItem {
        return FileItem(
            id = file.absolutePath,
            name = file.name,
            path = file.absolutePath,
            sizeBytes = file.length(),
            lastModified = file.lastModified(),
            isDirectory = false,
            extension = file.name.substringAfterLast('.', "").lowercase(),
            category = categorizeFile(file.name, isDirectory = false)
        )
    }

    private fun bytesToHex(bytes: ByteArray): String {
        val hexChars = "0123456789ABCDEF"
        val result = StringBuilder(bytes.size * 2)
        for (byte in bytes) {
            val i = byte.toInt()
            result.append(hexChars[i shr 4 and 0x0f])
            result.append(hexChars[i and 0x0f])
        }
        return result.toString()
    }
}
