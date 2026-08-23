package com.init.files

import com.init.files.domain.model.DuplicateGroup
import com.init.files.domain.model.DuplicateSelectFilter
import com.init.files.domain.model.FileCategory
import com.init.files.domain.model.FileItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DuplicateFinderLogicTest {

    private val primary = FileItem(
        id = "/storage/emulated/0/DCIM/photo.jpg",
        name = "photo.jpg",
        path = "/storage/emulated/0/DCIM/photo.jpg",
        sizeBytes = 2 * 1024 * 1024L,
        lastModified = 1000L,
        category = FileCategory.IMAGES
    )

    private val copy1 = FileItem(
        id = "/storage/emulated/0/Download/photo_copy.jpg",
        name = "photo_copy.jpg",
        path = "/storage/emulated/0/Download/photo_copy.jpg",
        sizeBytes = 2 * 1024 * 1024L,
        lastModified = 2000L,
        category = FileCategory.IMAGES
    )

    private val copy2 = FileItem(
        id = "/storage/emulated/0/Pictures/backup.jpg",
        name = "backup.jpg",
        path = "/storage/emulated/0/Pictures/backup.jpg",
        sizeBytes = 2 * 1024 * 1024L,
        lastModified = 3000L,
        category = FileCategory.IMAGES
    )

    @Test
    fun testDuplicateGroupCalculations() {
        val group = DuplicateGroup(
            checksum = "A1B2C3D4E5F6",
            sizeBytes = 2 * 1024 * 1024L,
            primaryFile = primary,
            duplicateFiles = listOf(copy1, copy2)
        )

        assertEquals(3, group.totalFilesCount)
        // 2 duplicates * 2MB = 4MB wasted space
        assertEquals(4 * 1024 * 1024L, group.wastedBytes)
        assertEquals("4.0 MB", group.formattedWastedSize)
        assertEquals("2.0 MB", group.formattedSingleSize)
    }

    @Test
    fun testKeepOldestSelectionLogic() {
        val group = DuplicateGroup(
            checksum = "SHA256HASH",
            sizeBytes = 2 * 1024 * 1024L,
            primaryFile = primary,
            duplicateFiles = listOf(copy1, copy2)
        )

        // Keep oldest should keep 'primary' (timestamp 1000L) and select copy1 (2000L) and copy2 (3000L)
        val oldest = group.allFiles.minByOrNull { it.lastModified }
        val toDelete = group.allFiles.filter { it.path != oldest?.path }.map { it.path }.toSet()

        assertEquals(2, toDelete.size)
        assertFalse(toDelete.contains(primary.path))
        assertTrue(toDelete.contains(copy1.path))
        assertTrue(toDelete.contains(copy2.path))
    }

    @Test
    fun testKeepNewestSelectionLogic() {
        val group = DuplicateGroup(
            checksum = "SHA256HASH",
            sizeBytes = 2 * 1024 * 1024L,
            primaryFile = primary,
            duplicateFiles = listOf(copy1, copy2)
        )

        // Keep newest should keep 'copy2' (timestamp 3000L) and select primary and copy1
        val newest = group.allFiles.maxByOrNull { it.lastModified }
        val toDelete = group.allFiles.filter { it.path != newest?.path }.map { it.path }.toSet()

        assertEquals(2, toDelete.size)
        assertTrue(toDelete.contains(primary.path))
        assertTrue(toDelete.contains(copy1.path))
        assertFalse(toDelete.contains(copy2.path))
    }
}
