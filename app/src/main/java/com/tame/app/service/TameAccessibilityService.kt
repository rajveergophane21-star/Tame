package com.tame.app.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.tame.app.TameApp
import com.tame.app.data.model.AppCatalog
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

    private var blockOverlay: BlockOverlay? = null
    private var blockedPkg: String? = null
    private val snoozeUntil = HashMap<String, Long>()

    private fun snoozed(target: String) = System.currentTimeMillis() < (snoozeUntil[target] ?: 0L)

    override fun onServiceConnected() {
        super.onServiceConnected()
        scope.launch { TameApp.repo.data.collect { data = it } }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val e = event ?: return
        val pkg = e.packageName?.toString() ?: return
        if (pkg == packageName) return
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
            if (!snoozed(pkg)) enforce(rule.mode, appLabel(pkg), liftLabel(rule), pkg)
            return
        }

        // 2) Short-form feed — FEED rule targets are catalog feed keys
        val key = AppCatalog.keyForPackage(pkg)
        if (key == null) { clearFeed(); return }
        val app = AppCatalog[key] ?: return

        // We're in a known short-form app. The counter/time follow being IN the app
        // (package is stable while scrolling, so the overlay never flickers off).
        // Strict on-screen detection is only used to decide when to BLOCK the feed.
        if (feedKey != key) { feedKey = key; feedTickAt = now }
        tickFeedTime(now)

        val detected = isFeedOnScreen(key)
        val feedName = app.feed ?: app.name
        if (!snoozed(key) && detected) {
            data.rules.firstOrNull { it.kind == RuleKind.FEED && it.targets.contains(key) && it.isActiveAt(now) }?.let { rule ->
                clearFeed(); enforce(rule.mode, feedName, liftLabel(rule), key); return
            }
            data.rules.firstOrNull { it.kind == RuleKind.FEED && it.targets.contains(key) && it.limit > 0 }?.let { limitRule ->
                if (data.settings.todayReels >= limitRule.limit) { enforce(limitRule.mode, feedName, "tomorrow", key); return }
            }
        }
        val limitRule = data.rules.firstOrNull { it.kind == RuleKind.FEED && it.targets.contains(key) && it.limit > 0 }
        val limit = limitRule?.limit ?: data.settings.reelLimit
        val reels = data.settings.todayReels
        val ratio = if (limit > 0) reels.toFloat() / limit else 0f
        OverlayManager.showOrUpdate(this, reels, limit, ratio)
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

    /** Show the stop screen as a TYPE_ACCESSIBILITY_OVERLAY (reliable from a service). */
    private fun enforce(mode: RuleMode, name: String, lifts: String, target: String) {
        val now = System.currentTimeMillis()
        if (blockOverlay?.isShowing == true || now - lastTriggerAt < 600) return
        lastTriggerAt = now
        OverlayManager.hide(this)
        if (mode == RuleMode.BLOCK) {
            scope.launch { TameApp.repo.update { it.copy(settings = it.settings.copy(turnbacks = it.settings.turnbacks + 1)) } }
        }
        blockedPkg = currentPkg
        val palette = Accents.byKey(data.settings.accentKey)
        val overlay = BlockOverlay(this, palette, onHome = { runCatching { performGlobalAction(GLOBAL_ACTION_HOME) } })
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
            feedKey = null
            OverlayManager.hide(this)
        }
        feedTickAt = 0L
    }

    /** Heuristic: is the app's short-form feed currently on screen? */
    private fun isFeedOnScreen(key: String): Boolean {
        val app = AppCatalog[key] ?: return false
        if (app.feedIsWholeApp) return true
        if (app.feedSignatures.isEmpty()) return false
        val root = rootInActiveWindow ?: return false
        return nodeMatches(root, app.feedSignatures)
    }

    private fun nodeMatches(root: AccessibilityNodeInfo, signatures: List<String>): Boolean {
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        var visited = 0
        while (queue.isNotEmpty() && visited < 500) {
            val node = queue.removeFirst()
            visited++
            val id = node.viewIdResourceName?.lowercase()
            if (id != null && signatures.any { id.contains(it) }) return true
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { queue.add(it) }
            }
        }
        return false
    }

    override fun onInterrupt() {}

    override fun onUnbind(intent: Intent?): Boolean {
        dismissOverlay()
        OverlayManager.hide(this)
        scope.cancel()
        return super.onUnbind(intent)
    }
}
