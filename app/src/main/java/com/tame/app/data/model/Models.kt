package com.tame.app.data.model

import kotlinx.serialization.Serializable
import java.util.Calendar

enum class RuleKind { FEED, APP }
enum class RuleMode { FRICTION, BLOCK }
enum class SchedMode { ALL_DAY, TIMER, CUSTOM }

/**
 * A standing rule. [targets] are app keys from [AppCatalog]. For FEED rules the
 * feed itself is blocked; for APP rules the whole app is blocked.
 */
@Serializable
data class Rule(
    val id: String,
    val kind: RuleKind,
    val targets: List<String>,
    val mode: RuleMode,
    val schedMode: SchedMode = SchedMode.ALL_DAY,
    val days: List<Boolean> = List(7) { true },   // Mon..Sun
    val fromHour: Int = 9,
    val toHour: Int = 17,
    val timerEndsAt: Long? = null,                 // epoch millis for TIMER
    val limit: Int = 0,                            // reels/day (FEED only); 0 = none
    val committed: Boolean = false,
    val committedUntil: Long = 0L,                 // epoch millis the commit-lock holds until
) {
    /** Locked (no edit/delete/disable) while a commitment is still in force. */
    fun isLocked(nowMillis: Long): Boolean = committedUntil > nowMillis

    /** Is the rule in force at [nowMillis]? */
    fun isActiveAt(nowMillis: Long): Boolean = when (schedMode) {
        SchedMode.ALL_DAY -> true
        SchedMode.TIMER -> timerEndsAt != null && nowMillis < timerEndsAt
        SchedMode.CUSTOM -> {
            val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
            // Calendar.MONDAY=2 ... SUNDAY=1 -> map to 0..6 (Mon..Sun)
            val dow = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7
            val prevDow = (dow + 6) % 7
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            fun on(d: Int) = days.getOrElse(d) { false }
            if (fromHour <= toHour) {
                on(dow) && hour in fromHour until toHour
            } else {
                // overnight window: evening tail belongs to today, morning tail to the previous day
                (hour >= fromHour && on(dow)) || (hour < toHour && on(prevDow))
            }
        }
    }
}

@Serializable
data class Habit(
    val id: String,
    val name: String,
    val remind: String = "09:00",   // HH:mm
    val remindOn: Boolean = true,
    val days: List<Boolean> = List(7) { true },
    val grid: List<Int>,            // 28 cells, 0/1; last index = today
) {
    val doneToday: Boolean get() = grid.lastOrNull() == 1
    val streak: Int
        get() {
            if (grid.isEmpty()) return 0
            var i = grid.size - 1
            var s = 0
            if (grid[i] == 0) i--
            while (i >= 0 && grid[i] == 1) { s++; i-- }
            return s
        }
}

@Serializable
data class Settings(
    val onboarded: Boolean = false,
    val accentKey: String = "grove",
    val counterStyle: String = "bubble",  // bubble | ring | minimal
    val blockStyle: String = "frank",     // frank | fullstop | calm
    val reelLimit: Int = 40,              // global default daily reel limit
    val counterEnabled: Boolean = true,   // show the floating reel counter while scrolling
    val todayReels: Int = 0,              // reels watched today (reset daily)
    val reelSeconds: Int = 0,             // seconds spent on feeds today (reset daily)
    val lastReelDay: String = "",         // yyyy-MM-dd marker for daily reset
    // rolling stats shown on the "You" screen (real, updated by the services)
    val turnbacks: Int = 0,               // times a block/friction sent you back
    val focusMinutes: Int = 0,            // focus minutes accumulated
    val daysUnderLimit: Int = 0,          // days under the reel limit (last 7)
    val focusUntil: Long = 0L,            // epoch millis a Focus session blocks everything until (0 = off)
)

/** Whole persisted state — serialized to a single JSON blob in DataStore. */
@Serializable
data class TameData(
    val settings: Settings = Settings(),
    val rules: List<Rule> = emptyList(),
    val habits: List<Habit> = emptyList(),
)
