package app.moviematch.presentation.components

import androidx.compose.ui.graphics.Color

/**
 * Returns this colour, or the role resolved by [fallback] when it is [Color.Unspecified].
 *
 * Components use `Color.Unspecified` as the default of their semantic colour parameters so the
 * caller can omit them and still get a **theme-aware** MD3 role (a plain `Color` default cannot read
 * `MaterialTheme`).
 */
internal inline fun Color.takeOrElse(fallback: () -> Color): Color =
    if (this == Color.Unspecified) fallback() else this
