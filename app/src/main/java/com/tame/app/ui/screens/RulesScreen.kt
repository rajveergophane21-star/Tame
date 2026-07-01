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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.tame.app.data.model.RuleKind
import com.tame.app.data.model.RuleMode
import com.tame.app.ui.AppViewModel
import com.tame.app.ui.components.AppIconStack
import com.tame.app.ui.components.FrankFill
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
fun RulesScreen(vm: AppViewModel) {
    val a = accent

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TameColors.Surface)
            .verticalScroll(rememberScrollState())
            .padding(start = 22.dp, end = 22.dp, top = 58.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // ── header ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(
                    "Standing rules",
                    style = TextStyle(
                        fontFamily = Hanken,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = TameColors.TextFaint2,
                    ),
                )
                Text(
                    "Rules",
                    style = TextStyle(
                        fontFamily = Bricolage,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 30.sp,
                        letterSpacing = (-0.02).em,
                        color = TameColors.Ink,
                    ),
                )
            }
            // ── New pill ──
            Row(
                modifier = Modifier
                    .height(42.dp)
                    .shadow(8.dp, RoundedCornerShape(24.dp), clip = false)
                    .clip(RoundedCornerShape(24.dp))
                    .background(a.primary)
                    .tap { vm.startAddRule() }
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                TameIcons.Plus(16.dp, Color.White, sw = 3f)
                Text(
                    "New",
                    style = TextStyle(
                        fontFamily = Hanken,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.White,
                    ),
                )
            }
        }

        // ── list ──
        Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
            vm.rules.forEach { r ->
                RuleCard(
                    iconStack = { AppIconStack(r.targets, 48.dp, iconFor = vm::iconBitmap) },
                    title = vm.ruleTitle(r, 2),
                    committed = r.isLocked(System.currentTimeMillis()),
                    sub = (if (r.kind == RuleKind.FEED) "Feed" else "App") + " · " + schedLabel(r),
                    block = r.mode == RuleMode.BLOCK,
                    limitText = if (r.kind == RuleKind.FEED && r.limit > 0) "${r.limit}/day" else "",
                    onTap = { vm.openRule(r.id) },
                )
            }

            if (vm.rules.isEmpty()) {
                EmptyState(accentColor = a.primary, onAdd = { vm.startAddRule() })
            }
        }
    }
}

@Composable
private fun RuleCard(
    iconStack: @Composable () -> Unit,
    title: String,
    committed: Boolean,
    sub: String,
    block: Boolean,
    limitText: String,
    onTap: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(22.dp), clip = false)
            .clip(RoundedCornerShape(22.dp))
            .background(TameColors.Card)
            .tap { onTap() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(modifier = Modifier.size(48.dp)) { iconStack() }

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Text(
                    title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(
                        fontFamily = Hanken,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = TameColors.Ink,
                    ),
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (committed) {
                    SvgIcon(
                        listOf(
                            VRect(5f, 11f, 14f, 9f, rx = 2f, sw = 2.6f),
                            VPath("M8 11V8a4 4 0 018 0v3", sw = 2.6f),
                        ),
                        13.dp,
                        TameColors.CommitGold2,
                    )
                }
            }
            Spacer(Modifier.height(2.dp))
            Text(
                sub,
                style = TextStyle(
                    fontFamily = Hanken,
                    fontWeight = FontWeight.Normal,
                    fontSize = 13.sp,
                    color = TameColors.TextFaint,
                ),
            )
        }

        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            ModeTag(block)
            if (limitText.isNotEmpty()) {
                Text(
                    limitText,
                    style = TextStyle(
                        fontFamily = Hanken,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                        color = TameColors.TextHint,
                    ),
                )
            }
        }
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

@Composable
private fun EmptyState(accentColor: Color, onAdd: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(1.dp, RoundedCornerShape(22.dp), clip = false)
            .clip(RoundedCornerShape(22.dp))
            .background(TameColors.Card)
            .padding(horizontal = 24.dp, vertical = 34.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(84.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(TameColors.FieldBg),
            contentAlignment = Alignment.BottomCenter,
        ) {
            FrankFill("happy", modifier = Modifier.fillMaxSize())
        }
        Spacer(Modifier.height(14.dp))
        Text(
            "No rules yet",
            style = TextStyle(
                fontFamily = Bricolage,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 19.sp,
                color = TameColors.Ink,
            ),
        )
        Spacer(Modifier.height(5.dp))
        Text(
            "Add one to give Ape a break from a feed or app.",
            textAlign = TextAlign.Center,
            style = TextStyle(
                fontFamily = Hanken,
                fontWeight = FontWeight.Normal,
                fontSize = 13.5.sp,
                lineHeight = (13.5 * 1.45).sp,
                color = TameColors.TextFaint,
            ),
            modifier = Modifier.widthIn(max = 220.dp),
        )
        Spacer(Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .height(46.dp)
                .wrapContentWidth()
                .clip(RoundedCornerShape(26.dp))
                .background(accentColor)
                .tap { onAdd() }
                .padding(horizontal = 22.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "Add your first rule",
                style = TextStyle(
                    fontFamily = Hanken,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.White,
                ),
            )
        }
    }
}
