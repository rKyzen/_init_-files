package com.init.files.ui.screens.vault

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
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.init.files.domain.model.FileCategory
import com.init.files.domain.model.FileItem
import com.init.files.domain.model.VaultItem
import com.init.files.domain.model.VaultState
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
import com.init.files.ui.components.InitDropdownMenu
import com.init.files.ui.components.InitTopBar
import java.io.File

@Composable
fun VaultScreen(
    viewModel: VaultViewModel,
    onNavigateBack: () -> Unit,
    onOpenFilePreview: (FileItem) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val pinInput by viewModel.pinInput.collectAsState()
    val setupStep by viewModel.setupStep.collectAsState()
    val hintInput by viewModel.hintInput.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val vaultConfig by viewModel.vaultConfig.collectAsState()

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var itemToDelete by remember { mutableStateOf<VaultItem?>(null) }
    var showHintDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val activity = context as? androidx.fragment.app.FragmentActivity

    // Auto-launch biometrics on opening locked vault if available and enabled
    LaunchedEffect(uiState, vaultConfig.biometricsAvailable, vaultConfig.biometricsEnabled) {
        if (uiState is VaultState.Locked && vaultConfig.biometricsAvailable && vaultConfig.biometricsEnabled && activity != null) {
            viewModel.launchBiometrics(activity)
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
                            if (vaultConfig.biometricsAvailable) {
                                IconButton(onClick = { viewModel.toggleBiometrics(!vaultConfig.biometricsEnabled) }) {
                                    Icon(
                                        imageVector = Icons.Default.Fingerprint,
                                        contentDescription = "Toggle Biometrics",
                                        tint = if (vaultConfig.biometricsEnabled) SignalAccent else MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
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
                    subtitle = "ZERO-KNOWLEDGE AES-256",
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
                        if (vaultConfig.hint != null && uiState is VaultState.Locked) {
                            IconButton(onClick = { showHintDialog = true }) {
                                Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = "PIN Hint", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
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
                    VaultPinTerminal(
                        title = when (setupStep) {
                            VaultSetupStep.ENTER_NEW_PIN -> "CREATE MASTER PIN"
                            VaultSetupStep.CONFIRM_NEW_PIN -> "CONFIRM MASTER PIN"
                            VaultSetupStep.ENTER_HINT -> "SET RECOVERY HINT"
                        },
                        subtitle = when (setupStep) {
                            VaultSetupStep.ENTER_NEW_PIN -> "Set a 4-6 digit security PIN for your private vault."
                            VaultSetupStep.CONFIRM_NEW_PIN -> "Re-enter your PIN to verify."
                            VaultSetupStep.ENTER_HINT -> "Optional: Enter a hint to help remember your PIN."
                        },
                        pinInput = pinInput,
                        errorMessage = errorMessage,
                        isHintStep = setupStep == VaultSetupStep.ENTER_HINT,
                        hintValue = hintInput,
                        onHintChange = { viewModel.onHintChange(it) },
                        onDigitPress = { viewModel.onDigitPress(it) },
                        onBackspacePress = { viewModel.onBackspacePress() },
                        onClearPress = { viewModel.onClearPress() },
                        onSubmitPress = {
                            if (setupStep == VaultSetupStep.ENTER_HINT) {
                                viewModel.finalizeVaultSetup()
                            } else {
                                viewModel.submitPin()
                            }
                        }
                    )
                }

                is VaultState.Locked -> {
                    VaultPinTerminal(
                        title = "VAULT LOCKED",
                        subtitle = "Enter your Master PIN or authenticate with Biometrics to access vault.",
                        pinInput = pinInput,
                        errorMessage = errorMessage,
                        isHintStep = false,
                        hintValue = "",
                        onHintChange = {},
                        onDigitPress = { viewModel.onDigitPress(it) },
                        onBackspacePress = { viewModel.onBackspacePress() },
                        onClearPress = { viewModel.onClearPress() },
                        onSubmitPress = { viewModel.submitPin() },
                        onBiometricPress = if (vaultConfig.biometricsAvailable && vaultConfig.biometricsEnabled && activity != null) {
                            { viewModel.launchBiometrics(activity) }
                        } else null
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
                            text = state.message.uppercase(),
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
                            // Header badge info
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
                                                    name = decryptedFile.name,
                                                    path = decryptedFile.absolutePath,
                                                    sizeBytes = decryptedFile.length(),
                                                    lastModified = decryptedFile.lastModified(),
                                                    isDirectory = false,
                                                    extension = decryptedFile.name.substringAfterLast('.', "").lowercase(),
                                                    category = item.category
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

                        // Floating multi-selection action bar
                        AnimatedVisibility(
                            visible = state.isSelectionMode,
                            enter = slideInVertically { it } + fadeIn(),
                            exit = slideOutVertically { it } + fadeOut(),
                            modifier = Modifier.align(Alignment.BottomCenter)
                        ) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .navigationBarsPadding()
                                    .padding(16.dp)
                                    .shadow(12.dp, RoundedCornerShape(16.dp))
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
                                    InitButton(
                                        text = "RESTORE (${state.selectedIds.size})",
                                        isPrimary = true,
                                        onClick = { viewModel.restoreSelected(state.selectedIds) }
                                    )

                                    InitButton(
                                        text = "DELETE",
                                        isPrimary = false,
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

    // Confirmation dialog for deletion
    if (showDeleteConfirmDialog) {
        val count = if (itemToDelete != null) 1 else (uiState as? VaultState.Unlocked)?.selectedIds?.size ?: 0
        ConfirmationDialog(
            title = "PERMANENTLY DELETE",
            message = "Are you sure you want to permanently erase $count encrypted file(s)? This action cannot be undone.",
            confirmText = "ERASE PERMANENTLY",
            isDestructive = true,
            onDismiss = {
                showDeleteConfirmDialog = false
                itemToDelete = null
            },
            onConfirm = {
                if (itemToDelete != null) {
                    viewModel.deleteSelected(setOf(itemToDelete!!.id))
                } else {
                    val selected = (uiState as? VaultState.Unlocked)?.selectedIds ?: emptySet()
                    viewModel.deleteSelected(selected)
                }
                showDeleteConfirmDialog = false
                itemToDelete = null
            }
        )
    }

    // PIN Hint Dialog
    if (showHintDialog && vaultConfig.hint != null) {
        ConfirmationDialog(
            title = "PIN RECOVERY HINT",
            message = "Hint: ${vaultConfig.hint}",
            confirmText = "OK",
            isDestructive = false,
            onDismiss = { showHintDialog = false },
            onConfirm = { showHintDialog = false }
        )
    }
}

/**
 * Vault Keypad Terminal for setting up or entering Master PIN.
 */
@Composable
fun VaultPinTerminal(
    title: String,
    subtitle: String,
    pinInput: String,
    errorMessage: String?,
    isHintStep: Boolean = false,
    hintValue: String = "",
    onDigitPress: (String) -> Unit,
    onBackspacePress: () -> Unit,
    onClearPress: () -> Unit,
    onHintChange: (String) -> Unit = {},
    onSubmitPress: () -> Unit = {},
    onBiometricPress: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                modifier = Modifier.size(56.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceElevated,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (onBiometricPress != null) Icons.Default.Fingerprint else Icons.Default.Security,
                        contentDescription = null,
                        tint = SignalAccent,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = title.uppercase(),
                fontFamily = MichromaFontFamily,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = subtitle,
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(0.9f)
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (isHintStep) {
                OutlinedTextField(
                    value = hintValue,
                    onValueChange = onHintChange,
                    singleLine = true,
                    placeholder = {
                        Text("e.g. My favorite lucky number", fontFamily = JetBrainsMonoFontFamily, fontSize = 12.sp)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SignalAccent,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                InitButton(
                    text = "ACTIVATE PRIVATE VAULT",
                    isPrimary = true,
                    onClick = onSubmitPress,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                // Monospace PIN glyph indicator
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 0 until 6) {
                        val filled = i < pinInput.length
                        Surface(
                            modifier = Modifier.size(16.dp),
                            shape = CircleShape,
                            color = if (filled) SignalAccent else Color.Transparent,
                            border = androidx.compose.foundation.BorderStroke(
                                1.5.dp,
                                if (filled) SignalAccent else MaterialTheme.colorScheme.outlineVariant
                            )
                        ) {}
                    }
                }

                if (onBiometricPress != null) {
                    Spacer(modifier = Modifier.height(20.dp))
                    InitButton(
                        text = "UNLOCK WITH BIOMETRICS",
                        leadingIcon = Icons.Default.Fingerprint,
                        isPrimary = false,
                        onClick = onBiometricPress,
                        modifier = Modifier.fillMaxWidth(0.85f)
                    )
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = errorMessage,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        if (!isHintStep) {
            // Keypad Layout (0-9, Backspace, Clear/Biometrics)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val keypadRows = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("C", "0", "DEL")
                )

                for (row in keypadRows) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        for (key in row) {
                            val isBioKey = key == "C" && pinInput.isEmpty() && onBiometricPress != null
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable {
                                        when {
                                            isBioKey -> onBiometricPress()
                                            key == "C" -> onClearPress()
                                            key == "DEL" -> onBackspacePress()
                                            else -> onDigitPress(key)
                                        }
                                    },
                                color = MaterialTheme.colorScheme.surfaceElevated,
                                shape = RoundedCornerShape(14.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isBioKey) SignalAccent.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    when {
                                        isBioKey -> {
                                            Icon(
                                                imageVector = Icons.Default.Fingerprint,
                                                contentDescription = "Biometrics",
                                                tint = SignalAccent,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                        key == "DEL" -> {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.Backspace,
                                                contentDescription = "Backspace",
                                                tint = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        else -> {
                                            Text(
                                                text = key,
                                                fontFamily = MichromaFontFamily,
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (key == "C") MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
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
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.formattedSize,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = " • ",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = item.formattedDate,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.size(32.dp)
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
                        text = { Text("OPEN / PREVIEW", fontFamily = JetBrainsMonoFontFamily, fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        onClick = {
                            menuExpanded = false
                            onItemClick()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("RESTORE TO STORAGE", fontFamily = JetBrainsMonoFontFamily, fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        onClick = {
                            menuExpanded = false
                            onRestore()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("PERMANENTLY DELETE", fontFamily = JetBrainsMonoFontFamily, fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                        leadingIcon = { Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(16.dp)) },
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
    // Try to resolve path from content resolver or stream copy if needed
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
