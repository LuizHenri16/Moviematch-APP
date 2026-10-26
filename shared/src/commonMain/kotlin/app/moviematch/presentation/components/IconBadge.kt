package app.moviematch.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import app.moviematch.presentation.theme.Dimens

/**
 * Square container for a small icon — the compact badge of the card headers.
 *
 * A MD3 tonal surface (`secondaryContainer` by default) with the small corner. No elevation: it is
 * one tone above the card that owns it.
 */
@Composable
fun IconBadge(
    modifier: Modifier = Modifier,
    size: Dp = Dimens.IconBadgeMedium,
    containerColor: Color = Color.Unspecified,
    contentColor: Color = Color.Unspecified,
    content: @Composable BoxScope.() -> Unit,
) {
    Surface(
        modifier = modifier.size(size),
        shape = MaterialTheme.shapes.small,
        color = containerColor.takeOrElse { MaterialTheme.colorScheme.secondaryContainer },
        contentColor = contentColor.takeOrElse { MaterialTheme.colorScheme.onSecondaryContainer },
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
            content = content,
        )
    }
}

/**
 * Standard MD3 icon button for headers (back, logout, overflow).
 *
 * The 40dp visual target sits inside the component's 48dp minimum touch target. [contentDescription]
 * is required so screen readers announce the action. [overPoster] paints a scrim container so the
 * icon stays legible over artwork.
 */
@Composable
fun HeaderIconButton(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    containerColor: Color = Color.Unspecified,
    size: Dp = Dimens.IconButtonSize,
    overPoster: Boolean = false,
    content: @Composable () -> Unit,
) {
    val resolved = when {
        overPoster -> Color.Black.copy(alpha = 0.55f)
        containerColor != Color.Unspecified -> containerColor
        else -> Color.Transparent
    }

    if (resolved == Color.Transparent) {
        IconButton(
            onClick = onClick,
            modifier = modifier.size(Dimens.TouchTarget),
        ) {
            Box(modifier = Modifier.size(size), contentAlignment = Alignment.Center) {
                content()
            }
        }
    } else {
        Surface(
            onClick = onClick,
            modifier = modifier.size(Dimens.TouchTarget),
            shape = MaterialTheme.shapes.small,
            color = resolved,
            // Over artwork the scrim is always dark, so the ink must be white in both schemes —
            // `onSurface` would be nearly black on the light theme.
            contentColor = if (overPoster) Color.White else MaterialTheme.colorScheme.onSurface,
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Box(modifier = Modifier.size(size), contentAlignment = Alignment.Center) {
                    content()
                }
            }
        }
    }
}
