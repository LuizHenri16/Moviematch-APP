package app.moviematch.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import app.moviematch.presentation.theme.Dimens

/**
 * Small status label used inside card headers ("12 avaliações", "Sintonia boa", scores).
 *
 * A non-interactive MD3 chip: 32dp tall, small (8dp) corner, `secondaryContainer` by default. It is
 * not clickable, so it never carries a state layer or a touch target.
 */
@Composable
fun Pill(
    label: String,
    modifier: Modifier = Modifier,
    containerColor: Color = Color.Unspecified,
    contentColor: Color = Color.Unspecified,
    borderColor: Color = Color.Unspecified,
) {
    Surface(
        modifier = modifier.heightIn(min = Dimens.ChipHeightDefault),
        shape = MaterialTheme.shapes.small,
        color = containerColor.takeOrElse { MaterialTheme.colorScheme.secondaryContainer },
        contentColor = contentColor.takeOrElse { MaterialTheme.colorScheme.onSecondaryContainer },
        border = if (borderColor == Color.Unspecified) {
            null
        } else {
            BorderStroke(Dimens.BorderWidth, borderColor)
        },
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = Dimens.Spacing12, vertical = Dimens.Spacing4),
        )
    }
}
