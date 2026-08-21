package com.init.files.ui.screens.category

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.init.files.R
import com.init.files.data.storage.openFileWithSystem
import com.init.files.domain.model.FileCategory
import com.init.files.domain.model.FileItem
import com.init.files.domain.model.ViewMode
import com.init.files.theme.JetBrainsMonoFontFamily
import com.init.files.theme.MichromaFontFamily
import com.init.files.ui.components.ConfirmationDialog
import com.init.files.ui.components.DotMatrixEmptyPattern
import com.init.files.ui.components.InitButton
import com.init.files.ui.components.InitTopBar
import com.init.files.ui.components.InitVideoLoading
import com.init.files.ui.components.PermissionRationaleBanner
import com.init.files.ui.components.SortBottomSheet
import com.init.files.ui.components.hasStoragePermission
import com.init.files.ui.components.requestStoragePermission
import com.init.files.ui.screens.browse.BatchActionBar
import com.init.files.ui.screens.browse.FileItemGridCell
import com.init.files.ui.screens.browse.FileItemRow
import com.init.files.ui.screens.browse.shareSelectedFiles

@Composable
fun CategoryScreen(
    category: FileCategory,
    viewModel: CategoryViewModel,
    onNavigateBack: () -> Unit,
    onOpenFilePreview: (FileItem) -> Unit
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showSortSheet by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var hasPermission by remember { mutableStateOf(hasStoragePermission(context)) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        val granted = hasStoragePermission(context)
        hasPermission = granted
        if (granted) {
            viewModel.refresh()
        }
    }

    LaunchedEffect(category) {
        viewModel.setCategory(category)
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
                title = if (state.isMultiSelectMode) "${state.selectedItems.size} SELECTED" else category.label,
                subtitle = "${state.items.size} ITEMS",
                navigationIcon = {
                    IconButton(onClick = {
                        if (state.isMultiSelectMode) viewModel.clearSelection() else onNavigateBack()
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
                        IconButton(onClick = { viewModel.toggleViewMode() }) {
                            Icon(
                                imageVector = if (state.viewMode == ViewMode.LIST) Icons.Default.GridView else Icons.AutoMirrored.Filled.ViewList,
                                contentDescription = "Toggle View",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(onClick = { showSortSheet = true }) {
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
                    onCopy = {},
                    onMove = {},
                    onDelete = { showDeleteConfirm = true },
                    onZip = {},
                    onShare = { shareSelectedFiles(context, state.selectedItems.toList()) },
                    onInfo = if (state.selectedItems.size == 1) {
                        { onOpenFilePreview(state.selectedItems.first()) }
                    } else null,
                    onInvert = {}
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (!hasPermission) {
                Box(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                    PermissionRationaleBanner(
                        onRequestPermission = { requestStoragePermission(context) }
                    )
                }
            }

            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    InitVideoLoading(size = 72.dp, label = "INDEXING ${category.label}...")
                }
            } else if (state.items.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
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
                                text = "Grant All Files Access permission to scan and display ${category.label.lowercase()}.",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 11.sp,
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
                                text = "NO ${category.label} FOUND",
                                fontFamily = MichromaFontFamily,
                                fontSize = 14.sp,
                                letterSpacing = 1.2.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            } else {
                if (state.viewMode == ViewMode.LIST) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(state.items, key = { it.path }) { item ->
                            val isSelected = state.selectedItems.contains(item)
                            FileItemRow(
                                item = item,
                                isSelected = isSelected,
                                isMultiSelectMode = state.isMultiSelectMode,
                                onClick = {
                                    if (state.isMultiSelectMode) {
                                        viewModel.toggleSelection(item)
                                    } else {
                                        viewModel.recordFileAccess(item)
                                        onOpenFilePreview(item)
                                    }
                                },
                                onLongClick = { viewModel.toggleSelection(item) },
                                onRename = {},
                                onDelete = { viewModel.toggleSelection(item); showDeleteConfirm = true },
                                onInfo = { onOpenFilePreview(item) },
                                onShare = { shareSelectedFiles(context, listOf(item)) }
                            )
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 100.dp),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(12.dp)
                    ) {
                        items(state.items, key = { it.path }) { item ->
                            val isSelected = state.selectedItems.contains(item)
                            FileItemGridCell(
                                item = item,
                                isSelected = isSelected,
                                isMultiSelectMode = state.isMultiSelectMode,
                                onClick = {
                                    if (state.isMultiSelectMode) {
                                        viewModel.toggleSelection(item)
                                    } else {
                                        viewModel.recordFileAccess(item)
                                        onOpenFilePreview(item)
                                    }
                                },
                                onLongClick = { viewModel.toggleSelection(item) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showSortSheet) {
        SortBottomSheet(
            currentConfig = state.sortConfig,
            onDismiss = { showSortSheet = false },
            onApply = { viewModel.setSortConfig(it) }
        )
    }

    if (showDeleteConfirm) {
        ConfirmationDialog(
            title = stringResource(R.string.dialog_delete_title),
            message = stringResource(R.string.dialog_delete_message, state.selectedItems.size),
            confirmText = stringResource(R.string.action_delete),
            isDestructive = true,
            onDismiss = { showDeleteConfirm = false },
            onConfirm = {
                showDeleteConfirm = false
                viewModel.deleteSelected()
            }
        )
    }
}
