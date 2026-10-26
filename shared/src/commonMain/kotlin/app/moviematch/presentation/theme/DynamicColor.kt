package app.moviematch.presentation.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable

/**
 * Wallpaper-derived Material You scheme, or `null` when the platform cannot provide one.
 *
 * Implemented on Android (API 31+); iOS has no system-wide dynamic palette and returns `null`, which
 * makes [MovieMatchTheme] use the static brand schemes.
 */
@Composable
expect fun dynamicColorScheme(darkTheme: Boolean): ColorScheme?
