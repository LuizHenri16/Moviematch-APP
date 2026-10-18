package app.moviematch.presentation.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.moviematch.domain.model.ContentItem
import app.moviematch.presentation.common.dataOrNull
import app.moviematch.presentation.common.errorOrNull
import app.moviematch.presentation.common.isLoading
import app.moviematch.presentation.components.HeaderIconButton
import app.moviematch.presentation.components.ScreenHeader
import app.moviematch.presentation.components.StateError
import app.moviematch.presentation.components.icons.IconLogout
import app.moviematch.presentation.components.icons.iconSize
import app.moviematch.presentation.components.skeletons.HomeSkeleton
import app.moviematch.presentation.home.components.CriticsDuelCard
import app.moviematch.presentation.home.components.FavoriteGenresCard
import app.moviematch.presentation.home.components.GroupConsensusCard
import app.moviematch.presentation.home.components.HarmonyIndexCard
import app.moviematch.presentation.home.components.PopcornThermometerCard
import app.moviematch.presentation.home.components.RandomMovieCard
import app.moviematch.presentation.navigation.rememberBottomBarBottomPadding
import app.moviematch.presentation.theme.Dimens
import app.moviematch.presentation.theme.TextSecondary
import org.koin.compose.viewmodel.koinViewModel

/**
 * Home tab: the six-bento dashboard.
 *
 * All numbers come from [app.moviematch.domain.homeStats] through [HomeViewModel]; the screen only
 * maps them to components. While the first load is in flight the same grid is rendered as
 * [HomeSkeleton], so the layout never jumps.
 *
 * @param onLogout ends the session; `null` when the app has no Supabase credentials, which hides the
 *   header button instead of offering an action that cannot do anything.
 */
@Composable
fun HomeScreen(
    onOpenDetails: (ContentItem) -> Unit,
    onLogout: (() -> Unit)?,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val pickResult by viewModel.pickResult.collectAsStateWithLifecycle()
    var emptyQueueNotice by remember { mutableStateOf(false) }

    LaunchedEffect(pickResult) {
        when (val result = pickResult) {
            is RandomPick.Picked -> {
                viewModel.consumePick()
                onOpenDetails(result.item)
            }

            RandomPick.EmptyQueue -> {
                emptyQueueNotice = true
                viewModel.consumePick()
            }

            RandomPick.Idle -> Unit
        }
    }

    val stats = state.dataOrNull()

    // The header carries the logout action, and only when there is a session to end.
    val headerAction: (@Composable () -> Unit)? = onLogout?.let { logout ->
        {
            HeaderIconButton(onClick = logout) {
                IconLogout(modifier = Modifier.iconSize(18.dp), color = TextSecondary)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        ScreenHeader(
            title = "Home",
            subtitle = "Acompanhe seus Filmes e Séries",
            action = headerAction,
        )

        when {
            state.isLoading -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(
                        horizontal = Dimens.ScreenPadding,
                        vertical = Dimens.Spacing8,
                    ),
            ) {
                HomeSkeleton()
            }

            stats == null -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                StateError(
                    message = state.errorOrNull() ?: "Não foi possível carregar sua lista.",
                    onRetry = viewModel::load,
                )
            }

            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(
                        start = Dimens.ScreenPadding,
                        end = Dimens.ScreenPadding,
                        top = Dimens.Spacing8,
                        bottom = rememberBottomBarBottomPadding(),
                    ),
                verticalArrangement = Arrangement.spacedBy(Dimens.Spacing16),
            ) {
                PopcornThermometerCard(
                    totalList = stats.totalList,
                    watched = stats.watched,
                    watchedPercentage = stats.watchedPercentage,
                    queue = stats.queue,
                )

                HarmonyIndexCard(
                    harmonyPercentage = stats.harmonyPercentage,
                    statusBadge = stats.harmonyBadge,
                    sessionsCount = stats.bothRatedCount,
                    agreementAvg = stats.agreementAvg,
                    verdictTitle = stats.harmonyTitle,
                )

                CriticsDuelCard(
                    evaluationsCount = stats.evaluationsCount,
                    mariaAvg = stats.mariaAvg,
                    mariaRatingsCount = stats.mariaRatingsCount,
                    luizAvg = stats.luizAvg,
                    luizRatingsCount = stats.luizRatingsCount,
                    diffPoints = stats.diffPoints,
                )

                GroupConsensusCard(topMovies = stats.consensus)

                FavoriteGenresCard(
                    totalMovies = stats.totalList,
                    genres = stats.genres,
                )

                RandomMovieCard(
                    onPickRandomMovie = {
                        emptyQueueNotice = false
                        viewModel.pickRandom()
                    },
                    emptyQueueNotice = emptyQueueNotice,
                )
            }
        }
    }
}
