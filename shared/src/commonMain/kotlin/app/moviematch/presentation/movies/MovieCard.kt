package app.moviematch.presentation.movies

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
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.moviematch.domain.model.ContentType
import app.moviematch.presentation.common.formatDecimals1
import app.moviematch.presentation.components.Avatar
import app.moviematch.presentation.components.BentoCard
import app.moviematch.presentation.components.Chip
import app.moviematch.presentation.components.ChipSize
import app.moviematch.presentation.components.ChipTone
import app.moviematch.presentation.components.PosterImage
import app.moviematch.presentation.components.icons.IconFilm
import app.moviematch.presentation.components.icons.IconTv
import app.moviematch.presentation.components.icons.iconSize
import app.moviematch.presentation.theme.CardMetaStyle
import app.moviematch.presentation.theme.Dimens
import app.moviematch.presentation.theme.MicroLabelStyle

/**
 * Watch-list row.
 *
 * A Material 3 [BentoCard]: poster with a category badge, title/metadata/genre chips, the
 * "assistido" toggle and the ratings bar (Maria • média • Luiz) tinted by score band. Depth
 * comes from the `surfaceContainer*` tones; nothing inside the card carries elevation.
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
                    .padding(start = Dimens.Spacing16, end = Dimens.Spacing8),
            ) {
                Text(
                    text = summary.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = metaLine,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(Dimens.Spacing8))
                GenreAndTypeChips(type = entry.type, genres = summary.genres)
            }

            WatchedToggle(watched = summary.isWatched, onToggle = onToggleWatched)
        }

        Spacer(Modifier.height(Dimens.Spacing16))

        RatingsBar(
            mariaRating = summary.mariaRating,
            luizRating = summary.luizRating,
            averageRating = summary.averageRating,
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
            Chip(
                label = badge,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(
                        top = Dimens.Spacing8,
                        start = Dimens.Spacing8,
                        end = Dimens.Spacing8,
                    ),
                size = ChipSize.Small,
                tone = ChipTone.Category,
            )
        }
    }
}

/**
 * "Assistido" checkbox of the row.
 *
 * The 40dp tonal disc sits inside a 48dp target that owns the [Role.Checkbox] semantics; on the
 * checked state it switches to the `primary` / `onPrimary` roles.
 */
@Composable
private fun WatchedToggle(watched: Boolean, onToggle: () -> Unit) {
    Box(
        modifier = Modifier
            .size(Dimens.TouchTarget)
            .toggleable(
                value = watched,
                role = Role.Checkbox,
                onValueChange = { onToggle() },
            )
            .semantics {
                contentDescription = if (watched) "Assistido" else "Marcar como assistido"
            },
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.size(Dimens.IconBadgeMedium),
            shape = MaterialTheme.shapes.small,
            color = if (watched) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surfaceContainerHighest
            },
            contentColor = if (watched) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                if (watched) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

/**
 * Ratings strip: an `surfaceContainerHighest` bar holding the two reviewer scores and the average.
 *
 * The average is the only tinted block, following the score bands (`primaryContainer` for >= 8,
 * `surfaceContainerHigh` for >= 6, `errorContainer` below that).
 */
@Composable
private fun RatingsBar(
    mariaRating: Double?,
    luizRating: Double?,
    averageRating: Double?,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(Dimens.RatingBarHeight),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(Dimens.Spacing4),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing4),
        ) {
            RatingBadge(
                modifier = Modifier.weight(1f),
                name = "Maria",
                score = mariaRating,
            )

            if (averageRating != null) {
                AverageBadge(score = averageRating)
            }

            RatingBadge(
                modifier = Modifier.weight(1f),
                name = "Luiz",
                score = luizRating,
            )
        }
    }
}

@Composable
private fun RatingBadge(name: String, score: Double?, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.padding(horizontal = Dimens.Spacing4, vertical = Dimens.Spacing4),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing4),
    ) {
        Avatar(name = name, size = Dimens.AvatarSmall)
        Text(
            text = name,
            modifier = Modifier.weight(1f, fill = false),
            style = CardMetaStyle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = score?.formatDecimals1() ?: "—",
            style = if (score != null) {
                MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.ExtraBold)
            } else {
                MaterialTheme.typography.labelSmall
            },
            color = if (score != null) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}

@Composable
private fun AverageBadge(score: Double) {
    val colors = averageColorsFor(score)
    Surface(
        shape = MaterialTheme.shapes.small,
        color = colors.container,
        contentColor = colors.content,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Dimens.Spacing8, vertical = Dimens.Spacing4),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing4),
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                modifier = Modifier.size(13.dp),
            )
            Text(
                text = score.formatDecimals1(),
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.ExtraBold),
            )
            Text(
                text = "MÉDIA",
                style = MicroLabelStyle,
            )
        }
    }
}

@Composable
private fun MediaTypeChip(type: ContentType) {
    val isSeries = type == ContentType.TV
    Chip(
        label = if (isSeries) "Série" else "Filme",
        size = ChipSize.Small,
        tone = ChipTone.Lavender,
        leadingIcon = {
            val iconColor = MaterialTheme.colorScheme.onTertiaryContainer
            if (isSeries) {
                IconTv(modifier = Modifier.iconSize(14.dp), color = iconColor)
            } else {
                IconFilm(modifier = Modifier.iconSize(14.dp), color = iconColor)
            }
        },
    )
}

@Composable
private fun GenreChip(genre: String) {
    Chip(label = genre, tone = ChipTone.Neutral)
}

/** Container/content pair of the average badge, chosen by score band. */
private data class AverageColors(
    val container: Color,
    val content: Color,
)

/** Score bands of the average badge: great (>= 8), good (>= 6) and the error band below that. */
@Composable
private fun averageColorsFor(score: Double): AverageColors = when {
    score >= 8.0 -> AverageColors(
        container = MaterialTheme.colorScheme.primaryContainer,
        content = MaterialTheme.colorScheme.onPrimaryContainer,
    )

    score >= 6.0 -> AverageColors(
        container = MaterialTheme.colorScheme.surfaceContainerHigh,
        content = MaterialTheme.colorScheme.onSurface,
    )

    else -> AverageColors(
        container = MaterialTheme.colorScheme.errorContainer,
        content = MaterialTheme.colorScheme.onErrorContainer,
    )
}
