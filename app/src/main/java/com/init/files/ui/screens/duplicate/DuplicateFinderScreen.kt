package com.init.files.ui.screens.duplicate

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.init.files.domain.model.DuplicateGroup
import com.init.files.domain.model.DuplicateScanState
import com.init.files.domain.model.DuplicateSelectFilter
import com.init.files.domain.model.FileCategory
import com.init.files.domain.model.FileItem
import com.init.files.domain.model.formatByteSize
import com.init.files.theme.JetBrainsMonoFontFamily
import com.init.files.theme.MichromaFontFamily
import com.init.files.theme.SignalAccent
import com.init.files.theme.surfaceElevated
import com.init.files.ui.components.ConfirmationDialog
import com.init.files.ui.components.DotMatrixEmptyPattern
import com.init.files.ui.components.InitBadge
import com.init.files.ui.components.InitButton
import com.init.files.ui.components.InitCard
import com.init.files.ui.components.InitTopBar
import kotlinx.coroutines.launch

@Composable
fun DuplicateFinderScreen(
    viewModel: DuplicateViewModel,
    onNavigateBack: () -> Unit,
    onOpenFilePreview: (FileItem) -> Unit
) {
    val scanState by viewModel.scanState.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val selectedPaths by viewModel.selectedPaths.collectAsState()

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            InitTopBar(
                title = "DUPLICATE FINDER",
                subtitle = when (val state = scanState) {
                    is DuplicateScanState.Scanning -> "SCANNING STORAGE..."
                    is DuplicateScanState.Completed -> "${state.groups.size} DUPLICATE SETS IDENTIFIED"
                    else -> "BYTE-EXACT SHA-256 SCAN"
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.startScan() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Rescan",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = scanState) {
                is DuplicateScanState.Idle -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        InitButton(
                            text = "START DUPLICATE SCAN",
                            isPrimary = true,
                            onClick = { viewModel.startScan() }
                        )
                    }
                }

                is DuplicateScanState.Scanning -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            color = SignalAccent,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = "SCANNING & HASHING...",
                            fontFamily = MichromaFontFamily,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${state.filesScanned} files indexed",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (state.currentFileName.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = state.currentFileName,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.outline,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(0.8f)
                            )
                        }
                    }
                }

                is DuplicateScanState.Deleting -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            color = SignalAccent,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = "RECLAIMING STORAGE SPACE...",
                            fontFamily = MichromaFontFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                is DuplicateScanState.Completed -> {
                    if (state.groups.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            DotMatrixEmptyPattern()
                            Spacer(modifier = Modifier.height(20.dp))
                            Text(
                                text = "NO DUPLICATES FOUND",
                                fontFamily = MichromaFontFamily,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Your storage is cleanly optimized. No identical file collisions detected.",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(0.85f)
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            InitButton(
                                text = "RESCAN STORAGE",
                                isPrimary = false,
                                onClick = { viewModel.startScan() }
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Top Summary Metric Card
                            item {
                                InitCard(
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "RECLAIMABLE SPACE",
                                                fontFamily = MichromaFontFamily,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 1.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            InitBadge(
                                                text = "${state.groups.size} SETS",
                                                isAccent = true
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Text(
                                            text = formatByteSize(state.totalWastedBytes),
                                            fontFamily = MichromaFontFamily,
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SignalAccent
                                        )

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Text(
                                            text = "Byte-for-byte exact matches verified via cryptographic SHA-256 digests.",
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            // Category filter chips
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val filterCategories = listOf(
                                        FileCategory.ALL,
                                        FileCategory.IMAGES,
                                        FileCategory.VIDEOS,
                                        FileCategory.AUDIO,
                                        FileCategory.DOCUMENTS,
                                        FileCategory.ARCHIVES
                                    )

                                    for (cat in filterCategories) {
                                        val selected = selectedCategory == cat
                                        FilterChip(
                                            selected = selected,
                                            onClick = { viewModel.setCategoryFilter(cat) },
                                            label = {
                                                Text(
                                                    text = cat.label.uppercase(),
                                                    fontFamily = JetBrainsMonoFontFamily,
                                                    fontSize = 10.sp,
                                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                                )
                                            },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = MaterialTheme.colorScheme.onSurface,
                                                selectedLabelColor = MaterialTheme.colorScheme.surface,
                                                containerColor = MaterialTheme.colorScheme.surfaceElevated,
                                                labelColor = MaterialTheme.colorScheme.onSurface
                                            ),
                                            shape = RoundedCornerShape(10.dp),
                                            border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                        )
                                    }
                                }
                            }

                            // Smart Quick-Select Action Bar
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    QuickSelectButton(
                                        text = "KEEP OLDEST (AUTO)",
                                        onClick = { viewModel.applySelectFilter(DuplicateSelectFilter.KEEP_OLDEST) }
                                    )
                                    QuickSelectButton(
                                        text = "KEEP NEWEST",
                                        onClick = { viewModel.applySelectFilter(DuplicateSelectFilter.KEEP_NEWEST) }
                                    )
                                    QuickSelectButton(
                                        text = "SELECT ALL COPIES",
                                        onClick = { viewModel.applySelectFilter(DuplicateSelectFilter.ALL_DUPLICATES) }
                                    )
                                    QuickSelectButton(
                                        text = "DESELECT",
                                        onClick = { viewModel.applySelectFilter(DuplicateSelectFilter.DESELECT_ALL) }
                                    )
                                }
                            }

                            // Duplicate Groups
                            items(state.groups, key = { it.checksum }) { group ->
                                DuplicateGroupCard(
                                    group = group,
                                    selectedPaths = selectedPaths,
                                    onTogglePath = { viewModel.togglePathSelection(it) },
                                    onPreview = onOpenFilePreview
                                )
                            }
                        }

                        // Floating Reclaim/Delete Action Bar
                        val selectedCount = selectedPaths.size
                        val selectedBytes = state.groups.flatMap { it.allFiles }
                            .filter { selectedPaths.contains(it.path) }
                            .sumOf { it.sizeBytes }

                        AnimatedVisibility(
                            visible = selectedCount > 0,
                            enter = slideInVertically { it } + fadeIn(),
                            exit = slideOutVertically { it } + fadeOut(),
                            modifier = Modifier.align(Alignment.BottomCenter)
                        ) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .navigationBarsPadding()
                                    .padding(16.dp)
                                    .shadow(16.dp, RoundedCornerShape(16.dp))
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f), RoundedCornerShape(16.dp)),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "$selectedCount DUPLICATES SELECTED",
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Reclaim ${formatByteSize(selectedBytes)}",
                                            fontFamily = MichromaFontFamily,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SignalAccent
                                        )
                                    }

                                    InitButton(
                                        text = "CLEAN & RECLAIM",
                                        isPrimary = true,
                                        isDestructive = true,
                                        onClick = { showDeleteConfirmDialog = true }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Confirmation Dialog before deletion
    if (showDeleteConfirmDialog) {
        val selectedCount = selectedPaths.size
        val state = scanState as? DuplicateScanState.Completed
        val selectedBytes = state?.groups?.flatMap { it.allFiles }
            ?.filter { selectedPaths.contains(it.path) }
            ?.sumOf { it.sizeBytes } ?: 0L

        ConfirmationDialog(
            title = "CLEAN DUPLICATE FILES",
            message = "Permanently remove $selectedCount duplicate files to reclaim ${formatByteSize(selectedBytes)} of storage? Original files will remain safe.",
            confirmText = "CLEAN DUPLICATES",
            isDestructive = true,
            onDismiss = { showDeleteConfirmDialog = false },
            onConfirm = {
                showDeleteConfirmDialog = false
                viewModel.deleteSelectedDuplicates(useTrash = false) { count, bytes ->
                    scope.launch {
                        snackbarHostState.showSnackbar("Reclaimed ${formatByteSize(bytes)} ($count duplicates removed)")
                    }
                }
            }
        )
    }
}

@Composable
fun QuickSelectButton(
    text: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .height(32.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceElevated,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun DuplicateGroupCard(
    group: DuplicateGroup,
    selectedPaths: Set<String>,
    onTogglePath: (String) -> Unit,
    onPreview: (FileItem) -> Unit
) {
    InitCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Group header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(32.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceElevated,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = getCategoryIcon(group.primaryFile.category),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = group.primaryFile.name,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${group.totalFilesCount} copies • ${group.formattedSingleSize} each",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                InitBadge(
                    text = "WASTE ${group.formattedWastedSize}",
                    isAccent = true
                )
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                thickness = 1.dp
            )

            // List of copies within this group
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                for (file in group.allFiles) {
                    val isPrimary = file.path == group.primaryFile.path
                    val isChecked = selectedPaths.contains(file.path)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onTogglePath(file.path) }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isChecked) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            contentDescription = if (isChecked) "Selected" else "Unselected",
                            tint = if (isChecked) SignalAccent else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(20.dp)
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isPrimary) {
                                    InitBadge(
                                        text = "ORIGINAL",
                                        isAccent = true
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                Text(
                                    text = file.formattedDate,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = file.path,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.outline,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        IconButton(
                            onClick = { onPreview(file) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = "Preview",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun getCategoryIcon(category: FileCategory): ImageVector {
    return when (category) {
        FileCategory.IMAGES -> Icons.Default.Image
        FileCategory.VIDEOS -> Icons.Default.Movie
        FileCategory.AUDIO -> Icons.Default.MusicNote
        FileCategory.DOCUMENTS -> Icons.Default.Description
        else -> Icons.AutoMirrored.Filled.InsertDriveFile
    }
}
