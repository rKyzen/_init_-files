package com.init.files.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.init.files.R
import com.init.files.data.storage.openFileWithSystem
import com.init.files.domain.model.FileCategory
import com.init.files.domain.model.FileItem
import com.init.files.domain.model.StorageVolumeInfo
import com.init.files.domain.model.formatByteSize
import com.init.files.theme.JetBrainsMonoFontFamily
import com.init.files.theme.MichromaFontFamily
import com.init.files.theme.SignalAccent
import com.init.files.ui.components.InitBadge
import com.init.files.ui.components.InitCard
import com.init.files.ui.components.InitSectionHeader
import com.init.files.ui.components.InitSegmentedProgressBar
import com.init.files.ui.components.PermissionRationaleBanner
import com.init.files.ui.components.PermissionRationaleDialog
import com.init.files.ui.components.hasStoragePermission
import com.init.files.ui.components.requestStoragePermission
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToBrowse: (String) -> Unit,
    onNavigateToCategory: (FileCategory) -> Unit,
    onNavigateToTrash: () -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToAnalyzer: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onOpenFilePreview: (FileItem) -> Unit
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()
    var hasPermission by remember { mutableStateOf(hasStoragePermission(context)) }
    var showPermissionDialog by remember { mutableStateOf(!hasPermission) }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        val granted = hasStoragePermission(context)
        hasPermission = granted
        if (granted) {
            showPermissionDialog = false
            viewModel.refresh()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = drawerState.isOpen,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                drawerContentColor = MaterialTheme.colorScheme.onSurface,
                drawerShape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp),
                modifier = Modifier
                    .width(300.dp)
                    .fillMaxHeight()
                    .border(width = 1.dp, color = MaterialTheme.colorScheme.outlineVariant, shape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                ) {
                    // Header
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 20.dp)
                    ) {
                        Text(
                            text = "/files",
                            fontFamily = MichromaFontFamily,
                            fontSize = 22.sp,
                            letterSpacing = 1.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "system utility subsystem",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 10.sp,
                            letterSpacing = 1.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)

                    Spacer(modifier = Modifier.height(12.dp))

                    // Navigation Items
                    HomeDrawerItem(
                        label = "storage analytics",
                        icon = Icons.Default.DataUsage,
                        onClick = {
                            scope.launch { drawerState.close() }
                            onNavigateToAnalyzer()
                        }
                    )

                    HomeDrawerItem(
                        label = "trash",
                        icon = Icons.Default.DeleteOutline,
                        badgeText = if (state.trashCount > 0) "${state.trashCount}" else null,
                        onClick = {
                            scope.launch { drawerState.close() }
                            onNavigateToTrash()
                        }
                    )

                    HomeDrawerItem(
                        label = "settings",
                        icon = Icons.Default.Settings,
                        onClick = {
                            scope.launch { drawerState.close() }
                            onNavigateToSettings()
                        }
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    // Storage Bar Footer in Drawer
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
                    val primaryVolume = state.volumes.firstOrNull()
                    if (primaryVolume != null) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    scope.launch { drawerState.close() }
                                    onNavigateToBrowse(primaryVolume.path)
                                },
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 18.dp, vertical = 16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Storage,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "internal storage",
                                            fontFamily = MichromaFontFamily,
                                            fontSize = 11.sp,
                                            letterSpacing = 0.8.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Text(
                                        text = "${(primaryVolume.usagePercentage * 100).toInt()}%",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = if (primaryVolume.usagePercentage >= 0.85f) SignalAccent else MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                InitSegmentedProgressBar(
                                    progress = primaryVolume.usagePercentage,
                                    segments = 18
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = "${primaryVolume.formattedUsed} / ${primaryVolume.formattedTotal} • ${primaryVolume.formattedFree} free",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Menu Button before /files
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Menu",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // /files Title
                        Text(
                            text = "/files",
                            fontFamily = MichromaFontFamily,
                            fontSize = 15.sp,
                            letterSpacing = 0.5.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(end = 12.dp)
                        )

                        // Search fills the rest of the space
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(onClick = onNavigateToSearch),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "search files...",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(6.dp))
                    }
                }
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background),
                contentPadding = PaddingValues(start = 0.dp, end = 0.dp, top = 0.dp, bottom = 48.dp)
            ) {
                // Storage Access Required Warning Banner if not granted
                if (!hasPermission) {
                    item {
                        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                            PermissionRationaleBanner(
                                onRequestPermission = { requestStoragePermission(context) }
                            )
                        }
                    }
                }

                // Storage Drives Section
                item {
                    InitSectionHeader(title = stringResource(R.string.section_storage))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        state.volumes.forEach { volume ->
                            StorageDriveCard(
                                volume = volume,
                                onClick = { onNavigateToBrowse(volume.path) }
                            )
                        }
                    }
                }

                // Categories Section (Balanced 2x4 grid including TRASH)
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    InitSectionHeader(title = stringResource(R.string.section_categories))
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        val catMap = state.categories.associateBy { it.category }

                        // Row 1: IMAGES, VIDEOS
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CategoryCard(
                                summary = catMap[FileCategory.IMAGES] ?: CategorySummary(FileCategory.IMAGES),
                                modifier = Modifier.weight(1f),
                                onClick = { onNavigateToCategory(FileCategory.IMAGES) }
                            )
                            CategoryCard(
                                summary = catMap[FileCategory.VIDEOS] ?: CategorySummary(FileCategory.VIDEOS),
                                modifier = Modifier.weight(1f),
                                onClick = { onNavigateToCategory(FileCategory.VIDEOS) }
                            )
                        }

                        // Row 2: AUDIO, DOCUMENTS
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CategoryCard(
                                summary = catMap[FileCategory.AUDIO] ?: CategorySummary(FileCategory.AUDIO),
                                modifier = Modifier.weight(1f),
                                onClick = { onNavigateToCategory(FileCategory.AUDIO) }
                            )
                            CategoryCard(
                                summary = catMap[FileCategory.DOCUMENTS] ?: CategorySummary(FileCategory.DOCUMENTS),
                                modifier = Modifier.weight(1f),
                                onClick = { onNavigateToCategory(FileCategory.DOCUMENTS) }
                            )
                        }

                        // Row 3: APKS, ARCHIVES
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CategoryCard(
                                summary = catMap[FileCategory.APKS] ?: CategorySummary(FileCategory.APKS),
                                modifier = Modifier.weight(1f),
                                onClick = { onNavigateToCategory(FileCategory.APKS) }
                            )
                            CategoryCard(
                                summary = catMap[FileCategory.ARCHIVES] ?: CategorySummary(FileCategory.ARCHIVES),
                                modifier = Modifier.weight(1f),
                                onClick = { onNavigateToCategory(FileCategory.ARCHIVES) }
                            )
                        }

                        // Row 4: DOWNLOADS, TRASH
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CategoryCard(
                                summary = catMap[FileCategory.DOWNLOADS] ?: CategorySummary(FileCategory.DOWNLOADS),
                                modifier = Modifier.weight(1f),
                                onClick = { onNavigateToCategory(FileCategory.DOWNLOADS) }
                            )
                            TrashShortcutCard(
                                count = state.trashCount,
                                totalSizeBytes = state.trashSizeBytes,
                                modifier = Modifier.weight(1f),
                                onClick = onNavigateToTrash
                            )
                        }
                    }
                }

                // Pinned Folders Section
                if (state.pinnedFolders.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        InitSectionHeader(
                            title = stringResource(R.string.section_pinned),
                            badgeText = "${state.pinnedFolders.size}"
                        )
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(state.pinnedFolders, key = { it.path }) { folder ->
                                PinnedFolderCard(
                                    folder = folder,
                                    onClick = { onNavigateToBrowse(folder.path) }
                                )
                            }
                        }
                    }
                }

                // Recent Files Section
                if (state.recentFiles.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        InitSectionHeader(
                            title = stringResource(R.string.section_recents),
                            badgeText = "${state.recentFiles.size}"
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            state.recentFiles.take(8).forEach { file ->
                                RecentFileRow(
                                    file = file,
                                    onClick = {
                                        if (file.isDirectory || File(file.path).isDirectory) {
                                            onNavigateToBrowse(file.path)
                                        } else {
                                            onOpenFilePreview(file)
                                        }
                                    },
                                    onInfoClick = { onOpenFilePreview(file) }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showPermissionDialog && !hasPermission) {
            PermissionRationaleDialog(
                onDismiss = { showPermissionDialog = false },
                onGrant = {
                    showPermissionDialog = false
                    requestStoragePermission(context)
                }
            )
        }
    }
}

/**
 * Drawer navigation row.
 */
@Composable
private fun HomeDrawerItem(
    label: String,
    icon: ImageVector,
    badgeText: String? = null,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = label,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                letterSpacing = 0.8.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            if (badgeText != null) {
                InitBadge(text = badgeText, isAccent = true)
            }
        }
    }
}

/**
 * Storage Drive capacity card with segmented bar.
 */
@Composable
fun StorageDriveCard(
    volume: StorageVolumeInfo,
    onClick: () -> Unit
) {
    InitCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Icon(
                        imageVector = Icons.Default.Storage,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = volume.name.lowercase(),
                        fontFamily = MichromaFontFamily,
                        fontSize = 13.sp,
                        letterSpacing = 1.sp,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            InitSegmentedProgressBar(
                progress = volume.usagePercentage,
                segments = 24
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${volume.formattedUsed} used of ${volume.formattedTotal}",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 11.sp,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${(volume.usagePercentage * 100).toInt()}%",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    maxLines = 1,
                    softWrap = false,
                    color = if (volume.usagePercentage >= 0.85f) SignalAccent else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

/**
 * Category shortcut tile.
 */
@Composable
fun CategoryCard(
    summary: CategorySummary,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val icon = when (summary.category) {
        FileCategory.IMAGES -> Icons.Default.Image
        FileCategory.VIDEOS -> Icons.Default.Movie
        FileCategory.AUDIO -> Icons.Default.MusicNote
        FileCategory.DOCUMENTS -> Icons.Default.Description
        FileCategory.APKS -> Icons.AutoMirrored.Filled.InsertDriveFile
        FileCategory.ARCHIVES -> Icons.Default.Archive
        FileCategory.DOWNLOADS -> Icons.Default.Download
        else -> Icons.Default.Folder
    }

    InitCard(
        modifier = modifier,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = summary.category.label,
                    fontFamily = MichromaFontFamily,
                    fontSize = 11.sp,
                    letterSpacing = 0.8.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "${summary.count} files",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        softWrap = false
                    )
                    if (summary.totalSizeBytes > 0) {
                        Text(
                            text = " • ${formatByteSize(summary.totalSizeBytes)}",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

/**
 * Trash shortcut card on Home.
 */
@Composable
fun TrashShortcutCard(
    count: Int,
    totalSizeBytes: Long,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    InitCard(
        modifier = modifier,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "trash",
                    fontFamily = MichromaFontFamily,
                    fontSize = 11.sp,
                    letterSpacing = 0.8.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (count == 0) "Empty" else "$count items",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        softWrap = false
                    )
                    if (totalSizeBytes > 0) {
                        Text(
                            text = " • ${formatByteSize(totalSizeBytes)}",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

/**
 * Pinned folder shortcut card.
 */
@Composable
fun PinnedFolderCard(
    folder: FileItem,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .width(140.dp)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Icon(
                imageVector = Icons.Default.FolderSpecial,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = folder.name,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "PINNED",
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 9.sp,
                maxLines = 1,
                softWrap = false,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Recent file list item row with direct opening and info action.
 */
@Composable
fun RecentFileRow(
    file: FileItem,
    onClick: () -> Unit,
    onInfoClick: (() -> Unit)? = null
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when (file.category) {
                    FileCategory.IMAGES -> Icons.Default.Image
                    FileCategory.VIDEOS -> Icons.Default.Movie
                    FileCategory.AUDIO -> Icons.Default.MusicNote
                    FileCategory.DOCUMENTS -> Icons.Default.Description
                    else -> Icons.AutoMirrored.Filled.InsertDriveFile
                },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(18.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.name,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 12.sp,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = file.formattedDate,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 9.sp,
                        maxLines = 1,
                        softWrap = false,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = " • ",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 9.sp,
                        maxLines = 1,
                        softWrap = false,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = file.formattedSize,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 9.sp,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (onInfoClick != null) {
                IconButton(
                    onClick = onInfoClick,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Details",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
