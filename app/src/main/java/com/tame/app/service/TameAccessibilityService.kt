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

    override fun onServiceConnected() {
        super.onServiceConnected()
        scope.launch { TameApp.repo.data.collect { data = it } }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val e = event ?: return
        val pkg = e.packageName?.toString() ?: return
        if (pkg == packageName) return
        when (e.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> { currentPkg = pkg; handleForeground(pkg) }
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> if (pkg == currentPkg) handleForeground(pkg)
            AccessibilityEvent.TYPE_VIEW_SCROLLED -> handleScroll(pkg)
        }
    }

    private fun handleForeground(pkg: String) {
        val now = System.currentTimeMillis()
        val key = AppCatalog.keyForPackage(pkg)
        if (key == null) { clearFeed(); return }
        val app = AppCatalog[key] ?: return

        // 1) Whole-app block / friction
        data.rules.firstOrNull { it.kind == RuleKind.APP && it.targets.contains(key) && it.isActiveAt(now) }?.let { rule ->
            clearFeed()
            enforce(rule.mode, app.name, liftLabel(rule))
            return
        }

        // 2) Short-form feed
        if (isFeedOnScreen(key)) {
            feedKey = key
            val feedName = app.feed ?: app.name
            // active feed rule blocks the feed during its schedule
            data.rules.firstOrNull { it.kind == RuleKind.FEED && it.targets.contains(key) && it.isActiveAt(now) }?.let { rule ->
                clearFeed()
                enforce(rule.mode, feedName, liftLabel(rule))
                return
            }
            // daily reel limit
            val limitRule = data.rules.firstOrNull { it.kind == RuleKind.FEED && it.targets.contains(key) && it.limit > 0 }
            val reels = data.settings.todayReels
            if (limitRule != null && reels >= limitRule.limit) {
                enforce(limitRule.mode, feedName, "tomorrow")
                return
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
        if (limitRule != null && reels >= limitRule.limit) {
            enforce(limitRule.mode, AppCatalog[key]?.feed ?: "this feed", "tomorrow")
        }
    }

    private fun enforce(mode: RuleMode, name: String, lifts: String) {
        val now = System.currentTimeMillis()
        if (now - lastTriggerAt < 2500) return
        lastTriggerAt = now
        OverlayManager.hide(this)
        if (mode == RuleMode.BLOCK) {
            scope.launch { TameApp.repo.update { it.copy(settings = it.settings.copy(turnbacks = it.settings.turnbacks + 1)) } }
        }
        val intent = Intent(this, StopActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            putExtra(StopActivity.EXTRA_MODE, if (mode == RuleMode.BLOCK) "block" else "friction")
            putExtra(StopActivity.EXTRA_NAME, name)
            putExtra(StopActivity.EXTRA_LIFTS, lifts)
            putExtra(StopActivity.EXTRA_BLOCK_STYLE, data.settings.blockStyle)
        }
        runCatching { startActivity(intent) }
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
        OverlayManager.hide(this)
        scope.cancel()
        return super.onUnbind(intent)
    }
}
