package com.tame.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.unit.sp
import com.tame.app.ui.ToastState
import com.tame.app.ui.theme.Hanken
import com.tame.app.ui.theme.TameColors
import com.tame.app.ui.theme.accent

/** The dark toast pill shown for confirmations / "locked" buzzes. */
@Composable
fun ToastBubble(toast: ToastState?, modifier: Modifier = Modifier) {
    AnimatedVisibility(visible = toast != null, enter = fadeIn(), exit = fadeOut(), modifier = modifier) {
        val t = toast ?: return@AnimatedVisibility
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(30.dp))
                .background(TameColors.Ink)
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            if (t.ok) TameIcons.Check(16.dp, accent.primary) else TameIcons.Lock(16.dp, accent.pop, sw = 2.4f)
            Text(
                t.message,
                style = TextStyle(fontFamily = Hanken, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color.White),
            )
        }
    }
}
