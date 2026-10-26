package app.moviematch.presentation.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import app.moviematch.domain.ConsensusEntry
import app.moviematch.domain.GenreIcon
import app.moviematch.domain.GenreStat
import app.moviematch.presentation.common.formatDecimals1
import app.moviematch.presentation.common.formatDecimals2Trimmed
import app.moviematch.presentation.components.AppSmallButton
import app.moviematch.presentation.components.Avatar
import app.moviematch.presentation.components.BentoCard
import app.moviematch.presentation.components.BentoTile
import app.moviematch.presentation.components.DonutRing
import app.moviematch.presentation.components.IconBadge
import app.moviematch.presentation.components.Pill
import app.moviematch.presentation.components.ProgressBar
import app.moviematch.presentation.components.icons.IconClapperboard
import app.moviematch.presentation.components.icons.IconCompass
import app.moviematch.presentation.components.icons.IconFlame
import app.moviematch.presentation.components.icons.IconFrown
import app.moviematch.presentation.components.icons.IconHeart
import app.moviematch.presentation.components.icons.IconSize
import app.moviematch.presentation.components.icons.IconSmile
import app.moviematch.presentation.components.icons.iconSize
import app.moviematch.presentation.theme.Dimens
import app.moviematch.presentation.theme.MicroLabelStyle

/**
 * The six bento cards of the Home grid.
 *
 * Each one mirrors the matching reference component (`PopcornThermometerCard`, `HarmonyIndexCard`,
 * `CriticsDuelCard`, `GroupConsensusCard`, `FavoriteGenresCard`, `RandomMovieCard`): same copy, same
 * geometry, same hierarchy — they only consume already-derived data (`HomeStats`), never the
 * repository, which keeps them pure and previewable.
 *
 * Visual language is Material 3: colour comes from `MaterialTheme.colorScheme` roles, type from the
 * MD3 scale and depth from the `surfaceContainer*` tones supplied by [BentoCard] / [BentoTile].
 */

/** 1. "Termômetro de Pipoca" — the three library counters. */
@Composable
fun PopcornThermometerCard(
    totalList: Int,
    watched: Int,
    watchedPercentage: Int,
    queue: Int,
    modifier: Modifier = Modifier,
) {
    BentoCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = Dimens.Spacing16),
            ) {
                Text(
                    text = "Termômetro de Pipoca",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(Dimens.Spacing8))
                Text(
                    text = "O nível de sintonia do grupo e quem é o cinéfilo mais exigente da roda " +
                        "nas noites de cinema.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            IconBadge(size = Dimens.IconBadgeLarge) {
                IconClapperboard(
                    modifier = Modifier.iconSize(IconSize.Default),
                    color = LocalContentColor.current,
                )
            }
        }

        Spacer(Modifier.height(Dimens.Spacing16))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing8),
        ) {
            StatTile(
                modifier = Modifier.weight(1f),
                label = "NA LISTA",
                value = totalList.toString(),
                caption = "curadoria",
            )
            StatTile(
                modifier = Modifier.weight(1f),
                label = "ASSISTIDOS",
                value = watched.toString(),
                caption = "$watchedPercentage% do total",
            )
            StatTile(
                modifier = Modifier.weight(1f),
                label = "NA FILA",
                value = queue.toString(),
                caption = "pendentes",
            )
        }
    }
}

@Composable
private fun StatTile(
    label: String,
    value: String,
    caption: String,
    modifier: Modifier = Modifier,
) {
    BentoTile(modifier = modifier, padding = Dimens.Spacing8) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Dimens.Spacing8),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = label,
                style = MicroLabelStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(Dimens.Spacing8))
            Text(
                text = value,
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(Dimens.Spacing4))
            Text(
                text = caption,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** 2. "Índice de Sintonia" — donut gauge plus the verdict copy. */
@Composable
fun HarmonyIndexCard(
    harmonyPercentage: Int,
    statusBadge: String,
    sessionsCount: Int,
    agreementAvg: Double,
    verdictTitle: String,
    modifier: Modifier = Modifier,
) {
    val description = if (sessionsCount == 0) {
        "Ainda não há sessões avaliadas pelos dois. Hora de marcar uma noite de cinema!"
    } else {
        "Em $sessionsCount sessões, a galera concorda no veredito em quase " +
            "${agreementAvg.formatDecimals1()}. O controle remoto está em total harmonia!"
    }

    BentoCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Índice de Sintonia",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = Dimens.Spacing12),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Pill(label = statusBadge)
        }

        Spacer(Modifier.height(Dimens.Spacing24))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing24),
        ) {
            DonutRing(percentage = harmonyPercentage, size = 90.dp, strokeWidth = 5.dp) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$harmonyPercentage%",
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "ACORDO",
                        style = MicroLabelStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = verdictTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(Dimens.Spacing8))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** 3. "Duelo de Críticos" — individual averages plus the fun fact. */
@Composable
fun CriticsDuelCard(
    evaluationsCount: Int,
    member1Name: String,
    member1Avg: Double?,
    member1RatingsCount: Int,
    member2Name: String,
    member2Avg: Double?,
    member2RatingsCount: Int,
    diffPoints: Double?,
    modifier: Modifier = Modifier,
) {
    val funFact = criticsFunFact(member1Name, member1Avg, member2Name, member2Avg, diffPoints)

    BentoCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = Dimens.Spacing12),
            ) {
                Text(
                    text = "Duelo de Críticos",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Médias individuais da roda",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Pill(label = "$evaluationsCount avaliações")
        }

        Spacer(Modifier.height(Dimens.Spacing24))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing16),
        ) {
            CriticTile(
                modifier = Modifier.weight(1f),
                name = member1Name,
                average = member1Avg,
                ratingsCount = member1RatingsCount,
            )
            CriticTile(
                modifier = Modifier.weight(1f),
                name = member2Name,
                average = member2Avg,
                ratingsCount = member2RatingsCount,
            )
        }

        Spacer(Modifier.height(Dimens.Spacing12))

        BentoTile(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing16),
            ) {
                IconBadge(size = Dimens.IconBadgeSmall) {
                    IconFlame(
                        modifier = Modifier.iconSize(IconSize.Small),
                        color = LocalContentColor.current,
                    )
                }
                Text(
                    text = buildAnnotatedString {
                        withStyle(
                            SpanStyle(
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                            ),
                        ) {
                            append(funFact.title)
                        }
                        append(funFact.body)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun CriticTile(
    name: String,
    average: Double?,
    ratingsCount: Int,
    modifier: Modifier = Modifier,
) {
    val displayName = name.ifBlank { "Aguardando…" }

    BentoTile(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Dimens.Spacing8),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Avatar(name = name.ifBlank { null }, size = Dimens.IconBadgeMedium)
            Spacer(Modifier.height(Dimens.Spacing4))
            Text(
                text = displayName,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(Dimens.Spacing4))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing4),
            ) {
                Text(
                    text = average?.formatDecimals1() ?: "—",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(14.dp),
                )
            }
            Spacer(Modifier.height(Dimens.Spacing4))
            Text(
                text = "$ratingsCount notas dadas",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** 4. "Consenso do Grupo" — the three best averages. */
@Composable
fun GroupConsensusCard(
    topMovies: List<ConsensusEntry>,
    memberNames: Pair<String, String>? = null,
    modifier: Modifier = Modifier,
) {
    BentoCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = Dimens.Spacing12),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing8),
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = "Consenso do Grupo",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = "Maior Média",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(Dimens.Spacing24))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Dimens.Spacing12),
        ) {
            if (topMovies.isEmpty()) {
                BentoTile(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Nenhuma sessão avaliada ainda. Dêem notas para formar o consenso!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                topMovies.forEach { movie -> ConsensusRow(movie, memberNames) }
            }
        }
    }
}

@Composable
private fun ConsensusRow(movie: ConsensusEntry, memberNames: Pair<String, String>?) {
    // Initials of the real pair; the numbered fallback keeps the row readable before a group exists.
    val label1 = memberNames?.first?.trim()?.take(1)?.uppercase().orEmpty().ifBlank { "1" }
    val label2 = memberNames?.second?.trim()?.take(1)?.uppercase().orEmpty().ifBlank { "2" }

    BentoTile(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing12),
        ) {
            IconBadge(size = Dimens.IconBadgeSmall) {
                Text(
                    text = movie.rank.toString(),
                    style = MaterialTheme.typography.labelLarge,
                    color = LocalContentColor.current,
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = Dimens.Spacing8),
            ) {
                Text(
                    text = movie.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = listOfNotNull(
                        movie.year?.toString(),
                        "$label1: ${movie.mariaRating?.formatDecimals1() ?: "—"}",
                        "$label2: ${movie.luizRating?.formatDecimals1() ?: "—"}",
                    ).joinToString(" • "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Pill(label = movie.averageRating.formatDecimals2Trimmed())
        }
    }
}

/** 5. "Gêneros Favoritos" — distribution of the leading genre. */
@Composable
fun FavoriteGenresCard(
    totalMovies: Int,
    genres: List<GenreStat>,
    modifier: Modifier = Modifier,
) {
    BentoCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Gêneros Favoritos",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = Dimens.Spacing12),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "$totalMovies Títulos",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(Dimens.Spacing24))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Dimens.Spacing24),
        ) {
            if (genres.isEmpty()) {
                BentoTile(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Sem títulos na lista ainda. Adicionem para ver os gêneros favoritos da roda!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                genres.forEach { genre -> GenreRow(genre) }
            }
        }
    }
}

@Composable
private fun GenreRow(genre: GenreStat) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Dimens.Spacing8),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing8),
            ) {
                GenreIconGlyph(genre.iconType)
                Text(
                    text = genre.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Text(
                text = "${genre.count} títulos (${genre.percentage}%)",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        ProgressBar(value = genre.percentage.toDouble())
    }
}

/** The four genre glyphs: horror gets the frown, comedy the smile, adventure the compass. */
@Composable
private fun GenreIconGlyph(type: GenreIcon) {
    val modifier = Modifier.iconSize(IconSize.Small)
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    when (type) {
        GenreIcon.GHOST -> IconFrown(modifier = modifier, color = color)
        GenreIcon.SMILE -> IconSmile(modifier = modifier, color = color)
        GenreIcon.COMPASS -> IconCompass(modifier = modifier, color = color)
        GenreIcon.HEART -> IconHeart(modifier = modifier, color = color)
    }
}

/** 6. "Sugestão para filme!" — the random draw call to action. */
@Composable
fun RandomMovieCard(
    onPickRandomMovie: () -> Unit,
    modifier: Modifier = Modifier,
    emptyQueueNotice: Boolean = false,
) {
    BentoCard(modifier = modifier, padding = Dimens.Spacing16) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = Dimens.Spacing12),
            ) {
                Text(
                    text = "Sugestão para filme!",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Sem ideia do que assistir?",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            AppSmallButton(
                label = "Sortear Filme",
                onClick = onPickRandomMovie,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = LocalContentColor.current,
                        modifier = Modifier.size(14.dp),
                    )
                },
            )
        }

        if (emptyQueueNotice) {
            Spacer(Modifier.height(Dimens.Spacing12))
            BentoTile(
                modifier = Modifier.fillMaxWidth(),
                padding = Dimens.Spacing8,
                containerColor = MaterialTheme.colorScheme.errorContainer,
                borderColor = Color.Unspecified,
            ) {
                Text(
                    text = "Não há filmes ou séries pendentes para sortear. Adicione um título na lista!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        }
    }
}

/** Copy of the "fun fact" line, ported from `CriticsDuelCard`. */
private data class FunFact(val title: String, val body: String)

private fun criticsFunFact(
    member1Name: String,
    member1Avg: Double?,
    member2Name: String,
    member2Avg: Double?,
    diffPoints: Double?,
): FunFact {
    if (member1Avg == null && member2Avg == null) {
        return FunFact(
            title = "Roda em silêncio:",
            body = " ainda não há notas para declarar o crítico mais exigente.",
        )
    }
    if (member1Avg == null || member2Avg == null) {
        val ratedName = (if (member1Avg != null) member1Name else member2Name)
            .ifBlank { "Alguém" }
        val otherName = (if (member1Avg != null) member2Name else member1Name)
            .ifBlank { "A outra pessoa" }
        return FunFact(
            title = "$ratedName já tem nota:",
            body = " $otherName ainda não avaliou nenhum filme na roda.",
        )
    }
    if (member1Avg == member2Avg) {
        return FunFact(
            title = "Empate técnico:",
            body = " ${member1Avg.formatDecimals1()} para os dois. Na próxima sessão o duelo continua!",
        )
    }
    val strictest = if (member1Avg > member2Avg) member1Name else member2Name
    val rival = if (strictest == member1Name) member2Name else member1Name
    val points = diffPoints ?: 0.0
    val plural = if (points > 1) "s" else ""
    return FunFact(
        title = "$strictest lidera o rigor da roda:",
        body = " ${points.formatDecimals1()} ponto$plural acima de $rival na média do clube!",
    )
}
