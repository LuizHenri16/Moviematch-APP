package app.moviematch.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp

/**
 * Material 3 shape scale.
 *
 * The five slots mirror the MD3 spec (4 / 8 / 12 / 16 / 28dp). Components choose a slot through
 * `MaterialTheme.shapes` instead of building a `RoundedCornerShape` from a raw `Dp`.
 */
private val MovieMatchShapes = Shapes(
    extraSmall = RoundedCornerShape(Dimens.RadiusMicro),
    small = RoundedCornerShape(Dimens.RadiusSmall),
    medium = RoundedCornerShape(Dimens.RadiusTile),
    large = RoundedCornerShape(Dimens.RadiusCard),
    extraLarge = RoundedCornerShape(Dimens.DialogRadius),
)

/**
 * Builds a rounded shape from a caller-provided radius (parameterised skeletons).
 *
 * Kept in the theme so component files never instantiate a shape from a raw `Dp`.
 */
fun roundedShape(radius: Dp): Shape = RoundedCornerShape(radius)

/**
 * Root theme applied to every platform entry point (Android `MainActivity`, iOS `MainViewController`).
 *
 * It is a **stock Material 3 theme** with three inputs:
 * * [darkTheme] follows the system by default (`isSystemInDarkTheme()`), so the app has a real light
 *   mode instead of a pinned dark identity;
 * * [dynamicColor] enables **Material You**: on Android 12+ the scheme is derived from the wallpaper
 *   by [dynamicColorScheme]; every other platform (and older Android) falls back to the static
 *   [MovieMatchLightColorScheme] / [MovieMatchDarkColorScheme] generated from [BrandSeed];
 * * [content] is the app UI.
 *
 * ```kotlin
 * MovieMatchTheme { AppContent() }
 * ```
 */
@Composable
fun MovieMatchTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme: ColorScheme = when {
        dynamicColor -> dynamicColorScheme(darkTheme)
            ?: if (darkTheme) MovieMatchDarkColorScheme else MovieMatchLightColorScheme

        darkTheme -> MovieMatchDarkColorScheme
        else -> MovieMatchLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MovieMatchTypography,
        shapes = MovieMatchShapes,
        content = content,
    )
}
