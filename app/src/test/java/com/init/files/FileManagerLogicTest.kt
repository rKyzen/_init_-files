package com.init.files

import com.init.files.domain.model.FileCategory
import com.init.files.domain.model.FileItem
import com.init.files.domain.model.SortConfig
import com.init.files.domain.model.SortField
import com.init.files.domain.model.SortOrder
import org.junit.Assert.assertEquals
import org.junit.Test

class FileManagerLogicTest {

    private val sampleItems = listOf(
        FileItem(
            id = "1", name = "Zulu.txt", path = "/a/Zulu.txt",
            sizeBytes = 100L, lastModified = 1000L, isDirectory = false, extension = "txt"
        ),
        FileItem(
            id = "2", name = "Alpha.pdf", path = "/a/Alpha.pdf",
            sizeBytes = 500L, lastModified = 3000L, isDirectory = false, extension = "pdf"
        ),
        FileItem(
            id = "3", name = "Beta.jpg", path = "/a/Beta.jpg",
            sizeBytes = 200L, lastModified = 2000L, isDirectory = false, extension = "jpg"
        ),
        FileItem(
            id = "4", name = "Docs", path = "/a/Docs",
            sizeBytes = 0L, lastModified = 4000L, isDirectory = true
        )
    )

    private fun sortList(items: List<FileItem>, config: SortConfig): List<FileItem> {
        val folders = items.filter { it.isDirectory }
        val files = items.filter { !it.isDirectory }

        val comparator = when (config.field) {
            SortField.NAME -> compareBy<FileItem> { it.name.lowercase() }
            SortField.SIZE -> compareBy<FileItem> { it.sizeBytes }
            SortField.DATE -> compareBy<FileItem> { it.lastModified }
            SortField.TYPE -> compareBy<FileItem> { it.extension }
        }

        val sortedFolders = if (config.order == SortOrder.ASCENDING) {
            folders.sortedWith(comparator)
        } else {
            folders.sortedWith(comparator.reversed())
        }

        val sortedFiles = if (config.order == SortOrder.ASCENDING) {
            files.sortedWith(comparator)
        } else {
            files.sortedWith(comparator.reversed())
        }

        return sortedFolders + sortedFiles
    }

    @Test
    fun testSortByNameAscending() {
        val sorted = sortList(sampleItems, SortConfig(SortField.NAME, SortOrder.ASCENDING))
        assertEquals("Docs", sorted[0].name) // Folders first
        assertEquals("Alpha.pdf", sorted[1].name)
        assertEquals("Beta.jpg", sorted[2].name)
        assertEquals("Zulu.txt", sorted[3].name)
    }

    @Test
    fun testSortBySizeDescending() {
        val sorted = sortList(sampleItems, SortConfig(SortField.SIZE, SortOrder.DESCENDING))
        assertEquals("Docs", sorted[0].name) // Folders first
        assertEquals("Alpha.pdf", sorted[1].name) // 500B
        assertEquals("Beta.jpg", sorted[2].name) // 200B
        assertEquals("Zulu.txt", sorted[3].name) // 100B
    }

    @Test
    fun testSortByDateAscending() {
        val sorted = sortList(sampleItems, SortConfig(SortField.DATE, SortOrder.ASCENDING))
        assertEquals("Docs", sorted[0].name)
        assertEquals("Zulu.txt", sorted[1].name) // 1000L
        assertEquals("Beta.jpg", sorted[2].name) // 2000L
        assertEquals("Alpha.pdf", sorted[3].name) // 3000L
    }

    @Test
    fun testSortByTypeAscending() {
        val sorted = sortList(sampleItems, SortConfig(SortField.TYPE, SortOrder.ASCENDING))
        assertEquals("Docs", sorted[0].name)
        assertEquals("Beta.jpg", sorted[1].name) // jpg
        assertEquals("Alpha.pdf", sorted[2].name) // pdf
        assertEquals("Zulu.txt", sorted[3].name) // txt
    }
}
