package com.tame.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tame.app.ui.Screen
import com.tame.app.ui.theme.Hanken
import com.tame.app.ui.theme.TameColors
import com.tame.app.ui.theme.accent

/** Status bar overlay — 9:41 plus signal/wifi/battery glyphs. */
@Composable
fun StatusBar(dark: Boolean, modifier: Modifier = Modifier) {
    val tint = if (dark) Color.White else Color(0xFF1A211D)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .padding(start = 28.dp, end = 28.dp, top = 17.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            "9:41",
            style = TextStyle(fontFamily = Hanken, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = tint),
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            // signal bars
            SvgIcon(
                listOf(
                    VRect(0f, 9f, 3f, 4f, rx = 1f, fill = true),
                    VRect(5f, 6f, 3f, 7f, rx = 1f, fill = true),
                    VRect(10f, 3f, 3f, 10f, rx = 1f, fill = true),
                    VRect(15f, 0f, 3f, 13f, rx = 1f, fill = true),
                ),
                size = 17.dp, tint = tint, viewport = 18f,
            )
            // wifi
            SvgIcon(listOf(VPath("M8 11.5L1 4.5a9.5 9.5 0 0114 0L8 11.5z", fill = true)), size = 16.dp, tint = tint, viewport = 16f)
            // battery
            SvgIcon(
                listOf(
                    VRect(1f, 1f, 21f, 11f, rx = 3f, sw = 1.4f),
                    VRect(3f, 3f, 15f, 7f, rx = 1.5f, fill = true),
                    VRect(23.5f, 4f, 2f, 5f, rx = 1f, fill = true),
                ),
                size = 24.dp, tint = tint, viewport = 26f,
            )
        }
    }
}

/** Bottom navigation bar (Home / Rules / Habits / You). */
@Composable
fun BottomNav(current: Screen, onHome: () -> Unit, onRules: () -> Unit, onHabits: () -> Unit, onYou: () -> Unit, modifier: Modifier = Modifier) {
    val rulesActive = current == Screen.RULES || current == Screen.DETAIL || current == Screen.ADD_RULE
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(TameColors.Surface.copy(alpha = 0.94f))
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 16.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.SpaceAround,
    ) {
        NavItem(current == Screen.HOME, "Home", onHome, Modifier.weight(1f)) { c -> TameIcons.NavHome(24.dp, c) }
        NavItem(rulesActive, "Rules", onRules, Modifier.weight(1f)) { c -> TameIcons.Shield(24.dp, c) }
        NavItem(current == Screen.HABITS, "Habits", onHabits, Modifier.weight(1f)) { c -> TameIcons.NavHabits(24.dp, c) }
        NavItem(current == Screen.SETTINGS, "You", onYou, Modifier.weight(1f)) { c -> TameIcons.NavYou(24.dp, c) }
    }
}

@Composable
private fun NavItem(active: Boolean, label: String, onClick: () -> Unit, modifier: Modifier, icon: @Composable (Color) -> Unit) {
    val c = if (active) accent.primary else TameColors.NavInactive
    Column(
        modifier = modifier.clickable(indication = null, interactionSource = remember { MutableInteractionSource() }, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .height(3.dp)
                .width(18.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(if (active) c else Color.Transparent)
        )
        icon(c)
        Text(label, style = TextStyle(fontFamily = Hanken, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, color = c))
    }
}

/** Home-gesture pill at the very bottom. */
@Composable
fun GesturePill(dark: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .width(134.dp)
            .height(5.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(if (dark) Color.White.copy(alpha = 0.5f) else Color(0xFF1A211D).copy(alpha = 0.32f))
    )
}
