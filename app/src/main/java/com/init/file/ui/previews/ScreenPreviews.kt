package com.init.file.ui.previews

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.init.file.domain.model.FileCategory
import com.init.file.domain.model.FileItem
import com.init.file.domain.model.FileOperation
import com.init.file.domain.model.OperationProgress
import com.init.file.domain.model.StorageBreakdown
import com.init.file.domain.model.StorageVolumeInfo
import com.init.file.theme.InitTheme
import com.init.file.ui.components.DotMatrixEmptyPattern
import com.init.file.ui.components.InitBadge
import com.init.file.ui.components.InitBreadcrumbs
import com.init.file.ui.components.InitButton
import com.init.file.ui.components.InitCard
import com.init.file.ui.components.InitSectionHeader
import com.init.file.ui.components.InitSegmentedProgressBar
import com.init.file.ui.components.InitTopBar
import com.init.file.ui.components.PermissionRationaleBanner
import com.init.file.ui.screens.browse.BatchActionBar
import com.init.file.ui.screens.browse.ClipboardBanner
import com.init.file.ui.screens.analyzer.JunkCleanerCard
import com.init.file.ui.screens.analyzer.LargestFileRow
import com.init.file.ui.screens.analyzer.StorageUsageBreakdownCard
import com.init.file.ui.screens.browse.FileItemGridCell
import com.init.file.ui.screens.browse.FileItemRow
import com.init.file.ui.screens.home.CategoryCard
import com.init.file.ui.screens.home.CategorySummary
import com.init.file.ui.screens.home.PinnedFolderCard
import com.init.file.ui.screens.home.RecentFileRow
import com.init.file.ui.screens.home.StorageDriveCard
import com.init.file.ui.screens.preview.CodeTextPreview
import com.init.file.ui.screens.preview.PreviewUiState
import com.init.file.ui.screens.preview.TechnicalAttributesCard
import com.init.file.ui.screens.search.SearchFilterChip
import com.init.file.ui.screens.search.SearchResultRow
import com.init.file.ui.screens.splash.SplashScreen

private val sampleFile = FileItem(
    id = "1",
    name = "kernel_panic_dump_2026.log",
    path = "/storage/emulated/0/Download/kernel_panic_dump_2026.log",
    sizeBytes = 4194304L,
    lastModified = 1700000000000L,
    isDirectory = false,
    mimeType = "text/plain",
    extension = "log",
    category = FileCategory.DOCUMENTS
)

private val sampleFolder = FileItem(
    id = "2",
    name = "Android_OS_Builds",
    path = "/storage/emulated/0/Android_OS_Builds",
    sizeBytes = 0L,
    lastModified = 1700000000000L,
    isDirectory = true,
    childrenCount = 42
)

private val sampleVolume = StorageVolumeInfo(
    id = "vol1",
    name = "INTERNAL STORAGE",
    path = "/storage/emulated/0",
    totalBytes = 256L * 1024L * 1024L * 1024L,
    freeBytes = 64L * 1024L * 1024L * 1024L
)

@Preview(name = "TopBar & Breadcrumbs", showBackground = true)
@Composable
fun PreviewTopBar() {
    InitTheme(darkTheme = true) {
        Column(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
            InitTopBar(title = "_init_ /files", subtitle = "SYSTEM UTILITY SUITE")
            Spacer(modifier = Modifier.height(8.dp))
            InitBreadcrumbs(
                currentPath = "/storage/emulated/0/Download/Archives",
                onNavigateToPath = {}
            )
        }
    }
}

@Preview(name = "Storage Drive & Progress", showBackground = true)
@Composable
fun PreviewStorageDrive() {
    InitTheme(darkTheme = true) {
        Column(modifier = Modifier.padding(16.dp)) {
            StorageDriveCard(volume = sampleVolume, onClick = {})
            Spacer(modifier = Modifier.height(12.dp))
            InitSegmentedProgressBar(progress = 0.88f)
        }
    }
}

@Preview(name = "File Rows & Grid Cell", showBackground = true)
@Composable
fun PreviewFileItems() {
    InitTheme(darkTheme = true) {
        Column(modifier = Modifier.padding(16.dp)) {
            FileItemRow(
                item = sampleFile,
                isSelected = false,
                isMultiSelectMode = false,
                onClick = {},
                onLongClick = {},
                onRename = {},
                onDelete = {},
                onShare = {}
            )
            Spacer(modifier = Modifier.height(8.dp))
            FileItemRow(
                item = sampleFolder,
                isSelected = true,
                isMultiSelectMode = true,
                onClick = {},
                onLongClick = {},
                onRename = {},
                onDelete = {},
                onShare = {}
            )
            Spacer(modifier = Modifier.height(12.dp))
            FileItemGridCell(
                item = sampleFile,
                isSelected = false,
                isMultiSelectMode = false,
                onClick = {},
                onLongClick = {}
            )
        }
    }
}

@Preview(name = "Batch Action & Clipboard Banners", showBackground = true)
@Composable
fun PreviewActionBars() {
    InitTheme(darkTheme = true) {
        Column(modifier = Modifier.padding(16.dp)) {
            BatchActionBar(
                selectedCount = 3,
                onCopy = {},
                onMove = {},
                onDelete = {},
                onZip = {},
                onShare = {},
                onInvert = {}
            )
            Spacer(modifier = Modifier.height(12.dp))
            ClipboardBanner(
                itemsCount = 5,
                operation = FileOperation.COPY,
                onPaste = {},
                onCancel = {}
            )
        }
    }
}

@Preview(name = "Home Category & Pinned Cards", showBackground = true)
@Composable
fun PreviewHomeCards() {
    InitTheme(darkTheme = true) {
        Column(modifier = Modifier.padding(16.dp)) {
            CategoryCard(
                summary = CategorySummary(FileCategory.IMAGES, 1420, 3500000000L),
                onClick = {}
            )
            Spacer(modifier = Modifier.height(8.dp))
            PinnedFolderCard(folder = sampleFolder, onClick = {})
            Spacer(modifier = Modifier.height(8.dp))
            RecentFileRow(file = sampleFile, onClick = {})
        }
    }
}

@Preview(name = "Storage Analyzer Breakdown", showBackground = true)
@Composable
fun PreviewStorageBreakdown() {
    val breakdown = StorageBreakdown(
        volumeInfo = sampleVolume,
        imagesBytes = 40L * 1024L * 1024L * 1024L,
        videosBytes = 90L * 1024L * 1024L * 1024L,
        audioBytes = 15L * 1024L * 1024L * 1024L,
        documentsBytes = 10L * 1024L * 1024L * 1024L,
        apksBytes = 5L * 1024L * 1024L * 1024L,
        archivesBytes = 8L * 1024L * 1024L * 1024L,
        downloadsBytes = 12L * 1024L * 1024L * 1024L,
        otherBytes = 12L * 1024L * 1024L * 1024L,
        largestFiles = listOf(sampleFile),
        largestFolders = listOf(sampleFolder),
        junkFiles = listOf(sampleFile),
        junkTotalBytes = 524288000L
    )

    InitTheme(darkTheme = true) {
        Column(modifier = Modifier.padding(16.dp)) {
            StorageUsageBreakdownCard(breakdown = breakdown)
            Spacer(modifier = Modifier.height(12.dp))
            JunkCleanerCard(breakdown = breakdown, isCleaning = false, onClean = {})
            Spacer(modifier = Modifier.height(8.dp))
            LargestFileRow(rank = 1, item = sampleFile, onClick = {})
        }
    }
}

@Preview(name = "File Details & Code Preview", showBackground = true)
@Composable
fun PreviewFileDetails() {
    InitTheme(darkTheme = true) {
        Column(modifier = Modifier.padding(16.dp)) {
            CodeTextPreview(
                textContent = "fun main() {\n    println(\"SYS_READY [OK]\")\n    initFilesBootSequence()\n}",
                isLoading = false
            )
            Spacer(modifier = Modifier.height(12.dp))
            TechnicalAttributesCard(
                fileItem = sampleFile,
                state = PreviewUiState(
                    fileItem = sampleFile,
                    sha256Hash = "E3B0C44298FC1C149AFBF4C8996FB92427AE41E4649B934CA495991B7852B855",
                    md5Hash = "D41D8CD98F00B204E9800998ECF8427E"
                )
            )
        }
    }
}

@Preview(name = "Splash Screen", showBackground = true)
@Composable
fun PreviewSplashScreen() {
    InitTheme(darkTheme = true) {
        SplashScreen(onSplashFinished = {})
    }
}
