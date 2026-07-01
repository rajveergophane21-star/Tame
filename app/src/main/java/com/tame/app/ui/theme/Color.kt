package com.tame.app.ui.theme

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color

/**
 * Full token set for one theme. [TameColors] exposes these as live getters so the whole
 * UI re-themes when [TameColors.applyDark] flips the active palette — no call-site changes.
 */
data class TamePalette(
    val surface: Color, val card: Color, val ink: Color, val inkCard: Color,
    val inkDark: Color, val frictionBg: Color, val heroOver: Color,
    val textMuted: Color, val textFaint: Color, val textFaint2: Color,
    val textSoft: Color, val textSoft2: Color, val textHint: Color, val textHint2: Color,
    val navInactive: Color, val iconFaint: Color,
    val onDarkSub: Color, val onDarkSub2: Color, val onTakeoverSub: Color,
    val hairline: Color, val divider: Color, val navBorder: Color, val trackOff: Color,
    val chipBg: Color, val segmentBg: Color, val fieldBg: Color, val rowUnselected: Color,
    val dashedBorder: Color, val grabber: Color,
    val blockRed: Color, val blockTagBg: Color, val frictionTagText: Color, val frictionTagBg: Color,
    val missedRed: Color, val overRed: Color, val todayCell: Color, val cellPre: Color,
    val habitDotOff: Color, val habitCheckOff: Color,
    val commitChipBg: Color, val commitGold: Color, val commitGold2: Color, val commitNoteSub: Color,
    val scrim: Color,
)

/** Light palette — lifted verbatim from the Tame design (Tame.dc.html). */
val LightPalette = TamePalette(
    surface = Color(0xFFF5F3EC), card = Color(0xFFFFFFFF), ink = Color(0xFF19211C), inkCard = Color(0xFF19211C),
    inkDark = Color(0xFF10160F), frictionBg = Color(0xFF11183A), heroOver = Color(0xFF241310),
    textMuted = Color(0xFF6E756C), textFaint = Color(0xFF8A9088), textFaint2 = Color(0xFF9AA098),
    textSoft = Color(0xFF7B827A), textSoft2 = Color(0xFF5B635C), textHint = Color(0xFFA7ADA3), textHint2 = Color(0xFFB4AF9F),
    navInactive = Color(0xFFA0A69D), iconFaint = Color(0xFFC4C0B2),
    onDarkSub = Color(0xFF9FB0A6), onDarkSub2 = Color(0xFF6E7A72), onTakeoverSub = Color(0xFF8FA39A),
    hairline = Color(0xFFECE8DC), divider = Color(0xFFF0ECE0), navBorder = Color(0xFFE9E5DA), trackOff = Color(0xFFD9D5C8),
    chipBg = Color(0xFFEAF1ED), segmentBg = Color(0xFFEDE9DD), fieldBg = Color(0xFFF4F1E8), rowUnselected = Color(0xFFFBFAF5),
    dashedBorder = Color(0xFFDDD8C9), grabber = Color(0xFFD5D1C4),
    blockRed = Color(0xFFC0392B), blockTagBg = Color(0xFFFBEAE6), frictionTagText = Color(0xFF1F7A52), frictionTagBg = Color(0xFFE4F2EA),
    missedRed = Color(0xFFE8978A), overRed = Color(0xFFE1574C), todayCell = Color(0xFFF1EEE2), cellPre = Color(0xFFEDEADE),
    habitDotOff = Color(0xFFEBE7DB), habitCheckOff = Color(0xFFCFCABA),
    commitChipBg = Color(0xFFFBF3DC), commitGold = Color(0xFFB8860B), commitGold2 = Color(0xFFB89020), commitNoteSub = Color(0xFF9C8C58),
    scrim = Color(0x73101A0F),
)

/** Dark palette — warm-neutral dark to keep APE's calm feel (not pure black). */
val DarkPalette = TamePalette(
    surface = Color(0xFF121512), card = Color(0xFF1D211B), ink = Color(0xFFECEFE9), inkCard = Color(0xFF272C24),
    inkDark = Color(0xFF10160F), frictionBg = Color(0xFF11183A), heroOver = Color(0xFF2A140F),
    textMuted = Color(0xFFA9B0A5), textFaint = Color(0xFF959C92), textFaint2 = Color(0xFF9CA298),
    textSoft = Color(0xFFA6ADA1), textSoft2 = Color(0xFFC0C6BB), textHint = Color(0xFF848A80), textHint2 = Color(0xFF8A8E80),
    navInactive = Color(0xFF808A7E), iconFaint = Color(0xFF565C52),
    onDarkSub = Color(0xFF9FB0A6), onDarkSub2 = Color(0xFF7C887F), onTakeoverSub = Color(0xFF8FA39A),
    hairline = Color(0xFF2A2F27), divider = Color(0xFF2A2F27), navBorder = Color(0xFF242920), trackOff = Color(0xFF3A4036),
    chipBg = Color(0xFF25302A), segmentBg = Color(0xFF262B23), fieldBg = Color(0xFF232821), rowUnselected = Color(0xFF1D211B),
    dashedBorder = Color(0xFF3A4036), grabber = Color(0xFF3A4036),
    blockRed = Color(0xFFF08979), blockTagBg = Color(0xFF3A211E), frictionTagText = Color(0xFF6FCB9E), frictionTagBg = Color(0xFF1C2A22),
    missedRed = Color(0xFFB75B52), overRed = Color(0xFFE1574C), todayCell = Color(0xFF2A2F27), cellPre = Color(0xFF20241E),
    habitDotOff = Color(0xFF2A2F27), habitCheckOff = Color(0xFF565C52),
    commitChipBg = Color(0xFF322B16), commitGold = Color(0xFFD8A93A), commitGold2 = Color(0xFFD8B24A), commitNoteSub = Color(0xFFB7A874),
    scrim = Color(0xB3080B08),
)

/**
 * Live theme tokens. Each value is a getter reading a snapshot-state palette, so flipping
 * [applyDark] recomposes every composable that reads a token — no per-screen changes needed.
 * Accent/pop colors are separate — see [AccentPalette] / [LocalAccent].
 */
object TameColors {
    private val palette = mutableStateOf(LightPalette)

    /** Swap the active palette (called from the theme / activity when the setting changes). */
    fun applyDark(dark: Boolean) {
        val target = if (dark) DarkPalette else LightPalette
        if (palette.value !== target) palette.value = target
    }

    // surfaces
    val Surface get() = palette.value.surface
    val Card get() = palette.value.card
    val Ink get() = palette.value.ink            // primary text (flips light in dark mode)
    val InkCard get() = palette.value.inkCard    // dark contrast card bg (stays dark, white text)
    val InkDark get() = palette.value.inkDark    // takeover background
    val FrictionBg get() = palette.value.frictionBg
    val HeroOver get() = palette.value.heroOver

    // text greys
    val TextMuted get() = palette.value.textMuted
    val TextFaint get() = palette.value.textFaint
    val TextFaint2 get() = palette.value.textFaint2
    val TextSoft get() = palette.value.textSoft
    val TextSoft2 get() = palette.value.textSoft2
    val TextHint get() = palette.value.textHint
    val TextHint2 get() = palette.value.textHint2
    val NavInactive get() = palette.value.navInactive
    val IconFaint get() = palette.value.iconFaint
    val OnDarkSub get() = palette.value.onDarkSub
    val OnDarkSub2 get() = palette.value.onDarkSub2
    val OnTakeoverSub get() = palette.value.onTakeoverSub

    // lines / fills
    val Hairline get() = palette.value.hairline
    val Divider get() = palette.value.divider
    val NavBorder get() = palette.value.navBorder
    val TrackOff get() = palette.value.trackOff
    val ChipBg get() = palette.value.chipBg
    val SegmentBg get() = palette.value.segmentBg
    val FieldBg get() = palette.value.fieldBg
    val RowUnselected get() = palette.value.rowUnselected
    val DashedBorder get() = palette.value.dashedBorder
    val Grabber get() = palette.value.grabber

    // semantic
    val BlockRed get() = palette.value.blockRed
    val BlockTagBg get() = palette.value.blockTagBg
    val FrictionTagText get() = palette.value.frictionTagText
    val FrictionTagBg get() = palette.value.frictionTagBg
    val MissedRed get() = palette.value.missedRed
    val OverRed get() = palette.value.overRed
    val TodayCell get() = palette.value.todayCell
    val CellPre get() = palette.value.cellPre      // habit grid: day before the habit existed
    val HabitDotOff get() = palette.value.habitDotOff
    val HabitCheckOff get() = palette.value.habitCheckOff
    val CommitChipBg get() = palette.value.commitChipBg
    val CommitGold get() = palette.value.commitGold
    val CommitGold2 get() = palette.value.commitGold2
    val CommitNoteSub get() = palette.value.commitNoteSub
    val Scrim get() = palette.value.scrim
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
