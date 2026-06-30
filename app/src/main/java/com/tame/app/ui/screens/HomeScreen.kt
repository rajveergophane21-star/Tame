package com.tame.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.foundation.Canvas
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.tame.app.data.model.Habit
import com.tame.app.data.model.Rule
import com.tame.app.data.model.RuleKind
import com.tame.app.data.model.RuleMode
import com.tame.app.ui.AppViewModel
import com.tame.app.ui.components.AppIconStack
import com.tame.app.ui.components.TameIcons
import com.tame.app.ui.components.Frank
import com.tame.app.ui.components.tap
import com.tame.app.ui.schedLabel
import com.tame.app.ui.theme.Bricolage
import com.tame.app.ui.theme.Hanken
import com.tame.app.ui.theme.TameColors
import com.tame.app.ui.theme.accent
import kotlin.math.min

@Composable
fun HomeScreen(vm: AppViewModel) {
    val a = accent

    val ratio = vm.reelRatio()
    val overLimit = ratio >= 1f

    // hero derived (lines 1003-1013)
    val heroBg = if (overLimit) TameColors.HeroOver else TameColors.Card
    val heroGlow = when {
        overLimit -> Color(0x59E1574C)              // rgba(225,87,76,.35)
        ratio >= 0.85f -> Color(0x40F2B705)         // rgba(242,183,5,.25)
        else -> Color(0x242F7A5A)                   // rgba(47,122,90,.14)
    }
    val heroTitleColor = if (overLimit) Color.White else TameColors.Ink
    val heroSubColor = if (overLimit) Color(0x99FFFFFF) else TameColors.TextFaint2
    val ringTrack = if (overLimit) Color(0x24FFFFFF) else Color(0xFFEFEBDF)
    val ringColor = when {
        overLimit -> TameColors.OverRed
        ratio >= 0.85f -> a.pop
        else -> a.primary
    }
    val ringFraction = min(1f, ratio)             // C*(1-min(1,ratio)) -> swept fraction
    val heroKicker = when {
        overLimit -> "Limit reached"
        ratio >= 0.85f -> "Almost there"
        else -> "Reels today"
    }
    val heroTitle = when {
        overLimit -> "Ape is fried — feeds locked."
        ratio >= 0.85f -> "Easy now. ${vm.settings.reelLimit - vm.settings.todayReels} left."
        else -> "${vm.settings.reelSeconds / 60} min watched"
    }

    val focusSub = vm.focus?.let { "On · ${it.label}" } ?: "Block everything for a bit"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TameColors.Surface)
            .verticalScroll(rememberScrollState())
            .padding(start = 22.dp, top = 58.dp, end = 22.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // ── Header ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(
                    "Sunday, June 29",
                    style = TextStyle(
                        fontFamily = Hanken, fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp, color = TameColors.TextFaint2, letterSpacing = 0.02.em,
                    ),
                )
                Text(
                    "Good evening",
                    modifier = Modifier.padding(top = 1.dp),
                    style = TextStyle(
                        fontFamily = Bricolage, fontWeight = FontWeight.ExtraBold,
                        fontSize = 25.sp, color = TameColors.Ink, letterSpacing = (-0.02).em,
                    ),
                )
            }
            // streak chip
            Row(
                modifier = Modifier
                    .shadow(1.dp, RoundedCornerShape(30.dp), clip = false)
                    .clip(RoundedCornerShape(30.dp))
                    .background(TameColors.Card)
                    .padding(horizontal = 13.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                TameIcons.Bolt(15.dp, a.pop)
                Text(
                    "${vm.bestStreak()}",
                    style = TextStyle(
                        fontFamily = Hanken, fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp, color = TameColors.Ink,
                    ),
                )
                Text(
                    "day streak",
                    style = TextStyle(
                        fontFamily = Hanken, fontWeight = FontWeight.Normal,
                        fontSize = 13.sp, color = TameColors.TextFaint2,
                    ),
                )
            }
        }

        // ── permission banner (blocking can't work without these) ──
        if (!vm.permAccess || !vm.permOverlay) {
            PermissionBanner(vm)
        }

        // ── HERO card ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(8.dp, RoundedCornerShape(28.dp), clip = false)
                .clip(RoundedCornerShape(28.dp))
                .background(heroBg)
                .padding(start = 22.dp, top = 24.dp, end = 22.dp, bottom = 22.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Frank(mood = vm.homeMood(), size = 96.dp)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        heroKicker.uppercase(),
                        style = TextStyle(
                            fontFamily = Hanken, fontWeight = FontWeight.Bold,
                            fontSize = 13.sp, color = heroSubColor, letterSpacing = 0.08.em,
                        ),
                    )
                    Text(
                        heroTitle,
                        modifier = Modifier.padding(top = 4.dp),
                        style = TextStyle(
                            fontFamily = Bricolage, fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp, color = heroTitleColor, lineHeight = 20.sp,
                        ),
                    )
                }
                // reel ring
                Box(modifier = Modifier.size(96.dp), contentAlignment = Alignment.Center) {
                    ReelRing(
                        modifier = Modifier.size(96.dp),
                        track = ringTrack,
                        color = ringColor,
                        fraction = ringFraction,
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "${vm.settings.todayReels}",
                            style = TextStyle(
                                fontFamily = Bricolage, fontWeight = FontWeight.ExtraBold,
                                fontSize = 25.sp, color = heroTitleColor, lineHeight = 25.sp,
                            ),
                        )
                        Text(
                            "of ${vm.settings.reelLimit}",
                            modifier = Modifier.padding(top = 1.dp),
                            style = TextStyle(
                                fontFamily = Hanken, fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp, color = heroSubColor,
                            ),
                        )
                    }
                }
            }
        }

        // ── Focus now ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(8.dp, RoundedCornerShape(22.dp), clip = false)
                .clip(RoundedCornerShape(22.dp))
                .background(TameColors.Ink)
                .tap { vm.openFocusSheet() }
                .padding(horizontal = 20.dp, vertical = 17.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0x1AFFFFFF)),
                contentAlignment = Alignment.Center,
            ) {
                TameIcons.Clock(22.dp, a.pop)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Focus now",
                    style = TextStyle(
                        fontFamily = Hanken, fontWeight = FontWeight.Bold,
                        fontSize = 16.sp, color = Color.White,
                    ),
                )
                Text(
                    focusSub,
                    modifier = Modifier.padding(top = 1.dp),
                    style = TextStyle(
                        fontFamily = Hanken, fontWeight = FontWeight.Normal,
                        fontSize = 13.sp, color = TameColors.OnDarkSub,
                    ),
                )
            }
            TameIcons.ChevronRight(20.dp, TameColors.OnDarkSub2)
        }

        // ── Active rules header ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, start = 4.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                "Active",
                style = TextStyle(
                    fontFamily = Hanken, fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp, color = TameColors.Ink,
                ),
            )
            Text(
                "All rules",
                modifier = Modifier.tap { vm.goRules() },
                style = TextStyle(
                    fontFamily = Hanken, fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp, color = a.primary,
                ),
            )
        }

        // ── Rules list ──
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            vm.rules.forEach { r -> RuleRow(vm, r) }
        }

        // ── Habits header ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, start = 4.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                "Today's habits",
                style = TextStyle(
                    fontFamily = Hanken, fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp, color = TameColors.Ink,
                ),
            )
            Text(
                "History",
                modifier = Modifier.tap { vm.goHabits() },
                style = TextStyle(
                    fontFamily = Hanken, fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp, color = a.primary,
                ),
            )
        }

        // ── Habits strip ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            vm.habits.forEach { h ->
                HabitCard(vm, h, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun PermissionBanner(vm: AppViewModel) {
    val a = accent
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(TameColors.Ink)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            "Finish setup to start blocking",
            style = TextStyle(fontFamily = Bricolage, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, color = Color.White),
        )
        Text(
            "APE needs these two permissions before it can block apps, pause feeds, or count reels.",
            style = TextStyle(fontFamily = Hanken, fontSize = 13.sp, color = TameColors.OnDarkSub, lineHeight = 18.sp),
        )
        if (!vm.permAccess) {
            PermissionAction("Turn on accessibility access") { vm.requestAccess() }
        }
        if (!vm.permOverlay) {
            PermissionAction("Allow display over other apps") { vm.requestOverlay() }
        }
    }
}

@Composable
private fun PermissionAction(label: String, onClick: () -> Unit) {
    val a = accent
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(a.primary)
            .tap { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            label,
            style = TextStyle(fontFamily = Hanken, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White),
        )
        TameIcons.ArrowRight(18.dp, Color.White)
    }
}

@Composable
private fun RuleRow(vm: AppViewModel, r: Rule) {
    val a = accent
    val block = r.mode == RuleMode.BLOCK
    val sub = (if (r.kind == RuleKind.FEED) "Feed · " else "App · ") + schedLabel(r)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(1.dp, RoundedCornerShape(20.dp), clip = false)
            .clip(RoundedCornerShape(20.dp))
            .background(TameColors.Card)
            .tap { vm.openRule(r.id) }
            .padding(horizontal = 15.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        AppIconStack(r.targets, 46.dp, iconFor = vm::iconBitmap)
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Text(
                    vm.ruleTitle(r, 1),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                    style = TextStyle(
                        fontFamily = Hanken, fontWeight = FontWeight.Bold,
                        fontSize = 15.sp, color = TameColors.Ink,
                    ),
                )
                if (r.committed) {
                    TameIcons.Lock(13.dp, TameColors.CommitGold2, sw = 2.6f)
                }
            }
            Text(
                sub,
                modifier = Modifier.padding(top = 1.dp),
                style = TextStyle(
                    fontFamily = Hanken, fontWeight = FontWeight.Normal,
                    fontSize = 13.sp, color = TameColors.TextFaint,
                ),
            )
        }
        // mode tag
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(if (block) TameColors.BlockTagBg else TameColors.FrictionTagBg)
                .padding(horizontal = 10.dp, vertical = 5.dp),
        ) {
            Text(
                if (block) "BLOCK" else "FRICTION",
                style = TextStyle(
                    fontFamily = Hanken, fontWeight = FontWeight.ExtraBold,
                    fontSize = 11.sp, letterSpacing = 0.03.em,
                    color = if (block) TameColors.BlockRed else TameColors.FrictionTagText,
                ),
            )
        }
    }
}

@Composable
private fun HabitCard(vm: AppViewModel, h: Habit, modifier: Modifier = Modifier) {
    val a = accent
    val done = h.doneToday
    val dotBg = if (done) a.primary else TameColors.HabitDotOff
    val checkColor = if (done) Color.White else TameColors.HabitCheckOff
    Column(
        modifier = modifier
            .shadow(1.dp, RoundedCornerShape(18.dp), clip = false)
            .clip(RoundedCornerShape(18.dp))
            .background(TameColors.Card)
            .tap { vm.toggleHabit(h.id) }
            .padding(start = 10.dp, top = 14.dp, end = 10.dp, bottom = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(dotBg),
            contentAlignment = Alignment.Center,
        ) {
            TameIcons.Check(17.dp, checkColor, sw = 3f)
        }
        Text(
            h.name,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            style = TextStyle(
                fontFamily = Hanken, fontWeight = FontWeight.SemiBold,
                fontSize = 11.5.sp, color = TameColors.TextSoft2, lineHeight = 14.sp,
            ),
        )
    }
}

/**
 * Circular progress ring matching the design's SVG: viewBox 120, r=50, stroke 12,
 * rotated -90deg (starts at top), round cap. [fraction] in 0..1 is the swept arc.
 */
@Composable
private fun ReelRing(modifier: Modifier, track: Color, color: Color, fraction: Float) {
    Canvas(modifier = modifier) {
        val vp = 120f
        val s = size.minDimension / vp
        val strokeW = 12f * s
        val r = 50f * s
        val center = Offset(size.width / 2f, size.height / 2f)
        val topLeft = Offset(center.x - r, center.y - r)
        val arcSize = Size(r * 2f, r * 2f)
        // track
        drawCircle(
            color = track,
            radius = r,
            center = center,
            style = Stroke(width = strokeW),
        )
        // progress
        if (fraction > 0f) {
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360f * fraction,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeW, cap = StrokeCap.Round),
            )
        }
    }
}
