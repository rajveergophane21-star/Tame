package com.tame.app.ui.takeover

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tame.app.ui.AppViewModel
import com.tame.app.ui.components.FrankFill
import com.tame.app.ui.components.SvgIcon
import com.tame.app.ui.components.VPath
import com.tame.app.ui.components.tap
import com.tame.app.ui.theme.Bricolage
import com.tame.app.ui.theme.Hanken
import com.tame.app.ui.theme.TameColors
import com.tame.app.ui.theme.accent

@Composable
fun ReelsTakeover(vm: AppViewModel) {
    val a = accent

    val deck = AppViewModel.reelDeck
    val reel = deck[(vm.reelIndex % deck.size + deck.size) % deck.size]
    val hue = Color(reel.hue)

    val settings = vm.settings
    val ratio = vm.reelRatio()
    val counterPct = (ratio.coerceAtMost(1f) * 100f).toInt().coerceIn(0, 100)
    val counterColor = when {
        ratio >= 1f -> TameColors.OverRed
        ratio >= 0.82f -> a.pop
        else -> a.primary
    }
    val counterMood = vm.counterMood()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF000000)),
    ) {
        // ── tappable feed (advances one reel per tap) ──
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        // 160deg: ~ top-left toward bottom (mostly downward)
                        colorStops = arrayOf(
                            0f to hue,
                            0.55f to Color(0xFF1A1A22),
                            1f to Color(0xFF000000),
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(0f, Float.POSITIVE_INFINITY),
                    ),
                )
                .tap { vm.scrollReel() },
        ) {
            // big translucent play circle centered at ~42% of height
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = BiasAlignment(0f, -0.16f), // vertical centre at 42% of height
            ) {
                Box(
                    modifier = Modifier
                        .size(74.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFFFFFFFF).copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center,
                ) {
                    SvgIcon(
                        shapes = listOf(VPath(d = "M8 5v14l11-7z", fill = true)),
                        size = 30.dp,
                        tint = Color(0xFFFFFFFF),
                    )
                }
            }

            // bottom content row
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(start = 18.dp, end = 18.dp, bottom = 40.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                // left: handle + follow + caption
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(9.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(50))
                                .background(hue)
                                .border(2.dp, Color(0xFFFFFFFF), RoundedCornerShape(50)),
                        )
                        Text(
                            text = reel.handle,
                            style = TextStyle(
                                fontFamily = Hanken,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color(0xFFFFFFFF),
                            ),
                        )
                        Text(
                            text = "Follow",
                            style = TextStyle(
                                fontFamily = Hanken,
                                fontWeight = FontWeight.Normal,
                                fontSize = 12.sp,
                                color = Color(0xFFFFFFFF).copy(alpha = 0.7f),
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .border(1.dp, Color(0xFFFFFFFF).copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 7.dp, vertical = 2.dp),
                        )
                    }
                    Text(
                        text = reel.caption,
                        style = TextStyle(
                            fontFamily = Hanken,
                            fontWeight = FontWeight.Normal,
                            fontSize = 14.sp,
                            lineHeight = 19.6.sp,
                            color = Color(0xFFFFFFFF).copy(alpha = 0.92f),
                        ),
                        modifier = Modifier
                            .padding(top = 10.dp)
                            .widthIn(max = 250.dp),
                    )
                }

                // right: action column
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    ActionStat(
                        icon = {
                            SvgIcon(
                                shapes = listOf(
                                    VPath(
                                        d = "M12 21s-7-4.5-9.5-9C1 9 2.5 5.5 6 5.5c2 0 3 1 4 2.5 1-1.5 2-2.5 4-2.5 3.5 0 5 3.5 3.5 6.5C19 16.5 12 21 12 21z",
                                        fill = true,
                                    ),
                                ),
                                size = 28.dp,
                                tint = Color(0xFFFFFFFF),
                            )
                        },
                        label = reel.likes,
                    )
                    ActionStat(
                        icon = {
                            SvgIcon(
                                shapes = listOf(
                                    VPath(
                                        d = "M21 11.5a8.4 8.4 0 01-12 7.6L3 21l1.9-6A8.4 8.4 0 1121 11.5z",
                                        fill = true,
                                    ),
                                ),
                                size = 27.dp,
                                tint = Color(0xFFFFFFFF),
                            )
                        },
                        label = reel.comments,
                    )
                    SvgIcon(
                        shapes = listOf(
                            VPath(
                                d = "M4 12v7a1 1 0 001 1h14a1 1 0 001-1v-7M16 6l-4-4-4 4M12 2v13",
                                fill = false,
                                sw = 2f,
                            ),
                        ),
                        size = 27.dp,
                        tint = Color(0xFFFFFFFF),
                    )
                }
            }
        }

        // ── close (X) top-left ──
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 18.dp, top = 58.dp)
                .size(38.dp)
                .clip(RoundedCornerShape(50))
                .background(Color(0xFF000000).copy(alpha = 0.35f))
                .tap { vm.exitReels() },
            contentAlignment = Alignment.Center,
        ) {
            SvgIcon(
                shapes = listOf(VPath(d = "M6 6l12 12M18 6L6 18", fill = false, sw = 2.4f)),
                size = 20.dp,
                tint = Color(0xFFFFFFFF),
            )
        }

        // ── counter widget top-right ──
        when (settings.counterStyle) {
            "ring" -> CounterRing(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 14.dp, top = 56.dp),
                value = settings.todayReels,
                color = counterColor,
                ratio = ratio,
            )
            "minimal" -> CounterMinimal(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 14.dp, top = 58.dp),
                value = settings.todayReels,
                color = counterColor,
            )
            else -> CounterBubble(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 14.dp, top = 56.dp),
                value = settings.todayReels,
                limit = settings.reelLimit,
                color = counterColor,
                pct = counterPct,
                mood = counterMood,
            )
        }
    }
}

@Composable
private fun ActionStat(icon: @Composable () -> Unit, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        icon()
        Text(
            text = label,
            style = TextStyle(
                fontFamily = Hanken,
                fontWeight = FontWeight.Normal,
                fontSize = 11.sp,
                color = Color(0xFFFFFFFF),
            ),
        )
    }
}

@Composable
private fun CounterBubble(
    modifier: Modifier,
    value: Int,
    limit: Int,
    color: Color,
    pct: Int,
    mood: String,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF10120F).copy(alpha = 0.62f))
            .border(1.dp, Color(0xFFFFFFFF).copy(alpha = 0.14f), RoundedCornerShape(20.dp))
            .padding(start = 8.dp, top = 8.dp, end = 13.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(50))
                .background(Color(0xFFFFFFFF).copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center,
        ) {
            FrankFill(
                mood = mood,
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(50)),
            )
        }
        Column {
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = "$value",
                    style = TextStyle(
                        fontFamily = Bricolage,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp,
                        lineHeight = 20.sp,
                        color = Color(0xFFFFFFFF),
                    ),
                )
                Text(
                    text = "/$limit",
                    style = TextStyle(
                        fontFamily = Hanken,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = Color(0xFFFFFFFF).copy(alpha = 0.55f),
                    ),
                )
            }
            // progress bar
            Box(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .width(62.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0xFFFFFFFF).copy(alpha = 0.18f)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(pct / 100f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(3.dp))
                        .background(color),
                )
            }
        }
    }
}

@Composable
private fun CounterRing(
    modifier: Modifier,
    value: Int,
    color: Color,
    ratio: Float,
) {
    Box(
        modifier = modifier
            .size(64.dp)
            .clip(RoundedCornerShape(50))
            .background(Color(0xFF10120F).copy(alpha = 0.62f))
            .border(1.dp, Color(0xFFFFFFFF).copy(alpha = 0.14f), RoundedCornerShape(50)),
        contentAlignment = Alignment.Center,
    ) {
        // ring: viewport 48, r=20, stroke-width 4, dasharray 125.66 (= 2*pi*20),
        // dashoffset = 125.66*(1-ratio); rotated -90deg so it starts at top.
        Canvas(modifier = Modifier.size(64.dp)) {
            val scale = size.minDimension / 48f
            val stroke = 4f * scale
            val r = 20f * scale
            val arcSize = Size(r * 2, r * 2)
            val topLeft = Offset(size.width / 2 - r, size.height / 2 - r)
            // track
            drawArc(
                color = Color(0xFFFFFFFF).copy(alpha = 0.16f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            // progress (start at top = -90deg, clockwise)
            val sweep = 360f * ratio.coerceIn(0f, 1f)
            if (sweep > 0f) {
                drawArc(
                    color = color,
                    startAngle = -90f,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
            }
        }
        Text(
            text = "$value",
            style = TextStyle(
                fontFamily = Bricolage,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp,
                color = Color(0xFFFFFFFF),
            ),
        )
    }
}

@Composable
private fun CounterMinimal(
    modifier: Modifier,
    value: Int,
    color: Color,
) {
    val t = rememberInfiniteTransition(label = "pulse")
    val pulse by t.animateFloat(
        initialValue = 1f, targetValue = 0.35f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "dot",
    )
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF10120F).copy(alpha = 0.6f))
            .border(1.dp, Color(0xFFFFFFFF).copy(alpha = 0.14f), RoundedCornerShape(18.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .alpha(pulse)
                .clip(RoundedCornerShape(50))
                .background(color),
        )
        Text(
            text = "$value",
            style = TextStyle(
                fontFamily = Bricolage,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 15.sp,
                color = Color(0xFFFFFFFF),
            ),
        )
    }
}
