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
        if (isFeedOnScreen(key)) {
            feedKey = key
            val feedName = app.feed ?: app.name
            val activeRule = data.rules.firstOrNull { it.kind == RuleKind.FEED && it.targets.contains(key) && it.isActiveAt(now) }
            val limitRule = data.rules.firstOrNull { it.kind == RuleKind.FEED && it.targets.contains(key) && it.limit > 0 }
            val reels = data.settings.todayReels
            if (!snoozed(key)) {
                if (activeRule != null) { clearFeed(); enforce(activeRule.mode, feedName, liftLabel(activeRule), key); return }
                if (limitRule != null && reels >= limitRule.limit) { enforce(limitRule.mode, feedName, "tomorrow", key); return }
            }
            val limit = limitRule?.limit ?: data.settings.reelLimit
            val ratio = if (limit > 0) reels.toFloat() / limit else 0f
            OverlayManager.showOrUpdate(this, reels, limit, ratio)
        } else {
            clearFeed()
        }
    }

    private fun handleScroll(pkg: String) {
        val key = AppCatalog.keyForPackage(pkg) ?: return
        if (key != feedKey) return
        val now = System.currentTimeMillis()
        // an active feed block/friction rule takes precedence over counting
        if (!snoozed(key)) {
            data.rules.firstOrNull { it.kind == RuleKind.FEED && it.targets.contains(key) && it.isActiveAt(now) }?.let { rule ->
                clearFeed()
                enforce(rule.mode, AppCatalog[key]?.feed ?: "this feed", liftLabel(rule), key)
                return
            }
        }
        if (now - lastScrollAt < 700) return
        lastScrollAt = now

        scope.launch {
            TameApp.repo.update { it.copy(settings = it.settings.copy(todayReels = it.settings.todayReels + 1)) }
        }
        val reels = data.settings.todayReels + 1
        val limitRule = data.rules.firstOrNull { it.kind == RuleKind.FEED && it.targets.contains(key) && it.limit > 0 }
        val limit = limitRule?.limit ?: data.settings.reelLimit
        val ratio = if (limit > 0) reels.toFloat() / limit else 0f
        OverlayManager.showOrUpdate(this, reels, limit, ratio)
        if (limitRule != null && reels >= limitRule.limit && !snoozed(key)) {
            enforce(limitRule.mode, AppCatalog[key]?.feed ?: "this feed", "tomorrow", key)
        }
    }

    /** Show the stop screen as a TYPE_ACCESSIBILITY_OVERLAY (reliable from a service). */
    private fun enforce(mode: RuleMode, name: String, lifts: String, target: String) {
        val now = System.currentTimeMillis()
        if (now - lastTriggerAt < 2500 || blockOverlay?.isShowing == true) return
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
    }

    private fun clearFeed() {
        if (feedKey != null) {
            feedKey = null
            OverlayManager.hide(this)
        }
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
