package com.init.file.ui.screens.trash

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.SelectAll
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.init.file.domain.model.TrashItem
import com.init.file.domain.model.formatByteSize
import com.init.file.theme.JetBrainsMonoFontFamily
import com.init.file.theme.MichromaFontFamily
import com.init.file.theme.SignalAccent
import com.init.file.ui.components.ConfirmationDialog
import com.init.file.ui.components.DotMatrixEmptyPattern
import com.init.file.ui.components.InitBadge
import com.init.file.ui.components.InitTopBar
import com.init.file.ui.components.InitVideoLoading

@Composable
fun TrashScreen(
    viewModel: TrashViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showEmptyTrashConfirm by remember { mutableStateOf(false) }
    var itemToDeletePermanently by remember { mutableStateOf<TrashItem?>(null) }
    var showDeleteSelectedConfirm by remember { mutableStateOf(false) }

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
                title = if (state.isMultiSelectMode) "${state.selectedItems.size} selected" else "trash",
                subtitle = "${state.items.size} items • ${formatByteSize(state.totalSizeBytes)}",
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
                        if (state.items.isNotEmpty()) {
                            IconButton(onClick = { showEmptyTrashConfirm = true }) {
                                Icon(Icons.Default.DeleteForever, contentDescription = "Empty Trash", tint = MaterialTheme.colorScheme.onSurface)
                            }
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
                TrashBatchActionBar(
                    selectedCount = state.selectedItems.size,
                    onRestore = { viewModel.restoreSelected() },
                    onDeletePermanently = { showDeleteSelectedConfirm = true }
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
            // 30-day auto-purge policy banner
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "items in trash will be permanently deleted after 30 days",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    InitVideoLoading(size = 72.dp, label = "loading trash...")
                }
            } else if (state.items.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                        DotMatrixEmptyPattern()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "trash is empty",
                            fontFamily = MichromaFontFamily,
                            fontSize = 14.sp,
                            letterSpacing = 1.2.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Deleted and purged items will appear here and can be restored.",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 96.dp)
                ) {
                    items(state.items, key = { it.id }) { item ->
                        val isSelected = state.selectedItems.contains(item)
                        TrashItemRow(
                            item = item,
                            isSelected = isSelected,
                            isMultiSelectMode = state.isMultiSelectMode,
                            onClick = {
                                if (state.isMultiSelectMode) {
                                    viewModel.toggleSelection(item)
                                }
                            },
                            onLongClick = { viewModel.toggleSelection(item) },
                            onRestore = { viewModel.restoreItem(item) },
                            onDeletePermanently = { itemToDeletePermanently = item }
                        )
                    }
                }
            }
        }
    }

    if (showEmptyTrashConfirm) {
        ConfirmationDialog(
            title = "EMPTY TRASH",
            message = "All ${state.items.size} items in trash will be permanently erased. This action cannot be undone.",
            confirmText = "EMPTY TRASH",
            isDestructive = true,
            onDismiss = { showEmptyTrashConfirm = false },
            onConfirm = {
                showEmptyTrashConfirm = false
                viewModel.emptyTrash()
            }
        )
    }

    if (itemToDeletePermanently != null) {
        ConfirmationDialog(
            title = "PERMANENTLY DELETE",
            message = "Erase '${itemToDeletePermanently?.name}' permanently from device storage?",
            confirmText = "ERASE",
            isDestructive = true,
            onDismiss = { itemToDeletePermanently = null },
            onConfirm = {
                itemToDeletePermanently?.let { viewModel.permanentlyDeleteItem(it) }
                itemToDeletePermanently = null
            }
        )
    }

    if (showDeleteSelectedConfirm) {
        ConfirmationDialog(
            title = "DELETE SELECTED ITEMS",
            message = "Permanently erase ${state.selectedItems.size} selected items?",
            confirmText = "ERASE ALL",
            isDestructive = true,
            onDismiss = { showDeleteSelectedConfirm = false },
            onConfirm = {
                showDeleteSelectedConfirm = false
                viewModel.deleteSelectedPermanently()
            }
        )
    }
}

/**
 * Industrial technical batch action bar for Trash.
 */
@Composable
fun TrashBatchActionBar(
    selectedCount: Int,
    onRestore: () -> Unit,
    onDeletePermanently: () -> Unit
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
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            InitBadge(
                text = "$selectedCount SELECTED",
                isAccent = true
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Technical RESTORE button
                Surface(
                    modifier = Modifier
                        .border(1.dp, MaterialTheme.colorScheme.onSurface, RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp))
                        .combinedClickable(onClick = onRestore),
                    color = MaterialTheme.colorScheme.onSurface,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Restore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "RESTORE",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 0.8.sp,
                            color = MaterialTheme.colorScheme.surface
                        )
                    }
                }

                // Technical DELETE PERMANENTLY button
                Surface(
                    modifier = Modifier
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp))
                        .combinedClickable(onClick = onDeletePermanently),
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteForever,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "DELETE",
                            fontFamily = MichromaFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 0.8.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

/**
 * Technical Trash item row with monospace typography and industrial action buttons.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TrashItemRow(
    item: TrashItem,
    isSelected: Boolean,
    isMultiSelectMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onRestore: () -> Unit,
    onDeletePermanently: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp)
            .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        color = if (isSelected) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp)
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

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (item.isDirectory) Icons.Default.Folder else Icons.AutoMirrored.Filled.InsertDriveFile,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
            }

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
                Text(
                    text = item.originalPath,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 10.sp,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (item.isDirectory) "dir" else item.formattedSize,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        softWrap = false
                    )
                    Text(
                        text = " • ",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.outline,
                        maxLines = 1,
                        softWrap = false
                    )
                    Text(
                        text = "deleted ${item.formattedDeletedDate}",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        softWrap = false
                    )
                    Text(
                        text = " • ",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.outline,
                        maxLines = 1,
                        softWrap = false
                    )
                    Text(
                        text = item.expiryLabel,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (item.daysRemaining <= 3) SignalAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (!isMultiSelectMode) {
                Spacer(modifier = Modifier.width(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = onRestore,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Restore,
                            contentDescription = "Restore",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onDeletePermanently,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteForever,
                            contentDescription = "Delete Permanently",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
