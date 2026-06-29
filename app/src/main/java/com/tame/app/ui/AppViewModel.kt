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
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Single source of UI truth — a Compose-state port of the design prototype's
 * component logic, with persistence via [TameRepository] and real Android side
 * effects via [SystemActions] / [AlarmScheduler].
 */
class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val repo: TameRepository = TameApp.repo

    // ── persisted state (collected from the repository) ──
    var data by mutableStateOf(repo.snapshot()); private set

    // ── transient UI state ──
    var screen by mutableStateOf(if (data.settings.onboarded) Screen.HOME else Screen.ONBOARDING); private set
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

    // ── system bridge / permissions ──
    var systemActions: SystemActions? = null
    var permAccess by mutableStateOf(false); private set
    var permOverlay by mutableStateOf(false); private set

    private var frictionJob: Job? = null
    private var focusJob: Job? = null
    private var toastJob: Job? = null

    init {
        viewModelScope.launch { repo.data.collect { data = it } }
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

    /** Icon for a rule target (package). Feed keys use branded badges instead. */
    fun iconBitmap(target: String): ImageBitmap? =
        installedApps.firstOrNull { it.packageName == target }?.icon ?: InstalledApps.entry(target)?.icon

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
        }
    }
    fun requestAccess() { systemActions?.openAccessibilitySettings() }
    fun requestOverlay() { systemActions?.openOverlaySettings() }

    // ── onboarding ──
    fun onbNext() { if (onbStep >= 4) finishOnboarding() else onbStep++ }
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
                            id = "r" + System.currentTimeMillis(),
                            kind = RuleKind.FEED, targets = ids.take(3),
                            mode = RuleMode.FRICTION, schedMode = SchedMode.ALL_DAY, limit = 40,
                        )
                    ) + d.rules
                } else d.rules
                d.copy(settings = d.settings.copy(onboarded = true), rules = rules)
            }
            screen = Screen.HOME
            flash(if (added) "First rule created" else "You're all set")
        }
    }
    fun replayIntro() {
        viewModelScope.launch {
            repo.update { it.copy(settings = it.settings.copy(onboarded = false)) }
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
        draft = draft?.let { d -> d.copy(days = d.days.toMutableList().also { it[i] = !it[i] }, schedMode = SchedMode.CUSTOM) }
    }
    fun stepFromH(n: Int) { draft = draft?.let { it.copy(fromHour = ((it.fromHour + n) % 24 + 24) % 24, schedMode = SchedMode.CUSTOM) } }
    fun stepToH(n: Int) { draft = draft?.let { it.copy(toHour = ((it.toHour + n) % 24 + 24) % 24, schedMode = SchedMode.CUSTOM) } }
    fun stepDraftLimit(d: Int) { draft = draft?.let { it.copy(limit = (it.limit + d).coerceIn(5, 300)) } }
    fun toggleLimitOn() { draft = draft?.let { it.copy(limitOn = !it.limitOn) } }
    fun toggleDraftCommit() { draft = draft?.let { it.copy(committed = !it.committed) } }

    fun editRule() {
        val r = currentRule() ?: return
        if (r.committed) { buzz(); return }
        draft = Draft(
            kind = r.kind, targets = r.targets.toSet(), mode = r.mode,
            schedMode = r.schedMode, days = r.days, fromHour = r.fromHour, toHour = r.toHour,
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
        val useLimit = d.kind == RuleKind.FEED && d.limitOn
        viewModelScope.launch {
            if (d.editingId != null) {
                repo.update { td ->
                    td.copy(rules = td.rules.map { r ->
                        if (r.id != d.editingId) r else r.copy(
                            kind = d.kind, targets = ids, mode = d.mode, schedMode = d.schedMode,
                            days = d.days, fromHour = d.fromHour, toHour = d.toHour,
                            limit = if (useLimit) d.limit else 0, committed = d.committed,
                        )
                    })
                }
                detailId = d.editingId; screen = Screen.DETAIL; draft = null; flash("Rule updated")
            } else {
                val r = Rule(
                    id = "r" + System.currentTimeMillis(), kind = d.kind, targets = ids, mode = d.mode,
                    schedMode = d.schedMode, days = d.days, fromHour = d.fromHour, toHour = d.toHour,
                    timerEndsAt = if (d.schedMode == SchedMode.TIMER) System.currentTimeMillis() + 60 * 60_000L else null,
                    limit = if (useLimit) d.limit else 0, committed = d.committed,
                )
                repo.update { td -> td.copy(rules = listOf(r) + td.rules) }
                screen = Screen.RULES; draft = null; flash("Rule saved")
            }
        }
    }
    fun cancelDraft() { draft = null; goRules() }

    // ── detail ──
    fun toggleRuleCommit() {
        val r = currentRule() ?: return
        if (r.committed) { buzz(); return }
        persist { d -> d.copy(rules = d.rules.map { if (it.id == r.id) it.copy(committed = true) else it }) }
    }
    fun deleteRule() {
        val r = currentRule() ?: return
        if (r.committed) { buzz("Committed — can't delete this yet"); return }
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
    fun setFocus(min: Int, label: String) {
        focusJob?.cancel()
        val secs = if (min > 0) min * 60 else null
        focus = FocusState(minutes = min, label = label, leftSecs = secs)
        sheet = null; screen = Screen.FOCUS_RUN
        if (secs != null) {
            focusJob = viewModelScope.launch {
                var left = secs
                while (left > 0) {
                    delay(1000); left -= 1
                    focus = focus?.copy(leftSecs = left)
                }
                bumpFocusMinutes(min)
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
        focus = null; screen = Screen.HOME
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
        editHabit = EditHabitState(id = h.id, isNew = false, name = h.name, remind = h.remind, remindOn = h.remindOn, days = h.days)
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
    fun toggleEditDay(i: Int) { editHabit = editHabit?.let { it.copy(days = it.days.toMutableList().also { d -> d[i] = !d[i] }) } }

    fun saveEdit() {
        val e = editHabit ?: return
        val name = e.name.trim().ifEmpty { "New habit" }
        viewModelScope.launch {
            var saved: Habit? = null
            repo.update { d ->
                if (e.isNew) {
                    val nh = Habit(id = "h" + System.currentTimeMillis(), name = name, remind = e.remind, remindOn = e.remindOn, days = e.days, grid = List(28) { 0 })
                    saved = nh
                    d.copy(habits = d.habits + nh)
                } else {
                    d.copy(habits = d.habits.map { h ->
                        if (h.id != e.id) h else h.copy(name = name, remind = e.remind, remindOn = e.remindOn, days = e.days).also { saved = it }
                    })
                }
            }
            saved?.let { syncAlarm(it) }
            if (saved?.remindOn == true) systemActions?.requestExactAlarmIfNeeded()
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

    // ── daily reset ──
    fun ensureDay() = viewModelScope.launch {
        repo.update { d ->
            val today = TameRepository.today()
            val last = d.settings.lastReelDay
            if (last == today) return@update d
            val elapsed = daysBetween(last, today)
            val under = last.isNotEmpty() && d.settings.todayReels <= d.settings.reelLimit
            val days = (if (under) d.settings.daysUnderLimit + 1 else d.settings.daysUnderLimit).coerceIn(0, 7)
            val habits = if (elapsed > 0) d.habits.map { rollGrid(it, elapsed) } else d.habits
            d.copy(
                settings = d.settings.copy(todayReels = 0, reelSeconds = 0, lastReelDay = today, daysUnderLimit = days),
                habits = habits,
            )
        }
    }

    private fun daysBetween(last: String, today: String): Int {
        if (last.isEmpty()) return 0
        return try {
            ChronoUnit.DAYS.between(LocalDate.parse(last), LocalDate.parse(today)).toInt().coerceIn(0, 28)
        } catch (e: Exception) { 1 }
    }

    /** Advance a 28-cell habit history by [shift] days so the last cell is always today. */
    private fun rollGrid(h: Habit, shift: Int): Habit {
        if (h.grid.isEmpty()) return h.copy(grid = List(28) { 0 })
        val n = h.grid.size
        val s = shift.coerceIn(0, n)
        if (s == 0) return h
        return h.copy(grid = (h.grid.drop(s) + List(s) { 0 }).takeLast(n))
    }

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
