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

    // A focusUntil value we just cleared locally (via "Stop focus"). A stale disk emission
    // still carrying it must not re-arm the session before our own write lands.
    @Volatile private var clearedFocusUntil = 0L

    private fun snoozed(target: String) = System.currentTimeMillis() < (snoozeUntil[target] ?: 0L)

    // Input-method packages (the keyboard) — their windows must not count as a
    // foreground app change, or the overlay/counter flickers off while typing.
    private val imePackages: Set<String> by lazy {
        runCatching {
            getSystemService(InputMethodManager::class.java)?.enabledInputMethodList?.mapNotNull { it.packageName }?.toSet()
        }.getOrNull() ?: emptySet()
    }

    // Apps never blocked during a Focus session (so you can reach home + place calls).
    private val essentialPackages: Set<String> by lazy {
        val out = mutableSetOf(packageName, "android", "com.android.systemui", "com.android.phone", "com.android.server.telecom")
        runCatching {
            packageManager.resolveActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0)
                ?.activityInfo?.packageName?.let { out.add(it) }
            packageManager.resolveActivity(Intent(Intent.ACTION_DIAL), 0)
                ?.activityInfo?.packageName?.let { out.add(it) }
        }
        out
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
            periodicRecheck()
            maybeFlushReels(System.currentTimeMillis())
            maybeUpdateWidget()
            handler.postDelayed(this, 900)
        }
    }

    // Push today's reels + feed time to the home-screen widget, but only when a DISPLAYED
    // value changed. The widget shows whole minutes, so dedupe on minutes — keying on raw
    // seconds would fire a cross-process RemoteViews update nearly every 900ms tick while
    // a feed is open (battery/IPC churn, and launchers throttle chatty widgets).
    private var lastWidgetReels = -1
    private var lastWidgetMins = -1
    private fun maybeUpdateWidget() {
        val reels = currentReels()
        val secs = data.settings.reelSeconds + (feedMillisAcc / 1000L).toInt()
        val mins = secs / 60
        if (reels == lastWidgetReels && mins == lastWidgetMins) return
        lastWidgetReels = reels; lastWidgetMins = mins
        com.tame.app.widget.ReelWidgetProvider.update(this, reels, secs)
    }

    private fun rolloverCheck() {
        val last = data.settings.lastReelDay
        if (last.isNotEmpty() && last != TameRepository.today()) {
            // Flush any pending reels and roll over in ONE coroutine, in order, so the flush
            // can't land after the reset and get mis-attributed to the new day.
            val n = unflushedReels
            unflushedReels = 0
            scope.launch {
                if (n > 0) TameApp.repo.update { it.copy(settings = it.settings.copy(todayReels = it.settings.todayReels + n)) }
                TameApp.repo.rolloverIfNeeded()
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        scope.launch {
            TameApp.repo.data.collect { newData ->
                var nd = newData
                // Ignore a stale disk echo of a Focus we just stopped locally (would otherwise
                // re-arm the block for a moment until our own focusUntil=0 write lands).
                val f = nd.settings.focusUntil
                if (f != 0L && f == clearedFocusUntil) {
                    nd = nd.copy(settings = nd.settings.copy(focusUntil = 0L))
                } else {
                    // Off (0) or a genuinely new session — stop suppressing so a stale value
                    // can't linger and wrongly cancel a later focus that happens to reuse it.
                    clearedFocusUntil = 0L
                }
                // A new calendar day (rollover) zeroes today's count on disk; mirror that into
                // the live in-memory counter. Post onto the main handler so the reset can't race
                // the reel-counting on the event thread (which also mutates these two fields).
                if (nd.settings.lastReelDay != data.settings.lastReelDay) {
                    val reset = nd.settings.todayReels
                    handler.post { liveReels = reset; unflushedReels = 0 }
                }
                // Same process as the app — keep the shared palette in sync so overlays match
                // the theme even if the service is running before the app was opened.
                com.tame.app.ui.theme.TameColors.applyDark(nd.settings.darkMode)
                data = nd
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

    /**
     * Safety-net re-evaluation of the current app/feed on a timer, so blocking still
     * applies when accessibility events are sparse or the screen read came back empty
     * on the last event (both common while scrolling a feed).
     */
    private fun periodicRecheck() {
        if (blockOverlay?.isShowing == true) return
        // Derive the REAL foreground app from the live window — currentPkg can be stale
        // (e.g. it still points at the last app while APE itself is in the foreground), which
        // would otherwise pop a block screen over APE. Skip if we can't read it.
        val pkg = activeRoot()?.packageName?.toString() ?: return
        if (ignoredWindow(pkg)) return
        currentPkg = pkg
        cachedScan = null // force a fresh window read
        handleForeground(pkg)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val e = event ?: return
        val pkg = e.packageName?.toString() ?: return
        // Events from our own package need care: the real app UI (MainActivity) coming to the
        // foreground SHOULD take a stop screen down so in-app controls are reachable — but our
        // OWN stop-screen overlay also reports events under our package name (the friction
        // countdown ticking, block-screen text), and dismissing on those tears the overlay down
        // and re-triggers the block in a loop (the blinking). So only react to MainActivity's
        // own window-state change; ignore everything else from our package.
        if (pkg == packageName) {
            val isAppUi = e.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
                e.className?.toString()?.contains("MainActivity") == true
            if (isAppUi && blockOverlay?.isShowing == true) dismissOverlay()
            return
        }
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
                // Only act on content-changed for the app we already consider foreground. A real
                // app switch reliably fires a state-change (handled above) or is caught by the
                // ~900ms periodic recheck (which reads the true active window). Treating a
                // content event from any other package as a switch would misfire on things like
                // a picture-in-picture video or a floating window and flicker a valid block.
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

        // 0) Focus session — block everything except APE and essential apps (launcher/dialer).
        if (data.settings.focusUntil > now && pkg !in essentialPackages) {
            clearFeed()
            enforceFocus(pkg)
            return
        }

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
        onFeedNow = scan.onFeed

        if (scan.onFeed) {
            tickFeedTime(now)
            // content-based counting (Instagram caption / YouTube @handle): a substantial change = new reel
            if ((app.reelTextIds.isNotEmpty() || app.reelSignature) && scan.reelText != null) maybeCountReel(key, scan.reelText)

            if (enforceFeed(key, app, now)) return
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
        // A null scan means we couldn't read the window this pass — don't fall through on a
        // stale onFeedNow from a previous app (that could count/enforce against the wrong feed).
        val scan = scanFeedCached(app, key, now) ?: return
        onFeedNow = scan.onFeed
        if (!onFeedNow) return

        // Re-apply blocking the instant you swipe, instead of waiting for the next
        // foreground/content event (which may not fire or may read an empty screen).
        if (enforceFeed(key, app, now)) return

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
    private fun enforce(mode: RuleMode, name: String, lifts: String, target: String, kind: RuleKind, wholeAppFeed: Boolean = false) {
        val now = System.currentTimeMillis()
        if (blockOverlay?.isShowing == true || now - lastTriggerAt < 600) return
        lastTriggerAt = now
        OverlayManager.hide(this)
        if (mode == RuleMode.BLOCK) {
            scope.launch { TameApp.repo.update { it.copy(settings = it.settings.copy(turnbacks = it.settings.turnbacks + 1)) } }
        }
        blockedPkg = currentPkg
        // FEED-in-app (IG Reels): press Back to leave just the feed and stay in the app.
        // Whole-app feed (TikTok) or APP block: go Home — Back won't escape an app that IS the
        // feed, so "Back to home" would otherwise just loop the block screen.
        val leave: () -> Unit = if (kind == RuleKind.FEED && !wholeAppFeed) {
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

    /**
     * Focus-session stop screen. Unlike a normal block it ALWAYS offers a "Stop focus"
     * exit right on the overlay, so a Focus session can never lock you out — even before
     * you reach APE. "Back to home" goes Home (the launcher is never blocked).
     */
    private fun enforceFocus(pkg: String) {
        val now = System.currentTimeMillis()
        if (blockOverlay?.isShowing == true || now - lastTriggerAt < 600) return
        lastTriggerAt = now
        OverlayManager.hide(this)
        blockedPkg = currentPkg
        val palette = Accents.byKey(data.settings.accentKey)
        val overlay = BlockOverlay(this, palette, onLeave = { runCatching { performGlobalAction(GLOBAL_ACTION_HOME) } })
        blockOverlay = overlay
        overlay.showFocusBlock(appLabel(pkg), onStopFocus = { stopFocus() })
    }

    /** End the Focus session immediately (from the overlay's "Stop focus"). */
    private fun stopFocus() {
        // Remember the value we're clearing so a stale disk emission carrying it can't re-arm
        // the session (see the collector), clear it in memory so the periodic recheck can't
        // re-block before the write lands, then persist and take the screen down.
        clearedFocusUntil = data.settings.focusUntil
        data = data.copy(settings = data.settings.copy(focusUntil = 0L))
        dismissOverlay()
        scope.launch { TameApp.repo.update { it.copy(settings = it.settings.copy(focusUntil = 0L)) } }
        runCatching { performGlobalAction(GLOBAL_ACTION_HOME) }
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

    /**
     * Scan the active window, reusing a *positive* result from the last ~250ms for the same app.
     * A negative ("not on feed") scan is never cached — right after the feed appears the first
     * read can still be empty, and caching that would suppress blocking until the next event.
     */
    private fun scanFeedCached(app: KnownApp, key: String, now: Long): FeedScan? {
        cachedScan?.let { if (cachedScanKey == key && now - cachedScanAt < 250L && it.onFeed) return it }
        val root = activeRoot() ?: return null
        val s = scanFeed(root, app)
        if (s.onFeed) { cachedScan = s; cachedScanKey = key; cachedScanAt = now }
        return s
    }

    /**
     * rootInActiveWindow is frequently null mid-scroll, which used to make blocking
     * intermittent. Fall back to the active window from the windows list.
     */
    private fun activeRoot(): AccessibilityNodeInfo? {
        rootInActiveWindow?.let { return it }
        return runCatching {
            val ws = windows
            ws.firstOrNull { it.isActive }?.root
                ?: ws.mapNotNull { it.root }.firstOrNull { it.packageName != packageName }
        }.getOrNull()
    }

    /** If a feed rule applies to [key] right now, step in. Returns true if it did. */
    private fun enforceFeed(key: String, app: KnownApp, now: Long): Boolean {
        if (snoozed(key)) return false
        val feedName = app.feed ?: app.name
        val feedRules = data.rules.filter {
            it.kind == RuleKind.FEED && it.targets.contains(key) && it.isActiveAt(now)
        }
        // A plain block/friction rule (no daily limit) locks the feed whenever it's active.
        feedRules.firstOrNull { it.limit <= 0 }?.let { rule ->
            clearFeed(); enforce(rule.mode, feedName, liftLabel(rule), key, RuleKind.FEED, app.feedIsWholeApp); return true
        }
        // A daily-limit rule only locks once today's count has reached the limit.
        feedRules.firstOrNull { it.limit > 0 && currentReels() >= it.limit }?.let { rule ->
            clearFeed(); enforce(rule.mode, feedName, "tomorrow", key, RuleKind.FEED, app.feedIsWholeApp); return true
        }
        return false
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
        // Final synchronous flush BEFORE cancelling the scope — the async batch flush would
        // otherwise be cancelled and up to ~10s of counted reels + feed seconds would vanish.
        val addSec = (feedMillisAcc / 1000L).toInt()
        val n = unflushedReels
        feedMillisAcc = 0L; unflushedReels = 0
        if (addSec > 0 || n > 0) {
            runCatching {
                TameApp.repo.updateBlocking {
                    it.copy(settings = it.settings.copy(
                        reelSeconds = it.settings.reelSeconds + addSec,
                        todayReels = it.settings.todayReels + n,
                    ))
                }
            }
        }
        dismissOverlay()
        OverlayManager.hide(this)
        scope.cancel()
        return super.onUnbind(intent)
    }
}
