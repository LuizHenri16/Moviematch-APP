package app.moviematch.presentation.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.moviematch.presentation.theme.Dimens
import app.moviematch.presentation.theme.roundedShape

/**
 * Loading placeholder block.
 *
 * The rule is still **content = skeleton, action = spinner**: a screen waiting for data renders
 * blocks that mirror the real layout (same geometry, radii and spacing) with a soft pulse
 * (`opacity 0.35 -> 0.65`, 900ms loop), so loading and loaded states are structurally
 * indistinguishable. The fill is the MD3 `surfaceContainerHighest` role.
 *
 * The pulse is shared by every instance through [rememberInfiniteTransition], which keeps the whole
 * screen in sync instead of each block animating on its own clock.
 */
@Composable
fun Skeleton(
    modifier: Modifier = Modifier,
    width: Dp? = null,
    height: Dp? = 16.dp,
    borderRadius: Dp = Dimens.SkeletonRadius,
) {
    val shape = roundedShape(borderRadius)
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha = transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "skeletonAlpha",
    )

    Box(
        modifier = modifier
            .then(if (width != null) Modifier.width(width) else Modifier.fillMaxWidth())
            .then(if (height != null) Modifier.height(height) else Modifier.fillMaxHeight())
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .alpha(alpha.value),
    )
}

/**
 * Square skeleton for avatars and icon badges.
 *
 * Pass [circular] when it stands in for an [Avatar] (and the donut gauge); otherwise it keeps the
 * small corner of an [IconBadge].
 */
@Composable
fun SkeletonSquare(
    size: Dp,
    modifier: Modifier = Modifier,
    circular: Boolean = false,
) {
    if (circular) {
        Box(
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                .alpha(
                    rememberInfiniteTransition(label = "skeletonCircle").animateFloat(
                        initialValue = 0.35f,
                        targetValue = 0.65f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 900),
                            repeatMode = RepeatMode.Reverse,
                        ),
                        label = "skeletonCircleAlpha",
                    ).value,
                ),
        )
    } else {
        Skeleton(modifier = modifier, width = size, height = size, borderRadius = Dimens.RadiusSmall)
    }
}
