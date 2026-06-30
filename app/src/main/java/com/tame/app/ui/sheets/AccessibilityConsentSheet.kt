package com.tame.app.ui.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.tame.app.ui.AppViewModel
import com.tame.app.ui.components.Frank
import com.tame.app.ui.components.TameIcons
import com.tame.app.ui.components.tap
import com.tame.app.ui.theme.Bricolage
import com.tame.app.ui.theme.Hanken
import com.tame.app.ui.theme.TameColors
import com.tame.app.ui.theme.accent

/**
 * Prominent in-app disclosure shown right before we send the user to enable Tame's
 * Accessibility access. Required by Google Play for non-accessibility-tool use: it must
 * appear in the normal flow (not buried in settings), explain what is accessed and how
 * it's used, and require an affirmative action with a clear way to decline.
 */
@Composable
fun AccessibilityConsentSheet(vm: AppViewModel) {
    val a = accent
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC0B0B0B)) // scrim
            .tap { vm.dismissConsent() },
        contentAlignment = Alignment.BottomCenter,
    ) {
        // card — consume taps so they don't fall through to the scrim
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(TameColors.Surface)
                .tap { }
                .padding(start = 26.dp, end = 26.dp, top = 24.dp, bottom = 34.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Frank(mood = "neutral", size = 92.dp)
            Spacer(Modifier.height(10.dp))
            Text(
                "Let Tame step in for you",
                style = TextStyle(
                    fontFamily = Bricolage, fontWeight = FontWeight.ExtraBold,
                    fontSize = 24.sp, letterSpacing = (-0.01).em, color = TameColors.Ink,
                ),
            )
            Spacer(Modifier.height(14.dp))

            DisclosureRow(
                accent = a.primary,
                title = "Why Tame needs this",
                body = "To block or add a pause to the feeds and apps you pick, Tame uses Accessibility access to see what's on your screen.",
            )
            DisclosureRow(
                accent = a.primary,
                title = "What it reads",
                body = "On-screen content, only to recognise short-form feeds (Reels, Shorts, For You, Spotlight) and the apps you chose to limit. It never reads passwords or messages for any other purpose.",
            )
            DisclosureRow(
                accent = a.primary,
                title = "Stays on your phone",
                body = "Tame has no account and no servers. Nothing you do is collected, sent off your device, or shared with anyone.",
            )

            Spacer(Modifier.height(20.dp))
            // affirmative action
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(30.dp))
                    .background(a.primary)
                    .tap { vm.acceptConsent() },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Turn on protection",
                    style = TextStyle(
                        fontFamily = Hanken, fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp, color = Color.White,
                    ),
                )
            }
            Spacer(Modifier.height(6.dp))
            // decline
            Box(
                modifier = Modifier.fillMaxWidth().height(48.dp).tap { vm.dismissConsent() },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Not now",
                    style = TextStyle(
                        fontFamily = Hanken, fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp, color = TameColors.TextFaint,
                    ),
                )
            }
        }
    }
}

@Composable
private fun DisclosureRow(accent: Color, title: String, body: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(TameColors.ChipBg),
            contentAlignment = Alignment.Center,
        ) {
            TameIcons.Check(15.dp, accent, sw = 3f)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = TextStyle(
                    fontFamily = Hanken, fontWeight = FontWeight.Bold,
                    fontSize = 14.5.sp, color = TameColors.Ink,
                ),
            )
            Spacer(Modifier.height(2.dp))
            Text(
                body,
                style = TextStyle(
                    fontFamily = Hanken, fontWeight = FontWeight.Normal,
                    fontSize = 13.sp, lineHeight = (13 * 1.4).sp, color = TameColors.TextMuted,
                ),
            )
        }
    }
}
