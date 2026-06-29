package com.tame.app.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.tame.app.R

private fun wght(weight: Int) = FontVariation.Settings(FontVariation.weight(weight))

/** Body / UI typeface — Hanken Grotesk (variable). */
val Hanken = FontFamily(
    Font(R.font.hanken_grotesk, FontWeight.Normal, variationSettings = wght(400)),
    Font(R.font.hanken_grotesk, FontWeight.Medium, variationSettings = wght(500)),
    Font(R.font.hanken_grotesk, FontWeight.SemiBold, variationSettings = wght(600)),
    Font(R.font.hanken_grotesk, FontWeight.Bold, variationSettings = wght(700)),
    Font(R.font.hanken_grotesk, FontWeight.ExtraBold, variationSettings = wght(800)),
)

/** Display / headline typeface — Bricolage Grotesque (variable). */
val Bricolage = FontFamily(
    Font(R.font.bricolage_grotesque, FontWeight.SemiBold, variationSettings = wght(600)),
    Font(R.font.bricolage_grotesque, FontWeight.Bold, variationSettings = wght(700)),
    Font(R.font.bricolage_grotesque, FontWeight.ExtraBold, variationSettings = wght(800)),
)
