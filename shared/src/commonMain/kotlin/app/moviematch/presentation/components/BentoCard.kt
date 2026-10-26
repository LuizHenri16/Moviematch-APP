package app.moviematch.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import app.moviematch.presentation.theme.Dimens

/**
 * Base content container: a Material 3 [Card].
 *
 * MD3 does not stack translucent sheets: a card is a filled surface one step above the canvas
 * (`surfaceContainerLow` over `surface`), with the large (16dp) corner shape. Depth is tonal, so the
 * default elevation is `0.dp`; only dialogs and menus float.
 *
 * @param containerColor override for semantic cards (error containers, hero blocks). Leave
 *   [Color.Unspecified] to use `surfaceContainerLow`.
 * @param accentGlow kept for source compatibility with the previous identity; it is a no-op because
 *   MD3 forbids decorative gradients on surfaces. Use a semantic container colour instead.
 */
@Composable
fun BentoCard(
    modifier: Modifier = Modifier,
    padding: Dp = Dimens.Spacing24,
    containerColor: Color = Color.Unspecified,
    elevation: Dp = Dimens.ElevationCard,
    @Suppress("UNUSED_PARAMETER") accentGlow: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = if (containerColor == Color.Unspecified) {
                MaterialTheme.colorScheme.surfaceContainerLow
            } else {
                containerColor
            },
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding),
            content = content,
        )
    }
}

/**
 * Nested block used *inside* a [BentoCard] for stats and list rows.
 *
 * A [Surface] with the medium (12dp) shape and `surfaceContainerHighest`, one step above the card.
 * It never carries elevation: only one surface tone should separate it from its parent.
 */
@Composable
fun BentoTile(
    modifier: Modifier = Modifier,
    padding: Dp = Dimens.Spacing16,
    containerColor: Color = Color.Unspecified,
    borderColor: Color = Color.Unspecified,
    content: @Composable BoxScope.() -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = if (containerColor == Color.Unspecified) {
            MaterialTheme.colorScheme.surfaceContainerHighest
        } else {
            containerColor
        },
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = if (borderColor == Color.Unspecified) {
            null
        } else {
            androidx.compose.foundation.BorderStroke(Dimens.BorderWidth, borderColor)
        },
    ) {
        Box(modifier = Modifier.padding(padding), content = content)
    }
}
