package com.tame.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/** Currently selected accent palette, provided by [TameTheme]. */
val LocalAccent = compositionLocalOf { Accents.Grove }

/** Convenience accessors used throughout the UI. */
val accent: AccentPalette
    @Composable @ReadOnlyComposable
    get() = LocalAccent.current

@Composable
fun TameTheme(
    accent: AccentPalette = Accents.Grove,
    content: @Composable () -> Unit,
) {
    // Built inside the composable so the default text colour tracks the live palette (dark mode).
    val baseText = TextStyle(
        fontFamily = Hanken,
        fontWeight = FontWeight.Normal,
        color = TameColors.Ink,
        letterSpacing = (-0.011).em,
    )
    val typography = Typography(
        bodyLarge = baseText.copy(fontSize = 16.sp),
        bodyMedium = baseText.copy(fontSize = 14.sp),
        bodySmall = baseText.copy(fontSize = 12.sp),
        labelLarge = baseText.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
    )
    val scheme = lightColorScheme(
        primary = accent.primary,
        background = TameColors.Surface,
        surface = TameColors.Surface,
        onBackground = TameColors.Ink,
        onSurface = TameColors.Ink,
    )
    CompositionLocalProvider(LocalAccent provides accent) {
        MaterialTheme(
            colorScheme = scheme,
            typography = typography,
            content = content,
        )
    }
}
