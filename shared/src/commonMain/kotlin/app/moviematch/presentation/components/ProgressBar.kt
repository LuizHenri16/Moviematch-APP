package app.moviematch.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.moviematch.presentation.theme.Accent
import app.moviematch.presentation.theme.SurfaceCardElevated

/**
 * Horizontal fill bar. `value` is expressed in percent (0..100) like the reference component and is
 * clamped, so callers can pass raw percentages straight from the aggregations.
 */
@Composable
fun ProgressBar(
    value: Double,
    modifier: Modifier = Modifier,
    color: Color = Accent,
    trackColor: Color = SurfaceCardElevated,
    height: Dp = 6.dp,
) {
    val clamped = value.coerceIn(0.0, 100.0)
    val shape = RoundedCornerShape(height / 2)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(shape)
            .background(trackColor, shape),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth((clamped / 100.0).toFloat())
                .fillMaxHeight()
                .background(color, shape),
        )
    }
}

/**
 * Donut gauge used by the "Índice de Sintonia" card.
 *
 * The reference draws a static ring; drawing the sweep proportional to the value keeps the same look
 * while carrying real information.
 */
@Composable
fun DonutRing(
    percentage: Int,
    modifier: Modifier = Modifier,
    size: Dp = 90.dp,
    strokeWidth: Dp = 5.dp,
    color: Color = Accent,
    trackColor: Color = SurfaceCardElevated,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val stroke = strokeWidth.toPx()
            val inset = stroke / 2
            val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            drawArc(
                color = color,
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
