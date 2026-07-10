package com.tame.app.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tame.app.util.AppEntry
import com.tame.app.util.InstalledApps
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.tame.app.TameApp
import com.tame.app.alarm.AlarmScheduler
import com.tame.app.data.TameRepository
import com.tame.app.data.model.AppCatalog
import com.tame.app.data.model.Habit
import com.tame.app.data.model.Rule
import com.tame.app.data.model.RuleKind
import com.tame.app.data.model.RuleMode
import com.tame.app.data.model.SchedMode
import com.tame.app.data.model.TameData
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Single source of UI truth — a Compose-state port of the design prototype's
 * component logic, with persistence via [TameRepository] and real Android side
 * effects via [SystemActions] / [AlarmScheduler].
 */
class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val repo: TameRepository = TameApp.repo

    // Fast, tiny flag mirrored on each onboarding change, so the first screen can be chosen
    // instantly without a blocking DataStore read on the main thread (that was a launch-ANR risk).
    private val bootPrefs = app.getSharedPreferences("tame_boot", android.content.Context.MODE_PRIVATE)

    // ── persisted state (collected from the repository) ──
    // Start empty; the DataStore stream fills it in a moment later (no main-thread disk read).
    var data by mutableStateOf(TameData()); private set

    // ── transient UI state ──
    var screen by mutableStateOf(if (bootPrefs.getBoolean("onboarded", false)) Screen.HOME else Screen.ONBOARDING); private set
    var onbStep by mutableStateOf(0); private set
    var selApps by mutableStateOf(emptySet<String>()); private set

    var draft by mutableStateOf<Draft?>(null); private set
    var detailId by mutableStateOf<String?>(null); private set
    var sheet by mutableStateOf<String?>(null); private set
    var editHabit by mutableStateOf<EditHabitState?>(null); private set
    var toast by mutableStateOf<ToastState?>(null); private set

    var friction by mutableStateOf(FrictionState()); private set
    var block by mutableStateOf(BlockState()); private set
    var focus by mutableStateOf<FocusState?>(null); private set
    var alarmHabitId by mutableStateOf<String?>(null); private set

    // ── installed apps (for the "whole app" picker + rule rendering) ──
    var installedApps by mutableStateOf<List<AppEntry>>(emptyList()); private set
    private var appsLoaded = false

    // Monotonic suffix so two rules/habits created in the same millisecond still get
    // distinct ids (their alarm request-codes derive from the id, so collisions would
    // otherwise make one reminder silently overwrite another).
    private var idSeq = 0
    private fun freshId(prefix: String): String = "$prefix${System.currentTimeMillis()}-${idSeq++}"

    /** Force a day list to exactly 7 entries (older/partial saved data may have fewer). */
    private fun normalizeDays(days: List<Boolean>): List<Boolean> = List(7) { days.getOrElse(it) { false } }

    // ── system bridge / permissions ──
    var systemActions: SystemActions? = null
    var permAccess by mutableStateOf(false); private set
    var permOverlay by mutableStateOf(false); private set
    var permBattery by mutableStateOf(true); private set
    // shows the prominent accessibility disclosure/consent before we send the user to
    // system settings (required by Google Play for non-accessibility-tool use)
    var showConsent by mutableStateOf(false); private set

    private var frictionJob: Job? = null
    private var focusJob: Job? = null
    private var toastJob: Job? = null

    private var focusRestored = false

    init {
        viewModelScope.launch {
            repo.data.collect { d ->
                data = d
                // Keep the theme in sync with the stored setting (no-ops if already applied).
                com.tame.app.ui.theme.TameColors.applyDark(d.settings.darkMode)
                // If a Focus session was still running (e.g. app was reopened), restore the
                // countdown UI so the End button is reachable. The service enforces it either way.
                if (!focusRestored) {
                    focusRestored = true
                    val left = d.settings.focusUntil - System.currentTimeMillis()
                    if (left > 0 && focus == null) restoreFocus((left / 1000).toInt())
                }
            }
        }
        loadInstalledApps()
    }

    fun loadInstalledApps() {
        if (appsLoaded) return
        appsLoaded = true
        viewModelScope.launch(Dispatchers.IO) {
            val apps = InstalledApps.load(getApplication())
            withContext(Dispatchers.Main) { installedApps = apps }
        }
    }

    /**
     * Real launcher icon for a rule target. Works for both whole-app targets (package
     * names) and short-form feed keys (e.g. "ig") — a feed key resolves to its installed
     * app's real icon. Returns null only when the app isn't installed (callers fall back
     * to the branded tile).
     */
    fun iconBitmap(target: String): ImageBitmap? {
        installedApps.firstOrNull { it.packageName == target }?.icon?.let { return it }
        InstalledApps.entry(target)?.icon?.let { return it }
        AppCatalog[target]?.packages?.forEach { pkg ->
            (installedApps.firstOrNull { it.packageName == pkg }?.icon ?: InstalledApps.entry(pkg)?.icon)?.let { return it }
        }
        return null
    }

    /** Is the short-form feed app for [key] actually installed on this device? */
    fun isFeedInstalled(key: String): Boolean {
        val pkgs = AppCatalog[key]?.packages ?: return false
        return pkgs.any { pkg -> installedApps.any { it.packageName == pkg } || InstalledApps.entry(pkg) != null }
    }

    // ── derived helpers ──
    val settings get() = data.settings
    val rules get() = data.rules
    val habits get() = data.habits

    fun reelRatio(): Float {
        val limit = settings.reelLimit
        return if (limit > 0) settings.todayReels.toFloat() / limit else 0f
    }

    fun bestStreak(): Int = habits.maxOfOrNull { it.streak } ?: 0

    fun homeMood(): String {
        if (focus != null) return "zen"
        val r = reelRatio()
        return when {
            r >= 1f -> "crying"
            r >= 0.85f -> "worried"
            r >= 0.55f -> "neutral"
            r > 0f -> "happy"
            else -> "zen"
        }
    }

    fun counterMood(): String {
        val r = reelRatio()
        return when {
            r >= 1f -> "panic"
            r >= 0.82f -> "worried"
            r >= 0.5f -> "neutral"
            else -> "happy"
        }
    }

    fun currentRule(): Rule? = rules.firstOrNull { it.id == detailId }
    fun alarmHabit(): Habit? = habits.firstOrNull { it.id == alarmHabitId }
    fun appName(target: String): String =
        AppCatalog[target]?.name
            ?: installedApps.firstOrNull { it.packageName == target }?.label
            ?: InstalledApps.entry(target)?.label
            ?: target

    fun ruleTitle(rule: Rule, max: Int = 2): String {
        val names = rule.targets.map { appName(it) }
        return when {
            names.size > max -> names.take(max).joinToString(", ") + " +" + (names.size - max)
            else -> names.joinToString(", ")
        }
    }

    // ── navigation ──
    private fun go(s: Screen) { screen = s; sheet = null }
    fun goHome() = go(Screen.HOME)
    fun goRules() = go(Screen.RULES)
    fun goHabits() = go(Screen.HABITS)
    fun goSettings() = go(Screen.SETTINGS)
    fun backHome() { screen = Screen.HOME }

    // ── permissions ──
    fun refreshPerms() {
        systemActions?.let {
            permAccess = it.isAccessibilityOn()
            permOverlay = it.isOverlayOn()
            permBattery = it.isBatteryUnrestricted()
        }
    }
    /**
     * Tapping the accessibility toggle does NOT jump straight to system settings — it first
     * shows the in-app disclosure of what the access is for (Play prominent-disclosure rule).
     * If it's already on, there's nothing to do.
     */
    fun requestAccess() {
        if (systemActions?.isAccessibilityOn() == true) return
        showConsent = true
    }
    /** User read the disclosure and tapped "Turn on" — the affirmative action, then to settings. */
    fun acceptConsent() { showConsent = false; systemActions?.openAccessibilitySettings() }
    fun dismissConsent() { showConsent = false }
    fun requestOverlay() { systemActions?.openOverlaySettings() }
    fun requestBattery() { systemActions?.openBatterySettings() }

    // ── onboarding ──
    fun onbNext() {
        // Don't let the main path advance past the permissions step with accessibility off —
        // otherwise the app looks "protected" on Home while blocking nothing. (The small "skip"
        // link is still an honest escape for anyone who really wants to look around first.)
        if (onbStep == 2 && !permAccess) { buzz("Turn on accessibility so Ape can step in"); return }
        if (onbStep >= 4) finishOnboarding() else onbStep++
    }
    fun onbBack() { if (onbStep > 0) onbStep-- }
    fun toggleSelApp(id: String) {
        selApps = if (selApps.contains(id)) selApps - id else selApps + id
    }
    fun finishOnboarding() {
        val ids = selApps.toList()
        viewModelScope.launch {
            var added = false
            repo.update { d ->
                val sig = ids.sorted()
                val exists = d.rules.any { it.kind == RuleKind.FEED && it.targets.sorted() == sig }
                val rules = if (ids.isNotEmpty() && !exists) {
                    added = true
                    listOf(
                        Rule(
                            id = freshId("r"),
                            kind = RuleKind.FEED, targets = ids.take(3),
                            // daily limit off by default — a plain all-day friction rule
                            mode = RuleMode.FRICTION, schedMode = SchedMode.ALL_DAY, limit = 0,
                        )
                    ) + d.rules
                } else d.rules
                d.copy(settings = d.settings.copy(onboarded = true), rules = rules)
            }
            bootPrefs.edit().putBoolean("onboarded", true).apply()
            screen = Screen.HOME
            flash(if (added) "First rule created" else "You're all set")
        }
    }
    fun replayIntro() {
        viewModelScope.launch {
            repo.update { it.copy(settings = it.settings.copy(onboarded = false)) }
            bootPrefs.edit().putBoolean("onboarded", false).apply()
            onbStep = 0; screen = Screen.ONBOARDING
        }
    }

    // ── home ──
    fun openFocusSheet() { sheet = "focus" }
    fun closeSheet() { sheet = null }
    fun openRule(id: String) { detailId = id; screen = Screen.DETAIL }

    fun toggleHabit(id: String) = persist { d ->
        d.copy(habits = d.habits.map { h ->
            if (h.id != id || h.grid.isEmpty()) h
            else h.copy(grid = h.grid.toMutableList().also { it[it.size - 1] = if (it.last() == 1) 0 else 1 })
        })
    }

    // ── add / edit rule ──
    fun startAddRule() { draft = Draft(); screen = Screen.ADD_RULE }
    fun setDraftKind(kind: RuleKind) { draft = draft?.copy(kind = kind, targets = emptySet(), search = "") }
    fun toggleDraftTarget(id: String) {
        draft = draft?.let { d -> d.copy(targets = if (d.targets.contains(id)) d.targets - id else d.targets + id) }
    }
    fun setDraftSearch(v: String) { draft = draft?.copy(search = v) }
    fun setDraftMode(mode: RuleMode) { draft = draft?.copy(mode = mode) }
    fun setSchedMode(mode: SchedMode) { draft = draft?.copy(schedMode = mode) }
    fun toggleCustomDay(i: Int) {
        draft = draft?.let { d ->
            val base = normalizeDays(d.days).toMutableList().also { it[i] = !it[i] }
            d.copy(days = base, schedMode = SchedMode.CUSTOM)
        }
    }
    fun stepFromH(n: Int) { draft = draft?.let { it.copy(fromHour = ((it.fromHour + n) % 24 + 24) % 24, schedMode = SchedMode.CUSTOM) } }
    fun stepToH(n: Int) { draft = draft?.let { it.copy(toHour = ((it.toHour + n) % 24 + 24) % 24, schedMode = SchedMode.CUSTOM) } }
    fun stepDraftLimit(d: Int) { draft = draft?.let { it.copy(limit = (it.limit + d).coerceIn(5, 300)) } }
    fun toggleLimitOn() { draft = draft?.let { it.copy(limitOn = !it.limitOn) } }
    fun toggleDraftCommit() { draft = draft?.let { it.copy(committed = !it.committed) } }

    fun editRule() {
        val r = currentRule() ?: return
        if (r.isLocked(System.currentTimeMillis())) { buzz(); return }
        draft = Draft(
            kind = r.kind, targets = r.targets.toSet(), mode = r.mode,
            schedMode = r.schedMode, days = normalizeDays(r.days), fromHour = r.fromHour, toHour = r.toHour,
            limit = if (r.limit > 0) r.limit else 40,
            limitOn = r.kind == RuleKind.FEED && r.limit > 0,
            committed = r.committed, editingId = r.id,
        )
        screen = Screen.ADD_RULE
    }

    fun saveDraft() {
        val d = draft ?: return
        val ids = d.targets.toList()
        if (ids.isEmpty()) { buzz("Pick at least one"); return }
        // A custom-schedule rule with no days chosen would never trigger — don't let it save
        // silently (it used to display "Every day" and do nothing).
        if (d.schedMode == SchedMode.CUSTOM && d.days.none { it }) { buzz("Pick at least one day"); return }
        val useLimit = d.kind == RuleKind.FEED && d.limitOn
        viewModelScope.launch {
            if (d.editingId != null) {
                repo.update { td ->
                    td.copy(rules = td.rules.map { r ->
                        if (r.id != d.editingId) r else r.copy(
                            kind = d.kind, targets = ids, mode = d.mode, schedMode = d.schedMode,
                            days = d.days, fromHour = d.fromHour, toHour = d.toHour,
                            limit = if (useLimit) d.limit else 0,
                            committed = d.committed, committedUntil = commitUntil(d.committed, r.committedUntil),
                        )
                    })
                }
                detailId = d.editingId; screen = Screen.DETAIL; draft = null; flash("Rule updated")
            } else {
                val r = Rule(
                    id = freshId("r"), kind = d.kind, targets = ids, mode = d.mode,
                    schedMode = d.schedMode, days = d.days, fromHour = d.fromHour, toHour = d.toHour,
                    timerEndsAt = if (d.schedMode == SchedMode.TIMER) System.currentTimeMillis() + 60 * 60_000L else null,
                    limit = if (useLimit) d.limit else 0,
                    committed = d.committed, committedUntil = commitUntil(d.committed, 0L),
                )
                repo.update { td -> td.copy(rules = listOf(r) + td.rules) }
                screen = Screen.RULES; draft = null; flash("Rule saved")
            }
        }
    }
    fun cancelDraft() { draft = null; goRules() }

    /** Commit locks a rule for 24 hours (auto-expires, so nothing is ever permanently stuck). */
    private fun commitUntil(committed: Boolean, existing: Long): Long = when {
        !committed -> 0L
        existing > System.currentTimeMillis() -> existing // keep an in-force lock, don't extend it
        else -> System.currentTimeMillis() + 24 * 60 * 60_000L
    }

    // ── detail ──
    fun toggleRuleCommit() {
        val r = currentRule() ?: return
        if (r.isLocked(System.currentTimeMillis())) { buzz(); return }
        persist { d -> d.copy(rules = d.rules.map { if (it.id == r.id) it.copy(committed = true, committedUntil = commitUntil(true, it.committedUntil)) else it }) }
    }
    fun deleteRule() {
        val r = currentRule() ?: return
        if (r.isLocked(System.currentTimeMillis())) { buzz("Committed — unlocks within 24h"); return }
        persist { d -> d.copy(rules = d.rules.filter { it.id != r.id }) }
        screen = Screen.RULES
    }
    fun detailBack() { goRules() }

    fun previewStop() {
        val r = currentRule() ?: return
        val name = ruleTitle(r, max = 1)
        if (r.mode == RuleMode.BLOCK) {
            block = BlockState(name = name, iconKey = r.targets.first(), lifts = liftLabel(r), kind = r.kind)
            screen = Screen.BLOCK
        } else {
            startFriction(name, r.kind)
        }
    }

    // ── friction / block takeovers ──
    fun startFriction(name: String, kind: RuleKind) {
        frictionJob?.cancel()
        friction = FrictionState(left = 8, total = 8, choosing = false, targetName = name, kind = kind)
        screen = Screen.FRICTION
        frictionJob = viewModelScope.launch {
            while (friction.left > 0) {
                delay(1000)
                val l = friction.left - 1
                friction = if (l <= 0) friction.copy(left = 0, choosing = true) else friction.copy(left = l)
            }
        }
    }
    fun frictionStay() { frictionJob?.cancel(); screen = Screen.HOME }
    fun frictionOpen() { frictionJob?.cancel(); screen = Screen.HOME }

    // ── focus ──
    // Persisting focusUntil is what lets the accessibility service actually block other apps
    // during a Focus session (open-ended sessions are capped at 8h so nothing gets stuck).
    fun setFocus(min: Int, label: String) {
        focusJob?.cancel()
        val secs = if (min > 0) min * 60 else null
        focus = FocusState(minutes = min, label = label, leftSecs = secs)
        sheet = null; screen = Screen.FOCUS_RUN
        val until = System.currentTimeMillis() + (secs?.times(1000L) ?: 8 * 60 * 60_000L)
        persist { it.copy(settings = it.settings.copy(focusUntil = until)) }
        if (secs != null) {
            focusJob = viewModelScope.launch {
                var left = secs
                while (left > 0) {
                    delay(1000); left -= 1
                    focus = focus?.copy(leftSecs = left)
                }
                bumpFocusMinutes(min)
                clearFocusPersist()
                focus = null; screen = Screen.HOME; flash("Focus complete — nice")
            }
        }
    }
    fun endFocus() {
        focusJob?.cancel()
        focus?.let { f ->
            val elapsed = if (f.minutes > 0 && f.leftSecs != null) f.minutes - f.leftSecs / 60 else 0
            if (elapsed > 0) bumpFocusMinutes(elapsed)
        }
        clearFocusPersist()
        focus = null; screen = Screen.HOME
    }
    private fun clearFocusPersist() = persist { it.copy(settings = it.settings.copy(focusUntil = 0L)) }

    /** Re-open the running Focus countdown after the app was reopened mid-session. */
    private fun restoreFocus(leftSecs: Int) {
        focusJob?.cancel()
        focus = FocusState(minutes = leftSecs / 60, label = "Focus", leftSecs = leftSecs)
        screen = Screen.FOCUS_RUN
        focusJob = viewModelScope.launch {
            var left = leftSecs
            while (left > 0) {
                delay(1000); left -= 1
                focus = focus?.copy(leftSecs = left)
            }
            clearFocusPersist()
            focus = null; screen = Screen.HOME
        }
    }
    private fun bumpFocusMinutes(min: Int) =
        persist { d -> d.copy(settings = d.settings.copy(focusMinutes = d.settings.focusMinutes + min)) }

    // ── alarm / habits ──
    fun openAlarm(id: String) { alarmHabitId = id; screen = Screen.ALARM }
    fun stopAlarm() {
        val id = alarmHabitId
        if (id != null) {
            persist { d ->
                d.copy(habits = d.habits.map { h ->
                    if (h.id != id || h.grid.isEmpty()) h else h.copy(grid = h.grid.toMutableList().also { it[it.size - 1] = 1 })
                })
            }
        }
        alarmHabitId = null; screen = Screen.HABITS
    }
    fun dismissAlarm() { alarmHabitId = null; screen = Screen.HABITS }

    fun newHabit() { editHabit = EditHabitState(isNew = true) }
    fun editOpen(id: String) {
        val h = habits.firstOrNull { it.id == id } ?: return
        editHabit = EditHabitState(id = h.id, isNew = false, name = h.name, remind = h.remind, remindOn = h.remindOn, days = normalizeDays(h.days))
    }
    fun closeEdit() { editHabit = null }
    fun setEditName(v: String) { editHabit = editHabit?.copy(name = v) }
    fun toggleEditRemind() { editHabit = editHabit?.let { it.copy(remindOn = !it.remindOn) } }
    fun stepEditTime(part: String, delta: Int) {
        editHabit = editHabit?.let { e ->
            val p = e.remind.split(":")
            var h = p.getOrNull(0)?.toIntOrNull() ?: 9
            var m = p.getOrNull(1)?.toIntOrNull() ?: 0
            if (part == "h") h = ((h + delta) % 24 + 24) % 24 else m = ((m + delta) % 60 + 60) % 60
            e.copy(remind = "%02d:%02d".format(h, m))
        }
    }
    fun toggleEditDay(i: Int) {
        editHabit = editHabit?.let { e ->
            val base = normalizeDays(e.days).toMutableList().also { d -> d[i] = !d[i] }
            e.copy(days = base)
        }
    }

    fun saveEdit() {
        val e = editHabit ?: return
        // A reminder with no repeat days would never ring — block the save with a hint
        // instead of persisting a dead reminder that showed a time but never fired.
        if (e.remindOn && e.days.none { it }) { buzz("Pick at least one day"); return }
        val name = e.name.trim().ifEmpty { "New habit" }
        viewModelScope.launch {
            var saved: Habit? = null
            repo.update { d ->
                if (e.isNew) {
                    val nh = Habit(id = freshId("h"), name = name, remind = e.remind, remindOn = e.remindOn, days = e.days, grid = List(28) { 0 }, createdAt = System.currentTimeMillis())
                    saved = nh
                    d.copy(habits = d.habits + nh)
                } else {
                    d.copy(habits = d.habits.map { h ->
                        if (h.id != e.id) h else h.copy(name = name, remind = e.remind, remindOn = e.remindOn, days = e.days).also { saved = it }
                    })
                }
            }
            saved?.let { syncAlarm(it) }
            editHabit = null
            flash(if (e.isNew) "Habit added" else "Habit saved")
        }
    }
    fun deleteEdit() {
        val e = editHabit ?: run { editHabit = null; return }
        if (e.isNew || e.id == null) { editHabit = null; return }
        val id = e.id
        persist { d -> d.copy(habits = d.habits.filter { it.id != id }) }
        AlarmScheduler.cancel(getApplication<Application>(), id)
        editHabit = null
    }

    private fun syncAlarm(h: Habit) {
        if (h.remindOn) AlarmScheduler.schedule(getApplication<Application>(), h)
        else AlarmScheduler.cancel(getApplication<Application>(), h.id)
    }

    /** Re-arm all habit alarms (used after boot / first launch). */
    fun rescheduleAllAlarms() {
        habits.forEach { if (it.remindOn) AlarmScheduler.schedule(getApplication<Application>(), it) }
    }

    // ── focus sheet shortcuts ──
    fun focus15() = setFocus(15, "15 min")
    fun focus30() = setFocus(30, "30 min")
    fun focus60() = setFocus(60, "1 hour")
    fun focusOpen() = setFocus(0, "until you stop")

    // ── reel counter widget ──
    fun toggleCounter() =
        persist { d -> d.copy(settings = d.settings.copy(counterEnabled = !d.settings.counterEnabled)) }

    // ── dark mode ──
    // Apply instantly (no wait for the DataStore round-trip) and mirror to bootPrefs so
    // MainActivity can set the palette at launch with no flash.
    fun toggleDark() {
        val next = !data.settings.darkMode
        com.tame.app.ui.theme.TameColors.applyDark(next)
        bootPrefs.edit().putBoolean("dark", next).apply()
        persist { d -> d.copy(settings = d.settings.copy(darkMode = next)) }
    }

    // ── home-screen widget ──
    /** Ask the launcher to pin the reels widget; fall back to manual instructions. */
    fun addHomeWidget() {
        if (systemActions?.pinHomeWidget() != true) {
            flash("Long-press your home screen → Widgets → APE")
        }
    }

    // ── daily reset (shared with the service) ──
    fun ensureDay() = viewModelScope.launch { repo.rolloverIfNeeded() }

    // ── toast ──
    fun flash(msg: String) = showToast(ToastState(msg, true), 1800)
    fun buzz(msg: String = "Locked while committed") = showToast(ToastState(msg, false), 2000)
    private fun showToast(t: ToastState, ms: Long) {
        toast = t
        toastJob?.cancel()
        toastJob = viewModelScope.launch { delay(ms); toast = null }
    }

    private fun persist(transform: (TameData) -> TameData) {
        viewModelScope.launch { repo.update(transform) }
    }
}
