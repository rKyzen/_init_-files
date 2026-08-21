package com.init.files.ui.screens.browse

import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.init.files.R
import com.init.files.data.storage.openFileWithSystem
import com.init.files.domain.model.FileCategory
import com.init.files.domain.model.FileItem
import com.init.files.domain.model.FileOperation
import com.init.files.domain.model.SortConfig
import com.init.files.domain.model.SortField
import com.init.files.domain.model.SortOrder
import com.init.files.domain.model.ViewMode
import com.init.files.theme.JetBrainsMonoFontFamily
import com.init.files.theme.MichromaFontFamily
import com.init.files.theme.SignalAccent
import com.init.files.theme.surfaceElevated
import com.init.files.ui.components.ConfirmationDialog
import com.init.files.ui.components.DotMatrixEmptyPattern
import com.init.files.ui.components.InitBadge
import com.init.files.ui.components.InitBreadcrumbs
import com.init.files.ui.components.InitButton
import com.init.files.ui.components.InitCard
import com.init.files.ui.components.InitTopBar
import com.init.files.ui.components.InitVideoLoading
import com.init.files.ui.components.OperationProgressDialog
import com.init.files.ui.components.PermissionRationaleBanner
import com.init.files.ui.components.PermissionRationaleDialog
import com.init.files.ui.components.SortBottomSheet
import com.init.files.ui.components.TextInputDialog
import com.init.files.ui.components.hasStoragePermission
import com.init.files.ui.components.requestStoragePermission
import java.io.File

@Composable
fun BrowseScreen(
    viewModel: BrowseViewModel,
    onNavigateBack: () -> Unit,
    onOpenFilePreview: (FileItem) -> Unit,
    onNavigateToSearch: (String) -> Unit,
    initialPath: String? = null
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var hasPermission by remember { mutableStateOf(hasStoragePermission(context)) }
    var showPermissionDialog by remember { mutableStateOf(false) }

    LaunchedEffect(initialPath) {
        if (!initialPath.isNullOrEmpty() && state.currentPath != initialPath) {
            viewModel.navigateTo(initialPath)
        }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        val granted = hasStoragePermission(context)
        hasPermission = granted
        if (granted) {
            showPermissionDialog = false
            viewModel.refresh()
        }
    }

    BackHandler {
        if (state.isMultiSelectMode) {
            viewModel.clearSelection()
        } else if (!viewModel.navigateUp()) {
            onNavigateBack()
        }
    }

    LaunchedEffect(state.snackbarMessage) {
        state.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            InitTopBar(
                title = if (state.isMultiSelectMode) {
                    "${state.selectedItems.size} selected"
                } else {
                    File(state.currentPath).name.ifEmpty { "storage" }.lowercase()
                },
                subtitle = "${state.items.size} items",
                navigationIcon = {
                    IconButton(onClick = {
                        if (state.isMultiSelectMode) {
                            viewModel.clearSelection()
                        } else if (!viewModel.navigateUp()) {
                            onNavigateBack()
                        }
                    }) {
                        Icon(
                            imageVector = if (state.isMultiSelectMode) Icons.Default.Close else Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    if (state.isMultiSelectMode) {
                        IconButton(onClick = { viewModel.selectAll() }) {
                            Icon(Icons.Default.SelectAll, contentDescription = "Select All", tint = MaterialTheme.colorScheme.onSurface)
                        }
                    } else {
                        IconButton(onClick = { viewModel.togglePinCurrentFolder() }) {
                            Icon(
                                imageVector = if (state.isCurrentFolderPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                                contentDescription = "Pin Folder",
                                tint = if (state.isCurrentFolderPinned) SignalAccent else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(onClick = { viewModel.toggleViewMode() }) {
                            Icon(
                                imageVector = if (state.viewMode == ViewMode.LIST) Icons.Default.GridView else Icons.AutoMirrored.Filled.ViewList,
                                contentDescription = "Toggle View",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(onClick = { viewModel.showDialog(BrowseDialogState.Sort) }) {
                            Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort", tint = MaterialTheme.colorScheme.onSurface)
                        }
                        IconButton(onClick = { viewModel.refresh() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            )
        },
        bottomBar = {
            AnimatedVisibility(
                visible = state.isMultiSelectMode,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                BatchActionBar(
                    selectedCount = state.selectedItems.size,
                    onCopy = { viewModel.copySelected() },
                    onMove = { viewModel.moveSelected() },
                    onDelete = { viewModel.showDialog(BrowseDialogState.DeleteConfirm(state.selectedItems.toList())) },
                    onZip = { viewModel.showDialog(BrowseDialogState.ZipPrompt(state.selectedItems.toList())) },
                    onShare = { shareSelectedFiles(context, state.selectedItems.toList()) },
                    onInfo = if (state.selectedItems.size == 1) {
                        { onOpenFilePreview(state.selectedItems.first()) }
                    } else null,
                    onInvert = { viewModel.invertSelection() }
                )
            }

            AnimatedVisibility(
                visible = !state.isMultiSelectMode && state.clipboard != null,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                state.clipboard?.let { clip ->
                    ClipboardBanner(
                        itemsCount = clip.first.size,
                        operation = clip.second,
                        onPaste = { viewModel.pasteClipboard() },
                        onCancel = { viewModel.cancelClipboard() }
                    )
                }
            }
        },
        floatingActionButton = {
            if (!state.isMultiSelectMode && state.clipboard == null) {
                var fabMenuExpanded by remember { mutableStateOf(false) }

                Box(modifier = Modifier.navigationBarsPadding()) {
                    FloatingActionButton(
                        onClick = { fabMenuExpanded = true },
                        containerColor = MaterialTheme.colorScheme.onSurface,
                        contentColor = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(2.dp)
                    ) {
                        Text(
                            text = "+",
                            fontFamily = MichromaFontFamily,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    DropdownMenu(
                        expanded = fabMenuExpanded,
                        onDismissRequest = { fabMenuExpanded = false },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                    ) {
                        DropdownMenuItem(
                            text = { Text("NEW FOLDER", fontFamily = JetBrainsMonoFontFamily, fontSize = 12.sp) },
                            leadingIcon = { Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface) },
                            onClick = {
                                fabMenuExpanded = false
                                viewModel.showDialog(BrowseDialogState.CreateFolder)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("NEW FILE", fontFamily = JetBrainsMonoFontFamily, fontSize = 12.sp) },
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.InsertDriveFile, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface) },
                            onClick = {
                                fabMenuExpanded = false
                                viewModel.showDialog(BrowseDialogState.CreateFile)
                            }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Interactive Breadcrumb bar
            InitBreadcrumbs(
                currentPath = state.currentPath,
                onNavigateToPath = { viewModel.navigateTo(it) }
            )

            // Permission Warning Banner if missing
            if (!hasPermission) {
                Box(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                    PermissionRationaleBanner(
                        onRequestPermission = { requestStoragePermission(context) }
                    )
                }
            }

            // Directional folder navigation transition (Slide in from Right going deeper, Slide in from Left going back)
            AnimatedContent(
                targetState = state.currentPath,
                transitionSpec = {
                    val isForward = targetState.length > initialState.length ||
                        (targetState.startsWith(initialState) && targetState != initialState)
                    if (isForward) {
                        (slideInHorizontally(
                            initialOffsetX = { fullWidth -> fullWidth },
                            animationSpec = tween(260, easing = FastOutSlowInEasing)
                        ) + fadeIn(animationSpec = tween(260)))
                            .togetherWith(
                                slideOutHorizontally(
                                    targetOffsetX = { fullWidth -> -fullWidth / 3 },
                                    animationSpec = tween(260, easing = FastOutSlowInEasing)
                                ) + fadeOut(animationSpec = tween(180))
                            )
                    } else {
                        (slideInHorizontally(
                            initialOffsetX = { fullWidth -> -fullWidth / 3 },
                            animationSpec = tween(260, easing = FastOutSlowInEasing)
                        ) + fadeIn(animationSpec = tween(260)))
                            .togetherWith(
                                slideOutHorizontally(
                                    targetOffsetX = { fullWidth -> fullWidth },
                                    animationSpec = tween(260, easing = FastOutSlowInEasing)
                                ) + fadeOut(animationSpec = tween(180))
                            )
                    }
                },
                label = "directory_transition",
                modifier = Modifier.fillMaxSize()
            ) { _ ->
                if (state.isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        InitVideoLoading(
                            size = 72.dp,
                            label = "FETCHING DIRECTORY..."
                        )
                    }
                } else if (state.items.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            if (!hasPermission) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "STORAGE ACCESS REQUIRED",
                                    fontFamily = MichromaFontFamily,
                                    fontSize = 13.sp,
                                    letterSpacing = 1.2.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Grant All Files Access permission to browse and display storage contents on Android 11+.",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                InitButton(
                                    text = "GRANT STORAGE ACCESS",
                                    isPrimary = true,
                                    onClick = { requestStoragePermission(context) }
                                )
                            } else {
                                DotMatrixEmptyPattern()
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = stringResource(R.string.empty_directory),
                                    fontFamily = MichromaFontFamily,
                                    fontSize = 14.sp,
                                    letterSpacing = 1.2.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "USE + TO CREATE FILES OR DIRECTORIES",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 10.sp,
                                    letterSpacing = 0.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    if (state.viewMode == ViewMode.LIST) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 96.dp)
                        ) {
                            items(
                                items = state.items,
                                key = { it.path }
                            ) { item ->
                                val isSelected = state.selectedItems.contains(item)
                                val isFolder = item.isDirectory || File(item.path).isDirectory
                                FileItemRow(
                                    item = item,
                                    isSelected = isSelected,
                                    isMultiSelectMode = state.isMultiSelectMode,
                                    onClick = {
                                        if (state.isMultiSelectMode) {
                                            viewModel.toggleSelection(item)
                                        } else if (isFolder) {
                                            viewModel.navigateTo(item.path)
                                        } else {
                                            viewModel.recordFileAccess(item)
                                            onOpenFilePreview(item)
                                        }
                                    },
                                    onLongClick = {
                                        viewModel.toggleSelection(item)
                                    },
                                    onRename = {
                                        viewModel.showDialog(BrowseDialogState.Rename(item))
                                    },
                                    onDelete = {
                                        viewModel.showDialog(BrowseDialogState.DeleteConfirm(listOf(item)))
                                    },
                                    onInfo = {
                                        onOpenFilePreview(item)
                                    },
                                    onExtract = if (item.extension == "zip") {
                                        { viewModel.extractZip(item) }
                                    } else null,
                                    onShare = {
                                        shareSelectedFiles(context, listOf(item))
                                    }
                                )
                            }
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 100.dp),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 96.dp)
                        ) {
                            items(
                                items = state.items,
                                key = { it.path }
                            ) { item ->
                                val isSelected = state.selectedItems.contains(item)
                                val isFolder = item.isDirectory || File(item.path).isDirectory
                                FileItemGridCell(
                                    item = item,
                                    isSelected = isSelected,
                                    isMultiSelectMode = state.isMultiSelectMode,
                                    onClick = {
                                        if (state.isMultiSelectMode) {
                                            viewModel.toggleSelection(item)
                                        } else if (isFolder) {
                                            viewModel.navigateTo(item.path)
                                        } else {
                                            viewModel.recordFileAccess(item)
                                            onOpenFilePreview(item)
                                        }
                                    },
                                    onLongClick = {
                                        viewModel.toggleSelection(item)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialogs Handling
    when (val dialog = state.dialogState) {
        is BrowseDialogState.CreateFolder -> {
            TextInputDialog(
                title = stringResource(R.string.dialog_create_folder_title),
                hint = stringResource(R.string.dialog_create_folder_hint),
                confirmButtonText = stringResource(R.string.action_confirm),
                onDismiss = { viewModel.dismissDialog() },
                onConfirm = { viewModel.createFolder(it) }
            )
        }
        is BrowseDialogState.CreateFile -> {
            TextInputDialog(
                title = stringResource(R.string.dialog_create_file_title),
                hint = stringResource(R.string.dialog_create_file_hint),
                confirmButtonText = stringResource(R.string.action_confirm),
                onDismiss = { viewModel.dismissDialog() },
                onConfirm = { viewModel.createFile(it) }
            )
        }
        is BrowseDialogState.Rename -> {
            TextInputDialog(
                title = stringResource(R.string.dialog_rename_title),
                hint = dialog.item.name,
                initialValue = dialog.item.name,
                confirmButtonText = stringResource(R.string.action_rename),
                onDismiss = { viewModel.dismissDialog() },
                onConfirm = { viewModel.renameItem(dialog.item, it) }
            )
        }
        is BrowseDialogState.DeleteConfirm -> {
            ConfirmationDialog(
                title = stringResource(R.string.dialog_delete_title),
                message = stringResource(R.string.dialog_delete_message, dialog.items.size),
                confirmText = stringResource(R.string.action_delete),
                isDestructive = true,
                onDismiss = { viewModel.dismissDialog() },
                onConfirm = { viewModel.deleteSelected() }
            )
        }
        is BrowseDialogState.ZipPrompt -> {
            TextInputDialog(
                title = "CREATE ZIP ARCHIVE",
                hint = "archive_name.zip",
                initialValue = "archive.zip",
                confirmButtonText = "COMPRESS",
                onDismiss = { viewModel.dismissDialog() },
                onConfirm = { viewModel.zipSelected(it) }
            )
        }
        is BrowseDialogState.Sort -> {
            SortBottomSheet(
                currentConfig = state.sortConfig,
                onDismiss = { viewModel.dismissDialog() },
                onApply = { viewModel.setSortConfig(it) }
            )
        }
        BrowseDialogState.None -> {}
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

    // Operation Progress Modal Dialog
    state.operationProgress?.let { progress ->
        OperationProgressDialog(
            progress = progress,
            onCancel = {}
        )
    }
}

/**
 * Technical monochrome file row with fixed-width typography and responsive scaling.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileItemRow(
    item: FileItem,
    isSelected: Boolean,
    isMultiSelectMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onInfo: (() -> Unit)? = null,
    onExtract: (() -> Unit)? = null,
    onShare: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 0.5.dp,
                color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        color = if (isSelected) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isMultiSelectMode) {
                Icon(
                    imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (isSelected) SignalAccent else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
            }

            // Category Outline Glyph Icon
            val iconVector = getFileIcon(item)
            val iconTint = MaterialTheme.colorScheme.onSurface

            Icon(
                imageVector = iconVector,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(22.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = item.formattedDate,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        softWrap = false
                    )
                    Text(
                        text = " • ",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.outline,
                        maxLines = 1,
                        softWrap = false
                    )
                    Text(
                        text = if (item.isDirectory) {
                            if (item.childrenCount != null) "${item.childrenCount} items" else "DIR"
                        } else {
                            item.formattedSize
                        },
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (!isMultiSelectMode) {
                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                    ) {
                        if (onInfo != null) {
                            DropdownMenuItem(
                                text = { Text("FILE INFO / DETAILS", fontFamily = JetBrainsMonoFontFamily, fontSize = 12.sp) },
                                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface) },
                                onClick = {
                                    menuExpanded = false
                                    onInfo()
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("RENAME", fontFamily = JetBrainsMonoFontFamily, fontSize = 12.sp) },
                            onClick = {
                                menuExpanded = false
                                onRename()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("SHARE", fontFamily = JetBrainsMonoFontFamily, fontSize = 12.sp) },
                            onClick = {
                                menuExpanded = false
                                onShare()
                            }
                        )
                        if (onExtract != null) {
                            DropdownMenuItem(
                                text = { Text("EXTRACT ZIP", fontFamily = JetBrainsMonoFontFamily, fontSize = 12.sp) },
                                onClick = {
                                    menuExpanded = false
                                    onExtract()
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("DELETE", fontFamily = JetBrainsMonoFontFamily, fontSize = 12.sp, fontWeight = FontWeight.Bold) },
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
}

/**
 * File grid item cell.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileItemGridCell(
    item: FileItem,
    isSelected: Boolean,
    isMultiSelectMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .padding(4.dp)
            .border(
                width = 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(2.dp)
            )
            .clip(RoundedCornerShape(2.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        color = if (isSelected) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .padding(10.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(2.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getFileIcon(item),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(24.dp)
                )
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.TopEnd
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp).padding(2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = item.name,
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 11.sp,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = if (item.isDirectory) {
                    if (item.childrenCount != null) "${item.childrenCount} items" else "DIR"
                } else item.formattedSize,
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 9.sp,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Bottom action bar for bulk operations with navigation bar insets.
 */
@Composable
fun BatchActionBar(
    selectedCount: Int,
    onCopy: () -> Unit,
    onMove: () -> Unit,
    onDelete: () -> Unit,
    onZip: () -> Unit,
    onShare: () -> Unit,
    onInfo: (() -> Unit)? = null,
    onInvert: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onCopy) {
                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = MaterialTheme.colorScheme.onSurface)
            }
            IconButton(onClick = onMove) {
                Icon(Icons.Default.ContentCut, contentDescription = "Move", tint = MaterialTheme.colorScheme.onSurface)
            }
            IconButton(onClick = onZip) {
                Icon(Icons.Default.Archive, contentDescription = "Zip", tint = MaterialTheme.colorScheme.onSurface)
            }
            IconButton(onClick = onShare) {
                Icon(Icons.Default.Share, contentDescription = "Share", tint = MaterialTheme.colorScheme.onSurface)
            }
            if (onInfo != null) {
                IconButton(onClick = onInfo) {
                    Icon(Icons.Default.Info, contentDescription = "Details", tint = MaterialTheme.colorScheme.onSurface)
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

/**
 * Clipboard banner showing pending copy/move operation with navigation bar insets.
 */
@Composable
fun ClipboardBanner(
    itemsCount: Int,
    operation: FileOperation,
    onPaste: () -> Unit,
    onCancel: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .border(1.dp, MaterialTheme.colorScheme.outline),
        color = MaterialTheme.colorScheme.surfaceElevated
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${if (operation == FileOperation.COPY) "COPY" else "MOVE"} $itemsCount ITEMS",
                    fontFamily = MichromaFontFamily,
                    fontSize = 11.sp,
                    letterSpacing = 0.8.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                InitButton(
                    text = stringResource(R.string.action_cancel),
                    isPrimary = false,
                    onClick = onCancel
                )
                Spacer(modifier = Modifier.width(8.dp))
                InitButton(
                    text = "PASTE",
                    isPrimary = true,
                    leadingIcon = Icons.Default.ContentPaste,
                    onClick = onPaste
                )
            }
        }
    }
}

fun getFileIcon(item: FileItem): ImageVector {
    if (item.isDirectory) return Icons.Default.Folder
    return when (item.category) {
        FileCategory.IMAGES -> Icons.Default.Image
        FileCategory.VIDEOS -> Icons.Default.Movie
        FileCategory.AUDIO -> Icons.Default.MusicNote
        FileCategory.DOCUMENTS -> Icons.Default.Description
        FileCategory.ARCHIVES -> Icons.Default.Archive
        FileCategory.APKS -> Icons.AutoMirrored.Filled.InsertDriveFile
        else -> Icons.AutoMirrored.Filled.InsertDriveFile
    }
}

fun shareSelectedFiles(context: Context, files: List<FileItem>) {
    if (files.isEmpty()) return
    val uris = ArrayList<android.net.Uri>()
    for (f in files) {
        val file = File(f.path)
        if (file.exists()) {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            uris.add(uri)
        }
    }
    if (uris.isEmpty()) return

    val intent = if (uris.size == 1) {
        Intent(Intent.ACTION_SEND).apply {
            type = "*/*"
            putExtra(Intent.EXTRA_STREAM, uris[0])
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    } else {
        Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "*/*"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    try {
        context.startActivity(Intent.createChooser(intent, "Share Files"))
    } catch (_: Exception) {}
}
