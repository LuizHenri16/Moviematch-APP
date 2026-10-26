package app.moviematch.presentation.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import app.moviematch.auth.AuthStore
import app.moviematch.config.Secrets
import app.moviematch.presentation.addmovie.AddMovieScreen
import app.moviematch.presentation.common.ContentSelectionStore
import app.moviematch.presentation.details.MovieDetailsScreen
import app.moviematch.presentation.group.GroupScreen
import app.moviematch.presentation.home.HomeScreen
import app.moviematch.presentation.movies.MoviesScreen
import app.moviematch.presentation.settings.SettingsScreen
import org.koin.compose.koinInject

/**
 * Shared application shell: the navigation graph plus the bottom navigation bar.
 *
 * [Routes.HOME], [Routes.MOVIES] and [Routes.ADD] are the tabs (the ones that keep the bar visible),
 * while [Routes.DETAILS], [Routes.SETTINGS] and [Routes.GROUP] are pushed destinations.
 *
 * Transitions are disabled on purpose: animating a swap between two full screens makes both of them
 * visible at the same time on device, so screens replace each other in a single frame. The bar is
 * tied to the composed destination instead of the back stack — see [visibleRoute].
 */
@Composable
fun AppShell(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val selectionStore = koinInject<ContentSelectionStore>()
    val authStore = koinInject<AuthStore>()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: Routes.HOME

    // Route of the destination that is actually composed and painted, which is *not* the route of the
    // back stack. Destinations report themselves (see [ReportVisibleRoute]), which keeps the bar in
    // step with the screen.
    var visibleRoute by remember { mutableStateOf(Routes.HOME) }

    // Destinations report themselves through this (see [ReportVisibleRoute]).
    val onVisibleRoute: (String) -> Unit = { route -> visibleRoute = route }

    // Header logout. It ends the real session and lets the gate in `App` swap this shell for the
    // login screen; no navigation happens here. Without Supabase credentials there is no session to
    // end, so the button is not offered at all (`null` hides it).
    val onLogout: (() -> Unit)? = if (Secrets.hasSupabaseConfig) {
        { authStore.signOut() }
    } else {
        null
    }

    Box(modifier = modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            // Navigation Compose would otherwise cross-fade every destination for 700ms.
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None },
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        ) {
            shellComposable(Routes.HOME, currentRoute, onVisibleRoute) {
                HomeScreen(
                    onOpenDetails = { item ->
                        selectionStore.select(item.id, item.type)
                        navController.navigate(Routes.DETAILS) { launchSingleTop = true }
                    },
                    onLogout = onLogout,
                )
            }

            shellComposable(Routes.MOVIES, currentRoute, onVisibleRoute) {
                MoviesScreen(
                    onOpenDetails = { entry ->
                        selectionStore.select(entry.item.id, entry.item.type)
                        navController.navigate(Routes.DETAILS) { launchSingleTop = true }
                    },
                    onAddMovie = { goToTab(navController, Routes.ADD) },
                    onLogout = onLogout,
                )
            }

            shellComposable(Routes.ADD, currentRoute, onVisibleRoute) {
                AddMovieScreen(onAdded = { goToTab(navController, Routes.MOVIES) })
            }

            shellComposable(Routes.DETAILS, currentRoute, onVisibleRoute) {
                MovieDetailsScreen(onBack = { navController.popBackStack() })
            }

            shellComposable(Routes.SETTINGS, currentRoute, onVisibleRoute) {
                SettingsScreen(
                    onBack = { navController.popBackStack() },
                    onOpenGroup = { navController.navigate(Routes.GROUP) { launchSingleTop = true } },
                )
            }

            shellComposable(Routes.GROUP, currentRoute, onVisibleRoute) {
                GroupScreen(onBack = { navController.popBackStack() })
            }
        }

        if (visibleRoute in BottomBarRoutes) {
            BottomBar(
                activeRoute = currentRoute,
                onNavigate = { route -> goToTab(navController, route) },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

/**
 * Switches between the top-level tabs without stacking destinations.
 *
 * The start destination stays under the stack and each tab keeps its own saved state (scroll
 * position, search query).
 */
private fun goToTab(navController: NavHostController, route: String) {
    navController.navigate(route) {
        popUpTo(Routes.HOME) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/**
 * Declares a screen of the shell.
 *
 * The canvas is painted here, per destination, instead of only on the `NavHost`: a screen being left
 * stays composed for a few frames, and without an opaque canvas of its own every gap of the arriving
 * screen would show the screen that is leaving through it.
 */
private fun NavGraphBuilder.shellComposable(
    route: String,
    currentRoute: String,
    onVisibleRoute: (String) -> Unit,
    content: @Composable () -> Unit,
) {
    composable(route = route) {
        ReportVisibleRoute(route, currentRoute, onVisibleRoute)
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            content()
        }
    }
}

/**
 * Reports [route] as the destination on screen, while it is the one being composed.
 *
 * The write happens during composition, before the bottom bar reads the state (the bar is the last
 * child of the shell `Box`). The [currentRoute] guard is there because the destination being left
 * stays composed for a while after the new one arrives: it must not claim the bar back.
 */
@Composable
private fun ReportVisibleRoute(route: String, currentRoute: String, onVisibleRoute: (String) -> Unit) {
    if (route == currentRoute) {
        onVisibleRoute(route)
    }
}
