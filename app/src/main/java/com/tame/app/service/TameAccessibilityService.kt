package com.tame.app.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.inputmethod.InputMethodManager
import com.tame.app.data.TameRepository
import com.tame.app.TameApp
import com.tame.app.data.model.AppCatalog
import com.tame.app.data.model.KnownApp
import com.tame.app.data.model.RuleKind
import com.tame.app.data.model.RuleMode
import com.tame.app.data.model.TameData
import com.tame.app.ui.liftLabel
import com.tame.app.ui.theme.Accents
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Watches the foreground app + short-form feeds and steps in with the stop
 * screen / floating reel counter. The feed-detection signatures live in
 * [AppCatalog] and are heuristic — they may need updating as apps change.
 */
class TameAccessibilityService : AccessibilityService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Volatile private var data: TameData = TameData()
    private var currentPkg: String? = null
    private var feedKey: String? = null
    private var lastScrollAt = 0L
    private var lastTriggerAt = 0L
    private var lastContentEvalAt = 0L

    // feed time tracking
    private var feedTickAt = 0L
    private var feedMillisAcc = 0L

    // content-based reel counting (a substantial caption/author text change = a new reel)
    private val lastReelText = HashMap<String, String>()
    private val recentReelTexts = HashMap<String, ArrayDeque<String>>()

    private var blockOverlay: BlockOverlay? = null
    private var blockedPkg: String? = null
    private val snoozeUntil = HashMap<String, Long>()

    private fun snoozed(target: String) = System.currentTimeMillis() < (snoozeUntil[target] ?: 0L)

    // Input-method packages (the keyboard) — their windows must not count as a
    // foreground app change, or the overlay/counter flickers off while typing.
    private val imePackages: Set<String> by lazy {
        runCatching {
            getSystemService(InputMethodManager::class.java)?.enabledInputMethodList?.mapNotNull { it.packageName }?.toSet()
        }.getOrNull() ?: emptySet()
    }

    /** Transient/system windows that should NOT be treated as the foreground app. */
    private fun ignoredWindow(pkg: String): Boolean =
        pkg == packageName ||
            pkg == "android" ||
            pkg == "com.android.systemui" ||
            pkg == "com.google.android.apps.wellbeing" ||
            pkg in imePackages

    private val handler = Handler(Looper.getMainLooper())
    // Safety net: re-evaluate the foreground app even when no events fire, so a rule
    // that starts mid-session (or a block dismissed by the system) re-applies.
    private val recheck = object : Runnable {
        override fun run() {
            rolloverCheck()
            recheckApps()
            handler.postDelayed(this, 1200)
        }
    }

    private fun rolloverCheck() {
        val last = data.settings.lastReelDay
        if (last.isNotEmpty() && last != TameRepository.today()) {
            scope.launch { TameApp.repo.rolloverIfNeeded() }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        scope.launch { TameApp.repo.data.collect { data = it } }
        handler.postDelayed(recheck, 1200)
    }

    private fun recheckApps() {
        val pkg = currentPkg ?: return
        if (pkg == packageName || blockOverlay?.isShowing == true) return
        val now = System.currentTimeMillis()
        data.rules.firstOrNull { it.kind == RuleKind.APP && it.targets.contains(pkg) && it.isActiveAt(now) }?.let { rule ->
            if (!snoozed(pkg)) enforce(rule.mode, appLabel(pkg), liftLabel(rule), pkg, RuleKind.APP)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val e = event ?: return
        val pkg = e.packageName?.toString() ?: return
        // Ignore the keyboard, status bar/notification shade, dialogs and our own
        // windows so they don't masquerade as a foreground-app change (this was
        // making the reel counter vanish after a few seconds).
        if (ignoredWindow(pkg)) return
        when (e.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                currentPkg = pkg
                // user navigated away from the blocked app/feed -> take the stop screen down
                if (blockOverlay?.isShowing == true && pkg != blockedPkg) dismissOverlay()
                handleForeground(pkg)
            }
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                // content-changed fires continuously while scrolling — throttle the node traversal
                val now = System.currentTimeMillis()
                if (pkg == currentPkg && now - lastContentEvalAt >= 500) {
                    lastContentEvalAt = now
                    handleForeground(pkg)
                }
            }
            AccessibilityEvent.TYPE_VIEW_SCROLLED -> handleScroll(pkg)
        }
    }

    private fun handleForeground(pkg: String) {
        val now = System.currentTimeMillis()

        // 1) Whole-app block / friction — APP rule targets are package names
        data.rules.firstOrNull { it.kind == RuleKind.APP && it.targets.contains(pkg) && it.isActiveAt(now) }?.let { rule ->
            clearFeed()
            if (!snoozed(pkg)) enforce(rule.mode, appLabel(pkg), liftLabel(rule), pkg, RuleKind.APP)
            return
        }

        // 2) Short-form feed — FEED rule targets are catalog feed keys
        val key = AppCatalog.keyForPackage(pkg)
        if (key == null) { clearFeed(); return }
        val app = AppCatalog[key] ?: return

        // We're in a known short-form app. The counter/time follow being IN the app
        // (package is stable while scrolling, so the overlay never flickers off).
        // One node scan tells us if the feed is on screen + the current reel's text.
        if (feedKey != key) { feedKey = key; feedTickAt = now }

        val root = rootInActiveWindow
        val scan = if (root != null) scanFeed(root, app) else FeedScan(app.feedIsWholeApp, null)
        val feedName = app.feed ?: app.name

        if (scan.onFeed) {
            tickFeedTime(now)
            // content-based counting (Instagram/YouTube): a substantial caption change = new reel
            if (app.reelTextIds.isNotEmpty() && scan.reelText != null) maybeCountReel(key, scan.reelText)

            if (!snoozed(key)) {
                data.rules.firstOrNull { it.kind == RuleKind.FEED && it.targets.contains(key) && it.isActiveAt(now) }?.let { rule ->
                    clearFeed(); enforce(rule.mode, feedName, liftLabel(rule), key, RuleKind.FEED); return
                }
                data.rules.firstOrNull { it.kind == RuleKind.FEED && it.targets.contains(key) && it.limit > 0 }?.let { limitRule ->
                    if (data.settings.todayReels >= limitRule.limit) { enforce(limitRule.mode, feedName, "tomorrow", key, RuleKind.FEED); return }
                }
            }
        } else {
            feedTickAt = now // not on the feed right now — don't accumulate time
        }

        val limitRule = data.rules.firstOrNull { it.kind == RuleKind.FEED && it.targets.contains(key) && it.limit > 0 }
        val limit = limitRule?.limit ?: data.settings.reelLimit
        val reels = data.settings.todayReels
        val ratio = if (limit > 0) reels.toFloat() / limit else 0f
        OverlayManager.showOrUpdate(this, reels, limit, ratio)
    }

    private fun maybeCountReel(key: String, text: String) {
        if (text == lastReelText[key]) return
        lastReelText[key] = text
        val recent = recentReelTexts.getOrPut(key) { ArrayDeque() }
        if (recent.contains(text)) return // scrolled back to a reel we already counted
        recent.addLast(text)
        if (recent.size > 40) recent.removeFirst()
        scope.launch { TameApp.repo.update { it.copy(settings = it.settings.copy(todayReels = it.settings.todayReels + 1)) } }
    }

    /** Accumulate time-on-feed and flush to storage every few seconds. */
    private fun tickFeedTime(now: Long) {
        if (feedTickAt > 0L) {
            val delta = now - feedTickAt
            if (delta in 1..15_000L) feedMillisAcc += delta
        }
        feedTickAt = now
        if (feedMillisAcc >= 5_000L) {
            val addSec = (feedMillisAcc / 1000L).toInt()
            feedMillisAcc -= addSec * 1000L
            scope.launch { TameApp.repo.update { it.copy(settings = it.settings.copy(reelSeconds = it.settings.reelSeconds + addSec)) } }
        }
    }

    private fun flushFeedTime() {
        val addSec = (feedMillisAcc / 1000L).toInt()
        feedMillisAcc = 0L
        feedTickAt = 0L
        if (addSec > 0) scope.launch { TameApp.repo.update { it.copy(settings = it.settings.copy(reelSeconds = it.settings.reelSeconds + addSec)) } }
    }

    private fun handleScroll(pkg: String) {
        val key = AppCatalog.keyForPackage(pkg) ?: return
        if (key != feedKey) return
        val app = AppCatalog[key] ?: return
        // apps with caption text (IG/YT) are counted by content change; only scroll-count the rest
        if (app.reelTextIds.isNotEmpty()) return
        val now = System.currentTimeMillis()
        if (now - lastScrollAt < 700) return
        lastScrollAt = now

        // count this reel; enforcement (limit / active rule) is handled in handleForeground
        scope.launch {
            TameApp.repo.update { it.copy(settings = it.settings.copy(todayReels = it.settings.todayReels + 1)) }
        }
        val reels = data.settings.todayReels + 1
        val limitRule = data.rules.firstOrNull { it.kind == RuleKind.FEED && it.targets.contains(key) && it.limit > 0 }
        val limit = limitRule?.limit ?: data.settings.reelLimit
        val ratio = if (limit > 0) reels.toFloat() / limit else 0f
        OverlayManager.showOrUpdate(this, reels, limit, ratio)
    }

    /** Show the stop screen as a full-screen overlay (reliable from a service). */
    private fun enforce(mode: RuleMode, name: String, lifts: String, target: String, kind: RuleKind) {
        val now = System.currentTimeMillis()
        if (blockOverlay?.isShowing == true || now - lastTriggerAt < 600) return
        lastTriggerAt = now
        OverlayManager.hide(this)
        if (mode == RuleMode.BLOCK) {
            scope.launch { TameApp.repo.update { it.copy(settings = it.settings.copy(turnbacks = it.settings.turnbacks + 1)) } }
        }
        blockedPkg = currentPkg
        // FEED: press Back to leave just the feed (stay in the app). APP: go Home.
        val leave: () -> Unit = if (kind == RuleKind.FEED) {
            { runCatching { performGlobalAction(GLOBAL_ACTION_BACK) } }
        } else {
            { runCatching { performGlobalAction(GLOBAL_ACTION_HOME) } }
        }
        val palette = Accents.byKey(data.settings.accentKey)
        val overlay = BlockOverlay(this, palette, onLeave = leave)
        blockOverlay = overlay
        if (mode == RuleMode.BLOCK) {
            overlay.showBlock(data.settings.blockStyle, name, lifts)
        } else {
            overlay.showFriction(name, onOpenAnyway = {
                snoozeUntil[target] = System.currentTimeMillis() + 60_000L
                blockedPkg = null
            })
        }
    }

    private fun appLabel(pkg: String): String =
        com.tame.app.util.InstalledApps.label(this, pkg)

    private fun dismissOverlay() {
        blockOverlay?.hide()
        blockOverlay = null
        blockedPkg = null
        lastTriggerAt = 0L
    }

    private fun clearFeed() {
        if (feedKey != null) {
            flushFeedTime()
            lastReelText.remove(feedKey)
            feedKey = null
            OverlayManager.hide(this)
        }
        feedTickAt = 0L
    }

    private data class FeedScan(val onFeed: Boolean, val reelText: String?)

    /**
     * One traversal of the window: is the short-form feed on screen, and what is the
     * current reel's caption/author text (used to count distinct reels)?
     */
    private fun scanFeed(root: AccessibilityNodeInfo, app: KnownApp): FeedScan {
        if (app.feedIsWholeApp) return FeedScan(true, null)
        if (app.feedViewIds.isEmpty() && app.feedDesc.isEmpty()) return FeedScan(false, null)
        var onFeed = false
        val text = StringBuilder()
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        var visited = 0
        while (queue.isNotEmpty() && visited < 900) {
            val node = queue.removeFirst()
            visited++
            val id = node.viewIdResourceName?.lowercase()
            if (id != null) {
                if (app.feedViewIds.any { id.contains(it) }) onFeed = true
                if (app.reelTextIds.any { id.contains(it) }) appendNodeText(node, text)
            }
            if (app.feedDesc.isNotEmpty()) {
                node.contentDescription?.toString()?.lowercase()?.let { d ->
                    if (app.feedDesc.any { d.contains(it) }) onFeed = true
                }
            }
            for (i in 0 until node.childCount) node.getChild(i)?.let { queue.add(it) }
        }
        return FeedScan(onFeed, text.toString().trim().ifBlank { null })
    }

    /** Collect text from a node's small subtree (the caption/author block). */
    private fun appendNodeText(node: AccessibilityNodeInfo, out: StringBuilder) {
        val q = ArrayDeque<AccessibilityNodeInfo>()
        q.add(node)
        var n = 0
        while (q.isNotEmpty() && n < 40) {
            val cur = q.removeFirst()
            n++
            cur.text?.toString()?.let { if (it.isNotBlank()) out.append(it).append(' ') }
            for (i in 0 until cur.childCount) cur.getChild(i)?.let { q.add(it) }
        }
    }

    override fun onInterrupt() {}

    override fun onUnbind(intent: Intent?): Boolean {
        handler.removeCallbacks(recheck)
        dismissOverlay()
        OverlayManager.hide(this)
        scope.cancel()
        return super.onUnbind(intent)
    }
}
