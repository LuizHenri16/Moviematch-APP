package app.moviematch.presentation.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

/**
 * MovieMatch is a dark-first, "cinema at night" experience, so the single colour scheme is a dark
 * one — there is intentionally no light scheme.
 */
private val MovieMatchColorScheme = darkColorScheme(
    primary = Accent,
    onPrimary = TextOnAccent,
    primaryContainer = AccentDeep,
    onPrimaryContainer = TextSoft,
    secondary = Lavender,
    onSecondary = TextOnAccent,
    background = Background,
    onBackground = TextPrimary,
    surface = Surface,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceCardElevated,
    onSurfaceVariant = TextSecondary,
    surfaceContainer = SurfaceCard,
    surfaceContainerHigh = SurfaceCardElevated,
    outline = TextMuted,
    outlineVariant = Border,
    error = Danger,
    onError = TextOnAccent,
    errorContainer = DangerSurface,
    onErrorContainer = Danger,
    scrim = SurfaceDeep,
)

private val MovieMatchShapes = Shapes(
    extraSmall = RoundedCornerShape(Dimens.RadiusSmall),
    small = RoundedCornerShape(Dimens.RadiusTile),
    medium = RoundedCornerShape(Dimens.RadiusCard),
    large = RoundedCornerShape(Dimens.RadiusCard),
    extraLarge = RoundedCornerShape(Dimens.RadiusPill),
)

/** Root theme applied to every platform entry point (Android `MainActivity`, iOS `MainViewController`). */
@Composable
fun MovieMatchTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MovieMatchColorScheme,
        typography = MovieMatchTypography,
        shapes = MovieMatchShapes,
        content = content,
    )
}
