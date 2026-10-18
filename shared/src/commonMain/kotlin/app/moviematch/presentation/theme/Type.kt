package app.moviematch.presentation.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * Typographic scale from the "Serene Bento Dark" guide (Plus Jakarta Sans).
 *
 * The font file is not bundled yet, so the platform sans-serif family is used as a stand-in. When
 * `PlusJakartaSans-*.ttf` is added to `composeResources/font`, only this file changes:
 * `FontFamily.SansSerif` below is replaced by the generated `Res.font` family.
 */
private val Family = FontFamily.SansSerif

val MovieMatchTypography = Typography(
    // 28sp / 700 — screen titles ("Home", "Filmes e Séries").
    displaySmall = TextStyle(
        fontFamily = Family,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.02).em,
    ),
    // 24sp / 700 — hero titles inside detail screens.
    headlineMedium = TextStyle(
        fontFamily = Family,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.02).em,
    ),
    // 20sp / 700 — bento card titles.
    headlineSmall = TextStyle(
        fontFamily = Family,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.01).em,
    ),
    // 18sp / 700 — list/card titles.
    titleLarge = TextStyle(
        fontFamily = Family,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
    ),
    // 17sp / 600 — sub-section titles.
    titleMedium = TextStyle(
        fontFamily = Family,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 24.sp,
    ),
    // 15sp / 600 — compact titles (list rows).
    titleSmall = TextStyle(
        fontFamily = Family,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = Family,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = Family,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = Family,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    ),
    // 14sp / 700 — button labels.
    labelLarge = TextStyle(
        fontFamily = Family,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    // 12sp / 600 with positive tracking — chips and pills.
    labelMedium = TextStyle(
        fontFamily = Family,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.02.em,
    ),
    // 11sp / 600 with positive tracking — micro labels ("NA LISTA", "MÉDIA").
    labelSmall = TextStyle(
        fontFamily = Family,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.05.em,
    ),
)

/** 10sp uppercase micro-caption used inside the bento stat tiles. */
val MicroLabelStyle = TextStyle(
    fontFamily = Family,
    fontWeight = FontWeight.Bold,
    fontSize = 10.sp,
    lineHeight = 14.sp,
    letterSpacing = 0.05.em,
)

/** 22sp / 700 — screen titles ("Home", "Filmes e Séries"), mirroring the reference `appTitle`. */
val ScreenTitleStyle = TextStyle(
    fontFamily = Family,
    fontWeight = FontWeight.Bold,
    fontSize = 22.sp,
    lineHeight = 28.sp,
    letterSpacing = (-0.02).em,
)

/** 11sp / 400 — the muted line under a screen title ("Nosso Catálogo"). */
val ScreenSubtitleStyle = TextStyle(
    fontFamily = Family,
    fontWeight = FontWeight.Normal,
    fontSize = 11.sp,
    lineHeight = 14.sp,
    letterSpacing = 0.8.sp,
)

/** 30sp / 800 uppercase — the hero title of the details screen. */
val HeroTitleStyle = TextStyle(
    fontFamily = Family,
    fontWeight = FontWeight.ExtraBold,
    fontSize = 30.sp,
    lineHeight = 36.sp,
    letterSpacing = (-0.5).sp,
)

/** 22sp / 800 — the big number inside a bento stat tile. */
val StatNumberStyle = TextStyle(
    fontFamily = Family,
    fontWeight = FontWeight.ExtraBold,
    fontSize = 22.sp,
    lineHeight = 28.sp,
)
