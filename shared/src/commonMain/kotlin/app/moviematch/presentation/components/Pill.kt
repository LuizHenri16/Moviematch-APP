package app.moviematch.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.moviematch.presentation.theme.Border
import app.moviematch.presentation.theme.Dimens
import app.moviematch.presentation.theme.SurfaceCardElevated
import app.moviematch.presentation.theme.TextSecondary

/**
 * Small status chip used inside bento card headers ("12 avaliações", "Sintonia Boa", scores).
 * Mirrors `Pill` from the reference design system.
 */
@Composable
fun Pill(
    label: String,
    modifier: Modifier = Modifier,
    containerColor: Color = SurfaceCardElevated,
    contentColor: Color = TextSecondary,
    borderColor: Color = Border,
) {
    val shape = RoundedCornerShape(Dimens.RadiusTile)
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = contentColor,
        modifier = modifier
            .clip(shape)
            .background(containerColor, shape)
            .border(1.dp, borderColor, shape)
            .padding(horizontal = Dimens.Spacing12, vertical = 6.dp),
    )
}
