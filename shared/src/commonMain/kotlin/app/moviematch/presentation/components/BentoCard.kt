package app.moviematch.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.moviematch.presentation.theme.Border
import app.moviematch.presentation.theme.Dimens
import app.moviematch.presentation.theme.Surface
import app.moviematch.presentation.theme.SurfaceCardElevated

/**
 * "Bento" container: the base building block of every screen.
 *
 * Mirrors `Card` from the reference design system — 28dp super-ellipse curvature, a solid dark fill
 * and a 1px hairline of `rgba(255,255,255,0.07)` that keeps the block readable against the canvas.
 */
@Composable
fun BentoCard(
    modifier: Modifier = Modifier,
    padding: Dp = Dimens.Spacing24,
    containerColor: Color = Surface,
    borderColor: Color = Border,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(Dimens.RadiusCard))
            .background(containerColor, RoundedCornerShape(Dimens.RadiusCard))
            .border(1.dp, borderColor, RoundedCornerShape(Dimens.RadiusCard))
            .padding(padding),
        content = content,
    )
}

/**
 * Smaller nested block used *inside* a [BentoCard] for stats and list rows (18dp radius).
 *
 * Mirrors `Tile` from the reference design system. It is a plain [androidx.compose.foundation.layout.Box]
 * so callers can stack content vertically (stat tiles) or horizontally (list rows).
 */
@Composable
fun BentoTile(
    modifier: Modifier = Modifier,
    padding: Dp = Dimens.Spacing16,
    containerColor: Color = SurfaceCardElevated,
    borderColor: Color = Border,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(Dimens.RadiusTile))
            .background(containerColor, RoundedCornerShape(Dimens.RadiusTile))
            .border(1.dp, borderColor, RoundedCornerShape(Dimens.RadiusTile))
            .padding(padding),
        content = content,
    )
}
