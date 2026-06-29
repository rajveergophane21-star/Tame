package com.tame.app.data.model

/**
 * Static catalog of apps the user can target. Pure Kotlin (no Compose) so the
 * accessibility service can use [packages] / [feedSignatures] too.
 *
 * [colorHex]/[fgHex] are ARGB longs (UI converts to Compose Color).
 * [feed] is the short-form feed name (null = not a short-form app).
 * [packages] are the known Android package names.
 * [feedSignatures] are lowercase resource-id / class substrings that indicate the
 * short-form feed is on screen. These are heuristic and may need updates as apps
 * change; treat them as a best-effort starting point.
 */
data class KnownApp(
    val key: String,
    val name: String,
    val letter: String,
    val colorHex: Long,
    val fgHex: Long,
    val feed: String? = null,
    val packages: List<String> = emptyList(),
    val feedSignatures: List<String> = emptyList(),
    /** If true, the whole app effectively IS the short-form feed (e.g. TikTok home). */
    val feedIsWholeApp: Boolean = false,
)

object AppCatalog {
    val apps: List<KnownApp> = listOf(
        KnownApp(
            "ig", "Instagram", "Ig", 0xFFE1306C, 0xFFFFFFFF, feed = "Reels",
            packages = listOf("com.instagram.android"),
            feedSignatures = listOf("clips_viewer", "reel_viewer", "clips_tab", "reels_tray"),
        ),
        KnownApp(
            "yt", "YouTube", "YT", 0xFFFF0000, 0xFFFFFFFF, feed = "Shorts",
            packages = listOf("com.google.android.youtube"),
            feedSignatures = listOf("reel_recycler", "reel_player_page", "shorts_", "reel_watch"),
        ),
        KnownApp(
            "tt", "TikTok", "TT", 0xFF0B0B0B, 0xFFFFFFFF, feed = "For You",
            packages = listOf("com.zhiliaoapp.musically", "com.ss.android.ugc.trill", "com.ss.android.ugc.aweme"),
            feedSignatures = listOf("feed", "for_you", "video_feed"),
            feedIsWholeApp = true,
        ),
        KnownApp(
            "sc", "Snapchat", "Sn", 0xFFFFFC00, 0xFF0B0B0B, feed = "Spotlight",
            packages = listOf("com.snapchat.android"),
            feedSignatures = listOf("spotlight", "discover_feed"),
        ),
        KnownApp(
            "fb", "Facebook", "Fb", 0xFF1877F2, 0xFFFFFFFF, feed = "Reels",
            packages = listOf("com.facebook.katana"),
            feedSignatures = listOf("reels", "video_home", "watch_feed"),
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
