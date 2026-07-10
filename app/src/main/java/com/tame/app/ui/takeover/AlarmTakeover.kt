package com.tame.app.ui.takeover

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.tame.app.ui.AppViewModel
import com.tame.app.ui.components.Frank
import com.tame.app.ui.components.glow
import com.tame.app.ui.components.tap
import com.tame.app.ui.theme.Bricolage
import com.tame.app.ui.theme.Hanken
import com.tame.app.ui.theme.TameColors
import com.tame.app.ui.theme.accent

@Composable
fun AlarmTakeover(vm: AppViewModel) {
    val habit = vm.alarmHabit() ?: return
    val a = accent

    // ringGlow pulse — opacity oscillates around the design's .12 base.
    val ringGlow = rememberInfiniteTransition(label = "ringGlow")
    val glowAlpha by ringGlow.animateFloat(
        initialValue = 0.10f,
        targetValue = 0.18f,
        animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Reverse),
        label = "glowAlpha",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TameColors.InkDark)
            .padding(horizontal = 36.dp),
        contentAlignment = Alignment.Center,
    ) {
        // radial pop glow behind everything
        Box(
            modifier = Modifier
                .size(380.dp)
                .alpha(glowAlpha)
                .glow(a.pop),
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Frank(mood = "neutral", size = 108.dp, idle = false, shake = true)

                Text(
                    text = habit.remind,
                    style = TextStyle(
                        fontFamily = Bricolage,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 46.sp,
                        lineHeight = 46.sp,
                        letterSpacing = (-0.01).em,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                    ),
                )

                Text(
                    text = habit.name,
                    style = TextStyle(
                        fontFamily = Hanken,
                        fontWeight = FontWeight.Normal,
                        fontSize = 17.sp,
                        color = TameColors.OnDarkSub,
                        textAlign = TextAlign.Center,
                    ),
                )
            }

            Column(
                modifier = Modifier
                    .padding(top = 40.dp)
                    .fillMaxWidth()
                    .widthIn(max = 300.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // "Done — mark it off"
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .clip(RoundedCornerShape(34.dp))
                        .background(a.pop)
                        .tap { vm.stopAlarm() },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Done — mark it off",
                        style = TextStyle(
                            fontFamily = Hanken,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 17.sp,
                            // fixed dark ink: sits on the gold pop button (Ink flips light in dark mode)
                            color = TameColors.InkDark,
                        ),
                    )
                }

                // "Not now"
                Box(
                    modifier = Modifier
                        .height(44.dp)
                        .wrapContentHeight(Alignment.CenterVertically)
                        .tap { vm.dismissAlarm() },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Not now",
                        style = TextStyle(
                            fontFamily = Hanken,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            color = TameColors.OnDarkSub,
                        ),
                    )
                }
            }
        }
    }
}
