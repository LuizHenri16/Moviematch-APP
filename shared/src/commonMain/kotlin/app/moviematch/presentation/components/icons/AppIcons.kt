package app.moviematch.presentation.components.icons

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme

/**
 * Hand-drawn, monochrome line icons.
 *
 * The Material 3 icon set of MovieMatch is drawn on the same 24x24 grid as Material Symbols, with
 * ~1.8-unit strokes, round caps and round joins. `material-icons-core` only ships a small, *filled*
 * set, and pulling `material-icons-extended` in for a handful of glyphs would add megabytes to both
 * apps, so the glyphs the cards need are drawn here with `Canvas`.
 *
 * Every icon is authored on a 24x24 grid and scales to whatever size the caller requests, so the
 * visual weight stays constant at any density.
 */
private const val GRID = 24f

/** Maps 24x24 design coordinates to device pixels for the current canvas. */
private fun DrawScope.u() = size.minDimension / GRID

private fun DrawScope.at(x: Float, y: Float): Offset {
    val unit = u()
    return Offset(x * unit, y * unit)
}

private fun DrawScope.stroke(width: Float = 1.8f): Stroke {
    val unit = u()
    return Stroke(width = unit * width, cap = StrokeCap.Round, join = StrokeJoin.Round)
}

/** Chevron pointing left — the "back" affordance in detail screens. */
@Composable
fun IconChevronLeft(modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Canvas(modifier = modifier) {
        val w = u() * 2f
        drawLine(color, at(15f, 5f), at(9f, 12f), w, StrokeCap.Round)
        drawLine(color, at(9f, 12f), at(15f, 19f), w, StrokeCap.Round)
    }
}

/** Chevron pointing right — list rows and "open" affordances. */
@Composable
fun IconChevronRight(modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Canvas(modifier = modifier) {
        val w = u() * 2f
        drawLine(color, at(9f, 5f), at(15f, 12f), w, StrokeCap.Round)
        drawLine(color, at(15f, 12f), at(9f, 19f), w, StrokeCap.Round)
    }
}

/** Clapperboard — "Termômetro de Pipoca" header badge. */
@Composable
fun IconClapperboard(modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Canvas(modifier = modifier) {
        val unit = u()
        val style = stroke()
        drawRoundRect(
            color = color,
            topLeft = at(3f, 9f),
            size = Size(18f * unit, 11f * unit),
            cornerRadius = CornerRadius(2.5f * unit),
            style = style,
        )
        drawRoundRect(
            color = color,
            topLeft = at(3f, 4f),
            size = Size(18f * unit, 5f * unit),
            cornerRadius = CornerRadius(2f * unit),
            style = style,
        )
        listOf(7f, 11f, 15f, 19f).forEach { x ->
            drawLine(color, at(x, 4.5f), at(x - 2f, 8.5f), unit * 1.2f, StrokeCap.Round)
        }
    }
}

/** Flame — "Duelo de Críticos" fun-fact badge. */
@Composable
fun IconFlame(modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Canvas(modifier = modifier) {
        val unit = u()
        val path = Path().apply {
            moveTo(12f * unit, 3f * unit)
            cubicTo(14f * unit, 7f * unit, 18f * unit, 9f * unit, 18f * unit, 14f * unit)
            cubicTo(18f * unit, 17.5f * unit, 15.5f * unit, 21f * unit, 12f * unit, 21f * unit)
            cubicTo(8.5f * unit, 21f * unit, 6f * unit, 17.5f * unit, 6f * unit, 14f * unit)
            cubicTo(6f * unit, 11f * unit, 8f * unit, 10f * unit, 9.5f * unit, 8f * unit)
            cubicTo(10.5f * unit, 6.8f * unit, 10.5f * unit, 5f * unit, 12f * unit, 3f * unit)
            close()
        }
        drawPath(path, color, style = stroke())
    }
}

/** Film strip — movie cards and the poster placeholder glyph. */
@Composable
fun IconFilm(modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Canvas(modifier = modifier) {
        val unit = u()
        drawRoundRect(
            color = color,
            topLeft = at(3f, 4f),
            size = Size(18f * unit, 16f * unit),
            cornerRadius = CornerRadius(2.5f * unit),
            style = stroke(),
        )
        listOf(4.5f, 8.5f, 15.5f, 19.5f).forEach { x ->
            drawLine(color, at(x, 6.5f), at(x, 8.5f), unit * 1.3f, StrokeCap.Round)
            drawLine(color, at(x, 15.5f), at(x, 17.5f), unit * 1.3f, StrokeCap.Round)
        }
    }
}

/** Television — tags series in search results and list rows. */
@Composable
fun IconTv(modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Canvas(modifier = modifier) {
        val unit = u()
        drawRoundRect(
            color = color,
            topLeft = at(3f, 7f),
            size = Size(18f * unit, 13f * unit),
            cornerRadius = CornerRadius(2.5f * unit),
            style = stroke(),
        )
        drawLine(color, at(8f, 3f), at(11f, 7f), unit * 1.8f, StrokeCap.Round)
        drawLine(color, at(16f, 3f), at(13f, 7f), unit * 1.8f, StrokeCap.Round)
    }
}

/** Document — "Sinopse" section header. */
@Composable
fun IconDocument(modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Canvas(modifier = modifier) {
        val unit = u()
        drawRoundRect(
            color = color,
            topLeft = at(5f, 3f),
            size = Size(14f * unit, 18f * unit),
            cornerRadius = CornerRadius(2.5f * unit),
            style = stroke(),
        )
        listOf(7.5f, 11f, 14.5f).forEach { y ->
            drawLine(color, at(8f, y), at(16f, y), unit * 1.6f, StrokeCap.Round)
        }
    }
}

/** Arrow leaving a frame — the logout affordance. */
@Composable
fun IconLogout(modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Canvas(modifier = modifier) {
        val unit = u()
        val frame = Path().apply {
            moveTo(13f * unit, 4f * unit)
            lineTo(6f * unit, 4f * unit)
            cubicTo(5.4f * unit, 4f * unit, 5f * unit, 4.4f * unit, 5f * unit, 5f * unit)
            lineTo(5f * unit, 19f * unit)
            cubicTo(5f * unit, 19.6f * unit, 5.4f * unit, 20f * unit, 6f * unit, 20f * unit)
            lineTo(13f * unit, 20f * unit)
        }
        drawPath(frame, color, style = stroke())
        drawLine(color, at(11f, 12f), at(20f, 12f), unit * 1.8f, StrokeCap.Round)
        drawLine(color, at(17f, 9f), at(20f, 12f), unit * 1.8f, StrokeCap.Round)
        drawLine(color, at(20f, 12f), at(17f, 15f), unit * 1.8f, StrokeCap.Round)
    }
}

/** Smiling face — "comédia/animação" genres. */
@Composable
fun IconSmile(modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Canvas(modifier = modifier) {
        val unit = u()
        val style = stroke()
        drawCircle(color, radius = 9f * unit, center = at(12f, 12f), style = style)
        drawCircle(color, radius = 1.1f * unit, center = at(9f, 10f))
        drawCircle(color, radius = 1.1f * unit, center = at(15f, 10f))
        drawArc(
            color = color,
            startAngle = 25f,
            sweepAngle = 130f,
            useCenter = false,
            topLeft = at(8f, 9f),
            size = Size(8f * unit, 7f * unit),
            style = style,
        )
    }
}

/** Sad face — "terror/suspense" genres. */
@Composable
fun IconFrown(modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Canvas(modifier = modifier) {
        val unit = u()
        val style = stroke()
        drawCircle(color, radius = 9f * unit, center = at(12f, 12f), style = style)
        drawCircle(color, radius = 1.1f * unit, center = at(9f, 10f))
        drawCircle(color, radius = 1.1f * unit, center = at(15f, 10f))
        drawArc(
            color = color,
            startAngle = 205f,
            sweepAngle = 130f,
            useCenter = false,
            topLeft = at(8f, 12f),
            size = Size(8f * unit, 7f * unit),
            style = style,
        )
    }
}

/** Compass — adventure/action/sci-fi genres. */
@Composable
fun IconCompass(modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Canvas(modifier = modifier) {
        val unit = u()
        val style = stroke()
        drawCircle(color, radius = 9f * unit, center = at(12f, 12f), style = style)
        val w = unit * 1.8f
        drawLine(color, at(15.5f, 8.5f), at(10.5f, 10.5f), w, StrokeCap.Round)
        drawLine(color, at(10.5f, 10.5f), at(8.5f, 15.5f), w, StrokeCap.Round)
        drawLine(color, at(8.5f, 15.5f), at(13.5f, 13.5f), w, StrokeCap.Round)
        drawLine(color, at(13.5f, 13.5f), at(15.5f, 8.5f), w, StrokeCap.Round)
    }
}

/** Heart — drama/romance genres. */
@Composable
fun IconHeart(modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Canvas(modifier = modifier) {
        val unit = u()
        val path = Path().apply {
            moveTo(12f * unit, 20f * unit)
            cubicTo(4f * unit, 14.5f * unit, 3.5f * unit, 9f * unit, 7f * unit, 6.5f * unit)
            cubicTo(9.5f * unit, 4.7f * unit, 11.2f * unit, 6.2f * unit, 12f * unit, 8f * unit)
            cubicTo(12.8f * unit, 6.2f * unit, 14.5f * unit, 4.7f * unit, 17f * unit, 6.5f * unit)
            cubicTo(20.5f * unit, 9f * unit, 20f * unit, 14.5f * unit, 12f * unit, 20f * unit)
            close()
        }
        drawPath(path, color, style = stroke())
    }
}

/** Three dots — the "mais opções" affordance in headers. */
@Composable
fun IconMore(modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Canvas(modifier = modifier) {
        listOf(6f, 12f, 18f).forEach { x ->
            drawCircle(color, radius = u() * 1.4f, center = at(x, 12f))
        }
    }
}

/** Ticket — the brand mark of the login screen ("Movie Match"). */
@Composable
fun IconTicket(modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Canvas(modifier = modifier) {
        val unit = u()
        drawRoundRect(
            color = color,
            topLeft = at(2f, 6f),
            size = Size(20f * unit, 12f * unit),
            cornerRadius = CornerRadius(3f * unit),
            style = stroke(),
        )
        // Perforated stub, the dashed line that reads as "ticket".
        listOf(7.6f, 11f, 14.4f).forEach { y ->
            drawLine(color, at(13f, y), at(13f, y + 1.2f), unit * 1.5f, StrokeCap.Round)
        }
    }
}

/** Padlock — the "só vocês dois" badge of the login profile card. */
@Composable
fun IconLock(modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Canvas(modifier = modifier) {
        val unit = u()
        val style = stroke(1.6f)
        drawArc(
            color = color,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = at(7.5f, 4.5f),
            size = Size(9f * unit, 9f * unit),
            style = style,
        )
        drawRoundRect(
            color = color,
            topLeft = at(5f, 10f),
            size = Size(14f * unit, 10f * unit),
            cornerRadius = CornerRadius(2.5f * unit),
            style = style,
        )
    }
}

/** Chain link — the avatar URL field of the settings screen. */
@Composable
fun IconLink(modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Canvas(modifier = modifier) {
        val unit = u()
        val style = stroke(1.6f)
        rotate(degrees = -45f, pivot = center) {
            drawRoundRect(
                color = color,
                topLeft = at(2.5f, 9.2f),
                size = Size(10.5f * unit, 5.6f * unit),
                cornerRadius = CornerRadius(2.8f * unit),
                style = style,
            )
            drawRoundRect(
                color = color,
                topLeft = at(11f, 9.2f),
                size = Size(10.5f * unit, 5.6f * unit),
                cornerRadius = CornerRadius(2.8f * unit),
                style = style,
            )
        }
    }
}

/** Sizes shared by every icon call site. */
object IconSize {
    val Small: Dp = 16.dp
    val Default: Dp = 20.dp
    val Large: Dp = 24.dp
}

/** Ready-to-use modifier: `IconFilm(modifier = Modifier.iconSize(IconSize.Small))`. */
fun Modifier.iconSize(size: Dp = IconSize.Default): Modifier = this.then(Modifier.size(size))
