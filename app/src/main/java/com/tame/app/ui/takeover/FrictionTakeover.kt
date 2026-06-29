package com.tame.app.ui.takeover

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.tame.app.ui.AppViewModel
import com.tame.app.ui.components.Frank
import com.tame.app.ui.components.breathe
import com.tame.app.ui.components.tap
import com.tame.app.ui.theme.Bricolage
import com.tame.app.ui.theme.TameColors
import com.tame.app.ui.theme.accent

@Composable
fun FrictionTakeover(vm: AppViewModel) {
    val a = accent
    val fr = vm.friction

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TameColors.FrictionBg),
        contentAlignment = Alignment.Center,
    ) {
        // ambient glow: 380x380 #5B7BE0 opacity .16 blur 70px
        Box(
            modifier = Modifier
                .size(380.dp)
                .blur(70.dp)
                .clip(CircleShape)
                .background(Color(0xFF5B7BE0).copy(alpha = 0.16f)),
        )

        if (fr.choosing) {
            Column(
                modifier = Modifier.padding(horizontal = 36.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(22.dp),
            ) {
                Frank(mood = "breathe", size = 104.dp, idle = false)

                Text(
                    text = "Still want ${fr.targetName}?",
                    textAlign = TextAlign.Center,
                    style = TextStyle(
                        fontFamily = Bricolage,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 30.sp,
                        lineHeight = 34.5.sp, // 1.15
                        letterSpacing = (-0.02).em,
                        color = Color.White,
                    ),
                    modifier = Modifier.widthIn(max = 280.dp),
                )

                Column(
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .fillMaxWidth()
                        .widthIn(max = 300.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // No — I'm good (accent)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp)
                            .clip(RoundedCornerShape(32.dp))
                            .background(a.primary)
                            .tap { vm.frictionStay() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "No — I'm good",
                            style = TextStyle(
                                fontFamily = Bricolage,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                color = Color.White,
                            ),
                        )
                    }
                    // Open anyway (muted)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clip(RoundedCornerShape(32.dp))
                            .tap { vm.frictionOpen() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "Open anyway",
                            style = TextStyle(
                                fontFamily = Bricolage,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                color = Color(0xFF8E9AD8),
                            ),
                        )
                    }
                }
            }
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(30.dp),
            ) {
                Box(
                    modifier = Modifier.size(200.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    // radial gradient glow + ring + Frank, all breathing together
                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .breathe(4000),
                        contentAlignment = Alignment.Center,
                    ) {
                        // radial-gradient(circle,#6E8BEA 0%,rgba(110,139,234,0) 70%)
                        Box(
                            modifier = Modifier
                                .size(200.dp)
                                .clip(CircleShape)
                                .drawBehind {
                                    drawCircle(
                                        brush = Brush.radialGradient(
                                            0f to Color(0xFF6E8BEA),
                                            0.7f to Color(0x006E8BEA),
                                            center = Offset(size.width / 2f, size.height / 2f),
                                            radius = size.width / 2f,
                                        ),
                                    )
                                },
                        )
                        // 150x150 ring, 2px white .25
                        Box(
                            modifier = Modifier
                                .size(150.dp)
                                .drawBehind {
                                    drawCircle(
                                        color = Color.White.copy(alpha = 0.25f),
                                        radius = size.minDimension / 2f - 1.dp.toPx(),
                                        style = Stroke(width = 2.dp.toPx()),
                                    )
                                },
                        )
                        Frank(mood = "breathe", size = 104.dp, idle = false)
                    }
                }

                Text(
                    text = "Breathe with Frank",
                    style = TextStyle(
                        fontFamily = Bricolage,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 22.sp,
                        letterSpacing = 0.01.em,
                        color = Color(0xFFDFE4FB),
                    ),
                )

                Text(
                    text = "${fr.left}",
                    modifier = Modifier.wrapContentHeight(),
                    style = TextStyle(
                        fontFamily = Bricolage,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 52.sp,
                        lineHeight = 52.sp, // line-height:1
                        color = Color.White,
                    ),
                )
            }
        }
    }
}
