package com.tame.app.service

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
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
import com.tame.app.TameApp
import com.tame.app.ui.components.Frank
import com.tame.app.ui.components.breathe
import com.tame.app.ui.components.tap
import com.tame.app.ui.theme.Accents
import com.tame.app.ui.theme.Bricolage
import com.tame.app.ui.theme.Hanken
import com.tame.app.ui.theme.LocalAccent
import com.tame.app.ui.theme.TameColors
import com.tame.app.ui.theme.TameTheme
import kotlinx.coroutines.delay

/**
 * The real over-other-apps stop screen, launched by [TameAccessibilityService].
 * BLOCK = hard stop → back to home. FRICTION = breathe, then choose to stay out or open anyway.
 */
class StopActivity : ComponentActivity() {

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) setShowWhenLocked(true)

        val mode = intent.getStringExtra(EXTRA_MODE) ?: "block"
        val name = intent.getStringExtra(EXTRA_NAME) ?: "this"
        val lifts = intent.getStringExtra(EXTRA_LIFTS) ?: "later today"
        val blockStyle = intent.getStringExtra(EXTRA_BLOCK_STYLE) ?: "frank"
        val palette = Accents.byKey(TameApp.repo.snapshot().settings.accentKey)

        setContent {
            TameTheme(accent = palette) {
                if (mode == "friction") {
                    FrictionStop(name = name, onStay = { countTurnback(); goHome() }, onOpen = { finish() })
                } else {
                    BlockStop(name = name, lifts = lifts, style = blockStyle, onBack = { goHome() })
                }
            }
        }
    }

    private fun goHome() {
        startActivity(Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        })
        finish()
    }

    private fun countTurnback() {
        TameApp.repo.updateBlocking { it.copy(settings = it.settings.copy(turnbacks = it.settings.turnbacks + 1)) }
    }

    override fun onBackPressed() { goHome() }

    companion object {
        const val EXTRA_MODE = "mode"
        const val EXTRA_NAME = "name"
        const val EXTRA_LIFTS = "lifts"
        const val EXTRA_BLOCK_STYLE = "block_style"
    }
}

@Composable
private fun BlockStop(name: String, lifts: String, style: String, onBack: () -> Unit) {
    Box(Modifier.fillMaxSize().background(TameColors.InkDark), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp)) {
            when (style) {
                "fullstop" -> {
                    Frank("angry", 104.dp)
                    Text("NOT\nNOW", textAlign = TextAlign.Center, style = TextStyle(fontFamily = Bricolage, fontWeight = FontWeight.ExtraBold, fontSize = 72.sp, lineHeight = 64.sp, color = Color.White, letterSpacing = (-0.03).em))
                    Text("$name is blocked. Back $lifts.", textAlign = TextAlign.Center, style = TextStyle(fontFamily = Hanken, fontSize = 16.sp, color = TameColors.OnTakeoverSub))
                }
                "calm" -> {
                    Frank("breathe", 124.dp, idle = false, modifier = Modifier.breathe(5000))
                    Text("Let it pass.", style = TextStyle(fontFamily = Bricolage, fontWeight = FontWeight.Bold, fontSize = 30.sp, color = Color(0xFFEDEFEA)))
                    Text("$name is closed. Back $lifts.", textAlign = TextAlign.Center, style = TextStyle(fontFamily = Hanken, fontSize = 15.sp, color = TameColors.OnTakeoverSub))
                }
                else -> {
                    Frank("zen", 124.dp)
                    Box(Modifier.clip(RoundedCornerShape(30.dp)).background(Color.White.copy(alpha = 0.08f)).padding(horizontal = 15.dp, vertical = 7.dp)) {
                        Text("BLOCKED", style = TextStyle(fontFamily = Hanken, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 0.08.em, color = LocalAccent.current.pop))
                    }
                    Text("Frank's keeping $name shut.", textAlign = TextAlign.Center, style = TextStyle(fontFamily = Bricolage, fontWeight = FontWeight.ExtraBold, fontSize = 32.sp, color = Color.White, letterSpacing = (-0.02).em))
                    Text("Opens again $lifts.", style = TextStyle(fontFamily = Hanken, fontSize = 15.sp, color = TameColors.OnTakeoverSub))
                }
            }
        }
        Box(
            Modifier.align(Alignment.BottomCenter).padding(bottom = 48.dp).height(56.dp)
                .clip(RoundedCornerShape(32.dp)).background(Color.White).padding(horizontal = 40.dp).tap { onBack() },
            contentAlignment = Alignment.Center,
        ) {
            Text("Back to home", style = TextStyle(fontFamily = Hanken, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = TameColors.InkDark))
        }
    }
}

@Composable
private fun FrictionStop(name: String, onStay: () -> Unit, onOpen: () -> Unit) {
    var left by remember { mutableIntStateOf(8) }
    LaunchedEffect(Unit) {
        while (left > 0) { delay(1000); left -= 1 }
    }
    Box(Modifier.fillMaxSize().background(TameColors.FrictionBg), contentAlignment = Alignment.Center) {
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
                Text("Still want $name?", textAlign = TextAlign.Center, style = TextStyle(fontFamily = Bricolage, fontWeight = FontWeight.ExtraBold, fontSize = 30.sp, color = Color.White, letterSpacing = (-0.02).em))
                Box(
                    Modifier.fillMaxWidth().height(58.dp).clip(RoundedCornerShape(32.dp))
                        .background(LocalAccent.current.primary).tap { onStay() },
                    contentAlignment = Alignment.Center,
                ) { Text("No — I'm good", style = TextStyle(fontFamily = Hanken, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = Color.White)) }
                Box(Modifier.height(52.dp).tap { onOpen() }, contentAlignment = Alignment.Center) {
                    Text("Open anyway", style = TextStyle(fontFamily = Hanken, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Color(0xFF8E9AD8)))
                }
            }
        }
    }
}
