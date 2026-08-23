package com.init.files.ui.screens.analyzer

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.init.files.R
import com.init.files.data.storage.openFileWithSystem
import com.init.files.domain.model.FileItem
import com.init.files.domain.model.StorageBreakdown
import com.init.files.domain.model.formatByteSize
import com.init.files.theme.CategoryApksColor
import com.init.files.theme.CategoryArchivesColor
import com.init.files.theme.CategoryAudioColor
import com.init.files.theme.CategoryDocsColor
import com.init.files.theme.CategoryDownloadsColor
import com.init.files.theme.CategoryImagesColor
import com.init.files.theme.CategoryOtherColor
import com.init.files.theme.CategoryVideosColor
import com.init.files.theme.JetBrainsMonoFontFamily
import com.init.files.theme.MichromaFontFamily
import com.init.files.theme.SignalAccent
import com.init.files.ui.components.ConfirmationDialog
import com.init.files.ui.components.InitBadge
import com.init.files.ui.components.InitButton
import com.init.files.ui.components.InitCard
import com.init.files.ui.components.InitDropdownMenu
import com.init.files.ui.components.InitSectionHeader
import com.init.files.ui.components.InitTopBar
import com.init.files.ui.components.InitVideoLoading
import com.init.files.ui.screens.browse.getFileIcon
import java.io.File

@Composable
fun StorageAnalyzerScreen(
    viewModel: AnalyzerViewModel,
    onNavigateBack: () -> Unit,
    onOpenFilePreview: (FileItem) -> Unit,
    onNavigateToFolder: (String) -> Unit,
    onNavigateToDuplicateFinder: () -> Unit = {}
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var itemToDelete by remember { mutableStateOf<FileItem?>(null) }

    LaunchedEffect(state.statusMessage) {
        state.statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearStatusMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            InitTopBar(
                title = "storage analyzer",
                subtitle = state.selectedVolume?.name?.lowercase() ?: "profile",
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadVolumesAndAnalyze() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = MaterialTheme.colorScheme.onSurface)
                    }
                }
            )
        }
    ) { paddingValues ->
        if (state.isAnalyzing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                InitVideoLoading(
                    size = 80.dp,
                    label = "ANALYZING STORAGE STRUCTURE..."
                )
            }
        } else {
            val breakdown = state.breakdown
            if (breakdown == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No volume selected", fontFamily = JetBrainsMonoFontFamily, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                var showJunkConfirm by remember { mutableStateOf(false) }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .background(MaterialTheme.colorScheme.background),
                    contentPadding = PaddingValues(bottom = 48.dp)
                ) {
                    // Breakdown Bar Section
                    item {
                        InitSectionHeader(title = "category distribution")
                        StorageUsageBreakdownCard(breakdown = breakdown)
                    }

                    // Junk Cleaner Section
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        InitSectionHeader(title = "cleanable junk / cache")
                        JunkCleanerCard(
                            breakdown = breakdown,
                            isCleaning = state.isCleaning,
                            onClean = { showJunkConfirm = true }
                        )
                    }

                    // Duplicate Cleaner Section
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        InitSectionHeader(title = "duplicate file cleanup")
                        InitCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            onClick = onNavigateToDuplicateFinder
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "DUPLICATE FINDER",
                                        fontFamily = MichromaFontFamily,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Scan storage for byte-for-byte identical files and reclaim wasted space.",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                InitButton(
                                    text = "SCAN",
                                    isPrimary = true,
                                    onClick = onNavigateToDuplicateFinder
                                )
                            }
                        }
                    }

                    // Largest Files Section
                    if (breakdown.largestFiles.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            InitSectionHeader(
                                title = stringResource(R.string.section_largest_files),
                                badgeText = "TOP ${breakdown.largestFiles.size}"
                            )
                        }

                        itemsIndexed(breakdown.largestFiles) { index, item ->
                            LargestFileRow(
                                rank = index + 1,
                                item = item,
                                onClick = { onOpenFilePreview(item) },
                                onInfoClick = { onOpenFilePreview(item) },
                                onNavigateToFolder = onNavigateToFolder,
                                onDelete = { itemToDelete = item }
                            )
                        }
                    }

                    // Largest Folders Section
                    if (breakdown.largestFolders.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            InitSectionHeader(
                                title = stringResource(R.string.section_largest_folders),
                                badgeText = "TOP ${breakdown.largestFolders.size}"
                            )
                        }

                        itemsIndexed(breakdown.largestFolders) { index, folder ->
                            LargestFolderRow(
                                rank = index + 1,
                                folder = folder,
                                onClick = { onNavigateToFolder(folder.path) },
                                onDelete = { itemToDelete = folder }
                            )
                        }
                    }
                }

                if (showJunkConfirm) {
                    ConfirmationDialog(
                        title = "PURGE JUNK FILES",
                        message = "Move ${breakdown.junkFiles.size} junk, cache, and temporary files (${formatByteSize(breakdown.junkTotalBytes)}) to Trash?",
                        confirmText = "PURGE",
                        isDestructive = true,
                        onDismiss = { showJunkConfirm = false },
                        onConfirm = {
                            showJunkConfirm = false
                            viewModel.performCleanJunk()
                        }
                    )
                }

                if (itemToDelete != null) {
                    ConfirmationDialog(
                        title = "MOVE TO TRASH",
                        message = "Move '${itemToDelete?.name}' to Trash? You can restore it anytime from the Trash screen.",
                        confirmText = "MOVE TO TRASH",
                        isDestructive = true,
                        onDismiss = { itemToDelete = null },
                        onConfirm = {
                            itemToDelete?.let { viewModel.moveToTrash(it) }
                            itemToDelete = null
                        }
                    )
                }
            }
        }
    }
}

/**
 * Flat monochrome stacked category distribution card.
 */
@Composable
fun StorageUsageBreakdownCard(
    breakdown: StorageBreakdown
) {
    val total = breakdown.volumeInfo.totalBytes.toFloat()
    val used = breakdown.volumeInfo.usedBytes.toFloat()

    InitCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header summary
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${breakdown.volumeInfo.formattedUsed} USED",
                    fontFamily = MichromaFontFamily,
                    fontSize = 13.sp,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${breakdown.volumeInfo.formattedTotal} TOTAL",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Multi-segment stacked horizontal bar in pure monochrome
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            ) {
                val segments = listOf(
                    breakdown.imagesBytes to CategoryImagesColor,
                    breakdown.videosBytes to CategoryVideosColor,
                    breakdown.audioBytes to CategoryAudioColor,
                    breakdown.documentsBytes to CategoryDocsColor,
                    breakdown.apksBytes to CategoryApksColor,
                    breakdown.archivesBytes to CategoryArchivesColor,
                    breakdown.downloadsBytes to CategoryDownloadsColor,
                    breakdown.otherBytes to CategoryOtherColor
                )

                for ((bytes, color) in segments) {
                    if (bytes > 0 && total > 0) {
                        val weight = (bytes.toFloat() / total).coerceAtLeast(0.005f)
                        Box(
                            modifier = Modifier
                                .weight(weight)
                                .height(14.dp)
                                .background(color)
                        )
                    }
                }

                // Free space segment
                val freeBytes = breakdown.volumeInfo.freeBytes
                if (freeBytes > 0 && total > 0) {
                    val weight = (freeBytes.toFloat() / total).coerceAtLeast(0.01f)
                    Box(
                        modifier = Modifier
                            .weight(weight)
                            .height(14.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Breakdown legend rows
            val legendItems = listOf(
                Triple("IMAGES", breakdown.imagesBytes, CategoryImagesColor),
                Triple("VIDEOS", breakdown.videosBytes, CategoryVideosColor),
                Triple("AUDIO", breakdown.audioBytes, CategoryAudioColor),
                Triple("DOCS", breakdown.documentsBytes, CategoryDocsColor),
                Triple("APKS", breakdown.apksBytes, CategoryApksColor),
                Triple("ARCHIVES", breakdown.archivesBytes, CategoryArchivesColor),
                Triple("DOWNLOADS", breakdown.downloadsBytes, CategoryDownloadsColor),
                Triple("OTHER", breakdown.otherBytes, CategoryOtherColor)
            ).filter { it.second > 0 }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                legendItems.chunked(2).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        rowItems.forEach { (label, bytes, color) ->
                            BreakdownLegendItem(
                                label = label,
                                sizeFormatted = formatByteSize(bytes),
                                color = color,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (rowItems.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BreakdownLegendItem(
    label: String,
    sizeFormatted: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, RoundedCornerShape(1.dp))
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = sizeFormatted,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * Junk file cleaner card.
 */
@Composable
fun JunkCleanerCard(
    breakdown: StorageBreakdown,
    isCleaning: Boolean,
    onClean: () -> Unit
) {
    InitCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CleaningServices,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SAFE CACHE / TEMP",
                        fontFamily = MichromaFontFamily,
                        fontSize = 12.sp,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                InitBadge(
                    text = "${breakdown.junkFiles.size} FILES",
                    isAccent = breakdown.junkFiles.isNotEmpty()
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = if (breakdown.junkFiles.isNotEmpty())
                    "Identified ${formatByteSize(breakdown.junkTotalBytes)} of temporary files, empty directories, and cache that can be safely moved to Trash."
                else
                    "Storage is clean. No temporary cache or empty directories detected.",
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (breakdown.junkFiles.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                InitButton(
                    text = if (isCleaning) "PURGING..." else "PURGE ${formatByteSize(breakdown.junkTotalBytes)}",
                    leadingIcon = Icons.Default.CleaningServices,
                    isPrimary = true,
                    enabled = !isCleaning,
                    onClick = onClean
                )
            }
        }
    }
}

/**
 * Largest file ranking row with quick open, details, and delete actions.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LargestFileRow(
    rank: Int,
    item: FileItem,
    onClick: () -> Unit,
    onInfoClick: () -> Unit = {},
    onNavigateToFolder: (String) -> Unit = {},
    onDelete: () -> Unit = {}
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp)
            .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = { menuExpanded = true }
            ),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = String.format("%02d", rank),
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = if (rank <= 3) SignalAccent else MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.width(10.dp))

            Icon(
                imageVector = getFileIcon(item),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(18.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 12.sp,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = item.path,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 9.sp,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            InitBadge(
                text = item.formattedSize,
                isAccent = rank <= 3
            )

            Spacer(modifier = Modifier.width(4.dp))

            // Move to Trash button
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Move to Trash",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }

            // 3-dot dropdown menu
            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More Options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }

                InitDropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = {
                            Text("OPEN", fontFamily = JetBrainsMonoFontFamily, fontSize = 11.sp)
                        },
                        leadingIcon = {
                            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        onClick = {
                            menuExpanded = false
                            onClick()
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Text("SHOW IN FOLDER", fontFamily = JetBrainsMonoFontFamily, fontSize = 11.sp)
                        },
                        leadingIcon = {
                            Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        onClick = {
                            menuExpanded = false
                            val parent = File(item.path).parent ?: item.path
                            onNavigateToFolder(parent)
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Text("DETAILS / INFO", fontFamily = JetBrainsMonoFontFamily, fontSize = 11.sp)
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        onClick = {
                            menuExpanded = false
                            onInfoClick()
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Text("MOVE TO TRASH", fontFamily = JetBrainsMonoFontFamily, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        },
                        leadingIcon = {
                            Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

/**
 * Largest folder ranking row with quick browse and delete actions.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LargestFolderRow(
    rank: Int,
    folder: FileItem,
    onClick: () -> Unit,
    onDelete: () -> Unit = {}
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp)
            .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = { menuExpanded = true }
            ),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = String.format("%02d", rank),
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = if (rank <= 3) SignalAccent else MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.width(10.dp))

            Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(18.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = folder.name,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 12.sp,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = folder.path,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 9.sp,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            InitBadge(
                text = if (folder.sizeBytes > 0) folder.formattedSize else "DIR",
                isAccent = rank <= 3
            )

            Spacer(modifier = Modifier.width(4.dp))

            // Move to Trash button
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Move to Trash",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }

            // 3-dot dropdown menu
            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More Options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }

                InitDropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = {
                            Text("BROWSE FOLDER", fontFamily = JetBrainsMonoFontFamily, fontSize = 11.sp)
                        },
                        leadingIcon = {
                            Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        onClick = {
                            menuExpanded = false
                            onClick()
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Text("MOVE TO TRASH", fontFamily = JetBrainsMonoFontFamily, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        },
                        leadingIcon = {
                            Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}
