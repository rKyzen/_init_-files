package com.init.files.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.init.files.theme.SignalAccent

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
