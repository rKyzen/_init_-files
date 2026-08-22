package com.init.files.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.init.files.theme.DarkBackground
import com.init.files.theme.DarkTextPrimary
import com.init.files.theme.JetBrainsMonoFontFamily
import com.init.files.theme.MichromaFontFamily
import com.init.files.theme.SignalAccent
import com.init.files.theme.SignalAccentDim

/**
 * Top App Bar styled as a system utility header with automatic status bar insets padding.
 */
@Composable
fun InitTopBar(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: (@Composable () -> Unit)? = null
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(0.dp)
            ),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    if (navigationIcon != null) {
                        navigationIcon()
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (subtitle != null) {
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
                if (actions != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End
                    ) {
                        actions()
                    }
                }
            }
        }
    }
}

/**
 * Interactive breadcrumb bar formatted in JetBrains Mono.
 * Allows tapping path tokens to jump directly to parent directories.
 */
@Composable
fun InitBreadcrumbs(
    currentPath: String,
    onNavigateToPath: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    // Break path into segments
    val segments = remember(currentPath) {
        val clean = currentPath.trimEnd('/')
        if (clean.isEmpty() || clean == "/") {
            listOf(PathSegment("root", "/"))
        } else {
            val parts = clean.split("/").filter { it.isNotEmpty() }
            var accumulated = ""
            val list = mutableListOf<PathSegment>()
            list.add(PathSegment("~", "/storage/emulated/0"))
            for (p in parts) {
                accumulated += "/$p"
                if (accumulated != "/storage" && accumulated != "/storage/emulated") {
                    list.add(PathSegment(p, accumulated))
                }
            }
            list
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$ ",
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = SignalAccent
            )

            segments.forEachIndexed { index, segment ->
                val isLast = index == segments.size - 1
                Text(
                    text = segment.name,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = if (isLast) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 12.sp,
                    color = if (isLast) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .clip(RoundedCornerShape(2.dp))
                        .clickable(!isLast) { onNavigateToPath(segment.fullPath) }
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                )

                if (!isLast) {
                    Text(
                        text = "/",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(horizontal = 1.dp)
                    )
                }
            }
        }
    }
}

data class PathSegment(val name: String, val fullPath: String)

/**
 * Mechanical, technical styled button with strong monochrome styling.
 */
@Composable
fun InitButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPrimary: Boolean = true,
    isDestructive: Boolean = false,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null
) {
    val bgColor = when {
        !enabled -> MaterialTheme.colorScheme.surfaceVariant
        isDestructive -> MaterialTheme.colorScheme.onSurface
        isPrimary -> MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.surface
    }

    val contentColor = when {
        !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        isDestructive -> MaterialTheme.colorScheme.surface
        isPrimary -> MaterialTheme.colorScheme.surface
        else -> MaterialTheme.colorScheme.onSurface
    }

    val borderColor = when {
        !enabled -> MaterialTheme.colorScheme.outlineVariant
        isDestructive -> MaterialTheme.colorScheme.onSurface
        isPrimary -> MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.outline
    }

    Surface(
        modifier = modifier
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick),
        color = bgColor,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text.uppercase(),
                fontFamily = if (isDestructive) MichromaFontFamily else JetBrainsMonoFontFamily,
                fontWeight = if (isDestructive) FontWeight.Bold else FontWeight.Medium,
                fontSize = if (isDestructive) 11.sp else 12.sp,
                letterSpacing = if (isDestructive) 1.2.sp else 0.8.sp,
                color = contentColor
            )
        }
    }
}

/**
 * Monospace metadata badge / pill.
 */
@Composable
fun InitBadge(
    text: String,
    modifier: Modifier = Modifier,
    isAccent: Boolean = false
) {
    val bgColor = if (isAccent) SignalAccent.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (isAccent) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
    val borderColor = if (isAccent) SignalAccent.copy(alpha = 0.7f) else MaterialTheme.colorScheme.outlineVariant

    Box(
        modifier = modifier
            .border(1.dp, borderColor, RoundedCornerShape(6.dp))
            .background(bgColor, RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = if (isAccent) FontWeight.Bold else FontWeight.Medium,
            fontSize = 10.sp,
            maxLines = 1,
            softWrap = false,
            color = textColor
        )
    }
}

/**
 * Segmented flat horizontal storage progress bar.
 */
@Composable
fun InitSegmentedProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    segments: Int = 20,
    accentThreshold: Float = 0.85f
) {
    val clamped = progress.coerceIn(0f, 1f)
    val activeSegments = (clamped * segments).toInt()
    val isHigh = clamped >= accentThreshold

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        for (i in 0 until segments) {
            val isActive = i < activeSegments
            val color = when {
                isActive && isHigh -> SignalAccent
                isActive -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(8.dp)
                    .background(color, RoundedCornerShape(3.dp))
            )
        }
    }
}

/**
 * Section header in Michroma without dot indicator.
 */
@Composable
fun InitSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    badgeText: String? = null,
    action: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title.lowercase(),
                fontFamily = MichromaFontFamily,
                fontSize = 13.sp,
                letterSpacing = 1.2.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (badgeText != null) {
                Spacer(modifier = Modifier.width(8.dp))
                InitBadge(text = badgeText)
            }
        }
        if (action != null) {
            action()
        }
    }
}

/**
 * Technical styled card container with thin borders.
 */
@Composable
fun InitCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val cardModifier = if (onClick != null) {
        modifier
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    } else {
        modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
    }

    Surface(
        modifier = cardModifier,
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp)
    ) {
        content()
    }
}

/**
 * Glassmorphic dropdown menu with tactile micro-stipple grain textured transparency and refined squircle border.
 */
@Composable
fun InitDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        shape = RoundedCornerShape(16.dp),
        containerColor = Color.Transparent,
        shadowElevation = 0.dp,
        border = null,
        modifier = modifier.texturedGlass(shape = RoundedCornerShape(16.dp), elevation = 14.dp),
        content = content
    )
}

