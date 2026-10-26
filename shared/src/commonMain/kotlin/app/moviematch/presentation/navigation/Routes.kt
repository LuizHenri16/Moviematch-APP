package app.moviematch.presentation.navigation

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
    const val GROUP = "group"
}

/** Destinations that keep the bottom navigation bar visible. */
val BottomBarRoutes: Set<String> = setOf(Routes.HOME, Routes.MOVIES, Routes.ADD)
