package app.moviematch.presentation.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.moviematch.presentation.theme.SurfaceCardElevated

/**
 * Loading placeholder block.
 *
 * Loading follows one rule from the design system: **content = skeleton, action = spinner**. Every
 * screen waiting for data renders blocks that mirror the real layout (same geometry, radii and
 * spacing) with a soft pulse (`opacity 0.45 -> 1`, 900ms loop), so loading and loaded states are
 * structurally indistinguishable.
 *
 * The pulse is shared by every instance through [rememberInfiniteTransition], which keeps the whole
 * screen in sync instead of each block animating on its own clock.
 */
@Composable
fun Skeleton(
    modifier: Modifier = Modifier,
    width: Dp? = null,
    height: Dp = 16.dp,
    borderRadius: Dp = 8.dp,
    circle: Boolean = false,
) {
    val shape = if (circle) RoundedCornerShape(percent = 50) else RoundedCornerShape(borderRadius)
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha = transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "skeletonAlpha",
    )

    Box(
        modifier = modifier
            .then(if (width != null) Modifier.width(width) else Modifier.fillMaxWidth())
            .height(height)
            .clip(shape)
            .background(SurfaceCardElevated)
            .alpha(alpha.value),
    )
}

/** Circular skeleton for avatars/icon badges. */
@Composable
fun SkeletonCircle(size: Dp, modifier: Modifier = Modifier) {
    Skeleton(modifier = modifier, width = size, height = size, circle = true)
}
