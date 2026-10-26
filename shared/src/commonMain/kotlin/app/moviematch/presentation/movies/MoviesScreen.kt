package app.moviematch.presentation.movies

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.moviematch.presentation.common.dataOrNull
import app.moviematch.presentation.common.errorOrNull
import app.moviematch.presentation.common.isLoading
import app.moviematch.presentation.components.AppSmallButton
import app.moviematch.presentation.components.BentoCard
import app.moviematch.presentation.components.Chip
import app.moviematch.presentation.components.ChipTone
import app.moviematch.presentation.components.HeaderIconButton
import app.moviematch.presentation.components.IconBadge
import app.moviematch.presentation.components.Pill
import app.moviematch.presentation.components.ProgressBar
import app.moviematch.presentation.components.ScreenTopBar
import app.moviematch.presentation.components.SearchInput
import app.moviematch.presentation.components.StateEmpty
import app.moviematch.presentation.components.StateError
import app.moviematch.presentation.components.icons.IconFilm
import app.moviematch.presentation.components.icons.IconLogout
import app.moviematch.presentation.components.icons.IconSize
import app.moviematch.presentation.components.icons.iconSize
import app.moviematch.presentation.components.skeletons.MovieCardSkeleton
import app.moviematch.presentation.navigation.rememberBottomBarBottomPadding
import app.moviematch.presentation.theme.CardMetaStyle
import app.moviematch.presentation.theme.Dimens
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.roundToInt

/**
 * "Filmes e Séries" tab: the shared catalogue.
 *
 * Ported from the reference `MoviesScreen`: a summary block (counters + progress), a search field,
 * the three filter chips, then the list of [MovieCard]s. Loading renders the same rows as
 * [MovieCardSkeleton] so the list never collapses into a blank frame.
 *
 * @param onAddMovie opens the "Adicionar" tab, the call to action of the empty state.
 * @param onLogout ends the session; `null` when the app has no Supabase credentials, which hides the
 *   header button instead of offering an action that cannot do anything.
 */
@Composable
fun MoviesScreen(
    onOpenDetails: (MovieEntry) -> Unit,
    onAddMovie: () -> Unit,
    onLogout: (() -> Unit)?,
    modifier: Modifier = Modifier,
    viewModel: MoviesViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val visible by viewModel.visible.collectAsStateWithLifecycle()

    val entries = state.dataOrNull().orEmpty()
    val watchedCount = entries.count { it.summary.isWatched }
    val watchlistCount = entries.size - watchedCount
    val watchedPercentage = if (entries.isEmpty()) {
        0
    } else {
        ((watchedCount.toDouble() / entries.size) * 100).roundToInt()
    }

    // The header carries the logout action, and only when there is a session to end.
    val headerAction: (@Composable () -> Unit)? = onLogout?.let { logout ->
        {
            HeaderIconButton(
                onClick = logout,
                contentDescription = "Sair da conta",
            ) {
                IconLogout(
                    modifier = Modifier.iconSize(18.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        ScreenTopBar(
            title = "Filmes e Séries",
            subtitle = "Nosso Catálogo",
            action = headerAction,
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = Dimens.ScreenPadding,
                end = Dimens.ScreenPadding,
                top = Dimens.Spacing8,
                bottom = rememberBottomBarBottomPadding(),
            ),
            verticalArrangement = Arrangement.spacedBy(Dimens.Spacing12),
        ) {
            item(key = "summary") {
                CatalogSummaryCard(
                    total = entries.size,
                    watched = watchedCount,
                    watchlist = watchlistCount,
                    watchedPercentage = watchedPercentage,
                )
            }

            item(key = "search") {
                SearchInput(
                    value = query,
                    onValueChange = viewModel::onQueryChange,
                    placeholder = "Buscar por título ou ano...",
                )
            }

            item(key = "filters") {
                FilterChips(
                    active = filter,
                    total = entries.size,
                    watchlist = watchlistCount,
                    watched = watchedCount,
                    onSelect = viewModel::onFilterChange,
                )
            }

            when {
                state.isLoading -> items(count = 4, key = { "skeleton-$it" }) {
                    MovieCardSkeleton()
                }

                state.errorOrNull() != null && entries.isEmpty() -> item(key = "error") {
                    StateError(
                        message = state.errorOrNull() ?: "Não foi possível carregar sua lista.",
                        onRetry = viewModel::load,
                    )
                }

                visible.isEmpty() -> item(key = "empty") {
                    StateEmpty(
                        title = if (entries.isEmpty()) "Sua lista está vazia" else "Nada por aqui",
                        message = if (entries.isEmpty()) {
                            "Nenhum título na lista ainda. Adicione o primeiro!"
                        } else {
                            "Nenhum título encontrado com esses filtros."
                        },
                        action = if (entries.isEmpty()) {
                            { AppSmallButton(label = "Adicionar título", onClick = onAddMovie) }
                        } else {
                            null
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                else -> items(
                    items = visible,
                    key = { entry -> "${entry.type}-${entry.item.id}" },
                ) { entry ->
                    MovieCard(
                        entry = entry,
                        onClick = { onOpenDetails(entry) },
                        onToggleWatched = { viewModel.toggleWatched(entry) },
                    )
                }
            }
        }
    }
}

/** "Adicionados" summary block: title, counters, and the watched progress bar. */
@Composable
private fun CatalogSummaryCard(
    total: Int,
    watched: Int,
    watchlist: Int,
    watchedPercentage: Int,
) {
    BentoCard(padding = Dimens.Spacing16) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing12),
            ) {
                IconBadge(size = Dimens.IconBadgeSmall) {
                    IconFilm(
                        modifier = Modifier.iconSize(IconSize.Small),
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
                Text(
                    text = "Adicionados",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            Pill(label = "$total Títulos")
        }

        Spacer(Modifier.height(Dimens.Spacing12))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            val onSurface = MaterialTheme.colorScheme.onSurface
            Text(
                text = buildAnnotatedString {
                    append("• ")
                    withStyle(SpanStyle(color = onSurface, fontWeight = FontWeight.Bold)) {
                        append("$watched assistidos")
                    }
                    append(" ($watchedPercentage%)")
                },
                style = CardMetaStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "• $watchlist na fila",
                style = CardMetaStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(Dimens.Spacing12))

        ProgressBar(value = watchedPercentage.toDouble())
    }
}

/** The three status chips ("Todos", "Para Assistir", "Assistidos"). */
@Composable
private fun FilterChips(
    active: MoviesFilter,
    total: Int,
    watchlist: Int,
    watched: Int,
    onSelect: (MoviesFilter) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing8),
    ) {
        FilterChip(
            label = "Todos ($total)",
            selected = active == MoviesFilter.ALL,
            onClick = { onSelect(MoviesFilter.ALL) },
        )
        FilterChip(
            label = "Para Assistir ($watchlist)",
            selected = active == MoviesFilter.WATCHLIST,
            onClick = { onSelect(MoviesFilter.WATCHLIST) },
        )
        FilterChip(
            label = "Assistidos ($watched)",
            selected = active == MoviesFilter.WATCHED,
            onClick = { onSelect(MoviesFilter.WATCHED) },
        )
    }
}

@Composable
private fun FilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    // The chip itself is 32dp tall; the surrounding box supplies the 48dp touch target.
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .heightIn(min = Dimens.TouchTarget)
            .selectable(
                selected = selected,
                interactionSource = interactionSource,
                indication = null,
                role = Role.Tab,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Chip(
            label = label,
            tone = ChipTone.Neutral,
            selected = selected,
            interactionSource = interactionSource,
        )
    }
}
