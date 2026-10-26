package app.moviematch.presentation.addmovie

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.moviematch.domain.model.ContentType
import app.moviematch.domain.model.SearchResult
import app.moviematch.presentation.common.dataOrNull
import app.moviematch.presentation.common.errorOrNull
import app.moviematch.presentation.common.formatDecimals1
import app.moviematch.presentation.components.AppSmallButton
import app.moviematch.presentation.components.Chip
import app.moviematch.presentation.components.ChipSize
import app.moviematch.presentation.components.ChipTone
import app.moviematch.presentation.components.PosterImage
import app.moviematch.presentation.components.ScreenTopBar
import app.moviematch.presentation.components.SearchInput
import app.moviematch.presentation.components.icons.IconFilm
import app.moviematch.presentation.components.icons.IconTv
import app.moviematch.presentation.components.icons.iconSize
import app.moviematch.presentation.components.skeletons.SearchResultsSkeleton
import app.moviematch.presentation.navigation.rememberBottomBarBottomPadding
import app.moviematch.presentation.theme.CardMetaStyle
import app.moviematch.presentation.theme.DialogBodyStyle
import app.moviematch.presentation.theme.Dimens
import org.koin.compose.viewmodel.koinViewModel

/**
 * "Adicionar" tab: searches the catalogue and adds a title to the shared list.
 *
 * Ported from the reference `AddMovieScreen`: a debounced search field (spinner while the request
 * is in flight), an inline error surface, and one card per hit with the media-type chip and the
 * add button — the hit currently being added is the only one whose button shows a spinner.
 */
@Composable
fun AddMovieScreen(
    onAdded: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AddMovieViewModel = koinViewModel(),
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val resultsState by viewModel.results.collectAsStateWithLifecycle()
    val searching by viewModel.searching.collectAsStateWithLifecycle()
    val addingId by viewModel.addingId.collectAsStateWithLifecycle()
    val addedTitle by viewModel.addedTitle.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    val results = resultsState.dataOrNull().orEmpty()
    val term = query.trim()

    // The mutation is done: switch back to the list, exactly like the reference `onSwitchTab`.
    LaunchedEffect(addedTitle) {
        if (addedTitle != null) {
            viewModel.consumeAddedTitle()
            onAdded()
        }
    }

    val searchError = resultsState.errorOrNull()

    // Keep the previous hits on screen while the next search runs; the skeleton is only shown when
    // there is nothing to keep (first search / empty history).
    var lastResults by remember { mutableStateOf<List<SearchResult>>(emptyList()) }
    LaunchedEffect(results, searching) {
        if (!searching && results.isNotEmpty()) lastResults = results
    }
    val displayResults = if (searching && lastResults.isNotEmpty()) lastResults else results

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        ScreenTopBar(
            title = "Adicionar Filme ou Série",
            subtitle = "Procure para adicionar à lista.",
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = Dimens.ScreenPadding),
        ) {
            SearchInput(
                value = query,
                onValueChange = viewModel::onQueryChange,
                placeholder = "Buscar filme ou série no TMDb...",
                trailing = {
                    if (searching) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 2.dp,
                        )
                    }
                },
            )

            Spacer(Modifier.height(Dimens.Spacing16))

            if (searchError != null) {
                ErrorBox(
                    message = searchError,
                    onRetry = { viewModel.onQueryChange(query) },
                )
                Spacer(Modifier.height(Dimens.Spacing16))
            } else if (error != null) {
                ErrorBox(message = error.orEmpty())
                Spacer(Modifier.height(Dimens.Spacing16))
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    bottom = rememberBottomBarBottomPadding(),
                ),
                verticalArrangement = Arrangement.spacedBy(Dimens.Spacing12),
            ) {
                if (searching && term.isNotEmpty() && displayResults.isEmpty()) {
                    item(key = "skeleton") { SearchResultsSkeleton() }
                } else {
                    items(
                        items = displayResults,
                        key = { result -> viewModel.keyOf(result) },
                    ) { result ->
                        SearchResultRow(
                            result = result,
                            adding = addingId == viewModel.keyOf(result),
                            enabled = addingId == null,
                            onAdd = { viewModel.add(result) },
                        )
                    }

                    if (term.isEmpty()) {
                        item(key = "hint") {
                            EmptyHint("Digite um título para buscar filmes ou séries no TMDb.")
                        }
                    } else if (displayResults.isEmpty() && error == null && searchError == null) {
                        item(key = "no-results") {
                            EmptyHint("Nenhum resultado encontrado.")
                        }
                    }
                }
            }
        }
    }
}

/** One search hit: compact poster, title + media chip, meta line, overview and the add button. */
@Composable
private fun SearchResultRow(
    result: SearchResult,
    adding: Boolean,
    enabled: Boolean,
    onAdd: () -> Unit,
) {
    val meta = listOfNotNull(
        result.year?.toString(),
        result.voteAverage?.let { "TMDb ${it.formatDecimals1()}" },
    ).joinToString(" • ")

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = Dimens.ElevationRow),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.Spacing12),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing12),
        ) {
            PosterImage(
                url = result.posterUrl,
                modifier = Modifier.size(
                    width = Dimens.PosterSmallWidth,
                    height = Dimens.PosterSmallHeight,
                ),
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Dimens.Spacing4),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing8),
                ) {
                    Text(
                        text = result.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    MediaTypeBadge(type = result.type)
                }

                if (meta.isNotEmpty()) {
                    Text(
                        text = meta,
                        style = CardMetaStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                if (result.overview.isNotBlank()) {
                    Text(
                        text = result.overview,
                        style = CardMetaStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            AddButton(adding = adding, enabled = enabled, onClick = onAdd)
        }
    }
}

@Composable
private fun MediaTypeBadge(type: ContentType) {
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

/** Primary action of a search hit: a MD3 [FilledIconButton] (40dp inside its 48dp target). */
@Composable
private fun AddButton(
    adding: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    FilledIconButton(
        onClick = onClick,
        modifier = Modifier.size(Dimens.IconButtonSize),
        enabled = enabled,
        shape = MaterialTheme.shapes.small,
    ) {
        if (adding) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                color = LocalContentColor.current,
                strokeWidth = 2.dp,
            )
        } else {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Adicionar",
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

/** Inline failure notice: an `errorContainer` surface with the retry action. */
@Composable
private fun ErrorBox(message: String, onRetry: (() -> Unit)? = null) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.Spacing12),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = message,
                modifier = Modifier.fillMaxWidth(),
                style = DialogBodyStyle,
                color = MaterialTheme.colorScheme.onErrorContainer,
                textAlign = TextAlign.Center,
            )
            if (onRetry != null) {
                Spacer(Modifier.height(Dimens.Spacing8))
                AppSmallButton(label = "Tentar novamente", onClick = onRetry)
            }
        }
    }
}

@Composable
private fun EmptyHint(message: String) {
    Text(
        text = message,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Dimens.Spacing24),
        style = DialogBodyStyle,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}
