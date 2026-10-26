package app.moviematch.presentation.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable

/** iOS has no wallpaper-derived palette: the static brand scheme is always used. */
@Composable
actual fun dynamicColorScheme(darkTheme: Boolean): ColorScheme? = null
