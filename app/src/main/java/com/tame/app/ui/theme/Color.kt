package com.tame.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Fixed palette lifted verbatim from the Tame design (Tame.dc.html).
 * Accent/pop colors are themeable — see [AccentPalette] / [LocalAccent].
 */
object TameColors {
    // surfaces
    val Surface = Color(0xFFF5F3EC)        // app background
    val Card = Color(0xFFFFFFFF)
    val Ink = Color(0xFF19211C)            // primary text / dark cards
    val InkDark = Color(0xFF10160F)        // takeover background
    val FrictionBg = Color(0xFF11183A)     // friction takeover (blue night)
    val HeroOver = Color(0xFF241310)       // hero card when over limit

    // text greys
    val TextMuted = Color(0xFF6E756C)
    val TextFaint = Color(0xFF8A9088)
    val TextFaint2 = Color(0xFF9AA098)
    val TextSoft = Color(0xFF7B827A)
    val TextSoft2 = Color(0xFF5B635C)
    val TextHint = Color(0xFFA7ADA3)
    val TextHint2 = Color(0xFFB4AF9F)
    val NavInactive = Color(0xFFA0A69D)
    val IconFaint = Color(0xFFC4C0B2)
    val OnDarkSub = Color(0xFF9FB0A6)      // muted text on dark cards
    val OnDarkSub2 = Color(0xFF6E7A72)
    val OnTakeoverSub = Color(0xFF8FA39A)  // muted text on block takeover

    // lines / fills
    val Hairline = Color(0xFFECE8DC)       // 1px card edge shadow
    val Divider = Color(0xFFF0ECE0)
    val NavBorder = Color(0xFFE9E5DA)
    val TrackOff = Color(0xFFD9D5C8)
    val ChipBg = Color(0xFFEAF1ED)         // icon chip (accent tint)
    val SegmentBg = Color(0xFFEDE9DD)
    val FieldBg = Color(0xFFF4F1E8)        // steppers, day chips off, empty frank bg
    val RowUnselected = Color(0xFFFBFAF5)
    val DashedBorder = Color(0xFFDDD8C9)
    val Grabber = Color(0xFFD5D1C4)

    // semantic
    val BlockRed = Color(0xFFC0392B)
    val BlockTagBg = Color(0xFFFBEAE6)
    val FrictionTagText = Color(0xFF1F7A52)
    val FrictionTagBg = Color(0xFFE4F2EA)
    val MissedRed = Color(0xFFE8978A)
    val OverRed = Color(0xFFE1574C)
    val TodayCell = Color(0xFFF1EEE2)
    val HabitDotOff = Color(0xFFEBE7DB)
    val HabitCheckOff = Color(0xFFCFCABA)
    val CommitChipBg = Color(0xFFFBF3DC)
    val CommitGold = Color(0xFFB8860B)
    val CommitGold2 = Color(0xFFB89020)
    val CommitNoteSub = Color(0xFF9C8C58)
    val Scrim = Color(0x73101A0F)          // rgba(16,22,15,.45)
}

/** Themeable accent pair. Default is "Grove". */
data class AccentPalette(
    val key: String,
    val name: String,
    val primary: Color,
    val pop: Color,
)

object Accents {
    val Grove = AccentPalette("grove", "Grove", Color(0xFF2F7A5A), Color(0xFFF2B705))
    val Plum = AccentPalette("plum", "Plum", Color(0xFF6D4FB0), Color(0xFF9BD17B))
    val Ocean = AccentPalette("ocean", "Ocean", Color(0xFF1F7A8C), Color(0xFFF2718C))
    val all = listOf(Grove, Plum, Ocean)
    fun byKey(key: String?): AccentPalette = all.firstOrNull { it.key == key } ?: Grove
}
