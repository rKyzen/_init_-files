package com.init.file

import com.init.file.domain.model.FileCategory
import com.init.file.domain.model.FileItem
import com.init.file.domain.model.categorizeFile
import com.init.file.domain.model.formatByteSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FileModelsTest {

    @Test
    fun testFormatByteSize() {
        assertEquals("0 B", formatByteSize(0L))
        assertEquals("512 B", formatByteSize(512L))
        assertEquals("1.0 KB", formatByteSize(1024L))
        assertEquals("1.5 KB", formatByteSize(1536L))
        assertEquals("1.0 MB", formatByteSize(1024L * 1024L))
        assertEquals("4.2 MB", formatByteSize((4.2 * 1024L * 1024L).toLong()))
        assertEquals("2.5 GB", formatByteSize((2.5 * 1024L * 1024L * 1024L).toLong()))
    }

    @Test
    fun testCategorizeFile() {
        assertEquals(FileCategory.ALL, categorizeFile("folder", isDirectory = true))

        assertEquals(FileCategory.IMAGES, categorizeFile("photo.jpg", isDirectory = false))
        assertEquals(FileCategory.IMAGES, categorizeFile("screenshot.PNG", isDirectory = false))
        assertEquals(FileCategory.IMAGES, categorizeFile("graphic.webp", isDirectory = false))

        assertEquals(FileCategory.VIDEOS, categorizeFile("movie.mp4", isDirectory = false))
        assertEquals(FileCategory.VIDEOS, categorizeFile("clip.mkv", isDirectory = false))

        assertEquals(FileCategory.AUDIO, categorizeFile("song.mp3", isDirectory = false))
        assertEquals(FileCategory.AUDIO, categorizeFile("track.flac", isDirectory = false))

        assertEquals(FileCategory.DOCUMENTS, categorizeFile("document.pdf", isDirectory = false))
        assertEquals(FileCategory.DOCUMENTS, categorizeFile("notes.md", isDirectory = false))
        assertEquals(FileCategory.DOCUMENTS, categorizeFile("source.kt", isDirectory = false))

        assertEquals(FileCategory.APKS, categorizeFile("app-debug.apk", isDirectory = false))
        assertEquals(FileCategory.APKS, categorizeFile("bundle.aab", isDirectory = false))

        assertEquals(FileCategory.ARCHIVES, categorizeFile("backup.zip", isDirectory = false))
        assertEquals(FileCategory.ARCHIVES, categorizeFile("archive.tar.gz", isDirectory = false))

        assertEquals(FileCategory.JUNK, categorizeFile("cache.tmp", isDirectory = false))
        assertEquals(FileCategory.JUNK, categorizeFile("system.log", isDirectory = false))
    }

    @Test
    fun testFileItemProperties() {
        val file = FileItem(
            id = "/storage/emulated/0/Download/file.pdf",
            name = "file.pdf",
            path = "/storage/emulated/0/Download/file.pdf",
            sizeBytes = 2048 * 1024,
            lastModified = 1700000000000L,
            isDirectory = false,
            mimeType = "application/pdf",
            extension = "pdf",
            category = FileCategory.DOCUMENTS
        )

        assertEquals("2.0 MB", file.formattedSize)
        assertFalse(file.isDirectory)
        assertEquals("pdf", file.extension)
        assertEquals(FileCategory.DOCUMENTS, file.category)
    }
}
