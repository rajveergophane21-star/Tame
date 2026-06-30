package com.tame.app.ui.sheets

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tame.app.ui.AppViewModel
import com.tame.app.ui.components.tap
import com.tame.app.ui.theme.Bricolage
import com.tame.app.ui.theme.Hanken
import com.tame.app.ui.theme.TameColors

@Composable
fun FocusSheet(vm: AppViewModel) {
    // Card slide-up: animate from below to resting position (sheetUp .32s).
    val slide by animateFloatAsState(
        targetValue = 0f,
        animationSpec = tween(durationMillis = 320, easing = LinearOutSlowInEasing),
        label = "focusSheetSlide",
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // Scrim — rgba(16,22,15,.45). Tap to close.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(TameColors.Scrim)
                .tap { vm.closeSheet() },
        )

        // Bottom card.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .graphicsLayer { translationY = slide * 120f }
                .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
                .background(TameColors.Surface)
                .padding(start = 22.dp, top = 14.dp, end = 22.dp, bottom = 34.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Grabber bar.
            Box(
                modifier = Modifier
                    .padding(bottom = 18.dp)
                    .width(42.dp)
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(TameColors.Grabber),
            )

            // Title.
            Text(
                text = "Focus for…",
                modifier = Modifier.fillMaxWidth(),
                style = TextStyle(
                    fontFamily = Bricolage,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp,
                    color = TameColors.Ink,
                    textAlign = TextAlign.Center,
                ),
            )

            // Subtitle.
            Text(
                text = "Everything closes. Frank goes quiet.",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                style = TextStyle(
                    fontFamily = Hanken,
                    fontWeight = FontWeight.Normal,
                    fontSize = 14.sp,
                    color = TameColors.TextFaint,
                    textAlign = TextAlign.Center,
                ),
            )

            // 2x2 grid of options.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FocusOption("15 min", Modifier.weight(1f)) { vm.focus15() }
                    FocusOption("30 min", Modifier.weight(1f)) { vm.focus30() }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FocusOption("1 hour", Modifier.weight(1f)) { vm.focus60() }
                    FocusOption("Until I stop", Modifier.weight(1f), dark = true) { vm.focusOpen() }
                }
            }
        }
    }
}

@Composable
private fun FocusOption(
    label: String,
    modifier: Modifier = Modifier,
    dark: Boolean = false,
    onClick: () -> Unit,
) {
    val base = if (dark) {
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(TameColors.Ink)
    } else {
        // box-shadow: 0 1px 0 #ECE8DC -> flat card with a 1px bottom edge.
        modifier
            .shadow(1.dp, RoundedCornerShape(18.dp), clip = false)
            .clip(RoundedCornerShape(18.dp))
            .background(TameColors.Card)
    }

    Box(
        modifier = base
            .tap { onClick() }
            .padding(18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = TextStyle(
                fontFamily = Bricolage,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp,
                color = if (dark) Color.White else TameColors.Ink,
            ),
        )
    }
}
