package com.tame.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.tame.app.data.model.Habit
import com.tame.app.ui.AppViewModel
import com.tame.app.ui.components.FrankFill
import com.tame.app.ui.components.TameIcons
import com.tame.app.ui.components.tap
import com.tame.app.ui.daysLabel
import com.tame.app.ui.fmt12
import com.tame.app.ui.theme.Bricolage
import com.tame.app.ui.theme.Hanken
import com.tame.app.ui.theme.TameColors
import com.tame.app.ui.theme.accent

@Composable
fun HabitsScreen(vm: AppViewModel) {
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
                    "Build the good loop",
                    style = TextStyle(
                        fontFamily = Hanken,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = TameColors.TextFaint2,
                    ),
                )
                Text(
                    "Habits",
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
                    .tap { vm.newHabit() }
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

        // ── legend ──
        Row(
            modifier = Modifier
                .padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(15.dp),
        ) {
            LegendItem("Done") {
                Box(
                    modifier = Modifier
                        .size(13.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(a.primary),
                )
            }
            LegendItem("Missed") {
                Box(
                    modifier = Modifier
                        .size(13.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(TameColors.MissedRed),
                )
            }
            LegendItem("Today · tap") {
                Box(
                    modifier = Modifier
                        .size(13.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(TameColors.TodayCell)
                        .dashedBorder(a.primary, 1.5.dp, 4.dp),
                )
            }
        }

        // ── habit cards ──
        vm.habits.forEach { h ->
            HabitCard(h = h, accentColor = a.primary, popColor = a.pop, vm = vm)
        }

        // ── empty state ──
        if (vm.habits.isEmpty()) {
            EmptyHabits(accentColor = a.primary, onAdd = { vm.newHabit() })
        }
    }
}

@Composable
private fun LegendItem(label: String, swatch: @Composable () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        swatch()
        Text(
            label,
            style = TextStyle(
                fontFamily = Hanken,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                color = TameColors.TextFaint,
            ),
        )
    }
}

@Composable
private fun HabitCard(h: Habit, accentColor: Color, popColor: Color, vm: AppViewModel) {
    val done = h.doneToday
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(22.dp), clip = false)
            .clip(RoundedCornerShape(22.dp))
            .background(TameColors.Card)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // ── top row ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // check button
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(50))
                    .background(if (done) accentColor else TameColors.HabitDotOff)
                    .tap { vm.toggleHabit(h.id) },
                contentAlignment = Alignment.Center,
            ) {
                TameIcons.Check(
                    20.dp,
                    if (done) Color.White else TameColors.HabitCheckOff,
                    sw = 3f,
                )
            }

            // name + pencil / repeat
            Column(
                modifier = Modifier
                    .weight(1f)
                    .tap { vm.editOpen(h.id) },
                verticalArrangement = Arrangement.spacedBy(2.dp),
                horizontalAlignment = Alignment.Start,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    Text(
                        h.name,
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
                    TameIcons.Pencil(13.dp, TameColors.IconFaint, sw = 2.2f)
                }
                Text(
                    daysLabel(h.days),
                    style = TextStyle(
                        fontFamily = Hanken,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = TameColors.TextFaint2,
                    ),
                )
            }

            // streak chip
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(TameColors.CommitChipBg)
                    .padding(horizontal = 11.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                TameIcons.Bolt(14.dp, popColor)
                Text(
                    h.streak.toString(),
                    style = TextStyle(
                        fontFamily = Hanken,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        color = TameColors.Ink,
                    ),
                )
            }
        }

        // ── 7-col grid of 28 cells ──
        val grid = h.grid
        val last = grid.size - 1
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            grid.chunked(7).forEachIndexed { rowIdx, row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    row.forEachIndexed { colIdx, v ->
                        val i = rowIdx * 7 + colIdx
                        val cell = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(7.dp))
                            .let {
                                when {
                                    v == 1 -> it.background(accentColor)
                                    i == last -> it
                                        .background(TameColors.TodayCell)
                                        .dashedBorder(accentColor, 2.dp, 7.dp)
                                    else -> it.background(TameColors.MissedRed)
                                }
                            }
                            .tap { vm.toggleHabitDay(h.id, i) }
                        Box(modifier = cell)
                    }
                }
            }
        }

        // ── reminder footer ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    drawLine(
                        color = TameColors.Divider,
                        start = Offset(0f, 0f),
                        end = Offset(size.width, 0f),
                        strokeWidth = 1.dp.toPx(),
                    )
                }
                .padding(top = 13.dp)
                .tap { vm.openAlarm(h.id) },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            TameIcons.Bell(17.dp, TameColors.TextFaint2, sw = 2f)
            Text(
                if (h.remindOn) "Reminder · " + fmt12(h.remind) else "No reminder",
                style = TextStyle(
                    fontFamily = Hanken,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = TameColors.TextSoft,
                ),
            )
            Spacer(Modifier.weight(1f))
            Text(
                "Ring it",
                style = TextStyle(
                    fontFamily = Hanken,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = accentColor,
                ),
            )
        }
    }
}

@Composable
private fun EmptyHabits(accentColor: Color, onAdd: () -> Unit) {
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
            "No habits yet",
            style = TextStyle(
                fontFamily = Bricolage,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 19.sp,
                color = TameColors.Ink,
            ),
        )
        Spacer(Modifier.height(5.dp))
        Text(
            "Add a small daily win — Frank will cheer you on.",
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
                "Add your first habit",
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

/** A dashed rounded-rect border (CSS `border: Npx dashed color`). */
private fun Modifier.dashedBorder(color: Color, width: androidx.compose.ui.unit.Dp, radius: androidx.compose.ui.unit.Dp) =
    this.drawBehind {
        val sw = width.toPx()
        val r = radius.toPx()
        drawRoundRect(
            color = color,
            topLeft = Offset(sw / 2f, sw / 2f),
            size = Size(size.width - sw, size.height - sw),
            cornerRadius = CornerRadius(r, r),
            style = Stroke(
                width = sw,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(sw * 2f, sw * 2f), 0f),
            ),
        )
    }
