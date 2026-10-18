package app.moviematch.presentation.addmovie

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.moviematch.domain.model.ContentType
import app.moviematch.domain.model.SearchResult
import app.moviematch.presentation.common.dataOrNull
import app.moviematch.presentation.common.formatDecimals1
import app.moviematch.presentation.components.BentoCard
import app.moviematch.presentation.components.PosterImage
import app.moviematch.presentation.components.SearchInput
import app.moviematch.presentation.components.icons.IconFilm
import app.moviematch.presentation.components.icons.IconSize
import app.moviematch.presentation.components.icons.IconTv
import app.moviematch.presentation.components.icons.iconSize
import app.moviematch.presentation.components.skeletons.SearchResultsSkeleton
import app.moviematch.presentation.navigation.rememberBottomBarBottomPadding
import app.moviematch.presentation.theme.ChipBorder
import app.moviematch.presentation.theme.ChipSurface
import app.moviematch.presentation.theme.Danger
import app.moviematch.presentation.theme.DangerBorder
import app.moviematch.presentation.theme.DangerSurface
import app.moviematch.presentation.theme.Dimens
import app.moviematch.presentation.theme.TextMuted
import app.moviematch.presentation.theme.TextOnAccent
import app.moviematch.presentation.theme.TextPrimary
import app.moviematch.presentation.theme.TextSecondary
import app.moviematch.presentation.theme.White
import org.koin.compose.viewmodel.koinViewModel

/**
 * "Adicionar" tab: searches the catalogue and adds a title to the shared list.
 *
 * Ported from the reference `AddMovieScreen`: a debounced search field (spinner while the request is
 * in flight), an inline error box, and one row per hit with the media-type chip and the add button —
 * the hit currently being added is the only one whose button shows a spinner.
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(
                start = Dimens.ScreenPadding,
                end = Dimens.ScreenPadding,
                top = Dimens.Spacing24,
            ),
    ) {
        Text(
            text = "Adicionar Filme ou Série",
            style = MaterialTheme.typography.headlineMedium,
            color = TextPrimary,
        )
        Spacer(Modifier.height(Dimens.Spacing8))
        Text(
            text = "Procure para adicionar à lista.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
        )

        Spacer(Modifier.height(20.dp))

        SearchInput(
            value = query,
            onValueChange = viewModel::onQueryChange,
            placeholder = "Buscar filme ou série no TMDb...",
            trailing = {
                if (searching) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(IconSize.Small),
                        color = TextMuted,
                        strokeWidth = 2.dp,
                    )
                }
            },
        )

        Spacer(Modifier.height(Dimens.Spacing16))

        if (error != null) {
            ErrorBox(message = error.orEmpty())
            Spacer(Modifier.height(Dimens.Spacing16))
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = rememberBottomBarBottomPadding()),
            verticalArrangement = Arrangement.spacedBy(Dimens.Spacing12),
        ) {
            if (searching && term.isNotEmpty()) {
                item(key = "skeleton") { SearchResultsSkeleton() }
            } else {
                items(
                    items = results,
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
                } else if (results.isEmpty() && error == null) {
                    item(key = "no-results") {
                        EmptyHint("Nenhum resultado encontrado.")
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

    BentoCard(padding = Dimens.Spacing12) {
        Row(
            modifier = Modifier.fillMaxWidth(),
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
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    MediaTypeBadge(type = result.type)
                }

                if (meta.isNotEmpty()) {
                    Text(
                        text = meta,
                        fontSize = 12.sp,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                if (result.overview.isNotBlank()) {
                    Text(
                        text = result.overview,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        color = TextMuted,
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
    val shape = RoundedCornerShape(Dimens.RadiusPill)
    Row(
        modifier = Modifier
            .clip(shape)
            .background(ChipSurface, shape)
            .border(1.dp, ChipBorder, shape)
            .padding(horizontal = Dimens.Spacing8, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing4),
    ) {
        if (isSeries) {
            IconTv(modifier = Modifier.iconSize(14.dp), color = TextSecondary)
        } else {
            IconFilm(modifier = Modifier.iconSize(14.dp), color = TextSecondary)
        }
        Text(
            text = if (isSeries) "SÉRIE" else "FILME",
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.4.sp,
            color = TextSecondary,
        )
    }
}

@Composable
private fun AddButton(
    adding: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(Dimens.IconButtonSize)
            .clip(CircleShape)
            .background(if (enabled) White else White.copy(alpha = 0.5f), CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (adding) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                color = TextOnAccent,
                strokeWidth = 2.dp,
            )
        } else {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Adicionar",
                tint = TextOnAccent,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun ErrorBox(message: String) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(DangerSurface, shape)
            .border(1.dp, DangerBorder, shape)
            .padding(Dimens.Spacing12),
    ) {
        Text(
            text = message,
            modifier = Modifier.fillMaxWidth(),
            fontSize = 13.sp,
            color = Danger,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun EmptyHint(message: String) {
    Text(
        text = message,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Dimens.Spacing24),
        fontSize = 13.sp,
        color = TextMuted,
        textAlign = TextAlign.Center,
    )
}
