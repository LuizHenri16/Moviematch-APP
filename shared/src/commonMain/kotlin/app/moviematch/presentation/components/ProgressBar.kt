package app.moviematch.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.moviematch.presentation.theme.Dimens

/**
 * Linear progress of the Material 3 specification.
 *
 * `value` stays a percentage (0..100) so callers can pass the aggregates straight through; it is
 * clamped before it reaches the MD3 [LinearProgressIndicator], which takes a 0..1 fraction.
 */
@Composable
fun ProgressBar(
    value: Double,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    trackColor: Color = Color.Unspecified,
    height: Dp = 4.dp,
) {
    val fraction = (value.coerceIn(0.0, 100.0) / 100.0).toFloat()
    LinearProgressIndicator(
        progress = { fraction },
        modifier = modifier
            .fillMaxWidth()
            .height(height),
        color = color.takeOrElse { MaterialTheme.colorScheme.primary },
        trackColor = trackColor.takeOrElse { MaterialTheme.colorScheme.surfaceContainerHighest },
        strokeCap = StrokeCap.Round,
        gapSize = 0.dp,
    )
}

/**
 * Donut gauge used by the "Índice de Sintonia" card.
 *
 * Material 3 has no built-in donut, so this keeps a small Canvas: a round-capped primary arc over the
 * `surfaceContainerHighest` track, with the percentage in the middle.
 */
@Composable
fun DonutRing(
    percentage: Int,
    modifier: Modifier = Modifier,
    size: Dp = 90.dp,
    strokeWidth: Dp = 8.dp,
    color: Color = Color.Unspecified,
    trackColor: Color = Color.Unspecified,
    content: @Composable BoxScope.() -> Unit,
) {
    val arcColor = color.takeOrElse { MaterialTheme.colorScheme.primary }
    val arcTrack = trackColor.takeOrElse { MaterialTheme.colorScheme.surfaceContainerHighest }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val stroke = strokeWidth.toPx()
            val inset = stroke / 2
            val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
            drawArc(
                color = arcTrack,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            drawArc(
                color = arcColor,
                startAngle = -90f,
                sweepAngle = 360f * (percentage.coerceIn(0, 100) / 100f),
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
        content()
    }
}
