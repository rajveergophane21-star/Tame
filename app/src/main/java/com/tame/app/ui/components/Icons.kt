package com.tame.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp

/** Named icons, transcribed from the `<svg>` paths in the Tame design. */
object TameIcons {
    @Composable fun ChevronLeft(size: Dp, tint: Color, modifier: Modifier = Modifier, sw: Float = 2.4f) =
        SvgIcon(listOf(VPath("M15 5l-7 7 7 7", sw = sw)), size, tint, modifier)

    @Composable fun ChevronRight(size: Dp, tint: Color, modifier: Modifier = Modifier, sw: Float = 2.4f) =
        SvgIcon(listOf(VPath("M9 6l6 6-6 6", sw = sw)), size, tint, modifier)

    @Composable fun ArrowRight(size: Dp, tint: Color, modifier: Modifier = Modifier, sw: Float = 2.6f) =
        SvgIcon(listOf(VPath("M5 12h14M13 6l6 6-6 6", sw = sw)), size, tint, modifier)

    @Composable fun Plus(size: Dp, tint: Color, modifier: Modifier = Modifier, sw: Float = 3f) =
        SvgIcon(listOf(VPath("M12 5v14M5 12h14", sw = sw)), size, tint, modifier)

    @Composable fun Check(size: Dp, tint: Color, modifier: Modifier = Modifier, sw: Float = 3f) =
        SvgIcon(listOf(VPath("M20 6L9 17l-5-5", sw = sw)), size, tint, modifier)

    @Composable fun Bolt(size: Dp, tint: Color, modifier: Modifier = Modifier) =
        SvgIcon(listOf(VPath("M13.5 2L6 13h5l-1.5 9L18 9h-5z", fill = true)), size, tint, modifier)

    @Composable fun Clock(size: Dp, tint: Color, modifier: Modifier = Modifier, sw: Float = 2.2f) =
        SvgIcon(listOf(VCircle(12f, 12f, 9f, sw = sw), VPath("M12 7v5l3.5 2", sw = sw)), size, tint, modifier)

    @Composable fun Shield(size: Dp, tint: Color, modifier: Modifier = Modifier, sw: Float = 2f) =
        SvgIcon(listOf(VPath("M12 3l7 3v6c0 4.4-3 7.6-7 9-4-1.4-7-4.6-7-9V6z", sw = sw)), size, tint, modifier)

    @Composable fun ShieldCheck(size: Dp, tint: Color, modifier: Modifier = Modifier, sw: Float = 2.2f) =
        SvgIcon(listOf(VPath("M12 3l7 3v6c0 4.4-3 7.6-7 9-4-1.4-7-4.6-7-9V6z", sw = sw), VPath("M9 12l2 2 4-4", sw = sw)), size, tint, modifier)

    @Composable fun Lock(size: Dp, tint: Color, modifier: Modifier = Modifier, sw: Float = 2.2f) =
        SvgIcon(listOf(VRect(5f, 11f, 14f, 9f, rx = 2f, sw = sw), VPath("M8 11V8a4 4 0 018 0v3", sw = sw)), size, tint, modifier)

    @Composable fun Eye(size: Dp, tint: Color, modifier: Modifier = Modifier, sw: Float = 2.2f) =
        SvgIcon(listOf(VPath("M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7-10-7-10-7z", sw = sw), VCircle(12f, 12f, 3f, sw = sw)), size, tint, modifier)

    @Composable fun Pencil(size: Dp, tint: Color, modifier: Modifier = Modifier, sw: Float = 2.2f) =
        SvgIcon(listOf(VPath("M14 4l6 6M3 21l1-5L16 4l4 4L8 20l-5 1z", sw = sw)), size, tint, modifier)

    @Composable fun Bell(size: Dp, tint: Color, modifier: Modifier = Modifier, sw: Float = 2f) =
        SvgIcon(listOf(VPath("M18 8a6 6 0 10-12 0c0 7-3 9-3 9h18s-3-2-3-9", sw = sw), VPath("M13.7 21a2 2 0 01-3.4 0", sw = sw)), size, tint, modifier)

    @Composable fun Search(size: Dp, tint: Color, modifier: Modifier = Modifier, sw: Float = 2f) =
        SvgIcon(listOf(VCircle(11f, 11f, 7f, sw = sw), VPath("M21 21l-3.5-3.5", sw = sw)), size, tint, modifier)

    @Composable fun Trash(size: Dp, tint: Color, modifier: Modifier = Modifier, sw: Float = 2.2f) =
        SvgIcon(listOf(VPath("M4 7h16M9 7V5a2 2 0 012-2h2a2 2 0 012 2v2M6 7l1 13a2 2 0 002 2h6a2 2 0 002-2l1-13", sw = sw)), size, tint, modifier)

    @Composable fun Replay(size: Dp, tint: Color, modifier: Modifier = Modifier, sw: Float = 2f) =
        SvgIcon(listOf(VPath("M3 12a9 9 0 109-9 9 9 0 00-7 3.3M3 3v4h4", sw = sw)), size, tint, modifier)

    @Composable fun Accessibility(size: Dp, tint: Color, modifier: Modifier = Modifier, sw: Float = 2f) =
        SvgIcon(listOf(VCircle(12f, 5f, 2.3f, sw = sw), VPath("M5 9h14M12 9v5m0 0l-3 6m3-6l3 6", sw = sw)), size, tint, modifier)

    @Composable fun Overlay(size: Dp, tint: Color, modifier: Modifier = Modifier, sw: Float = 2f) =
        SvgIcon(listOf(VRect(3f, 4f, 18f, 14f, rx = 2f, sw = sw), VRect(13f, 12f, 7f, 7f, rx = 1.5f, fill = true)), size, tint, modifier)

    @Composable fun NavHome(size: Dp, tint: Color, modifier: Modifier = Modifier, sw: Float = 2f) =
        SvgIcon(listOf(VPath("M3 10.5L12 3l9 7.5M5 9v11h5v-6h4v6h5V9", sw = sw)), size, tint, modifier)

    @Composable fun NavHabits(size: Dp, tint: Color, modifier: Modifier = Modifier, sw: Float = 2f) =
        SvgIcon(listOf(VRect(3f, 4f, 18f, 17f, rx = 3f, sw = sw), VPath("M3 9h18M8 2v4M16 2v4M8.5 14.5l2 2 3.5-4", sw = sw)), size, tint, modifier)

    @Composable fun NavYou(size: Dp, tint: Color, modifier: Modifier = Modifier, sw: Float = 2f) =
        SvgIcon(listOf(VCircle(12f, 9f, 3.5f, sw = sw), VPath("M5 20c0-3.5 3-6 7-6s7 2.5 7 6", sw = sw)), size, tint, modifier)

    @Composable fun Play(size: Dp, tint: Color, modifier: Modifier = Modifier) =
        SvgIcon(listOf(VPath("M8 5v14l11-7z", fill = true)), size, tint, modifier)

    @Composable fun Heart(size: Dp, tint: Color, modifier: Modifier = Modifier) =
        SvgIcon(listOf(VPath("M12 21s-7-4.5-9.5-9C1 9 2.5 5.5 6 5.5c2 0 3 1 4 2.5 1-1.5 2-2.5 4-2.5 3.5 0 5 3.5 3.5 6.5C19 16.5 12 21 12 21z", fill = true)), size, tint, modifier)

    @Composable fun Comment(size: Dp, tint: Color, modifier: Modifier = Modifier) =
        SvgIcon(listOf(VPath("M21 11.5a8.4 8.4 0 01-12 7.6L3 21l1.9-6A8.4 8.4 0 1121 11.5z", fill = true)), size, tint, modifier)

    @Composable fun Share(size: Dp, tint: Color, modifier: Modifier = Modifier, sw: Float = 2f) =
        SvgIcon(listOf(VPath("M4 12v7a1 1 0 001 1h14a1 1 0 001-1v-7M16 6l-4-4-4 4M12 2v13", sw = sw)), size, tint, modifier)

    @Composable fun Close(size: Dp, tint: Color, modifier: Modifier = Modifier, sw: Float = 2.4f) =
        SvgIcon(listOf(VPath("M6 6l12 12M18 6L6 18", sw = sw)), size, tint, modifier)
}
