package app.moviematch.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import app.moviematch.presentation.components.icons.IconFilm
import app.moviematch.presentation.components.icons.IconSize
import app.moviematch.presentation.components.icons.iconSize
import app.moviematch.presentation.theme.Dimens
import app.moviematch.presentation.theme.avatarColor
import app.moviematch.presentation.theme.avatarContentColor
import app.moviematch.presentation.theme.avatarInitial
import coil3.compose.AsyncImage

/**
 * Circular profile block: the downloaded avatar image when the profile has one, otherwise the
 * coloured initial.
 *
 * The container is a MD3 circle surface in the `primaryContainer` (blue lane) or `tertiaryContainer`
 * (lilac lane) role, with the matching `on*Container` ink — so it follows the light/dark scheme and
 * the wallpaper-derived palette. The initial is painted *underneath* the image and doubles as the
 * loading placeholder and the broken-link fallback.
 */
@Composable
fun Avatar(
    name: String?,
    modifier: Modifier = Modifier,
    url: String? = null,
    size: Dp = Dimens.AvatarMedium,
) {
    Surface(
        modifier = modifier.size(size),
        shape = CircleShape,
        color = avatarColor(name),
        contentColor = avatarContentColor(name),
    ) {
        Box(contentAlignment = Alignment.Center) {
            // Compensate the system font scale so the initial keeps the same visual proportion of
            // the block at 200% text size instead of overflowing it.
            val fontScale = LocalDensity.current.fontScale.coerceAtLeast(1f)
            Text(
                text = avatarInitial(name),
                color = avatarContentColor(name),
                fontWeight = FontWeight.SemiBold,
                fontSize = (size.value * 0.42f / fontScale).sp,
                maxLines = 1,
                softWrap = false,
            )

            if (url != null) {
                var failed by remember(url) { mutableStateOf(false) }
                if (!failed) {
                    AsyncImage(
                        model = url,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        onError = { failed = true },
                    )
                }
            }
        }
    }
}

/**
 * Poster thumbnail.
 *
 * A MD3 medium-shape surface (`surfaceContainerHigh`) with the film glyph underneath the artwork:
 * the glyph is the placeholder while the image loads and the fallback when the link is broken, so
 * the layout never collapses.
 */
@Composable
fun PosterImage(
    url: String?,
    modifier: Modifier = Modifier,
) {
    var failed by remember(url) { mutableStateOf(false) }

    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Box(contentAlignment = Alignment.Center) {
            IconFilm(modifier = Modifier.iconSize(IconSize.Default))

            if (url != null && !failed) {
                AsyncImage(
                    model = url,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    placeholder = null,
                    onError = { failed = true },
                )
            }
        }
    }
}
