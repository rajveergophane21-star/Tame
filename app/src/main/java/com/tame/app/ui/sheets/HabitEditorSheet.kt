package com.tame.app.ui.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tame.app.ui.AppViewModel
import com.tame.app.ui.components.SvgIcon
import com.tame.app.ui.components.TameSwitch
import com.tame.app.ui.components.VPath
import com.tame.app.ui.components.tap
import com.tame.app.ui.theme.Bricolage
import com.tame.app.ui.theme.Hanken
import com.tame.app.ui.theme.TameColors
import com.tame.app.ui.theme.accent

@Composable
fun HabitEditorSheet(vm: AppViewModel) {
    val eh = vm.editHabit ?: return
    val a = accent

    val parts = (eh.remind.ifEmpty { "09:00" }).split(":")
    val hour = parts.getOrNull(0) ?: "09"
    val min = parts.getOrNull(1) ?: "00"
    val dayLabels = listOf("M", "T", "W", "T", "F", "S", "S")

    // position:absolute inset:0 z-index:96 — full-screen overlay
    Box(modifier = Modifier.fillMaxSize()) {
        // scrim: rgba(16,22,15,.45)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(TameColors.Scrim)
                .tap { vm.closeEdit() },
        )

        // sheet card anchored to bottom
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .heightIn(max = 720.dp)
                .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
                .background(TameColors.Surface)
                .verticalScroll(rememberScrollState())
                .padding(start = 22.dp, end = 22.dp, top = 14.dp, bottom = 30.dp),
        ) {
            // grabber
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 14.dp)
                    .width(42.dp)
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(TameColors.Grabber),
            )

            // top bar: Cancel · title · Save
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "Cancel",
                    modifier = Modifier.tap { vm.closeEdit() },
                    style = TextStyle(
                        fontFamily = Hanken, fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp, color = TameColors.TextFaint,
                    ),
                )
                Text(
                    if (eh.isNew) "New habit" else "Edit habit",
                    style = TextStyle(
                        fontFamily = Hanken, fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp, color = TameColors.Ink,
                    ),
                )
                Text(
                    "Save",
                    modifier = Modifier.tap { vm.saveEdit() },
                    style = TextStyle(
                        fontFamily = Hanken, fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp, color = a.primary,
                    ),
                )
            }

            // Name card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 13.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(TameColors.Card)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                Text(
                    "Name",
                    modifier = Modifier.padding(bottom = 6.dp),
                    style = TextStyle(
                        fontFamily = Hanken, fontWeight = FontWeight.Bold,
                        fontSize = 12.sp, color = TameColors.TextFaint2,
                    ),
                )
                BasicTextField(
                    value = eh.name,
                    onValueChange = { vm.setEditName(it) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(
                        fontFamily = Hanken, fontWeight = FontWeight.Bold,
                        fontSize = 17.sp, color = TameColors.Ink,
                    ),
                    cursorBrush = SolidColor(a.primary),
                    decorationBox = { inner ->
                        if (eh.name.isEmpty()) {
                            Text(
                                "e.g. Read 10 min",
                                style = TextStyle(
                                    fontFamily = Hanken, fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp, color = TameColors.TextHint,
                                ),
                            )
                        }
                        inner()
                    },
                )
            }

            // Repeat on card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 13.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(TameColors.Card)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                Text(
                    "Repeat on",
                    modifier = Modifier.padding(bottom = 10.dp),
                    style = TextStyle(
                        fontFamily = Hanken, fontWeight = FontWeight.Bold,
                        fontSize = 12.sp, color = TameColors.TextFaint2,
                    ),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    dayLabels.forEachIndexed { i, label ->
                        val on = eh.days.getOrNull(i) == true
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(RoundedCornerShape(11.dp))
                                .background(if (on) a.primary else TameColors.FieldBg)
                                .tap { vm.toggleEditDay(i) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                label,
                                style = TextStyle(
                                    fontFamily = Hanken, fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp,
                                    color = if (on) TameColors.Card else TameColors.TextHint,
                                ),
                            )
                        }
                    }
                }
            }

            // Reminder card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 13.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(TameColors.Card)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .tap { vm.toggleEditRemind() },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Text(
                            "Reminder",
                            style = TextStyle(
                                fontFamily = Hanken, fontWeight = FontWeight.Bold,
                                fontSize = 15.sp, color = TameColors.Ink,
                            ),
                        )
                        Text(
                            "Rings until you stop it",
                            modifier = Modifier.padding(top = 1.dp),
                            style = TextStyle(
                                fontFamily = Hanken, fontWeight = FontWeight.Normal,
                                fontSize = 12.sp, color = TameColors.TextFaint2,
                            ),
                        )
                    }
                    TameSwitch(on = eh.remindOn)
                }

                if (eh.remindOn) {
                    // divider top border + stepper row
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 14.dp)
                            .height(1.dp)
                            .background(TameColors.Divider),
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally),
                    ) {
                        // hour group
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(9.dp),
                        ) {
                            StepperButton("–") { vm.stepEditTime("h", -1) }
                            TimeDigits(hour)
                            StepperButton("+") { vm.stepEditTime("h", 1) }
                        }
                        Text(
                            ":",
                            style = TextStyle(
                                fontFamily = Bricolage, fontWeight = FontWeight.ExtraBold,
                                fontSize = 26.sp, color = TameColors.IconFaint,
                            ),
                        )
                        // minute group
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(9.dp),
                        ) {
                            StepperButton("–") { vm.stepEditTime("m", -5) }
                            TimeDigits(min)
                            StepperButton("+") { vm.stepEditTime("m", 5) }
                        }
                    }
                }
            }

            // Delete (only when editing existing)
            if (!eh.isNew) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(TameColors.BlockTagBg)
                        .tap { vm.deleteEdit() },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                ) {
                    SvgIcon(
                        shapes = listOf(
                            VPath(
                                "M4 7h16M9 7V5a2 2 0 012-2h2a2 2 0 012 2v2M6 7l1 13a2 2 0 002 2h6a2 2 0 002-2l1-13",
                                sw = 2.2f,
                            ),
                        ),
                        size = 17.dp, tint = TameColors.BlockRed,
                    )
                    Text(
                        "Delete habit",
                        style = TextStyle(
                            fontFamily = Hanken, fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp, color = TameColors.BlockRed,
                        ),
                    )
                }
            }

            Spacer(Modifier.height(0.dp))
        }
    }
}

@Composable
private fun StepperButton(glyph: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(TameColors.Divider)
            .tap { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            glyph,
            style = TextStyle(
                fontFamily = Hanken, fontWeight = FontWeight.Bold,
                fontSize = 19.sp, color = TameColors.Ink,
            ),
        )
    }
}

@Composable
private fun TimeDigits(value: String) {
    Text(
        value,
        modifier = Modifier.width(42.dp),
        textAlign = TextAlign.Center,
        style = TextStyle(
            fontFamily = Bricolage, fontWeight = FontWeight.ExtraBold,
            fontSize = 30.sp, color = TameColors.Ink,
        ),
    )
}
