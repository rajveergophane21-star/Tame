package com.tame.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.tame.app.data.model.AppCatalog
import com.tame.app.ui.AppViewModel
import com.tame.app.ui.components.AppSquare
import com.tame.app.ui.components.SvgIcon
import com.tame.app.ui.components.TameIcons
import com.tame.app.ui.components.VPath
import com.tame.app.ui.components.tap
import com.tame.app.ui.theme.Bricolage
import com.tame.app.ui.theme.Hanken
import com.tame.app.ui.theme.TameColors
import com.tame.app.ui.theme.accent

private data class OnbStep(val title: String, val sub: String, val mood: String)

private val onbSteps = listOf(
    OnbStep("Meet Frank.", "He's your scroll instinct. Right now — wired.", "panic"),
    OnbStep("He never gets full.", "Every reel asks for one more. Frank obeys.", "worried"),
    OnbStep("Two switches.", "Tame needs these to step in for you.", "neutral"),
    OnbStep("What pulls you in?", "Pick the apps you lose time to.", "neutral"),
    OnbStep("Now Frank can rest.", "You set the limits. He'll relax.", "happy"),
)

@Composable
fun OnboardingScreen(vm: AppViewModel) {
    val a = accent
    val onbBg = Color(0xFFF5F3EC)
    val step = onbSteps.getOrElse(vm.onbStep) { onbSteps[0] }

    Box(modifier = Modifier.fillMaxSize().background(onbBg)) {
      Column(modifier = Modifier.fillMaxSize()) {
        // ── content area ──
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(start = 34.dp, top = 70.dp, end = 34.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // skip is positioned absolute top-right of the content area in the design;
            // we anchor it in a thin top row so it sits at top:54 right:24 visually.
            Box(modifier = Modifier.fillMaxWidth()) {
                // Frank
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier.height(188.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        com.tame.app.ui.components.Frank(mood = step.mood, size = 158.dp)
                    }

                    Spacer(Modifier.height(18.dp))
                    Text(
                        text = step.title,
                        textAlign = TextAlign.Center,
                        style = TextStyle(
                            fontFamily = Bricolage,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 34.sp,
                            lineHeight = (34 * 1.05f).sp,
                            letterSpacing = (-0.02).em,
                            color = TameColors.Ink,
                        ),
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = step.sub,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.widthIn(max = 260.dp),
                        style = TextStyle(
                            fontFamily = Hanken,
                            fontWeight = FontWeight.Normal,
                            fontSize = 16.sp,
                            lineHeight = (16 * 1.45f).sp,
                            color = TameColors.TextMuted,
                        ),
                    )

                    when (vm.onbStep) {
                        2 -> {
                            Spacer(Modifier.height(26.dp))
                            PermsBlock(vm)
                        }
                        3 -> {
                            Spacer(Modifier.height(24.dp))
                            AppsGrid(vm, onbBg)
                        }
                    }
                }

            }
        }

        // ── bottom bar ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 28.dp, top = 18.dp, end = 28.dp, bottom = 34.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (vm.onbStep > 0) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .shadow(6.dp, CircleShape, clip = false)
                        .clip(CircleShape)
                        .background(TameColors.Card)
                        .tap { vm.onbBack() },
                    contentAlignment = Alignment.Center,
                ) {
                    TameIcons.ChevronLeft(size = 20.dp, tint = TameColors.Ink)
                }
            }

            // progress dots
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                onbSteps.indices.forEach { i ->
                    val active = i == vm.onbStep
                    Box(
                        modifier = Modifier
                            .height(7.dp)
                            .width(if (active) 22.dp else 7.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (active) a.primary else TameColors.Grabber)
                    )
                }
            }

            // Next / Let's go pill
            Row(
                modifier = Modifier
                    .height(54.dp)
                    .shadow(8.dp, RoundedCornerShape(30.dp), clip = false)
                    .clip(RoundedCornerShape(30.dp))
                    .background(a.primary)
                    .tap { vm.onbNext() }
                    .padding(horizontal = 26.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                Text(
                    text = if (vm.onbStep >= 4) "Let's go" else "Next",
                    style = TextStyle(
                        fontFamily = Hanken,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.White,
                    ),
                )
                TameIcons.ArrowRight(size = 19.dp, tint = Color.White)
            }
        }
      }
      // skip — pinned to the top of the screen so it stays put across steps
      Text(
          text = "skip",
          modifier = Modifier
              .align(Alignment.TopEnd)
              .padding(top = 54.dp, end = 24.dp)
              .tap { vm.finishOnboarding() },
          style = TextStyle(
              fontFamily = Hanken,
              fontWeight = FontWeight.SemiBold,
              fontSize = 13.sp,
              letterSpacing = 0.02.em,
              color = TameColors.TextFaint2,
          ),
      )
    }
}

@Composable
private fun PermsBlock(vm: AppViewModel) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PermRow(
            title = "See what's on screen",
            sub = "Accessibility access",
            on = vm.permAccess,
            onTap = { vm.requestAccess() },
        ) { tint ->
            TameIcons.Accessibility(size = 22.dp, tint = tint)
        }
        PermRow(
            title = "Show the stop screen",
            sub = "Display over other apps",
            on = vm.permOverlay,
            onTap = { vm.requestOverlay() },
        ) { tint ->
            // overlay glyph: outline rect + filled accent rect (matches design)
            SvgIcon(
                shapes = listOf(
                    com.tame.app.ui.components.VRect(3f, 4f, 18f, 14f, rx = 2f, sw = 2f),
                    com.tame.app.ui.components.VRect(13f, 12f, 7f, 7f, rx = 1.5f, fill = true),
                ),
                size = 22.dp,
                tint = tint,
            )
        }
    }
}

@Composable
private fun PermRow(
    title: String,
    sub: String,
    on: Boolean,
    onTap: () -> Unit,
    icon: @Composable (Color) -> Unit,
) {
    val a = accent
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(18.dp), clip = false)
            .clip(RoundedCornerShape(18.dp))
            .background(TameColors.Card)
            .tap { onTap() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(TameColors.ChipBg),
            contentAlignment = Alignment.Center,
        ) {
            icon(a.primary)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = TextStyle(
                    fontFamily = Hanken,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = TameColors.Ink,
                ),
            )
            Spacer(Modifier.height(1.dp))
            Text(
                text = sub,
                style = TextStyle(
                    fontFamily = Hanken,
                    fontWeight = FontWeight.Normal,
                    fontSize = 13.sp,
                    color = TameColors.TextFaint,
                ),
            )
        }
        // permission track/knob (matches design's inline toggle, 50x30, knob 24)
        Box(
            modifier = Modifier
                .size(50.dp, 30.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(if (on) a.primary else TameColors.TrackOff)
        ) {
            Box(
                modifier = Modifier
                    .offset(x = if (on) 23.dp else 3.dp, y = 3.dp)
                    .size(24.dp)
                    .shadow(2.dp, CircleShape)
                    .clip(CircleShape)
                    .background(Color.White)
            )
        }
    }
}

@Composable
private fun AppsGrid(vm: AppViewModel, onbBg: Color) {
    val a = accent
    val apps = AppCatalog.apps
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        apps.chunked(4).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                row.forEach { app ->
                    val selected = vm.selApps.contains(app.key)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .tap { vm.toggleSelApp(app.key) },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            // accent outline ring (outline-offset:3px) — sits 3px outside the tile
                            if (selected) {
                                Box(
                                    modifier = Modifier
                                        .size(54.dp + (3.dp + 2.5.dp) * 2)
                                        .border(2.5.dp, a.primary, RoundedCornerShape(16.dp + 3.dp))
                                )
                            }
                            // tile + check badge anchored to the tile's top-right corner
                            Box(modifier = Modifier.size(54.dp)) {
                                val ic = vm.iconBitmap(app.key)
                                if (ic != null) {
                                    Image(
                                        bitmap = ic,
                                        contentDescription = app.name,
                                        modifier = Modifier.size(54.dp).clip(RoundedCornerShape(16.dp)),
                                    )
                                } else {
                                    AppSquare(
                                        key = app.key,
                                        size = 54.dp,
                                        corner = 16.dp,
                                        fontSize = 17.sp,
                                    )
                                }
                                if (selected) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .offset(x = 6.dp, y = (-6).dp)
                                            .size(21.dp)
                                            .clip(CircleShape)
                                            .background(onbBg)
                                            .padding(2.5.dp)
                                            .clip(CircleShape)
                                            .background(a.primary),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        SvgIcon(
                                            shapes = listOf(VPath("M20 6L9 17l-5-5", sw = 3.5f)),
                                            size = 11.dp,
                                            tint = Color.White,
                                        )
                                    }
                                }
                            }
                        }
                        Text(
                            text = app.name,
                            textAlign = TextAlign.Center,
                            style = TextStyle(
                                fontFamily = Hanken,
                                fontWeight = FontWeight.Medium,
                                fontSize = 11.sp,
                                color = TameColors.TextMuted,
                            ),
                        )
                    }
                }
                // pad incomplete trailing row to keep 4-col grid alignment
                repeat(4 - row.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}
