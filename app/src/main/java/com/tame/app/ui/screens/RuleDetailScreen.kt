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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.tame.app.data.model.RuleKind
import com.tame.app.data.model.RuleMode
import com.tame.app.ui.AppViewModel
import com.tame.app.ui.components.AppIconStack
import com.tame.app.ui.components.SvgIcon
import com.tame.app.ui.components.TameIcons
import com.tame.app.ui.components.VPath
import com.tame.app.ui.components.VRect
import com.tame.app.ui.components.tap
import com.tame.app.ui.schedLabel
import com.tame.app.ui.theme.Bricolage
import com.tame.app.ui.theme.Hanken
import com.tame.app.ui.theme.TameColors
import com.tame.app.ui.theme.accent

@Composable
fun RuleDetailScreen(vm: AppViewModel) {
    val a = accent
    val r = vm.currentRule() ?: return

    val block = r.mode == RuleMode.BLOCK
    val committed = r.committed

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TameColors.Surface)
            .verticalScroll(rememberScrollState())
            .padding(start = 22.dp, end = 22.dp, top = 54.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        // ── top row: back + delete ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .shadow(1.dp, CircleShape, clip = false)
                    .clip(CircleShape)
                    .background(TameColors.Card)
                    .tap { vm.detailBack() },
                contentAlignment = Alignment.Center,
            ) {
                TameIcons.ChevronLeft(20.dp, TameColors.Ink, sw = 2.4f)
            }
            Text(
                "Delete",
                style = TextStyle(
                    fontFamily = Hanken,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = TameColors.BlockRed,
                ),
                modifier = Modifier.tap { vm.deleteRule() },
            )
        }

        // ── header: icon + title/kind + mode tag ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(modifier = Modifier.size(58.dp)) {
                AppIconStack(r.targets, 58.dp)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    vm.ruleTitle(r, max = 99),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(
                        fontFamily = Bricolage,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 24.sp,
                        letterSpacing = (-0.02).em,
                        lineHeight = (24 * 1.1).sp,
                        color = TameColors.Ink,
                    ),
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    if (r.kind == RuleKind.FEED) "Short-form feed" else "Whole app",
                    style = TextStyle(
                        fontFamily = Hanken,
                        fontWeight = FontWeight.Normal,
                        fontSize = 13.sp,
                        color = TameColors.TextFaint,
                    ),
                )
            }
            ModeTag(block)
        }

        // ── mode card ──
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(1.dp, RoundedCornerShape(20.dp), clip = false)
                .clip(RoundedCornerShape(20.dp))
                .background(TameColors.Card)
                .padding(18.dp),
        ) {
            Text(
                if (block) "Block" else "Friction",
                style = TextStyle(
                    fontFamily = Hanken,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    color = TameColors.Ink,
                ),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                if (block) "The app closes and a stop screen sends you back."
                else "The app closes, a short pause plays, then you choose.",
                style = TextStyle(
                    fontFamily = Hanken,
                    fontWeight = FontWeight.Normal,
                    fontSize = 14.sp,
                    lineHeight = (14 * 1.45).sp,
                    color = TameColors.TextMuted,
                ),
            )
        }

        // ── stat cards: schedule + limit ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(11.dp),
        ) {
            StatCard(
                label = "Schedule",
                value = schedLabel(r),
                modifier = Modifier.weight(1f),
            )
            if (r.kind == RuleKind.FEED) {
                StatCard(
                    label = "Limit",
                    value = if (r.limit > 0) "${r.limit} reels / day" else "No limit",
                    modifier = Modifier.weight(1f),
                )
            }
        }

        // ── commit card ──
        val commitBg = if (committed) TameColors.CommitChipBg else TameColors.Ink
        val lockColor = if (committed) TameColors.CommitGold else a.pop
        val lockText = if (committed) TameColors.Ink else Color.White
        val lockSub = if (committed) TameColors.CommitNoteSub else TameColors.OnDarkSub
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(commitBg)
                .tap { vm.toggleRuleCommit() }
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            SvgIcon(
                listOf(
                    VRect(5f, 11f, 14f, 9f, rx = 2f, sw = 2.2f),
                    VPath("M8 11V8a4 4 0 018 0v3", sw = 2.2f),
                ),
                24.dp,
                lockColor,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    if (committed) "Committed — locked" else "Commit this rule",
                    style = TextStyle(
                        fontFamily = Hanken,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = lockText,
                    ),
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    if (committed) "Locked. You can't edit or turn this off — that's the point."
                    else "Turn on and you can't undo this rule until its schedule ends.",
                    style = TextStyle(
                        fontFamily = Hanken,
                        fontWeight = FontWeight.Normal,
                        fontSize = 12.5.sp,
                        lineHeight = (12.5 * 1.35).sp,
                        color = lockSub,
                    ),
                )
            }
            CommitToggle(on = committed, trackOnColor = a.primary)
        }

        // ── edit button (only when not committed) ──
        if (!committed) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .shadow(1.dp, RoundedCornerShape(30.dp), clip = false)
                    .clip(RoundedCornerShape(30.dp))
                    .background(TameColors.Card)
                    .tap { vm.editRule() },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                TameIcons.Pencil(17.dp, TameColors.Ink, sw = 2.2f)
                Spacer(Modifier.size(9.dp))
                Text(
                    "Edit rule",
                    style = TextStyle(
                        fontFamily = Hanken,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = TameColors.Ink,
                    ),
                )
            }
        }

        // ── preview stop button ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .clip(RoundedCornerShape(30.dp))
                .background(TameColors.Ink)
                .tap { vm.previewStop() },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            TameIcons.Eye(18.dp, Color.White, sw = 2.2f)
            Spacer(Modifier.size(9.dp))
            Text(
                "Preview the stop screen",
                style = TextStyle(
                    fontFamily = Hanken,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.White,
                ),
            )
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .shadow(1.dp, RoundedCornerShape(18.dp), clip = false)
            .clip(RoundedCornerShape(18.dp))
            .background(TameColors.Card)
            .padding(16.dp),
    ) {
        Text(
            label,
            style = TextStyle(
                fontFamily = Hanken,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                color = TameColors.TextFaint2,
            ),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            value,
            style = TextStyle(
                fontFamily = Hanken,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = TameColors.Ink,
            ),
        )
    }
}

@Composable
private fun ModeTag(block: Boolean) {
    val fg = if (block) TameColors.BlockRed else TameColors.FrictionTagText
    val bg = if (block) TameColors.BlockTagBg else TameColors.FrictionTagBg
    Text(
        if (block) "BLOCK" else "FRICTION",
        style = TextStyle(
            fontFamily = Hanken,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 11.sp,
            letterSpacing = 0.03.em,
            color = fg,
        ),
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    )
}

/** Static (non-interactive) version of the design's 50×30 pill toggle. */
@Composable
private fun CommitToggle(on: Boolean, trackOnColor: Color) {
    val track = if (on) trackOnColor else TameColors.TrackOff
    Box(
        modifier = Modifier
            .size(50.dp, 30.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(track),
    ) {
        Box(
            modifier = Modifier
                .padding(start = if (on) 23.dp else 3.dp, top = 3.dp)
                .size(24.dp)
                .clip(CircleShape)
                .background(Color.White),
        )
    }
}
