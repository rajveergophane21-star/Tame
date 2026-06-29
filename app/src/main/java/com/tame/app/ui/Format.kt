package com.tame.app.ui

import com.tame.app.data.model.Rule
import com.tame.app.data.model.SchedMode

private val dayShort = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

fun daysLabel(days: List<Boolean>): String {
    val on = days.mapIndexedNotNull { i, v -> if (v) i else null }
    if (on.isEmpty() || on.size == 7) return "Every day"
    if (on.size == 5 && on.all { it < 5 }) return "Weekdays"
    if (on.size == 2 && on[0] == 5 && on[1] == 6) return "Weekends"
    return on.joinToString(" · ") { dayShort[it] }
}

/** Human schedule string, matching the design (e.g. "All day", "Weekdays · 9:00 – 17:00"). */
fun schedLabel(rule: Rule): String = when (rule.schedMode) {
    SchedMode.ALL_DAY -> "All day"
    SchedMode.TIMER -> "Next 60 min"
    SchedMode.CUSTOM ->
        "${daysLabel(rule.days)} · ${rule.fromHour}:00 – ${rule.toHour}:00"
}

/** "Back {…}" phrasing on the stop screen. */
fun liftLabel(rule: Rule): String = when (rule.schedMode) {
    SchedMode.ALL_DAY -> "at midnight"
    SchedMode.TIMER -> "when your timer ends"
    SchedMode.CUSTOM -> "at " + hour12Label(rule.toHour)
}

private fun hour12Label(hour24: Int): String {
    val ap = if (hour24 >= 12) "PM" else "AM"
    var hr = hour24 % 12
    if (hr == 0) hr = 12
    return "$hr $ap"
}

/** "HH:mm" (24h) -> "9:30 PM". */
fun fmt12(time: String): String {
    val parts = time.split(":")
    val h = parts.getOrNull(0)?.toIntOrNull() ?: 9
    val m = parts.getOrNull(1) ?: "00"
    val ap = if (h >= 12) "PM" else "AM"
    var hr = h % 12
    if (hr == 0) hr = 12
    return "$hr:$m $ap"
}

/** minutes -> "5h 40m" / "40m". */
fun focusTimeLabel(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}

/** seconds -> "12:05" for the focus countdown. */
fun mmss(totalSecs: Int): String {
    val m = totalSecs / 60
    val s = totalSecs % 60
    return "$m:${s.toString().padStart(2, '0')}"
}
