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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import app.moviematch.presentation.theme.Amber
import app.moviematch.presentation.theme.Accent
import app.moviematch.presentation.theme.Dimens
import app.moviematch.presentation.theme.MicroLabelStyle
import app.moviematch.presentation.theme.StatNumberStyle
import app.moviematch.presentation.theme.TextMuted
import app.moviematch.presentation.theme.TextPrimary
import app.moviematch.presentation.theme.TextSecondary

/**
 * The six bento cards of the Home grid.
 *
 * Each one mirrors the matching reference component (`PopcornThermometerCard`, `HarmonyIndexCard`,
 * `CriticsDuelCard`, `GroupConsensusCard`, `FavoriteGenresCard`, `RandomMovieCard`): same copy, same
 * geometry, same hierarchy — they only consume already-derived data (`HomeStats`), never the
 * repository, which keeps them pure and previewable.
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
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextPrimary,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "O nível de sintonia do grupo e quem é o cinéfilo mais exigente da roda " +
                        "nas noites de cinema.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
            }

            IconBadge(size = Dimens.IconBadgeLarge) {
                IconClapperboard(
                    modifier = Modifier.iconSize(IconSize.Default),
                    color = TextSecondary,
                )
            }
        }

        Spacer(Modifier.height(Dimens.Spacing16))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
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
            Text(text = label, style = MicroLabelStyle, color = TextSecondary)
            Spacer(Modifier.height(6.dp))
            Text(text = value, style = StatNumberStyle, color = TextPrimary)
            Spacer(Modifier.height(Dimens.Spacing4))
            Text(
                text = caption,
                fontSize = 11.sp,
                color = TextMuted,
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
                text = "ÍNDICE DE SINTONIA",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
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
                        style = StatNumberStyle,
                        color = TextPrimary,
                    )
                    Text(
                        text = "ACORDO",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = verdictTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
            }
        }
    }
}

/** 3. "Duelo de Críticos" — individual averages plus the fun fact. */
@Composable
fun CriticsDuelCard(
    evaluationsCount: Int,
    mariaAvg: Double?,
    mariaRatingsCount: Int,
    luizAvg: Double?,
    luizRatingsCount: Int,
    diffPoints: Double?,
    modifier: Modifier = Modifier,
) {
    val funFact = criticsFunFact(mariaAvg, luizAvg, diffPoints)

    BentoCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.padding(end = Dimens.Spacing12)) {
                Text(
                    text = "Duelo de Críticos",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                )
                Text(
                    text = "Médias individuais da roda",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
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
                name = "Maria",
                average = mariaAvg,
                ratingsCount = mariaRatingsCount,
            )
            CriticTile(
                modifier = Modifier.weight(1f),
                name = "Luiz",
                average = luizAvg,
                ratingsCount = luizRatingsCount,
            )
        }

        Spacer(Modifier.height(Dimens.Spacing12))

        BentoTile(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing16),
            ) {
                IconBadge(size = Dimens.IconBadgeSmall) {
                    IconFlame(modifier = Modifier.iconSize(IconSize.Small), color = TextSecondary)
                }
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(color = TextPrimary, fontWeight = FontWeight.Bold)) {
                            append(funFact.title)
                        }
                        append(funFact.body)
                    },
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 16.sp),
                    color = TextSecondary,
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
    BentoTile(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Dimens.Spacing8),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Avatar(name = name, size = Dimens.IconBadgeMedium)
            Spacer(Modifier.height(Dimens.Spacing4))
            Text(
                text = name,
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary,
            )
            Spacer(Modifier.height(Dimens.Spacing4))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing4),
            ) {
                Text(
                    text = average?.formatDecimals1() ?: "—",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                )
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = Amber,
                    modifier = Modifier.size(14.dp),
                )
            }
            Spacer(Modifier.height(Dimens.Spacing4))
            Text(
                text = "$ratingsCount notas dadas",
                fontSize = 11.sp,
                color = TextMuted,
            )
        }
    }
}

/** 4. "Consenso do Grupo" — the three best averages. */
@Composable
fun GroupConsensusCard(
    topMovies: List<ConsensusEntry>,
    modifier: Modifier = Modifier,
) {
    BentoCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing8),
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = TextPrimary,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = "Consenso do Grupo",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                )
            }
            Text(
                text = "Maior Média",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
            )
        }

        Spacer(Modifier.height(Dimens.Spacing24))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (topMovies.isEmpty()) {
                BentoTile(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Nenhuma sessão avaliada ainda. Dêem notas para formar o consenso!",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                }
            } else {
                topMovies.forEach { movie -> ConsensusRow(movie) }
            }
        }
    }
}

@Composable
private fun ConsensusRow(movie: ConsensusEntry) {
    BentoTile(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing12),
        ) {
            IconBadge(size = Dimens.IconBadgeSmall) {
                Text(
                    text = movie.rank.toString(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = TextPrimary,
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
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = listOfNotNull(
                        movie.year?.toString(),
                        "M: ${movie.mariaRating?.formatDecimals1() ?: "—"}",
                        "L: ${movie.luizRating?.formatDecimals1() ?: "—"}",
                    ).joinToString(" • "),
                    fontSize = 11.sp,
                    color = TextSecondary,
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
                color = TextPrimary,
            )
            Text(
                text = "$totalMovies Títulos",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
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
                        color = TextSecondary,
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
                    color = TextPrimary,
                )
            }
            Text(
                text = "${genre.count} títulos (${genre.percentage}%)",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
            )
        }

        ProgressBar(value = genre.percentage.toDouble(), color = Accent)
    }
}

/** The four genre glyphs: horror gets the frown, comedy the smile, adventure the compass. */
@Composable
private fun GenreIconGlyph(type: GenreIcon) {
    val modifier = Modifier.iconSize(IconSize.Small)
    when (type) {
        GenreIcon.GHOST -> IconFrown(modifier = modifier, color = TextSecondary)
        GenreIcon.SMILE -> IconSmile(modifier = modifier, color = TextSecondary)
        GenreIcon.COMPASS -> IconCompass(modifier = modifier, color = TextSecondary)
        GenreIcon.HEART -> IconHeart(modifier = modifier, color = TextSecondary)
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
                    .padding(end = 10.dp),
            ) {
                Text(
                    text = "Sugestão para filme!",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary,
                )
                Text(
                    text = "Sem ideia do que assistir?",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                )
            }

            AppSmallButton(
                label = "Sortear Filme",
                onClick = onPickRandomMovie,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(14.dp),
                    )
                },
            )
        }

        if (emptyQueueNotice) {
            Spacer(Modifier.height(Dimens.Spacing12))
            Text(
                text = "Não há filmes ou séries pendentes para sortear. Adicione um título na lista!",
                style = MaterialTheme.typography.bodySmall,
                color = Amber,
            )
        }
    }
}

/** Copy of the "fun fact" line, ported from `CriticsDuelCard`. */
private data class FunFact(val title: String, val body: String)

private fun criticsFunFact(mariaAvg: Double?, luizAvg: Double?, diffPoints: Double?): FunFact {
    if (mariaAvg == null && luizAvg == null) {
        return FunFact(
            title = "Roda em silêncio:",
            body = " ainda não há notas para declarar o crítico mais exigente.",
        )
    }
    if (mariaAvg == null || luizAvg == null) {
        val ratedName = if (mariaAvg != null) "Maria" else "Luiz"
        return FunFact(
            title = "$ratedName já tem nota:",
            body = " o outro ainda não avaliou nenhum filme na roda.",
        )
    }
    if (mariaAvg == luizAvg) {
        return FunFact(
            title = "Empate técnico:",
            body = " ${mariaAvg.formatDecimals1()} para os dois. Na próxima sessão o duelo continua!",
        )
    }
    val strictest = if (mariaAvg > luizAvg) "Maria" else "Luiz"
    val rival = if (strictest == "Maria") "Luiz" else "Maria"
    val points = diffPoints ?: 0.0
    val plural = if (points > 1) "s" else ""
    return FunFact(
        title = "$strictest lidera o rigor da roda:",
        body = " ${points.formatDecimals1()} ponto$plural acima de $rival na média do clube!",
    )
}
