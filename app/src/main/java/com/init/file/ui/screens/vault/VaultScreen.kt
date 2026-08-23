package com.init.file.ui.screens.vault

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.init.file.domain.model.FileCategory
import com.init.file.domain.model.FileItem
import com.init.file.domain.model.VaultItem
import com.init.file.domain.model.VaultState
import com.init.file.domain.model.formatByteSize
import com.init.file.theme.JetBrainsMonoFontFamily
import com.init.file.theme.MichromaFontFamily
import com.init.file.theme.SignalAccent
import com.init.file.theme.surfaceElevated
import com.init.file.ui.components.ConfirmationDialog
import com.init.file.ui.components.DotMatrixEmptyPattern
import com.init.file.ui.components.InitBadge
import com.init.file.ui.components.InitButton
import com.init.file.ui.components.InitCard
import com.init.file.ui.components.InitDropdownMenu
import com.init.file.ui.components.InitTopBar
import java.io.File

@Composable
fun VaultScreen(
    viewModel: VaultViewModel,
    onNavigateBack: () -> Unit,
    onOpenFilePreview: (FileItem) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val vaultConfig by viewModel.vaultConfig.collectAsState()

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var itemToDelete by remember { mutableStateOf<VaultItem?>(null) }

    val context = LocalContext.current
    val activity = context as? FragmentActivity

    // Auto-launch biometrics on opening locked vault
    LaunchedEffect(uiState) {
        if (uiState is VaultState.Locked && activity != null) {
            viewModel.authenticateWithBiometrics(activity)
        }
    }

    // File picker to import into vault
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            val fileItems = uris.mapNotNull { uri ->
                val path = getPathFromUri(context, uri)
                if (path != null) {
                    val file = File(path)
                    if (file.exists()) {
                        FileItem(
                            id = file.absolutePath,
                            name = file.name,
                            path = file.absolutePath,
                            sizeBytes = file.length(),
                            lastModified = file.lastModified()
                        )
                    } else null
                } else null
            }
            if (fileItems.isNotEmpty()) {
                viewModel.importFiles(fileItems)
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            if (uiState is VaultState.Unlocked) {
                val state = uiState as VaultState.Unlocked
                InitTopBar(
                    title = "PRIVATE VAULT",
                    subtitle = if (state.isSelectionMode) "${state.selectedIds.size} SELECTED" else "${state.items.size} ENCRYPTED FILES",
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
                        if (state.isSelectionMode) {
                            IconButton(onClick = { viewModel.selectAll() }) {
                                Icon(Icons.Default.CheckCircle, contentDescription = "Select All", tint = MaterialTheme.colorScheme.onSurface)
                            }
                            IconButton(onClick = { viewModel.clearSelection() }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = MaterialTheme.colorScheme.onSurface)
                            }
                        } else {
                            IconButton(onClick = { filePickerLauncher.launch(arrayOf("*/*")) }) {
                                Icon(Icons.Default.Add, contentDescription = "Import Files", tint = SignalAccent)
                            }
                            IconButton(onClick = { viewModel.lockVault() }) {
                                Icon(Icons.Default.Lock, contentDescription = "Lock Vault", tint = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                )
            } else {
                InitTopBar(
                    title = "PRIVATE VAULT",
                    subtitle = "HARDWARE AES-256 BIOMETRIC",
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is VaultState.Uninitialized -> {
                    VaultBiometricPromptScreen(
                        title = "ACTIVATE SAFE VAULT",
                        subtitle = "Securely isolate confidential files inside hardware-backed AES-256 encrypted storage protected by your device's biometric authentication.",
                        buttonText = "ACTIVATE WITH BIOMETRICS",
                        errorMessage = errorMessage,
                        onAuthenticateClick = {
                            activity?.let { viewModel.authenticateWithBiometrics(it) }
                        }
                    )
                }

                is VaultState.Locked -> {
                    VaultBiometricPromptScreen(
                        title = "VAULT LOCKED",
                        subtitle = "Authenticate using fingerprint sensor or device biometrics to decrypt and access your confidential files.",
                        buttonText = "UNLOCK WITH BIOMETRICS",
                        errorMessage = errorMessage,
                        onAuthenticateClick = {
                            activity?.let { viewModel.authenticateWithBiometrics(it) }
                        }
                    )
                }

                is VaultState.Processing -> {
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
                            text = "SECURE PROCESSING...",
                            fontFamily = MichromaFontFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                is VaultState.Unlocked -> {
                    if (state.items.isEmpty()) {
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
                                text = "VAULT IS EMPTY",
                                fontFamily = MichromaFontFamily,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Confidential files moved here are encrypted with AES-256 and removed from shared storage.",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(0.85f)
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            InitButton(
                                text = "IMPORT FILES (+)",
                                isPrimary = true,
                                onClick = { filePickerLauncher.launch(arrayOf("*/*")) }
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "ENCRYPTED STORAGE",
                                        fontFamily = MichromaFontFamily,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    InitBadge(
                                        text = formatByteSize(state.totalEncryptedBytes),
                                        isAccent = true
                                    )
                                }
                            }

                            items(state.items, key = { it.id }) { item ->
                                val isSelected = state.selectedIds.contains(item.id)
                                VaultItemRow(
                                    item = item,
                                    isSelectionMode = state.isSelectionMode,
                                    isSelected = isSelected,
                                    onItemClick = {
                                        if (state.isSelectionMode) {
                                            viewModel.toggleItemSelection(item.id)
                                        } else {
                                            viewModel.decryptAndOpenPreview(item) { decryptedFile ->
                                                val previewItem = FileItem(
                                                    id = decryptedFile.absolutePath,
                                                    name = item.name,
                                                    path = decryptedFile.absolutePath,
                                                    sizeBytes = item.sizeBytes,
                                                    lastModified = item.encryptedAt,
                                                    mimeType = item.mimeType
                                                )
                                                onOpenFilePreview(previewItem)
                                            }
                                        }
                                    },
                                    onLongClick = {
                                        viewModel.toggleItemSelection(item.id)
                                    },
                                    onRestore = {
                                        viewModel.restoreSelected(setOf(item.id))
                                    },
                                    onDelete = {
                                        itemToDelete = item
                                        showDeleteConfirmDialog = true
                                    }
                                )
                            }
                        }

                        // Floating Selection Action Bar
                        AnimatedVisibility(
                            visible = state.isSelectionMode && state.selectedIds.isNotEmpty(),
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
                                    Text(
                                        text = "${state.selectedIds.size} SELECTED",
                                        fontFamily = MichromaFontFamily,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        InitButton(
                                            text = "RESTORE",
                                            leadingIcon = Icons.Default.Restore,
                                            isPrimary = false,
                                            onClick = {
                                                viewModel.restoreSelected(state.selectedIds)
                                            }
                                        )
                                        InitButton(
                                            text = "DELETE",
                                            leadingIcon = Icons.Default.DeleteForever,
                                            isPrimary = true,
                                            isDestructive = true,
                                            onClick = {
                                                showDeleteConfirmDialog = true
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteConfirmDialog) {
        val count = if (itemToDelete != null) 1 else (uiState as? VaultState.Unlocked)?.selectedIds?.size ?: 0
        ConfirmationDialog(
            title = "PERMANENTLY DELETE $count ITEM(S)?",
            message = "This will permanently destroy the encrypted file(s) from the Private Vault. This action cannot be undone.",
            confirmText = "DELETE FOREVER",
            isDestructive = true,
            onConfirm = {
                if (itemToDelete != null) {
                    viewModel.deleteSelected(setOf(itemToDelete!!.id))
                    itemToDelete = null
                } else {
                    val selected = (uiState as? VaultState.Unlocked)?.selectedIds ?: emptySet()
                    viewModel.deleteSelected(selected)
                }
                showDeleteConfirmDialog = false
            },
            onDismiss = {
                showDeleteConfirmDialog = false
                itemToDelete = null
            }
        )
    }
}

/**
 * Biometric Prompt and Activation Screen with Nothing OS tactile aesthetics.
 */
@Composable
fun VaultBiometricPromptScreen(
    title: String,
    subtitle: String,
    buttonText: String,
    errorMessage: String?,
    onAuthenticateClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .clickable(onClick = onAuthenticateClick),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceElevated,
            border = androidx.compose.foundation.BorderStroke(2.dp, SignalAccent.copy(alpha = 0.8f))
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Fingerprint,
                    contentDescription = "Biometric Sensor",
                    tint = SignalAccent,
                    modifier = Modifier.size(52.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = title.uppercase(),
            fontFamily = MichromaFontFamily,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.4.sp,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = subtitle,
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(0.9f)
        )

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = errorMessage,
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        InitButton(
            text = buttonText,
            leadingIcon = Icons.Default.Fingerprint,
            isPrimary = true,
            onClick = onAuthenticateClick,
            modifier = Modifier.fillMaxWidth(0.85f)
        )
    }
}

/**
 * Row displaying an individual encrypted item inside the vault.
 */
@Composable
fun VaultItemRow(
    item: VaultItem,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onItemClick: () -> Unit,
    onLongClick: () -> Unit,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    InitCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onItemClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelectionMode) {
                Icon(
                    imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (isSelected) SignalAccent else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
            } else {
                Surface(
                    modifier = Modifier.size(38.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceElevated,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = getCategoryIcon(item.category),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${item.formattedSize} • Encrypted ${item.formattedDate}",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!isSelectionMode) {
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

                    InitDropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("PREVIEW FILE", fontFamily = JetBrainsMonoFontFamily, fontSize = 12.sp) },
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface) },
                            onClick = {
                                menuExpanded = false
                                onItemClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("RESTORE TO STORAGE", fontFamily = JetBrainsMonoFontFamily, fontSize = 12.sp) },
                            leadingIcon = { Icon(Icons.Default.Restore, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface) },
                            onClick = {
                                menuExpanded = false
                                onRestore()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("DELETE FOREVER", fontFamily = JetBrainsMonoFontFamily, fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                            leadingIcon = { Icon(Icons.Default.DeleteForever, contentDescription = null, tint = SignalAccent) },
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

private fun getCategoryIcon(category: FileCategory): ImageVector {
    return when (category) {
        FileCategory.IMAGES -> Icons.Default.Image
        FileCategory.VIDEOS -> Icons.Default.Movie
        FileCategory.AUDIO -> Icons.Default.MusicNote
        FileCategory.DOCUMENTS -> Icons.Default.Description
        else -> Icons.AutoMirrored.Filled.InsertDriveFile
    }
}

private fun getPathFromUri(context: android.content.Context, uri: android.net.Uri): String? {
    if ("file".equals(uri.scheme, ignoreCase = true)) {
        return uri.path
    }
    try {
        val cursor = context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIdx = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                val displayName = if (nameIdx != -1) it.getString(nameIdx) else "imported_file"
                val tempFile = File(context.cacheDir, displayName)
                context.contentResolver.openInputStream(uri)?.use { input ->
                    java.io.FileOutputStream(tempFile).use { output ->
                        input.copyTo(output)
                    }
                }
                return tempFile.absolutePath
            }
        }
    } catch (_: Exception) {}
    return null
}
