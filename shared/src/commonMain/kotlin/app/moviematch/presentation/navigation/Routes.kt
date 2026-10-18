package app.moviematch.presentation.navigation

import androidx.compose.ui.unit.dp

/**
 * Route names of the shared navigation graph.
 *
 * They are deliberately argument-free: `navigation-compose` arguments resolve to a platform
 * `SavedState`/`Bundle`, which is not reachable from `commonMain`. The title a detail screen must
 * show travels through `ContentSelectionStore` instead — see that class for the full rationale.
 */
object Routes {
    const val HOME = "home"
    const val MOVIES = "movies"
    const val ADD = "add"
    const val DETAILS = "details"
    const val SETTINGS = "settings"
}

/** Destinations that keep the floating bottom bar visible. */
val BottomBarRoutes: Set<String> = setOf(Routes.HOME, Routes.MOVIES, Routes.ADD)

/**
 * Extra breathing room between the last scrollable item and the floating bar.
 *
 * The bar height itself lives in [app.moviematch.presentation.theme.Dimens.BottomBarContentHeight].
 */
val BottomBarSpacing = 16.dp
