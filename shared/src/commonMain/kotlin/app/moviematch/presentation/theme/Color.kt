package app.moviematch.presentation.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color

/**
 * Material Design 3 colour system of MovieMatch.
 *
 * The app no longer owns a hand-made "glass" palette: every colour is a **Material 3 role**
 * (`primary`, `onSurface`, `surfaceContainerHigh`, `errorContainer`, …) produced from the brand
 * seed [BrandSeed] with the official Material tonal-palette algorithm. The two schemes below are the
 * static fallback used on every platform; on Android 12+ [dynamicColorScheme] replaces them with the
 * wallpaper-derived palette (Material You).
 *
 * Rules that the whole UI follows:
 * * text and icons always use the `on*` role of the surface they sit on;
 * * borders and dividers use [ColorScheme.outlineVariant]; [ColorScheme.outline] is reserved for
 *   emphasised boundaries (focused fields, outlined buttons);
 * * elevation is expressed with the `surfaceContainer*` tones, not with shadows;
 * * [ColorScheme.error]/`errorContainer` are the only destructive colours.
 */
val BrandSeed = Color(0xFF7DD3FC)

/**
 * Light scheme — seed `#7DD3FC`.
 *
 * Generated with `@material/material-color-utilities` (`SchemeTonalSpot`, the default of the Material
 * Theme Builder). Do not edit a role by hand: regenerate the whole scheme from the seed instead.
 */
internal val MovieMatchLightColorScheme: ColorScheme = lightColorScheme(
    primary = Color(0xFF006686),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFC0E8FF),
    onPrimaryContainer = Color(0xFF001E2B),
    inversePrimary = Color(0xFF6FD2FF),
    secondary = Color(0xFF4D616C),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD0E6F3),
    onSecondaryContainer = Color(0xFF091E27),
    tertiary = Color(0xFF5E5A7D),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFE4DFFF),
    onTertiaryContainer = Color(0xFF1B1736),
    background = Color(0xFFFBFCFE),
    onBackground = Color(0xFF191C1E),
    surface = Color(0xFFFBFCFE),
    onSurface = Color(0xFF191C1E),
    surfaceVariant = Color(0xFFDCE3E9),
    onSurfaceVariant = Color(0xFF40484C),
    surfaceTint = Color(0xFF006686),
    surfaceDim = Color(0xFFD9DADC),
    surfaceBright = Color(0xFFF8F9FB),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF2F4F6),
    surfaceContainer = Color(0xFFEDEEF0),
    surfaceContainerHigh = Color(0xFFE7E8EA),
    surfaceContainerHighest = Color(0xFFE1E2E5),
    inverseSurface = Color(0xFF2E3133),
    inverseOnSurface = Color(0xFFF0F1F3),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    outline = Color(0xFF71787D),
    outlineVariant = Color(0xFFC0C7CD),
    scrim = Color(0xFF000000),
)

/**
 * Dark scheme — same seed.
 *
 * This is the "cinema room" mode of the brand, now expressed with MD3 surface containers instead of
 * translucent glass sheets.
 */
internal val MovieMatchDarkColorScheme: ColorScheme = darkColorScheme(
    primary = Color(0xFF6FD2FF),
    onPrimary = Color(0xFF003547),
    primaryContainer = Color(0xFF004D66),
    onPrimaryContainer = Color(0xFFC0E8FF),
    inversePrimary = Color(0xFF006686),
    secondary = Color(0xFFB4CAD6),
    onSecondary = Color(0xFF1F333D),
    secondaryContainer = Color(0xFF364954),
    onSecondaryContainer = Color(0xFFD0E6F3),
    tertiary = Color(0xFFC7C2EA),
    onTertiary = Color(0xFF302D4C),
    tertiaryContainer = Color(0xFF464364),
    onTertiaryContainer = Color(0xFFE4DFFF),
    background = Color(0xFF191C1E),
    onBackground = Color(0xFFE1E2E5),
    surface = Color(0xFF191C1E),
    onSurface = Color(0xFFE1E2E5),
    surfaceVariant = Color(0xFF40484C),
    onSurfaceVariant = Color(0xFFC0C7CD),
    surfaceTint = Color(0xFF6FD2FF),
    surfaceDim = Color(0xFF111415),
    surfaceBright = Color(0xFF37393B),
    surfaceContainerLowest = Color(0xFF0C0F10),
    surfaceContainerLow = Color(0xFF191C1E),
    surfaceContainer = Color(0xFF1D2022),
    surfaceContainerHigh = Color(0xFF282A2C),
    surfaceContainerHighest = Color(0xFF323537),
    inverseSurface = Color(0xFFE1E2E5),
    inverseOnSurface = Color(0xFF2E3133),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFB4AB),
    outline = Color(0xFF8A9297),
    outlineVariant = Color(0xFF40484C),
    scrim = Color(0xFF000000),
)

/**
 * Container of a profile avatar.
 *
 * The two members of a pair get the two expressive lanes of the scheme (`primaryContainer` and
 * `tertiaryContainer`), which follow light/dark and dynamic colour automatically. Maria keeps the
 * blue lane, everyone else gets the lilac one — the same split the previous palette encoded.
 */
@Composable
fun avatarColor(name: String?): Color = if (name == "Maria") {
    MaterialTheme.colorScheme.primaryContainer
} else {
    MaterialTheme.colorScheme.tertiaryContainer
}

/** Content colour that pairs with [avatarColor]; always an `on*Container` role. */
@Composable
fun avatarContentColor(name: String?): Color = if (name == "Maria") {
    MaterialTheme.colorScheme.onPrimaryContainer
} else {
    MaterialTheme.colorScheme.onTertiaryContainer
}

/** First letter of the display name, uppercased — used when there is no avatar image. */
fun avatarInitial(name: String?): String = (name?.take(1) ?: "?").uppercase()
