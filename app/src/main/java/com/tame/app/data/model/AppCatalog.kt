package com.tame.app.data.model

/**
 * Static catalog of apps the user can target. Pure Kotlin (no Compose) so the
 * accessibility service can use [packages] / feed signatures too.
 *
 * Feed-detection fields (heuristic, may need updates as apps change — IDs verified
 * against the open-source Curbox project, kt-rewrite branch):
 *  - [feedViewIds]      lowercase view-id substrings present when the short-form feed is on screen
 *  - [feedDesc]         lowercase content-description substrings indicating the feed (Snap/FB)
 *  - [reelTextIds]      view-id substrings of the caption/author text; a substantial change = a new reel
 *  - [feedIsWholeApp]   the whole app effectively IS the feed (TikTok)
 *  - [notFeedIds]       view-id substrings that, when present, mean we are NOT in the immersive
 *                       feed — e.g. the bottom-nav home tab, which is hidden in the reel viewer.
 *                       Instagram tags inline home-feed reels with the same clips_viewer id as
 *                       the real viewer, so this is what keeps the counter off the home feed.
 *  - [feedSelectedIds]  view-id substrings that count as "on feed" only when that node is the
 *                       selected tab (Instagram's Reels tab), overriding [notFeedIds].
 */
data class KnownApp(
    val key: String,
    val name: String,
    val letter: String,
    val colorHex: Long,
    val fgHex: Long,
    val feed: String? = null,
    val packages: List<String> = emptyList(),
    val feedViewIds: List<String> = emptyList(),
    val feedDesc: List<String> = emptyList(),
    val reelTextIds: List<String> = emptyList(),
    val feedIsWholeApp: Boolean = false,
    val notFeedIds: List<String> = emptyList(),
    val feedSelectedIds: List<String> = emptyList(),
)

object AppCatalog {
    val apps: List<KnownApp> = listOf(
        KnownApp(
            "ig", "Instagram", "Ig", 0xFFE1306C, 0xFFFFFFFF, feed = "Reels",
            packages = listOf("com.instagram.android"),
            feedViewIds = listOf("clips_viewer"),
            reelTextIds = listOf("clips_author", "clips_caption"),
            // Home feed & all normal screens show the bottom nav (feed_tab); the immersive
            // reel viewer hides it. The Reels tab itself is the selected clips_tab.
            notFeedIds = listOf("feed_tab"),
            feedSelectedIds = listOf("clips_tab"),
        ),
        KnownApp(
            "yt", "YouTube", "YT", 0xFFFF0000, 0xFFFFFFFF, feed = "Shorts",
            packages = listOf("com.google.android.youtube", "app.revanced.android.youtube"),
            // reel_progress_bar is the Shorts player's progress bar — present only while a
            // Short is playing, never on the home feed's Shorts shelf (verified against the
            // open-source Shorts-Blocker project). Count Shorts by swipe: the per-Short
            // caption view ids are unreliable across YouTube versions.
            feedViewIds = listOf("reel_progress_bar", "reel_player_page"),
        ),
        KnownApp(
            "tt", "TikTok", "TT", 0xFF0B0B0B, 0xFFFFFFFF, feed = "For You",
            packages = listOf("com.zhiliaoapp.musically", "com.ss.android.ugc.trill", "com.ss.android.ugc.aweme"),
            feedIsWholeApp = true,
        ),
        KnownApp(
            "sc", "Snapchat", "Sn", 0xFFFFFC00, 0xFF0B0B0B, feed = "Spotlight",
            packages = listOf("com.snapchat.android"),
            feedDesc = listOf("spotlight"),
        ),
        KnownApp(
            "fb", "Facebook", "Fb", 0xFF1877F2, 0xFFFFFFFF, feed = "Reels",
            packages = listOf("com.facebook.katana"),
            feedViewIds = listOf("video_home", "reels_viewer"),
            feedDesc = listOf("tap to show video controls"),
        ),
        KnownApp("th", "Threads", "Th", 0xFF0B0B0B, 0xFFFFFFFF, packages = listOf("com.instagram.barcelona")),
        KnownApp("x", "X", "X", 0xFF0B0B0B, 0xFFFFFFFF, packages = listOf("com.twitter.android")),
        KnownApp("rd", "Reddit", "Rd", 0xFFFF4500, 0xFFFFFFFF, packages = listOf("com.reddit.frontpage")),
        KnownApp("pin", "Pinterest", "Pi", 0xFFE60023, 0xFFFFFFFF, packages = listOf("com.pinterest")),
        KnownApp("wa", "WhatsApp", "Wa", 0xFF25D366, 0xFFFFFFFF, packages = listOf("com.whatsapp")),
        KnownApp("tg", "Telegram", "Tg", 0xFF29A9EB, 0xFFFFFFFF, packages = listOf("org.telegram.messenger")),
        KnownApp("ds", "Discord", "Ds", 0xFF5865F2, 0xFFFFFFFF, packages = listOf("com.discord")),
        KnownApp("tw", "Twitch", "Tw", 0xFF9146FF, 0xFFFFFFFF, packages = listOf("tv.twitch.android.app")),
        KnownApp("nf", "Netflix", "Nf", 0xFFE50914, 0xFFFFFFFF, packages = listOf("com.netflix.mediaclient")),
        KnownApp("sp", "Spotify", "Sp", 0xFF1DB954, 0xFFFFFFFF, packages = listOf("com.spotify.music")),
        KnownApp("li", "LinkedIn", "Li", 0xFF0A66C2, 0xFFFFFFFF, packages = listOf("com.linkedin.android")),
    )

    private val byKey = apps.associateBy { it.key }
    private val byPackage: Map<String, KnownApp> =
        apps.flatMap { app -> app.packages.map { it to app } }.toMap()

    val feedKeys: List<String> = apps.filter { it.feed != null }.map { it.key }

    operator fun get(key: String): KnownApp? = byKey[key]
    fun forPackage(pkg: String): KnownApp? = byPackage[pkg]
    fun keyForPackage(pkg: String): String? = byPackage[pkg]?.key
}
