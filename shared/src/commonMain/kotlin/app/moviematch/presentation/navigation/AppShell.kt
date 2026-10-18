package app.moviematch.presentation.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import app.moviematch.presentation.addmovie.AddMovieScreen
import app.moviematch.presentation.common.ContentSelectionStore
import app.moviematch.presentation.details.MovieDetailsScreen
import app.moviematch.presentation.home.HomeScreen
import app.moviematch.presentation.movies.MoviesScreen
import app.moviematch.presentation.settings.SettingsScreen
import app.moviematch.presentation.theme.Background
import org.koin.compose.koinInject

/**
 * Shared application shell: the navigation graph plus the floating bottom bar.
 *
 * The reference app keeps the three main tabs mounted in a single screen and pushes the detail and
 * settings screens on top of it. Here the same behaviour is expressed with `navigation-compose`:
 * [Routes.HOME], [Routes.MOVIES] and [Routes.ADD] are the tabs (the ones that keep the bar visible),
 * while [Routes.DETAILS] and [Routes.SETTINGS] are pushed destinations.
 */
@Composable
fun AppShell(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val selectionStore = koinInject<ContentSelectionStore>()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: Routes.HOME

    // Shared backdrop state. The content layer is marked as the Haze *source* and the floating bar
    // blurs it — the multiplatform equivalent of the reference app's native `BlurView`.
    val hazeState = rememberHazeState()

    Box(modifier = modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier
                .fillMaxSize()
                .background(Background)
                // The canvas lives on the content layer (not on the outer Box) so the bar's blur
                // samples a fully painted backdrop instead of a transparent layer.
                .hazeSource(hazeState),
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    onOpenDetails = { item ->
                        selectionStore.select(item.id, item.type)
                        navController.navigate(Routes.DETAILS) { launchSingleTop = true }
                    },
                    onLogout = { navController.navigate(Routes.SETTINGS) },
                )
            }

            composable(Routes.MOVIES) {
                MoviesScreen(
                    onOpenDetails = { entry ->
                        selectionStore.select(entry.item.id, entry.item.type)
                        navController.navigate(Routes.DETAILS) { launchSingleTop = true }
                    },
                    onLogout = { navController.navigate(Routes.SETTINGS) },
                )
            }

            composable(Routes.ADD) {
                AddMovieScreen(onAdded = { goToTab(navController, Routes.MOVIES) })
            }

            composable(Routes.DETAILS) {
                MovieDetailsScreen(onBack = { navController.popBackStack() })
            }

            composable(Routes.SETTINGS) {
                SettingsScreen(onBack = { navController.popBackStack() })
            }
        }

        if (currentRoute in BottomBarRoutes) {
            BottomBar(
                activeRoute = currentRoute,
                hazeState = hazeState,
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
 * position, search query), mirroring the always-mounted tabs of the reference `AppShell`.
 */
private fun goToTab(navController: NavHostController, route: String) {
    navController.navigate(route) {
        popUpTo(Routes.HOME) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
