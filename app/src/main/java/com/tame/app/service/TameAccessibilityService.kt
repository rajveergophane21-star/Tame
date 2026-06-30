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
    // true only while the short-form feed itself is on screen (not the rest of the app)
    @Volatile private var onFeedNow = false

    // Short-lived cache of the last window scan so content-changed and scroll events that
    // fire within a few hundred ms reuse one tree walk instead of each doing their own.
    private var cachedScan: FeedScan? = null
    private var cachedScanKey: String? = null
    private var cachedScanAt = 0L
    private var lastScrollAt = 0L
    private var lastTriggerAt = 0L
    private var lastContentEvalAt = 0L

    // feed time tracking
    private var feedTickAt = 0L
    private var feedMillisAcc = 0L

    // content-based reel counting (a substantial caption/author text change = a new reel)
    private val lastReelText = HashMap<String, String>()
    private val recentReelTexts = HashMap<String, ArrayDeque<String>>()

    // Live in-memory reel count. The floating counter reads this so it updates the
    // instant a reel is seen — no DataStore round-trip on the display path. New reels
    // are accumulated here and flushed to disk in batches (every ~10s and on feed exit),
    // so we write to storage a handful of times instead of once per reel.
    private var liveReels = -1          // -1 = not yet synced from disk
    private var unflushedReels = 0      // reels counted since the last flush
    private var lastReelPersistAt = 0L

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
            maybeFlushReels(System.currentTimeMillis())
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
        scope.launch {
            TameApp.repo.data.collect { newData ->
                // A new calendar day (rollover) zeroes today's count on disk; mirror that
                // into the live in-memory counter so the widget resets too.
                if (newData.settings.lastReelDay != data.settings.lastReelDay) {
                    liveReels = newData.settings.todayReels
                    unflushedReels = 0
                }
                data = newData
            }
        }
        handler.postDelayed(recheck, 1200)
    }

    /** The count to display/enforce against — live in-memory value, falling back to disk. */
    private fun currentReels(): Int = if (liveReels >= 0) liveReels else data.settings.todayReels

    /** Record one more reel in memory (instant); persistence happens later in a batch. */
    private fun bumpReel() {
        if (liveReels < 0) liveReels = data.settings.todayReels
        liveReels++
        unflushedReels++
    }

    /** Write accumulated reels to disk in one shot — at most every ~10s, or forced on feed exit. */
    private fun maybeFlushReels(now: Long, force: Boolean = false) {
        if (unflushedReels <= 0) return
        if (!force && now - lastReelPersistAt < 10_000L) return
        val n = unflushedReels
        unflushedReels = 0
        lastReelPersistAt = now
        scope.launch {
            TameApp.repo.update { it.copy(settings = it.settings.copy(todayReels = it.settings.todayReels + n)) }
        }
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

        val scan = scanFeedCached(app, key, now) ?: FeedScan(app.feedIsWholeApp, null)
        val feedName = app.feed ?: app.name
        onFeedNow = scan.onFeed

        if (scan.onFeed) {
            tickFeedTime(now)
            // content-based counting (Instagram caption / YouTube @handle): a substantial change = new reel
            if ((app.reelTextIds.isNotEmpty() || app.reelSignature) && scan.reelText != null) maybeCountReel(key, scan.reelText)

            if (!snoozed(key)) {
                val feedRules = data.rules.filter {
                    it.kind == RuleKind.FEED && it.targets.contains(key) && it.isActiveAt(now)
                }
                // A plain block/friction rule (no daily limit set) locks the feed the whole
                // time it's active.
                feedRules.firstOrNull { it.limit <= 0 }?.let { rule ->
                    clearFeed(); enforce(rule.mode, feedName, liftLabel(rule), key, RuleKind.FEED); return
                }
                // A rule WITH a daily limit must NOT fire on the first reel — it only locks
                // once today's count has reached the limit. (This was the bug: an all-day
                // limit rule is always "active", so the check above used to lock it instantly.)
                feedRules.firstOrNull { it.limit > 0 && currentReels() >= it.limit }?.let { rule ->
                    clearFeed(); enforce(rule.mode, feedName, "tomorrow", key, RuleKind.FEED); return
                }
            }
        } else {
            feedTickAt = now // not on the feed right now — don't accumulate time
        }

        // The floating counter belongs to the short/reel section only — show it while the
        // feed is on screen (and the user hasn't turned it off), hide it everywhere else.
        if (scan.onFeed && data.settings.counterEnabled) {
            val limitRule = data.rules.firstOrNull { it.kind == RuleKind.FEED && it.targets.contains(key) && it.limit > 0 }
            val limit = limitRule?.limit ?: data.settings.reelLimit
            val reels = currentReels()
            val ratio = if (limit > 0) reels.toFloat() / limit else 0f
            OverlayManager.showOrUpdate(this, reels, limit, ratio)
        } else {
            OverlayManager.hide(this)
        }
    }

    private fun maybeCountReel(key: String, text: String) {
        if (text == lastReelText[key]) return
        lastReelText[key] = text
        val recent = recentReelTexts.getOrPut(key) { ArrayDeque() }
        if (recent.contains(text)) return // scrolled back to a reel we already counted
        recent.addLast(text)
        if (recent.size > 40) recent.removeFirst()
        bumpReel()
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
        // Instagram is counted by caption change in handleForeground — don't scroll-count it.
        if (app.reelTextIds.isNotEmpty()) return
        val now = System.currentTimeMillis()
        if (now - lastScrollAt < 700) return
        lastScrollAt = now
        // A swipe needs the live window to decide what's on screen. Re-scan here (reusing a
        // very recent scan if one exists): it both refreshes the on-feed flag (which
        // foreground events can lag behind on a fast swipe) and gives us the current item's
        // signature for YouTube counting.
        val scan = scanFeedCached(app, key, now)
        if (scan != null) onFeedNow = scan.onFeed
        if (!onFeedNow) return

        // YouTube: count distinct Shorts by content signature (deduped, so this and the
        // content-changed path can't double-count). Others: one count per swipe.
        if (app.reelSignature) {
            scan?.reelText?.let { maybeCountReel(key, it) }
        } else {
            bumpReel()
        }
        if (!data.settings.counterEnabled) return
        val reels = currentReels()
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
                snoozeUntil[target] = System.currentTimeMillis() + 5 * 60_000L // 5 minutes
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
            maybeFlushReels(System.currentTimeMillis(), force = true)
            lastReelText.remove(feedKey)
            feedKey = null
            OverlayManager.hide(this)
        }
        onFeedNow = false
        feedTickAt = 0L
        cachedScan = null
        cachedScanKey = null
    }

    /** Scan the active window, reusing a result from the last ~250ms for the same app. */
    private fun scanFeedCached(app: KnownApp, key: String, now: Long): FeedScan? {
        cachedScan?.let { if (cachedScanKey == key && now - cachedScanAt < 250L) return it }
        val root = rootInActiveWindow ?: return null
        val s = scanFeed(root, app)
        cachedScan = s; cachedScanKey = key; cachedScanAt = now
        return s
    }

    private data class FeedScan(val onFeed: Boolean, val reelText: String?)

    /**
     * One traversal of the window: is the short-form feed on screen, and what is the
     * current reel's caption/author text (used to count distinct reels)?
     */
    private fun scanFeed(root: AccessibilityNodeInfo, app: KnownApp): FeedScan {
        if (app.feedIsWholeApp) return FeedScan(true, null)
        if (app.feedViewIds.isEmpty() && app.feedDesc.isEmpty() && app.feedSelectedIds.isEmpty()) {
            return FeedScan(false, null)
        }
        var hasFeedView = false   // reel content on screen (e.g. clips_viewer)
        var hasDesc = false       // feed content-description match (Snap/FB)
        var hasNotFeed = false    // a "not the immersive feed" marker (bottom nav home tab)
        var hasSelectedTab = false // the feed's own tab is selected (Reels tab)
        val text = StringBuilder()
        var handle: String? = null // creator @handle (YouTube signature, preferred — stable per Short)
        var longest = ""           // fallback signature: the longest text on screen (the title)
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        var visited = 0
        while (queue.isNotEmpty() && visited < 1000) {
            val node = queue.removeFirst()
            visited++
            val id = node.viewIdResourceName?.lowercase()
            if (id != null) {
                if (!hasFeedView && app.feedViewIds.any { id.contains(it) }) hasFeedView = true
                if (!hasNotFeed && app.notFeedIds.any { id.contains(it) }) hasNotFeed = true
                if (!hasSelectedTab && node.isSelected && app.feedSelectedIds.any { id.contains(it) }) hasSelectedTab = true
                if (app.reelTextIds.any { id.contains(it) }) appendNodeText(node, text)
            }
            if (app.reelSignature) {
                node.text?.toString()?.trim()?.let { t ->
                    if (handle == null && t.startsWith("@") && t.length in 2..40) handle = t
                    if (t.length in 6..120 && t.length > longest.length) longest = t
                }
            }
            if (app.feedDesc.isNotEmpty() && !hasDesc) {
                node.contentDescription?.toString()?.lowercase()?.let { d ->
                    if (app.feedDesc.any { d.contains(it) }) hasDesc = true
                }
            }
            // A selected feed tab is a definitive "on feed" — stop once we also have the
            // caption (for caption-counted apps), otherwise keep walking to capture it.
            if (hasSelectedTab && (app.reelTextIds.isEmpty() || text.isNotEmpty())) break
            for (i in 0 until node.childCount) node.getChild(i)?.let { queue.add(it) }
        }
        // On the feed when its tab is selected, OR reel content is up and the bottom-nav
        // home tab is absent (i.e. the immersive viewer, not the home feed's inline reels).
        val onFeed = hasSelectedTab || ((hasFeedView || hasDesc) && !hasNotFeed)
        val reelText = when {
            app.reelSignature -> handle ?: longest.ifBlank { null }
            else -> text.toString().trim().ifBlank { null }
        }
        return FeedScan(onFeed, reelText)
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
