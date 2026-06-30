package com.tame.app.ui.takeover

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
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
import com.tame.app.ui.theme.Bricolage
import com.tame.app.ui.theme.Hanken
import com.tame.app.ui.theme.TameColors
import com.tame.app.ui.theme.accent

@Composable
fun BlockTakeover(vm: AppViewModel) {
    val a = accent
    val bl = vm.block
    val style = vm.settings.blockStyle
    val name = bl.name
    val lifts = bl.lifts

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TameColors.InkDark)
            .padding(horizontal = 36.dp),
        contentAlignment = Alignment.Center,
    ) {
        // soft accent radial glow
        Box(
            modifier = Modifier
                .size(420.dp)
                .glow(a.primary.copy(alpha = 0.12f)),
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            when (style) {
                "fullstop" -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(22.dp),
                    ) {
                        Frank(
                            mood = "angry",
                            size = 104.dp,
                            modifier = Modifier.padding(bottom = 4.dp),
                        )
                        Text(
                            text = "NOT\nNOW",
                            textAlign = TextAlign.Center,
                            style = TextStyle(
                                fontFamily = Bricolage,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 72.sp,
                                lineHeight = (72 * 0.9f).sp,
                                letterSpacing = (-0.03).em,
                                color = Color.White,
                            ),
                        )
                        Text(
                            text = "$name is blocked. Back $lifts.",
                            textAlign = TextAlign.Center,
                            modifier = Modifier.widthIn(max = 240.dp),
                            style = TextStyle(
                                fontFamily = Hanken,
                                fontWeight = FontWeight.Normal,
                                fontSize = 16.sp,
                                lineHeight = (16 * 1.4f).sp,
                                color = TameColors.OnTakeoverSub,
                            ),
                        )
                    }
                }

                "calm" -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(24.dp),
                    ) {
                        Frank(
                            mood = "breathe",
                            size = 124.dp,
                            idle = false,
                            modifier = Modifier.breathe(5000),
                        )
                        Text(
                            text = "Let it pass.",
                            textAlign = TextAlign.Center,
                            style = TextStyle(
                                fontFamily = Bricolage,
                                fontWeight = FontWeight.Bold,
                                fontSize = 30.sp,
                                letterSpacing = (-0.01).em,
                                color = Color(0xFFEDEFEA),
                            ),
                        )
                        Text(
                            text = "$name is closed. Back $lifts.",
                            textAlign = TextAlign.Center,
                            modifier = Modifier.widthIn(max = 230.dp),
                            style = TextStyle(
                                fontFamily = Hanken,
                                fontWeight = FontWeight.Normal,
                                fontSize = 15.sp,
                                lineHeight = (15 * 1.5f).sp,
                                color = TameColors.OnTakeoverSub,
                            ),
                        )
                    }
                }

                else -> { // "frank" (default)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                    ) {
                        Frank(mood = "zen", size = 124.dp)
                        // "Blocked" pill
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(30.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                                .padding(horizontal = 15.dp, vertical = 7.dp),
                            horizontalArrangement = Arrangement.spacedBy(7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "BLOCKED",
                                style = TextStyle(
                                    fontFamily = Hanken,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    letterSpacing = 0.08.em,
                                    color = a.pop,
                                ),
                            )
                        }
                        Text(
                            text = "Ape's keeping $name shut.",
                            textAlign = TextAlign.Center,
                            modifier = Modifier.widthIn(max = 280.dp),
                            style = TextStyle(
                                fontFamily = Bricolage,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 32.sp,
                                lineHeight = (32 * 1.12f).sp,
                                letterSpacing = (-0.02).em,
                                color = Color.White,
                            ),
                        )
                        Text(
                            text = "Opens again $lifts.",
                            textAlign = TextAlign.Center,
                            style = TextStyle(
                                fontFamily = Hanken,
                                fontWeight = FontWeight.Normal,
                                fontSize = 15.sp,
                                color = TameColors.OnTakeoverSub,
                            ),
                        )
                    }
                }
            }

            // Back to home button
            Box(
                modifier = Modifier
                    .padding(top = 38.dp)
                    .height(56.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(Color.White)
                    .tap { vm.backHome() }
                    .padding(horizontal = 40.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Back to home",
                    style = TextStyle(
                        fontFamily = Hanken,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = TameColors.InkDark,
                    ),
                )
            }
        }
    }
}
