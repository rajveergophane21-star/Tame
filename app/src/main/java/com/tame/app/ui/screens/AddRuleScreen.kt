package com.tame.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tame.app.data.model.AppCatalog
import com.tame.app.data.model.RuleKind
import com.tame.app.data.model.RuleMode
import com.tame.app.data.model.SchedMode
import com.tame.app.ui.AppViewModel
import com.tame.app.ui.components.AppSquare
import com.tame.app.ui.components.SvgIcon
import com.tame.app.ui.components.TameSwitch
import com.tame.app.ui.components.VCircle
import com.tame.app.ui.components.VPath
import com.tame.app.ui.components.VRect
import com.tame.app.ui.components.tap
import com.tame.app.ui.theme.Bricolage
import com.tame.app.ui.theme.Hanken
import com.tame.app.ui.theme.TameColors
import com.tame.app.ui.theme.accent

@Composable
fun AddRuleScreen(vm: AppViewModel) {
    val a = accent
    val d = vm.draft ?: run {
        Box(Modifier.fillMaxSize().background(TameColors.Surface))
        return
    }

    val anyPick = d.targets.isNotEmpty()
    val pickedN = d.targets.size
    val targetsLabel =
        if (pickedN > 0) "Choose targets · $pickedN selected" else "Choose targets"

    val q = d.search.trim().lowercase()
    val pool = if (d.kind == RuleKind.FEED) AppCatalog.feedKeys else AppCatalog.apps.map { it.key }
    val picks = pool.filter { key ->
        val app = AppCatalog[key]
        q.isEmpty() || (app?.name?.lowercase()?.contains(q) == true)
    }
    val noPicks = d.kind == RuleKind.APP && q.isNotEmpty() && picks.isEmpty()

    Box(
        Modifier
            .fillMaxSize()
            .background(TameColors.Surface)
            .verticalScroll(rememberScrollState())
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 22.dp, end = 22.dp, top = 56.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // ── top bar ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "Cancel",
                    modifier = Modifier.tap { vm.cancelDraft() },
                    style = TextStyle(
                        fontFamily = Hanken, fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp, color = TameColors.TextFaint,
                    ),
                )
                Text(
                    if (d.editingId != null) "Edit rule" else "New rule",
                    style = TextStyle(
                        fontFamily = Hanken, fontWeight = FontWeight.Bold,
                        fontSize = 15.sp, color = TameColors.Ink,
                    ),
                )
                Text(
                    "Save",
                    modifier = Modifier.tap(enabled = anyPick) { vm.saveDraft() },
                    style = TextStyle(
                        fontFamily = Hanken, fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = if (anyPick) a.primary else Color(0xFFC7C3B5),
                    ),
                )
            }

            // ── segmented control ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(TameColors.SegmentBg)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                SegBtn("Short-form feed", d.kind == RuleKind.FEED, Modifier.weight(1f)) {
                    vm.setDraftKind(RuleKind.FEED)
                }
                SegBtn("Whole app", d.kind == RuleKind.APP, Modifier.weight(1f)) {
                    vm.setDraftKind(RuleKind.APP)
                }
            }

            // ── targets ──
            Column {
                SectionLabel(targetsLabel)

                if (d.kind == RuleKind.APP) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 9.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(TameColors.Card)
                            .border(1.dp, TameColors.Hairline, RoundedCornerShape(14.dp))
                            .padding(horizontal = 14.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(9.dp),
                    ) {
                        SvgIcon(
                            shapes = listOf(
                                VCircle(11f, 11f, 7f, sw = 2f),
                                VPath("M21 21l-3.5-3.5", sw = 2f),
                            ),
                            size = 17.dp, tint = TameColors.TextFaint2,
                        )
                        BasicTextField(
                            value = d.search,
                            onValueChange = { vm.setDraftSearch(it) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            textStyle = TextStyle(
                                fontFamily = Hanken, fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp, color = TameColors.Ink,
                            ),
                            cursorBrush = SolidColor(a.primary),
                            decorationBox = { inner ->
                                if (d.search.isEmpty()) {
                                    Text(
                                        "Search all apps",
                                        style = TextStyle(
                                            fontFamily = Hanken, fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp, color = TameColors.TextFaint2,
                                        ),
                                    )
                                }
                                inner()
                            },
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 264.dp)
                        .padding(1.dp),
                    verticalArrangement = Arrangement.spacedBy(9.dp),
                ) {
                    picks.forEach { key ->
                        val app = AppCatalog[key]
                        val sel = d.targets.contains(key)
                        val sub = if (d.kind == RuleKind.FEED) (app?.feed ?: "") else "App"
                        PickRow(
                            key = key,
                            name = app?.name ?: key,
                            sub = sub,
                            selected = sel,
                            accentColor = a.primary,
                        ) { vm.toggleDraftTarget(key) }
                    }
                    if (noPicks) {
                        NoResults(d.search)
                    }
                }
            }

            // ── when it triggers ──
            Column {
                SectionLabel("When it triggers")
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ModeCard(
                        title = "Friction",
                        sub = "Pause, then choose",
                        selected = d.mode == RuleMode.FRICTION,
                        accentColor = a.primary,
                        modifier = Modifier.weight(1f),
                    ) { vm.setDraftMode(RuleMode.FRICTION) }
                    ModeCard(
                        title = "Block",
                        sub = "Hard stop, turn back",
                        selected = d.mode == RuleMode.BLOCK,
                        accentColor = a.primary,
                        modifier = Modifier.weight(1f),
                    ) { vm.setDraftMode(RuleMode.BLOCK) }
                }
            }

            // ── schedule ──
            Column {
                SectionLabel("Schedule")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SchedChip("All day", d.schedMode == SchedMode.ALL_DAY, a.primary) {
                        vm.setSchedMode(SchedMode.ALL_DAY)
                    }
                    SchedChip("Next 60 min", d.schedMode == SchedMode.TIMER, a.primary) {
                        vm.setSchedMode(SchedMode.TIMER)
                    }
                    SchedChip("Custom", d.schedMode == SchedMode.CUSTOM, a.primary) {
                        vm.setSchedMode(SchedMode.CUSTOM)
                    }
                }
                if (d.schedMode == SchedMode.CUSTOM) {
                    Column(
                        modifier = Modifier
                            .padding(top = 12.dp)
                            .fillMaxWidth()
                            .shadow(1.dp, RoundedCornerShape(18.dp), clip = false)
                            .clip(RoundedCornerShape(18.dp))
                            .background(TameColors.Card)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        val dayNames = listOf("M", "T", "W", "T", "F", "S", "S")
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            dayNames.forEachIndexed { i, nm ->
                                DayChip(
                                    label = nm,
                                    on = d.days.getOrElse(i) { false },
                                    accentColor = a.primary,
                                    modifier = Modifier.weight(1f),
                                ) { vm.toggleCustomDay(i) }
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            HourStepper(
                                label = "From",
                                value = "${d.fromHour}:00",
                                modifier = Modifier.weight(1f),
                                onDec = { vm.stepFromH(-1) },
                                onInc = { vm.stepFromH(1) },
                            )
                            HourStepper(
                                label = "To",
                                value = "${d.toHour}:00",
                                modifier = Modifier.weight(1f),
                                onDec = { vm.stepToH(-1) },
                                onInc = { vm.stepToH(1) },
                            )
                        }
                    }
                }
            }

            // ── daily reel limit (feed only) ──
            if (d.kind == RuleKind.FEED) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(1.dp, RoundedCornerShape(18.dp), clip = false)
                        .clip(RoundedCornerShape(18.dp))
                        .background(TameColors.Card)
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .tap { vm.toggleLimitOn() },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Daily reel limit",
                                style = TextStyle(
                                    fontFamily = Hanken, fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp, color = TameColors.Ink,
                                ),
                            )
                            Text(
                                "Optional — lock the feed after a number",
                                modifier = Modifier.padding(top = 1.dp),
                                style = TextStyle(
                                    fontFamily = Hanken, fontWeight = FontWeight.Normal,
                                    fontSize = 12.sp, color = TameColors.TextFaint2,
                                ),
                            )
                        }
                        TameSwitch(on = d.limitOn)
                    }
                    if (d.limitOn) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                "Reels per day",
                                style = TextStyle(
                                    fontFamily = Hanken, fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp, color = TameColors.TextSoft,
                                ),
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                            ) {
                                StepRound("–") { vm.stepDraftLimit(-5) }
                                Text(
                                    d.limit.toString(),
                                    modifier = Modifier.width(38.dp),
                                    textAlign = TextAlign.Center,
                                    style = TextStyle(
                                        fontFamily = Bricolage, fontWeight = FontWeight.ExtraBold,
                                        fontSize = 22.sp, color = TameColors.Ink,
                                    ),
                                )
                                StepRound("+") { vm.stepDraftLimit(5) }
                            }
                        }
                    }
                }
            }

            // ── commit card (dark) ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(TameColors.Ink)
                    .tap { vm.toggleDraftCommit() }
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                SvgIcon(
                    shapes = listOf(
                        VRect(5f, 11f, 14f, 9f, rx = 2f, sw = 2.2f),
                        VPath("M8 11V8a4 4 0 018 0v3", sw = 2.2f),
                    ),
                    size = 22.dp, tint = a.pop,
                )
                Column(Modifier.weight(1f)) {
                    Text(
                        "Commit this rule",
                        style = TextStyle(
                            fontFamily = Hanken, fontWeight = FontWeight.Bold,
                            fontSize = 15.sp, color = Color.White,
                        ),
                    )
                    Text(
                        "Lock it — no edits or off-switch until it ends",
                        modifier = Modifier.padding(top = 2.dp),
                        style = TextStyle(
                            fontFamily = Hanken, fontWeight = FontWeight.Normal,
                            fontSize = 12.sp, color = TameColors.OnDarkSub,
                        ),
                    )
                }
                TameSwitch(on = d.committed)
            }

            // ── sticky bottom save ──
            Box(
                modifier = Modifier
                    .padding(top = 6.dp)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            0.0f to Color.Transparent,
                            0.22f to TameColors.Surface,
                            1.0f to TameColors.Surface,
                        )
                    )
                    .padding(top = 14.dp, bottom = 4.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .clip(RoundedCornerShape(30.dp))
                        .background(if (anyPick) a.primary else Color(0xFFC7C3B5))
                        .tap { vm.saveDraft() },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        if (d.editingId != null) "Update rule" else "Save rule",
                        style = TextStyle(
                            fontFamily = Hanken, fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp, color = Color.White,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 10.dp),
        style = TextStyle(
            fontFamily = Hanken, fontWeight = FontWeight.Bold,
            fontSize = 13.sp, color = TameColors.TextFaint2,
        ),
    )
}

@Composable
private fun SegBtn(
    label: String,
    active: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (active) TameColors.Ink else Color.Transparent)
            .tap { onClick() }
            .padding(13.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = TextStyle(
                fontFamily = Hanken, fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = if (active) Color.White else TameColors.TextSoft,
            ),
        )
    }
}

@Composable
private fun PickRow(
    key: String,
    name: String,
    sub: String,
    selected: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) TameColors.Card else TameColors.RowUnselected)
            .border(
                width = 2.dp,
                color = if (selected) accentColor else TameColors.Hairline,
                shape = RoundedCornerShape(16.dp),
            )
            .tap { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        AppSquare(key = key, size = 40.dp, corner = 12.dp, fontSize = 14.sp)
        Column(Modifier.weight(1f)) {
            Text(
                name,
                style = TextStyle(
                    fontFamily = Hanken, fontWeight = FontWeight.Bold,
                    fontSize = 15.sp, color = TameColors.Ink,
                ),
            )
            Text(
                sub,
                style = TextStyle(
                    fontFamily = Hanken, fontWeight = FontWeight.Normal,
                    fontSize = 12.sp, color = TameColors.TextFaint2,
                ),
            )
        }
        if (selected) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(accentColor),
                contentAlignment = Alignment.Center,
            ) {
                SvgIcon(
                    shapes = listOf(VPath("M20 6L9 17l-5-5", sw = 3.5f)),
                    size = 12.dp, tint = Color.White,
                )
            }
        }
    }
}

@Composable
private fun NoResults(query: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(TameColors.RowUnselected)
            .border(1.dp, TameColors.DashedBorder, RoundedCornerShape(16.dp))
            .padding(horizontal = 18.dp, vertical = 26.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SvgIcon(
            shapes = listOf(
                VCircle(11f, 11f, 7f, sw = 2f),
                VPath("M21 21l-3.5-3.5", sw = 2f),
            ),
            size = 30.dp, tint = TameColors.TextHint2,
        )
        Text(
            "No apps found",
            modifier = Modifier.padding(top = 10.dp),
            style = TextStyle(
                fontFamily = Hanken, fontWeight = FontWeight.Bold,
                fontSize = 14.sp, color = TameColors.TextSoft2,
            ),
        )
        Text(
            "Nothing matches “$query”",
            modifier = Modifier.padding(top = 2.dp),
            style = TextStyle(
                fontFamily = Hanken, fontWeight = FontWeight.Normal,
                fontSize = 12.5.sp, color = TameColors.TextFaint2,
            ),
        )
    }
}

@Composable
private fun ModeCard(
    title: String,
    sub: String,
    selected: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(if (selected) TameColors.ChipBg else TameColors.Card)
            .border(
                width = 2.dp,
                color = if (selected) accentColor else TameColors.Hairline,
                shape = RoundedCornerShape(18.dp),
            )
            .tap { onClick() }
            .padding(horizontal = 14.dp, vertical = 16.dp),
    ) {
        Text(
            title,
            style = TextStyle(
                fontFamily = Hanken, fontWeight = FontWeight.ExtraBold,
                fontSize = 15.sp, color = TameColors.Ink,
            ),
        )
        Text(
            sub,
            modifier = Modifier.padding(top = 3.dp),
            style = TextStyle(
                fontFamily = Hanken, fontWeight = FontWeight.Normal,
                fontSize = 12.sp, color = TameColors.TextSoft, lineHeight = 15.6.sp,
            ),
        )
    }
}

@Composable
private fun SchedChip(
    label: String,
    active: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(22.dp))
            .then(
                if (active) Modifier.background(accentColor)
                else Modifier
                    .shadow(1.dp, RoundedCornerShape(22.dp), clip = false)
                    .clip(RoundedCornerShape(22.dp))
                    .background(TameColors.Card)
            )
            .tap { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = TextStyle(
                fontFamily = Hanken, fontWeight = FontWeight.Bold,
                fontSize = 13.5.sp,
                color = if (active) Color.White else TameColors.TextSoft2,
            ),
        )
    }
}

@Composable
private fun DayChip(
    label: String,
    on: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .height(38.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(if (on) accentColor else TameColors.FieldBg)
            .tap { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = TextStyle(
                fontFamily = Hanken, fontWeight = FontWeight.ExtraBold,
                fontSize = 13.sp,
                color = if (on) Color.White else TameColors.TextHint,
            ),
        )
    }
}

@Composable
private fun HourStepper(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    onDec: () -> Unit,
    onInc: () -> Unit,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Text(
            label,
            modifier = Modifier.padding(start = 2.dp),
            style = TextStyle(
                fontFamily = Hanken, fontWeight = FontWeight.Bold,
                fontSize = 12.sp, color = TameColors.TextFaint2,
            ),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(13.dp))
                .background(TameColors.FieldBg)
                .padding(horizontal = 9.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            StepKnob("–", onDec)
            Text(
                value,
                style = TextStyle(
                    fontFamily = Bricolage, fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp, color = TameColors.Ink,
                ),
            )
            StepKnob("+", onInc)
        }
    }
}

/** Small white 30dp round stepper (custom-schedule From/To). */
@Composable
private fun StepKnob(symbol: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .shadow(1.dp, CircleShape, clip = false)
            .clip(CircleShape)
            .background(Color.White)
            .tap { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            symbol,
            style = TextStyle(
                fontFamily = Hanken, fontWeight = FontWeight.Bold,
                fontSize = 18.sp, color = TameColors.Ink,
            ),
        )
    }
}

/** Larger 34dp tinted round stepper (reels-per-day). */
@Composable
private fun StepRound(symbol: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(TameColors.Divider)
            .tap { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            symbol,
            style = TextStyle(
                fontFamily = Hanken, fontWeight = FontWeight.Bold,
                fontSize = 20.sp, color = TameColors.Ink,
            ),
        )
    }
}
