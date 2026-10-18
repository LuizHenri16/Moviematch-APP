package app.moviematch.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
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
 * Round icon button used in screen headers (back, logout, more).
 *
 * Mirrors the reference `headerIconButton` (38dp disc on `surfaceRaised`); the reference size is
 * rounded up to the shared [Dimens.IconButtonSize] token so every header across the app aligns.
 */
@Composable
fun HeaderIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = SurfaceCardElevated,
    size: Dp = Dimens.IconButtonSize,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(containerColor, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
        content = { content() },
    )
}
