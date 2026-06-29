package com.tame.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Dp

/**
 * A tiny SVG-faithful icon primitive. Shapes are declared in a 24×24 (default)
 * viewport, matching the `<svg>` markup in the Tame design, so icons can be
 * transcribed verbatim from the prototype.
 */
sealed interface Vec
data class VPath(val d: String, val fill: Boolean = false, val sw: Float = 2f) : Vec
data class VCircle(val cx: Float, val cy: Float, val r: Float, val fill: Boolean = false, val sw: Float = 2f) : Vec
data class VRect(
    val x: Float, val y: Float, val w: Float, val h: Float,
    val rx: Float = 0f, val fill: Boolean = false, val sw: Float = 2f,
) : Vec

@Composable
fun SvgIcon(
    shapes: List<Vec>,
    size: Dp,
    tint: Color,
    modifier: Modifier = Modifier,
    viewport: Float = 24f,
    cap: StrokeCap = StrokeCap.Round,
    join: StrokeJoin = StrokeJoin.Round,
) {
    Canvas(modifier = modifier.size(size)) {
        val s = this.size.minDimension / viewport
        withTransform({ scale(s, s, pivot = Offset.Zero) }) {
            shapes.forEach { drawVec(it, tint, cap, join) }
        }
    }
}

private fun DrawScope.drawVec(v: Vec, tint: Color, cap: StrokeCap, join: StrokeJoin) {
    when (v) {
        is VPath -> {
            val path = PathParser().parsePathString(v.d).toPath()
            if (v.fill) drawPath(path, tint, style = Fill)
            else drawPath(path, tint, style = Stroke(width = v.sw, cap = cap, join = join))
        }
        is VCircle -> {
            if (v.fill) drawCircle(tint, radius = v.r, center = Offset(v.cx, v.cy))
            else drawCircle(tint, radius = v.r, center = Offset(v.cx, v.cy), style = Stroke(width = v.sw, cap = cap, join = join))
        }
        is VRect -> {
            if (v.fill) drawRoundRect(tint, topLeft = Offset(v.x, v.y), size = Size(v.w, v.h), cornerRadius = CornerRadius(v.rx, v.rx))
            else drawRoundRect(tint, topLeft = Offset(v.x, v.y), size = Size(v.w, v.h), cornerRadius = CornerRadius(v.rx, v.rx), style = Stroke(width = v.sw, cap = cap, join = join))
        }
    }
}
