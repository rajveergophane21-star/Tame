package com.tame.app.ui

import com.tame.app.data.model.RuleKind
import com.tame.app.data.model.RuleMode
import com.tame.app.data.model.SchedMode

enum class Screen {
    ONBOARDING, HOME, RULES, ADD_RULE, DETAIL, HABITS, SETTINGS,
    BLOCK, FRICTION, FOCUS_RUN, ALARM,
}

/** Draft state for the Add / Edit rule flow. */
data class Draft(
    val kind: RuleKind = RuleKind.FEED,
    val targets: Set<String> = emptySet(),
    val mode: RuleMode = RuleMode.FRICTION,
    val schedMode: SchedMode = SchedMode.ALL_DAY,
    val days: List<Boolean> = listOf(true, true, true, true, true, false, false),
    val fromHour: Int = 9,
    val toHour: Int = 17,
    val limit: Int = 40,
    val limitOn: Boolean = false,
    val committed: Boolean = false,
    val search: String = "",
    val editingId: String? = null,
)

/** Draft state for the habit editor sheet. */
data class EditHabitState(
    val id: String? = null,
    val isNew: Boolean = true,
    val name: String = "",
    val remind: String = "09:00",
    val remindOn: Boolean = true,
    val days: List<Boolean> = List(7) { true },
)

data class FrictionState(
    val left: Int = 8,
    val total: Int = 8,
    val choosing: Boolean = false,
    val targetName: String = "this feed",
    val kind: RuleKind = RuleKind.FEED,
)

data class BlockState(
    val name: String = "",
    val iconKey: String = "tt",
    val lifts: String = "later today",
    val kind: RuleKind = RuleKind.APP,
)

data class FocusState(
    val minutes: Int = 0,        // 0 = open-ended
    val label: String = "",
    val leftSecs: Int? = null,   // null = open-ended
)

data class ToastState(val message: String, val ok: Boolean)

/**
 * Bridge so Compose screens can trigger real Android actions (opening system
 * permission settings) without holding an Activity reference. Implemented by
 * [com.tame.app.MainActivity] and set on the ViewModel.
 */
interface SystemActions {
    fun openAccessibilitySettings()
    fun openOverlaySettings()
    fun isAccessibilityOn(): Boolean
    fun isOverlayOn(): Boolean
    /** Is Tame exempt from battery optimisation (so the OS won't kill blocking/alarms)? */
    fun isBatteryUnrestricted(): Boolean
    /** Open the system battery-optimisation screen so the user can exempt Tame. */
    fun openBatterySettings()
    /** Ask the launcher to pin the reels home-screen widget. False if the launcher can't. */
    fun pinHomeWidget(): Boolean
}
