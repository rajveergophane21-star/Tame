package com.tame.app.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tame.app.R

private fun frankRes(mood: String): Int = when (mood) {
    "neutral" -> R.drawable.frank_normal
    "happy", "zen", "breathe", "sleep" -> R.drawable.frank_calm
    "worried", "sad" -> R.drawable.frank_sad
    "panic" -> R.drawable.frank_panic
    "crying" -> R.drawable.frank_crying
    "angry" -> R.drawable.frank_angry
    else -> R.drawable.frank_normal
}

/**
 * Frank the monkey. [mood] follows the design's mood vocabulary. When [idle] is
 * true he floats gently (or shakes when panicking).
 */
@Composable
fun Frank(
    mood: String,
    size: Dp,
    modifier: Modifier = Modifier,
    idle: Boolean = true,
) {
    val anim = when {
        mood == "panic" -> Modifier.frankShake()
        idle -> Modifier.frankFloat()
        else -> Modifier
    }
    Image(
        painter = painterResource(frankRes(mood)),
        contentDescription = "Frank",
        contentScale = ContentScale.Fit,
        modifier = modifier.size(size).then(anim),
    )
}

/** Frank cropped to fill its container (objectFit: cover, position 50% 18%). */
@Composable
fun FrankFill(mood: String, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(frankRes(mood)),
        contentDescription = "Frank",
        contentScale = ContentScale.Crop,
        alignment = BiasAlignment(0f, -0.64f),
        modifier = modifier,
    )
}

@Composable
private fun Modifier.frankFloat(): Modifier {
    val t = rememberInfiniteTransition(label = "float")
    val dy by t.animateFloat(
        initialValue = 0f, targetValue = -5f,
        animationSpec = infiniteRepeatable(tween(2500), RepeatMode.Reverse), label = "dy",
    )
    val px = with(LocalDensity.current) { dy.dp.toPx() }
    return this.graphicsLayer { translationY = px }
}

@Composable
private fun Modifier.frankShake(): Modifier {
    val t = rememberInfiniteTransition(label = "shake")
    val rot by t.animateFloat(
        initialValue = -3f, targetValue = 3f,
        animationSpec = infiniteRepeatable(tween(120), RepeatMode.Reverse), label = "rot",
    )
    val px = with(LocalDensity.current) { 2.dp.toPx() }
    return this.graphicsLayer { rotationZ = rot; translationX = (rot / 3f) * px }
}

/** Breathing scale used on the calm/focus/friction takeovers. */
@Composable
fun Modifier.breathe(durationMs: Int = 4000, min: Float = 0.78f, max: Float = 1.14f): Modifier {
    val t = rememberInfiniteTransition(label = "breathe")
    val s by t.animateFloat(
        initialValue = min, targetValue = max,
        animationSpec = infiniteRepeatable(tween(durationMs / 2), RepeatMode.Reverse), label = "scale",
    )
    return this.graphicsLayer { scaleX = s; scaleY = s }
}
