package com.tame.app.ui.takeover

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.tame.app.ui.AppViewModel
import com.tame.app.ui.components.Frank
import com.tame.app.ui.components.breathe
import com.tame.app.ui.components.glow
import com.tame.app.ui.components.tap
import com.tame.app.ui.mmss
import com.tame.app.ui.theme.Bricolage
import com.tame.app.ui.theme.Hanken
import com.tame.app.ui.theme.TameColors
import com.tame.app.ui.theme.accent

@Composable
fun FocusRunTakeover(vm: AppViewModel) {
    val a = accent
    val f = vm.focus ?: return

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TameColors.InkDark)
            .padding(horizontal = 36.dp),
        contentAlignment = Alignment.Center,
    ) {
        // Accent radial glow behind, breathing.
        Box(
            modifier = Modifier
                .size(440.dp)
                .breathe(6000)
                .glow(a.primary.copy(alpha = 0.2f)),
        )

        // Centered content stack + End focus button.
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Frank(
                    mood = "breathe",
                    size = 124.dp,
                    idle = false,
                    modifier = Modifier
                        .padding(bottom = 4.dp)
                        .breathe(5000),
                )

                Text(
                    text = "In focus",
                    style = TextStyle(
                        fontFamily = Hanken,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp,
                        letterSpacing = 0.14.em,
                        color = a.pop,
                        textAlign = TextAlign.Center,
                    ),
                )

                if (f.leftSecs != null) {
                    Text(
                        text = mmss(f.leftSecs),
                        style = TextStyle(
                            fontFamily = Bricolage,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 60.sp,
                            lineHeight = 60.sp,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                        ),
                    )
                } else {
                    Text(
                        text = "Frank's on guard",
                        style = TextStyle(
                            fontFamily = Bricolage,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 30.sp,
                            letterSpacing = (-0.01).em,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                        ),
                    )
                }

                Text(
                    text = "${f.label} · everything's paused",
                    modifier = Modifier.padding(top = 2.dp),
                    style = TextStyle(
                        fontFamily = Hanken,
                        fontWeight = FontWeight.Normal,
                        fontSize = 14.sp,
                        color = TameColors.OnTakeoverSub,
                        textAlign = TextAlign.Center,
                    ),
                )
            }

            // End focus button — 38px below the content stack.
            Box(
                modifier = Modifier
                    .padding(top = 38.dp)
                    .height(54.dp)
                    .clip(RoundedCornerShape(30.dp))
                    .background(Color.White.copy(alpha = 0.1f))
                    .tap { vm.endFocus() }
                    .padding(horizontal = 34.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "End focus",
                    style = TextStyle(
                        fontFamily = Hanken,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.White,
                    ),
                )
            }
        }
    }
}
