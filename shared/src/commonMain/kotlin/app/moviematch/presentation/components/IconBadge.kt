package app.moviematch.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import app.moviematch.presentation.theme.Dimens
import app.moviematch.presentation.theme.SurfaceCardElevated

/**
 * Circular container for a small icon — the "floating disc node" of the bento vocabulary.
 * Mirrors `IconBadge` from the reference design system.
 */
@Composable
fun IconBadge(
    modifier: Modifier = Modifier,
    size: Dp = Dimens.IconBadgeMedium,
    containerColor: Color = SurfaceCardElevated,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(containerColor, CircleShape),
        contentAlignment = Alignment.Center,
        content = content,
    )
}
