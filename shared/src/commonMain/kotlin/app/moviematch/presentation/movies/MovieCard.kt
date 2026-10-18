package app.moviematch.presentation.movies

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.moviematch.domain.model.ContentType
import app.moviematch.presentation.common.formatDecimals1
import app.moviematch.presentation.components.Avatar
import app.moviematch.presentation.components.BentoCard
import app.moviematch.presentation.components.PosterImage
import app.moviematch.presentation.components.icons.IconFilm
import app.moviematch.presentation.components.icons.IconTv
import app.moviematch.presentation.components.icons.iconSize
import app.moviematch.presentation.theme.Accent
import app.moviematch.presentation.theme.AccentDark
import app.moviematch.presentation.theme.Amber
import app.moviematch.presentation.theme.Border
import app.moviematch.presentation.theme.CategoryBadgeBorder
import app.moviematch.presentation.theme.CategoryBadgeSurface
import app.moviematch.presentation.theme.Dimens
import app.moviematch.presentation.theme.Lavender
import app.moviematch.presentation.theme.LavenderBorder
import app.moviematch.presentation.theme.LavenderSurface
import app.moviematch.presentation.theme.Outline
import app.moviematch.presentation.theme.ScoreBad
import app.moviematch.presentation.theme.ScoreBadBorder
import app.moviematch.presentation.theme.ScoreBadSurface
import app.moviematch.presentation.theme.ScoreGoodBorder
import app.moviematch.presentation.theme.ScoreGoodSurface
import app.moviematch.presentation.theme.ScoreGreatBorder
import app.moviematch.presentation.theme.ScoreGreatSurface
import app.moviematch.presentation.theme.Surface
import app.moviematch.presentation.theme.SurfaceDeep
import app.moviematch.presentation.theme.TextMuted
import app.moviematch.presentation.theme.TextPrimary
import app.moviematch.presentation.theme.TextSecondary

/**
 * Watch-list row.
 *
 * Ported 1:1 from the reference `MovieCard`: poster with a category badge, title/metadata/genre
 * chips, the "assistido" toggle, and the ratings bar (Maria • média • Luiz) tinted by score band.
 */
@Composable
fun MovieCard(
    entry: MovieEntry,
    onClick: () -> Unit,
    onToggleWatched: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val summary = entry.summary
    val metaLine = listOfNotNull(summary.year?.toString(), summary.duration).joinToString(" • ")
    val averageTheme = summary.averageRating?.let(::averageThemeFor)
    val averageRating = summary.averageRating

    BentoCard(
        modifier = modifier.clickable(onClick = onClick),
        padding = Dimens.Spacing16,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
        ) {
            PosterWithCategoryBadge(url = summary.posterUrl, badge = summary.badgeCategory)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 14.dp, end = Dimens.Spacing8),
            ) {
                Text(
                    text = summary.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = metaLine,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(Dimens.Spacing8))
                GenreAndTypeChips(type = entry.type, genres = summary.genres)
            }

            WatchedToggle(watched = summary.isWatched, onToggle = onToggleWatched)
        }

        Spacer(Modifier.height(14.dp))

        RatingsBar(
            mariaRating = summary.mariaRating,
            luizRating = summary.luizRating,
            averageRating = averageRating,
            averageTheme = averageTheme,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GenreAndTypeChips(type: ContentType, genres: List<String>) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing8),
        verticalArrangement = Arrangement.spacedBy(Dimens.Spacing8),
    ) {
        MediaTypeChip(type = type)
        genres.forEach { genre -> GenreChip(genre) }
    }
}

@Composable
private fun PosterWithCategoryBadge(url: String?, badge: String) {
    Box(
        modifier = Modifier.size(
            width = Dimens.PosterWidth,
            height = Dimens.PosterHeight,
        ),
    ) {
        PosterImage(
            url = url,
            modifier = Modifier.fillMaxSize(),
        )

        if (badge.isNotBlank()) {
            val shape = RoundedCornerShape(12.dp)
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(top = 6.dp, start = 6.dp, end = 6.dp),
            ) {
                Text(
                    text = badge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shape)
                        .background(CategoryBadgeSurface, shape)
                        .border(1.dp, CategoryBadgeBorder, shape)
                        .padding(vertical = 3.dp, horizontal = 6.dp),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    color = Lavender,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun WatchedToggle(watched: Boolean, onToggle: () -> Unit) {
    val shape = RoundedCornerShape(Dimens.RadiusTile)
    Box(
        modifier = Modifier
            .size(Dimens.IconBadgeMedium)
            .clip(shape)
            .then(
                if (watched) {
                    Modifier.background(Accent, shape)
                } else {
                    Modifier.border(2.dp, Outline, shape)
                },
            )
            .clickable(onClick = onToggle),
        contentAlignment = Alignment.Center,
    ) {
        if (watched) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Assistido",
                tint = AccentDark,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun RatingsBar(
    mariaRating: Double?,
    luizRating: Double?,
    averageRating: Double?,
    averageTheme: AverageTheme?,
) {
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(SurfaceDeep, shape)
            .padding(horizontal = Dimens.Spacing8, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        RatingBadge(
            modifier = Modifier.weight(1f),
            name = "Maria",
            score = mariaRating,
        )

        if (averageRating != null && averageTheme != null) {
            AverageBadge(score = averageRating, theme = averageTheme)
        }

        RatingBadge(
            modifier = Modifier.weight(1f),
            name = "Luiz",
            score = luizRating,
        )
    }
}

@Composable
private fun RatingBadge(name: String, score: Double?, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier = modifier
            .clip(shape)
            .background(Surface, shape)
            .padding(horizontal = Dimens.Spacing8, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Avatar(name = name, size = Dimens.AvatarSmall)
        Text(
            text = name,
            modifier = Modifier.weight(1f, fill = false),
            fontSize = 12.sp,
            color = TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = score?.formatDecimals1() ?: "—",
            fontSize = if (score != null) 13.sp else 11.sp,
            fontWeight = if (score != null) FontWeight.ExtraBold else FontWeight.Normal,
            color = if (score != null) TextPrimary else TextMuted,
        )
    }
}

@Composable
private fun AverageBadge(score: Double, theme: AverageTheme) {
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier = Modifier
            .clip(shape)
            .background(theme.container, shape)
            .border(1.dp, theme.border, shape)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing4),
    ) {
        Icon(
            imageVector = Icons.Default.Star,
            contentDescription = null,
            tint = theme.content,
            modifier = Modifier.size(13.dp),
        )
        Text(
            text = score.formatDecimals1(),
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold,
            color = TextPrimary,
        )
        Text(
            text = "MÉDIA",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
            color = theme.content,
        )
    }
}

@Composable
private fun MediaTypeChip(type: ContentType) {
    val isSeries = type == ContentType.TV
    val shape = RoundedCornerShape(Dimens.RadiusPill)
    Row(
        modifier = Modifier
            .clip(shape)
            .background(LavenderSurface, shape)
            .border(1.dp, LavenderBorder, shape)
            .padding(horizontal = Dimens.Spacing12, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing4),
    ) {
        if (isSeries) {
            IconTv(modifier = Modifier.iconSize(14.dp), color = Lavender)
        } else {
            IconFilm(modifier = Modifier.iconSize(14.dp), color = Lavender)
        }
        Text(
            text = if (isSeries) "SÉRIE" else "FILME",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
            color = Lavender,
        )
    }
}

@Composable
private fun GenreChip(genre: String) {
    val shape = RoundedCornerShape(Dimens.RadiusPill)
    Text(
        text = genre,
        modifier = Modifier
            .clip(shape)
            .background(Surface, shape)
            .border(1.dp, Border.copy(alpha = 0.7f), shape)
            .padding(horizontal = Dimens.Spacing12, vertical = 5.dp),
        fontSize = 12.sp,
        color = TextSecondary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

/** Score band tint of the average badge, ported from `getAverageRatingTheme`. */
private data class AverageTheme(
    val container: Color,
    val border: Color,
    val content: Color,
)

private fun averageThemeFor(score: Double): AverageTheme = when {
    score >= 8.0 -> AverageTheme(
        container = ScoreGreatSurface,
        border = ScoreGreatBorder,
        content = Accent,
    )

    score >= 6.0 -> AverageTheme(
        container = ScoreGoodSurface,
        border = ScoreGoodBorder,
        content = Amber,
    )

    else -> AverageTheme(
        container = ScoreBadSurface,
        border = ScoreBadBorder,
        content = ScoreBad,
    )
}
