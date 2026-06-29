package com.tame.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.ImageBitmap
import com.tame.app.ui.theme.TameColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.tame.app.data.model.AppCatalog
import com.tame.app.ui.theme.Bricolage
import androidx.compose.material3.Text
import androidx.compose.foundation.border

/** A single rounded app tile with its two-letter mark. */
@Composable
fun AppSquare(
    key: String,
    size: Dp,
    corner: Dp,
    fontSize: TextUnit,
    modifier: Modifier = Modifier,
) {
    val app = AppCatalog[key]
    val bg = Color(app?.colorHex ?: 0xFF888888)
    val fg = Color(app?.fgHex ?: 0xFFFFFFFF)
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(corner))
            .background(bg),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = app?.letter ?: "?",
            color = fg,
            style = TextStyle(fontFamily = Bricolage, fontWeight = FontWeight.ExtraBold, fontSize = fontSize),
        )
    }
}

/**
 * Stacked app icons for rule rows. One target = a single square; two+ = a pair
 * with a white ring on the front tile (matching the design's appIconStack).
 */
@Composable
fun AppIconStack(
    targets: List<String>,
    size: Dp,
    modifier: Modifier = Modifier,
    iconFor: (String) -> ImageBitmap? = { null },
) {
    val t = targets.take(2)
    Box(modifier = modifier.size(size)) {
        if (t.size <= 1) {
            TargetTile(t.firstOrNull() ?: "?", size, iconFor)
        } else {
            val d = size * 0.74f
            // back tile (top-left)
            TargetTile(t[0], d, iconFor, Modifier.align(Alignment.TopStart))
            // front tile (bottom-right) with white ring
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(d + 6.dp)
                    .clip(RoundedCornerShape(d * 0.3f + 3.dp))
                    .background(Color.White),
                contentAlignment = Alignment.Center,
            ) {
                TargetTile(t[1], d, iconFor)
            }
        }
    }
}

/** One tile: branded badge for catalog feed keys, real icon for installed packages. */
@Composable
private fun TargetTile(target: String, size: Dp, iconFor: (String) -> ImageBitmap?, modifier: Modifier = Modifier) {
    val catalog = AppCatalog[target]
    when {
        catalog != null -> AppSquare(target, size, size * 0.3f, (size.value * 0.34f).sp, modifier)
        else -> {
            val bmp = iconFor(target)
            if (bmp != null) {
                Image(
                    bitmap = bmp,
                    contentDescription = null,
                    modifier = modifier.size(size).clip(RoundedCornerShape(size * 0.3f)),
                )
            } else {
                Box(modifier = modifier.size(size).clip(RoundedCornerShape(size * 0.3f)).background(TameColors.FieldBg))
            }
        }
    }
}
