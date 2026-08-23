package com.init.file.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.init.file.theme.SignalAccent

/**
 * Draws a subtle dot-matrix background grid for terminal aesthetic.
 */
@Composable
fun DotMatrixGrid(
    modifier: Modifier = Modifier,
    dotSpacing: Dp = 16.dp,
    dotRadius: Dp = 1.dp,
    dotColor: Color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val spacingPx = dotSpacing.toPx()
        val radiusPx = dotRadius.toPx()
        val width = size.width
        val height = size.height

        var x = spacingPx / 2
        while (x < width) {
            var y = spacingPx / 2
            while (y < height) {
                drawCircle(
                    color = dotColor,
                    radius = radiusPx,
                    center = Offset(x, y)
                )
                y += spacingPx
            }
            x += spacingPx
        }
    }
}

/**
 * Dot-matrix flourish pattern for empty states.
 */
@Composable
fun DotMatrixEmptyPattern(
    modifier: Modifier = Modifier,
    rows: Int = 5,
    columns: Int = 9,
    dotSpacing: Dp = 10.dp,
    dotRadius: Dp = 1.5.dp,
    accentEvery: Int = 7
) {
    val baseColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    val accentColor = SignalAccent.copy(alpha = 0.9f)

    Canvas(
        modifier = modifier.size(
            width = (columns * dotSpacing.value).dp,
            height = (rows * dotSpacing.value).dp
        )
    ) {
        val spacingPx = dotSpacing.toPx()
        val radiusPx = dotRadius.toPx()

        for (r in 0 until rows) {
            for (c in 0 until columns) {
                val index = r * columns + c
                val isAccent = index % accentEvery == 0
                val color = if (isAccent) accentColor else baseColor
                val radius = if (isAccent) radiusPx * 1.3f else radiusPx

                drawCircle(
                    color = color,
                    radius = radius,
                    center = Offset(
                        x = (c + 0.5f) * spacingPx,
                        y = (r + 0.5f) * spacingPx
                    )
                )
            }
        }
    }
}

/**
 * Modifier for Nothing OS-style textured glassmorphism with tactile micro-stipple grain and frosted translucency.
 */
fun Modifier.texturedGlass(
    shape: Shape = RoundedCornerShape(16.dp),
    backgroundColor: Color? = null,
    borderColor: Color? = null,
    elevation: Dp = 12.dp,
    dotSpacing: Dp = 5.5.dp,
    dotRadius: Dp = 0.65.dp
): Modifier = composed {
    val isDark = isSystemInDarkTheme()
    val bg = backgroundColor ?: MaterialTheme.colorScheme.surface.copy(alpha = if (isDark) 0.93f else 0.96f)
    val borderCol = borderColor ?: MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (isDark) 0.65f else 0.45f)
    val dotColor = if (isDark) Color(0xFF888888).copy(alpha = 0.30f) else Color(0xFF666666).copy(alpha = 0.22f)

    this
        .shadow(
            elevation = elevation,
            shape = shape,
            spotColor = Color.Black.copy(alpha = if (isDark) 0.5f else 0.12f),
            ambientColor = Color.Black.copy(alpha = if (isDark) 0.3f else 0.06f)
        )
        .clip(shape)
        .background(bg, shape)
        .drawWithContent {
            // Draw grayed-out tactile dot matrix texture
            val spacingPx = dotSpacing.toPx()
            val radiusPx = dotRadius.toPx()
            val width = size.width
            val height = size.height

            var row = 0
            var y = spacingPx / 2
            while (y < height) {
                val xOffset = if (row % 2 == 1) spacingPx / 2 else 0f
                var x = (spacingPx / 2) + xOffset
                while (x < width) {
                    drawCircle(
                        color = dotColor,
                        radius = radiusPx,
                        center = Offset(x, y)
                    )
                    x += spacingPx
                }
                y += spacingPx
                row++
            }

            // Draw composable content
            drawContent()
        }
        .border(1.dp, borderCol, shape)
}

/**
 * Composable Surface container with tactile micro-stipple grain textured glassmorphism.
 */
@Composable
fun TexturedGlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    backgroundColor: Color? = null,
    borderColor: Color? = null,
    elevation: Dp = 12.dp,
    dotSpacing: Dp = 5.5.dp,
    dotRadius: Dp = 0.65.dp,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier.texturedGlass(
            shape = shape,
            backgroundColor = backgroundColor,
            borderColor = borderColor,
            elevation = elevation,
            dotSpacing = dotSpacing,
            dotRadius = dotRadius
        )
    ) {
        content()
    }
}

