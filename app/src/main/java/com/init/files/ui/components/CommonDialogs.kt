package com.init.files.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.init.files.R
import com.init.files.domain.model.FileOperation
import com.init.files.domain.model.OperationProgress
import com.init.files.domain.model.SortConfig
import com.init.files.domain.model.SortField
import com.init.files.domain.model.SortOrder
import com.init.files.domain.model.formatByteSize
import com.init.files.theme.JetBrainsMonoFontFamily
import com.init.files.theme.MichromaFontFamily
import com.init.files.theme.SignalAccent

/**
 * Text input dialog for New Folder, New File, Rename, and Zip actions.
 */
@Composable
fun TextInputDialog(
    title: String,
    hint: String,
    initialValue: String = "",
    confirmButtonText: String = stringResource(R.string.action_confirm),
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf(initialValue) }

    Dialog(onDismissRequest = onDismiss) {
        TexturedGlassSurface(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding(),
            shape = RoundedCornerShape(20.dp),
            elevation = 16.dp
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = title.uppercase(),
                    fontFamily = MichromaFontFamily,
                    fontSize = 14.sp,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                    placeholder = {
                        Text(
                            text = hint,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 14.sp
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        cursorColor = SignalAccent,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    InitButton(
                        text = stringResource(R.string.action_cancel),
                        isPrimary = false,
                        onClick = onDismiss
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    InitButton(
                        text = confirmButtonText,
                        isPrimary = true,
                        enabled = text.isNotBlank(),
                        onClick = {
                            if (text.isNotBlank()) {
                                onConfirm(text.trim())
                            }
                        }
                    )
                }
            }
        }
    }
}

/**
 * Technical confirmation dialog for destructive actions (e.g. Delete, Empty Trash).
 */
@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String = stringResource(R.string.action_delete),
    isDestructive: Boolean = true,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        TexturedGlassSurface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            borderColor = if (isDestructive) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f),
            elevation = 16.dp
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Warning",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = title.uppercase(),
                        fontFamily = MichromaFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        letterSpacing = 1.2.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = message,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    InitButton(
                        text = stringResource(R.string.action_cancel),
                        isPrimary = false,
                        onClick = onDismiss
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    InitButton(
                        text = confirmText,
                        isPrimary = !isDestructive,
                        isDestructive = isDestructive,
                        onClick = onConfirm
                    )
                }
            }
        }
    }
}

/**
 * Confirmation dialog alias for backward compatibility.
 */
@Composable
fun ConfirmationDialog(
    title: String,
    message: String,
    confirmText: String = stringResource(R.string.action_delete),
    isDestructive: Boolean = true,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) = ConfirmDialog(
    title = title,
    message = message,
    confirmText = confirmText,
    isDestructive = isDestructive,
    onDismiss = onDismiss,
    onConfirm = onConfirm
)

/**
 * Technical operation progress dialog.
 */
@Composable
fun OperationProgressDialog(
    progress: OperationProgress,
    onCancel: () -> Unit
) {
    Dialog(onDismissRequest = {}) {
        TexturedGlassSurface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            elevation = 16.dp
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when (progress.operation) {
                            FileOperation.COPY -> "COPYING..."
                            FileOperation.MOVE -> "MOVING..."
                            FileOperation.DELETE -> "DELETING..."
                            FileOperation.ZIP -> "COMPRESSING..."
                            FileOperation.UNZIP -> "EXTRACTING..."
                            else -> "PROCESSING..."
                        },
                        fontFamily = MichromaFontFamily,
                        fontSize = 13.sp,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (progress.isDone) {
                        InitBadge(text = "DONE", isAccent = true)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (progress.currentFileName.isNotEmpty()) {
                    Text(
                        text = progress.currentFileName,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                val floatProgress = if (progress.totalBytes > 0) {
                    (progress.processedBytes.toFloat() / progress.totalBytes.toFloat()).coerceIn(0f, 1f)
                } else if (progress.totalItems > 0) {
                    (progress.processedItems.toFloat() / progress.totalItems.toFloat()).coerceIn(0f, 1f)
                } else 0f

                InitSegmentedProgressBar(progress = floatProgress, segments = 24)

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${progress.processedItems}/${progress.totalItems} items",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (progress.totalBytes > 0) {
                        Text(
                            text = "${formatByteSize(progress.processedBytes)} / ${formatByteSize(progress.totalBytes)}",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (!progress.isDone) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        InitButton(
                            text = stringResource(R.string.action_cancel),
                            isPrimary = false,
                            onClick = onCancel
                        )
                    }
                }
            }
        }
    }
}

/**
 * Sort options bottom sheet with navigation bar insets padding.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SortBottomSheet(
    currentConfig: SortConfig,
    onDismiss: () -> Unit,
    onApply: (SortConfig) -> Unit
) {
    var selectedField by remember { mutableStateOf(currentConfig.field) }
    var selectedOrder by remember { mutableStateOf(currentConfig.order) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.Transparent,
        scrimColor = Color.Black.copy(alpha = 0.5f),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = null
    ) {
        TexturedGlassSurface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            elevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(20.dp)
            ) {
                Text(
                    text = stringResource(R.string.action_sort).uppercase(),
                    fontFamily = MichromaFontFamily,
                    fontSize = 14.sp,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "SORT CRITERIA",
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            SortOptionItem(
                label = stringResource(R.string.sort_name),
                selected = selectedField == SortField.NAME,
                onClick = { selectedField = SortField.NAME }
            )
            SortOptionItem(
                label = stringResource(R.string.sort_size),
                selected = selectedField == SortField.SIZE,
                onClick = { selectedField = SortField.SIZE }
            )
            SortOptionItem(
                label = stringResource(R.string.sort_date),
                selected = selectedField == SortField.DATE,
                onClick = { selectedField = SortField.DATE }
            )
            SortOptionItem(
                label = stringResource(R.string.sort_type),
                selected = selectedField == SortField.TYPE,
                onClick = { selectedField = SortField.TYPE }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "DIRECTION",
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            SortOptionItem(
                label = stringResource(R.string.sort_asc),
                selected = selectedOrder == SortOrder.ASCENDING,
                onClick = { selectedOrder = SortOrder.ASCENDING }
            )
            SortOptionItem(
                label = stringResource(R.string.sort_desc),
                selected = selectedOrder == SortOrder.DESCENDING,
                onClick = { selectedOrder = SortOrder.DESCENDING }
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                InitButton(
                    text = stringResource(R.string.action_cancel),
                    isPrimary = false,
                    onClick = onDismiss
                )
                Spacer(modifier = Modifier.width(8.dp))
                InitButton(
                    text = stringResource(R.string.action_apply),
                    isPrimary = true,
                    onClick = {
                        onApply(SortConfig(field = selectedField, order = selectedOrder))
                        onDismiss()
                    }
                )
            }
        }
    }
}
}

@Composable
fun SortOptionItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = SignalAccent,
                unselectedColor = MaterialTheme.colorScheme.outline
            )
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
