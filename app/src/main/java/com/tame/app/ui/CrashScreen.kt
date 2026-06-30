package com.tame.app.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tame.app.ui.components.tap
import com.tame.app.ui.theme.Bricolage
import com.tame.app.ui.theme.Hanken
import com.tame.app.ui.theme.TameColors
import com.tame.app.ui.theme.accent

/** Shown on next launch after a crash so the user can screenshot/copy the error. */
@Composable
fun CrashScreen(text: String, onDismiss: () -> Unit) {
    val a = accent
    val ctx = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TameColors.Surface)
            .padding(start = 22.dp, end = 22.dp, top = 64.dp, bottom = 34.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            "Something crashed",
            style = TextStyle(fontFamily = Bricolage, fontWeight = FontWeight.ExtraBold, fontSize = 26.sp, color = TameColors.Ink),
        )
        Text(
            "Please screenshot or copy this and send it over so it can be fixed.",
            style = TextStyle(fontFamily = Hanken, fontSize = 14.sp, color = TameColors.TextMuted),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF11150F))
                .verticalScroll(rememberScrollState())
                .padding(14.dp),
        ) {
            Text(
                text,
                style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color(0xFFE6E9E2)),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            Box2("Copy", filled = false, a.primary, Modifier.weight(1f)) { copyToClipboard(ctx, text) }
            Box2("Dismiss", filled = true, a.primary, Modifier.weight(1f)) { onDismiss() }
        }
    }
}

@Composable
private fun Box2(label: String, filled: Boolean, accentColor: Color, modifier: Modifier, onClick: () -> Unit) {
    Row(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(if (filled) accentColor else TameColors.Card)
            .tap { onClick() },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = TextStyle(
                fontFamily = Hanken, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                color = if (filled) Color.White else TameColors.Ink,
            ),
        )
    }
}

private fun copyToClipboard(ctx: Context, text: String) {
    val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("APE crash", text))
}
