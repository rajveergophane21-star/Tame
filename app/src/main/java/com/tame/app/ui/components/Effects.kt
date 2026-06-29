package com.tame.app.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp

/**
 * Soft ambient glow that renders on ALL API levels (unlike Modifier.blur, which is
 * a no-op below API 31). Draws a radial gradient from [color] at the centre fading
 * to transparent at the element's edge.
 */
fun Modifier.glow(color: Color): Modifier = this.drawBehind {
    drawCircle(
        brush = Brush.radialGradient(
            colorStops = arrayOf(0f to color, 0.55f to color, 1f to Color.Transparent),
            center = center,
            radius = size.minDimension / 2f,
        ),
        radius = size.minDimension / 2f,
        center = center,
    )
}

/** Dashed rounded-rectangle border (CSS `border: Npx dashed`), all API levels. */
fun Modifier.dashedBorder(color: Color, width: Dp, radius: Dp): Modifier = this.drawBehind {
    val stroke = width.toPx()
    val r = radius.toPx()
    drawRoundRect(
        color = color,
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(r, r),
        style = Stroke(
            width = stroke,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(stroke * 3f, stroke * 2.5f), 0f),
        ),
    )
}
