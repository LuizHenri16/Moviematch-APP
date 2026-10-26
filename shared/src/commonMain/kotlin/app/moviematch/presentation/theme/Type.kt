package app.moviematch.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em

/**
 * Material 3 type scale.
 *
 * [MovieMatchTypography] is the stock MD3 scale (`Roboto` on Android, the system sans on iOS): five
 * display/headline sizes, three title sizes, three body sizes and three label sizes. Nothing here is
 * brand-specific on purpose — hierarchy comes from choosing the right role, not from bespoke sizes.
 *
 * The named aliases below map the vocabulary the screens already use onto MD3 roles so a screen says
 * *what* a text is ("card title", "micro label") instead of repeating a size. They are composable
 * getters, so they always follow the active [MaterialTheme.typography].
 */
val MovieMatchTypography = Typography()

/** 11sp / 16sp, medium, wide tracking — short uppercase captions ("NA LISTA"). */
val MicroLabelStyle: TextStyle
    @Composable get() = MaterialTheme.typography.labelSmall.copy(
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.08.em,
    )

/** Screen title of a top app bar — `headlineSmall` (24sp). */
val ScreenTitleStyle: TextStyle
    @Composable get() = MaterialTheme.typography.headlineSmall

/** Supporting line under a screen title — `bodyMedium`. */
val ScreenSubtitleStyle: TextStyle
    @Composable get() = MaterialTheme.typography.bodyMedium

/** Title of a details hero — `headlineLarge` (32sp). */
val HeroTitleStyle: TextStyle
    @Composable get() = MaterialTheme.typography.headlineLarge

/** Big number inside a stat tile — `displaySmall` (36sp). */
val StatNumberStyle: TextStyle
    @Composable get() = MaterialTheme.typography.displaySmall

/** Title of a content card — `titleLarge` (22sp). */
val CardTitleStyle: TextStyle
    @Composable get() = MaterialTheme.typography.titleLarge

/** Meta line under a card title — `bodySmall` (12sp). */
val CardMetaStyle: TextStyle
    @Composable get() = MaterialTheme.typography.bodySmall

/** Title of a dialog — `headlineSmall` (24sp). */
val DialogTitleStyle: TextStyle
    @Composable get() = MaterialTheme.typography.headlineSmall

/** Body copy inside dialogs and compact cards — `bodyMedium` (14sp). */
val DialogBodyStyle: TextStyle
    @Composable get() = MaterialTheme.typography.bodyMedium

/** Numeric hero (invite code) — `displaySmall` with tabular tracking. */
val NumericHeroStyle: TextStyle
    @Composable get() = MaterialTheme.typography.displaySmall.copy(
        letterSpacing = 0.08.em,
    )
