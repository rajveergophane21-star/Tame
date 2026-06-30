package com.tame.app.service

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.tame.app.ui.components.Frank
import com.tame.app.ui.components.breathe
import com.tame.app.ui.components.glow
import com.tame.app.ui.components.tap
import com.tame.app.ui.theme.Bricolage
import com.tame.app.ui.theme.Hanken
import com.tame.app.ui.theme.LocalAccent
import com.tame.app.ui.theme.TameColors
import kotlinx.coroutines.delay

/** Stateless block stop screen. [style] = frank | fullstop | calm. */
@Composable
fun BlockContent(style: String, name: String, lifts: String, onBack: () -> Unit) {
    val accent = LocalAccent.current
    Box(Modifier.fillMaxSize().background(TameColors.InkDark).padding(horizontal = 36.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.size(420.dp).glow(accent.primary.copy(alpha = 0.12f)))
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            when (style) {
                "fullstop" -> Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(22.dp)) {
                    Frank("angry", 104.dp)
                    Text("NOT\nNOW", textAlign = TextAlign.Center, style = TextStyle(fontFamily = Bricolage, fontWeight = FontWeight.ExtraBold, fontSize = 72.sp, lineHeight = 64.sp, letterSpacing = (-0.03).em, color = Color.White))
                    Text("$name is blocked. Back $lifts.", textAlign = TextAlign.Center, modifier = Modifier.widthIn(max = 240.dp), style = TextStyle(fontFamily = Hanken, fontSize = 16.sp, lineHeight = 22.sp, color = TameColors.OnTakeoverSub))
                }
                "calm" -> Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(24.dp)) {
                    Frank("breathe", 124.dp, idle = false, modifier = Modifier.breathe(5000))
                    Text("Let it pass.", style = TextStyle(fontFamily = Bricolage, fontWeight = FontWeight.Bold, fontSize = 30.sp, color = Color(0xFFEDEFEA)))
                    Text("$name is closed. Back $lifts.", textAlign = TextAlign.Center, modifier = Modifier.widthIn(max = 230.dp), style = TextStyle(fontFamily = Hanken, fontSize = 15.sp, lineHeight = 22.sp, color = TameColors.OnTakeoverSub))
                }
                else -> Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    Frank("zen", 124.dp)
                    Box(Modifier.clip(RoundedCornerShape(30.dp)).background(Color.White.copy(alpha = 0.08f)).padding(horizontal = 15.dp, vertical = 7.dp)) {
                        Text("BLOCKED", style = TextStyle(fontFamily = Hanken, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 0.08.em, color = accent.pop))
                    }
                    Text("Frank's keeping $name shut.", textAlign = TextAlign.Center, modifier = Modifier.widthIn(max = 280.dp), style = TextStyle(fontFamily = Bricolage, fontWeight = FontWeight.ExtraBold, fontSize = 32.sp, lineHeight = 36.sp, letterSpacing = (-0.02).em, color = Color.White))
                    Text("Opens again $lifts.", style = TextStyle(fontFamily = Hanken, fontSize = 15.sp, color = TameColors.OnTakeoverSub))
                }
            }
            Box(
                Modifier.padding(top = 38.dp).height(56.dp).clip(RoundedCornerShape(32.dp)).background(Color.White).tap { onBack() }.padding(horizontal = 40.dp),
                contentAlignment = Alignment.Center,
            ) { Text("Back to home", style = TextStyle(fontFamily = Hanken, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = TameColors.InkDark)) }
        }
    }
}

/** Stateless friction stop screen: breathe countdown, then choose. */
@Composable
fun FrictionContent(name: String, onStay: () -> Unit, onOpen: () -> Unit) {
    val accent = LocalAccent.current
    var left by remember { mutableIntStateOf(8) }
    LaunchedEffect(Unit) { while (left > 0) { delay(1000); left -= 1 } }
    Box(Modifier.fillMaxSize().background(TameColors.FrictionBg), contentAlignment = Alignment.Center) {
        Box(Modifier.size(380.dp).glow(Color(0xFF5B7BE0).copy(alpha = 0.16f)))
        if (left > 0) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(30.dp)) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(200.dp)) {
                    Box(Modifier.size(150.dp).clip(CircleShape).background(Color(0xFF6E8BEA).copy(alpha = 0.18f)).breathe(4000))
                    Frank("breathe", 104.dp, idle = false, modifier = Modifier.breathe(4000))
                }
                Text("Breathe with Frank", style = TextStyle(fontFamily = Bricolage, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, color = Color(0xFFDFE4FB)))
                Text("$left", style = TextStyle(fontFamily = Bricolage, fontWeight = FontWeight.ExtraBold, fontSize = 52.sp, color = Color.White))
            }
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(22.dp), modifier = Modifier.padding(horizontal = 36.dp)) {
                Frank("breathe", 104.dp, idle = false)
                Text("Still want $name?", textAlign = TextAlign.Center, style = TextStyle(fontFamily = Bricolage, fontWeight = FontWeight.ExtraBold, fontSize = 30.sp, letterSpacing = (-0.02).em, color = Color.White))
                Box(Modifier.fillMaxWidth().height(58.dp).clip(RoundedCornerShape(32.dp)).background(accent.primary).tap { onStay() }, contentAlignment = Alignment.Center) {
                    Text("No — I'm good", style = TextStyle(fontFamily = Hanken, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = Color.White))
                }
                Box(Modifier.height(52.dp).tap { onOpen() }, contentAlignment = Alignment.Center) {
                    Text("Open anyway", style = TextStyle(fontFamily = Hanken, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Color(0xFF8E9AD8)))
                }
            }
        }
    }
}
