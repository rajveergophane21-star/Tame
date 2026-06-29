package com.tame.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.tame.app.ui.theme.TameColors
import com.tame.app.ui.theme.accent

/** The pill toggle used throughout the design (50×30 track, 24 knob). */
@Composable
fun TameSwitch(on: Boolean, modifier: Modifier = Modifier) {
    val track by animateColorAsState(if (on) accent.primary else TameColors.TrackOff, label = "track")
    val knobX by animateDpAsState(if (on) 23.dp else 3.dp, label = "knob")
    Box(
        modifier = modifier
            .size(50.dp, 30.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(track)
    ) {
        Box(
            modifier = Modifier
                .offset(x = knobX, y = 3.dp)
                .size(24.dp)
                .shadow(2.dp, CircleShape)
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}
