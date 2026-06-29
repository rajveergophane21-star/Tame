package com.tame.app.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.tame.app.ui.AppViewModel
import com.tame.app.ui.components.FrankFill
import com.tame.app.ui.components.SvgIcon
import com.tame.app.ui.components.TameIcons
import com.tame.app.ui.components.VCircle
import com.tame.app.ui.components.VPath
import com.tame.app.ui.components.VRect
import com.tame.app.ui.components.tap
import com.tame.app.ui.focusTimeLabel
import com.tame.app.ui.theme.AccentPalette
import com.tame.app.ui.theme.Bricolage
import com.tame.app.ui.theme.Hanken
import com.tame.app.ui.theme.TameColors
import com.tame.app.ui.theme.accent

@Composable
fun SettingsScreen(vm: AppViewModel) {
    val a = accent

    val committedCount = vm.rules.count { it.committed }
    val committedLabel =
        if (committedCount > 0) {
            committedCount.toString() + (if (committedCount == 1) " rule committed" else " rules committed")
        } else {
            "Nothing committed"
        }
    val committedSub =
        if (committedCount > 0) "Locked in — can't be turned off yet." else "Commit a rule to lock it in."

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TameColors.Surface)
            .verticalScroll(rememberScrollState())
            .padding(start = 22.dp, end = 22.dp, top = 58.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        // ── header ──
        Column {
            Text(
                "Your progress",
                style = TextStyle(
                    fontFamily = Hanken,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = TameColors.TextFaint2,
                ),
            )
            Text(
                "You",
                style = TextStyle(
                    fontFamily = Bricolage,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 30.sp,
                    letterSpacing = (-0.02).em,
                    color = TameColors.Ink,
                ),
            )
        }

        // ── dark progress card ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(26.dp))
                .background(TameColors.Ink)
                .padding(22.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(82.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color.White.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    FrankFill("happy", modifier = Modifier.fillMaxSize())
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Frank's proud.",
                        style = TextStyle(
                            fontFamily = Bricolage,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 22.sp,
                            letterSpacing = (-0.01).em,
                            color = Color.White,
                        ),
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${vm.settings.daysUnderLimit} of the last 7 days under your limit.",
                        style = TextStyle(
                            fontFamily = Hanken,
                            fontWeight = FontWeight.Normal,
                            fontSize = 13.5.sp,
                            lineHeight = (13.5 * 1.4).sp,
                            color = TameColors.OnDarkSub,
                        ),
                    )
                }
            }
        }

        // ── three stat cards ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(11.dp),
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                value = vm.settings.turnbacks.toString(),
                label1 = "Turned back",
                label2 = "this week",
            )
            StatCard(
                modifier = Modifier.weight(1f),
                value = focusTimeLabel(vm.settings.focusMinutes),
                label1 = "Focus time",
                label2 = "this week",
            )
            StatCard(
                modifier = Modifier.weight(1f),
                value = vm.bestStreak().toString(),
                label1 = "Best habit",
                label2 = "streak",
            )
        }

        // ── committed card ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(1.dp, RoundedCornerShape(20.dp), clip = false)
                .clip(RoundedCornerShape(20.dp))
                .background(TameColors.Card)
                .tap { vm.goRules() }
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(TameColors.CommitChipBg),
                contentAlignment = Alignment.Center,
            ) {
                TameIcons.Lock(20.dp, TameColors.CommitGold, sw = 2.2f)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    committedLabel,
                    style = TextStyle(
                        fontFamily = Hanken,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = TameColors.Ink,
                    ),
                )
                Spacer(Modifier.height(1.dp))
                Text(
                    committedSub,
                    style = TextStyle(
                        fontFamily = Hanken,
                        fontWeight = FontWeight.Normal,
                        fontSize = 12.5.sp,
                        color = TameColors.TextFaint,
                    ),
                )
            }
            TameIcons.ChevronRight(20.dp, TameColors.IconFaint, sw = 2.4f)
        }

        // ── permission status card ──
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(1.dp, RoundedCornerShape(22.dp), clip = false)
                .clip(RoundedCornerShape(22.dp))
                .background(TameColors.Card)
                .padding(horizontal = 18.dp),
        ) {
            PermissionRow(
                accent = a,
                label = "Accessibility access",
                icon = {
                    SvgIcon(
                        listOf(
                            VCircle(12f, 5f, 2.3f, sw = 2f),
                            VPath("M5 9h14M12 9v5m0 0l-3 6m3-6l3 6", sw = 2f),
                        ),
                        19.dp,
                        a.primary,
                    )
                },
                on = vm.permAccess,
                onRequest = { vm.requestAccess() },
                divider = true,
            )
            PermissionRow(
                accent = a,
                label = "Display over other apps",
                icon = {
                    SvgIcon(
                        listOf(
                            VRect(3f, 4f, 18f, 14f, rx = 2f, sw = 2f),
                            VRect(13f, 12f, 7f, 7f, rx = 1.5f, fill = true),
                        ),
                        19.dp,
                        a.primary,
                    )
                },
                on = vm.permOverlay,
                onRequest = { vm.requestOverlay() },
                divider = false,
            )
        }

        // ── privacy dark card ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(TameColors.Ink)
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            TameIcons.ShieldCheck(22.dp, a.pop, sw = 2.2f)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Everything stays on this phone",
                    style = TextStyle(
                        fontFamily = Hanken,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.5.sp,
                        color = Color.White,
                    ),
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "No account, no servers, fully offline",
                    style = TextStyle(
                        fontFamily = Hanken,
                        fontWeight = FontWeight.Normal,
                        fontSize = 12.5.sp,
                        color = TameColors.OnDarkSub,
                    ),
                )
            }
        }

        // ── replay intro ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(1.dp, RoundedCornerShape(18.dp), clip = false)
                .clip(RoundedCornerShape(18.dp))
                .background(TameColors.Card)
                .tap { vm.replayIntro() }
                .padding(horizontal = 18.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(TameColors.FieldBg),
                contentAlignment = Alignment.Center,
            ) {
                TameIcons.Replay(19.dp, TameColors.TextFaint, sw = 2f)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Replay intro",
                    style = TextStyle(
                        fontFamily = Hanken,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.5.sp,
                        color = TameColors.Ink,
                    ),
                )
                Spacer(Modifier.height(1.dp))
                Text(
                    "See how Frank works again",
                    style = TextStyle(
                        fontFamily = Hanken,
                        fontWeight = FontWeight.Normal,
                        fontSize = 12.5.sp,
                        color = TameColors.TextFaint,
                    ),
                )
            }
            TameIcons.ChevronRight(20.dp, TameColors.IconFaint, sw = 2.4f)
        }

        // ── footer ──
        Spacer(Modifier.height(4.dp))
        Text(
            "Tame v1.0 · stays on your phone",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = TextStyle(
                fontFamily = Hanken,
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp,
                color = TameColors.TextHint2,
            ),
        )
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    value: String,
    label1: String,
    label2: String,
) {
    Column(
        modifier = modifier
            .shadow(1.dp, RoundedCornerShape(20.dp), clip = false)
            .clip(RoundedCornerShape(20.dp))
            .background(TameColors.Card)
            .padding(horizontal = 14.dp, vertical = 16.dp),
    ) {
        Text(
            value,
            maxLines = 1,
            style = TextStyle(
                fontFamily = Bricolage,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 21.sp,
                lineHeight = 21.sp,
                color = TameColors.Ink,
            ),
        )
        Spacer(Modifier.height(5.dp))
        Text(
            label1,
            style = TextStyle(
                fontFamily = Hanken,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                lineHeight = (12 * 1.25).sp,
                color = TameColors.TextFaint,
            ),
        )
        Text(
            label2,
            style = TextStyle(
                fontFamily = Hanken,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                lineHeight = (12 * 1.25).sp,
                color = TameColors.TextFaint,
            ),
        )
    }
}

@Composable
private fun PermissionRow(
    accent: AccentPalette,
    label: String,
    icon: @Composable () -> Unit,
    on: Boolean,
    onRequest: () -> Unit,
    divider: Boolean,
) {
    val rowMod = if (on) Modifier else Modifier.tap { onRequest() }
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(rowMod)
                .padding(vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(TameColors.ChipBg),
                contentAlignment = Alignment.Center,
            ) {
                icon()
            }
            Text(
                label,
                modifier = Modifier.weight(1f),
                style = TextStyle(
                    fontFamily = Hanken,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.5.sp,
                    color = TameColors.Ink,
                ),
            )
            if (on) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    TameIcons.Check(15.dp, accent.primary, sw = 3f)
                    Text(
                        "On",
                        style = TextStyle(
                            fontFamily = Hanken,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = accent.primary,
                        ),
                    )
                }
            } else {
                Text(
                    "Off",
                    style = TextStyle(
                        fontFamily = Hanken,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = TameColors.TextFaint,
                    ),
                )
            }
        }
        if (divider) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(TameColors.Divider),
            )
        }
    }
}
