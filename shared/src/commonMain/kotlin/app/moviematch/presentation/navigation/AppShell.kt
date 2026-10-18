package app.moviematch.presentation.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import app.moviematch.presentation.addmovie.AddMovieScreen
import app.moviematch.auth.AuthStore
import app.moviematch.config.Secrets
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
 *
 * Transitions are disabled on purpose, for every destination. The reference stack is configured with
 * `animation: 'none'` (`App.tsx`) and the reference tabs swap with `display` plus a 160ms opacity
 * fade (`AppShell.tsx`); the fade was dropped here because animating a swap between two full screens
 * is what makes both of them visible at the same time on device. Screens now replace each other in a
 * single frame, which is also why the bottom bar is tied to the composed destination instead of the
 * back stack — see [visibleRoute].
 */
@Composable
fun AppShell(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val selectionStore = koinInject<ContentSelectionStore>()
    val authStore = koinInject<AuthStore>()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: Routes.HOME

    // Route of the destination that is actually composed and painted, which is *not* the route of the
    // back stack. That one changes within a millisecond of the tap, while the incoming screen is only
    // composed ~110ms later and the outgoing one is still drawn after that (both measured on device).
    // Driving the bar from the back stack therefore painted the bar of the arriving screen over the
    // screen that was still leaving — and, on the way back, the tab bar over the pushed screen.
    // Destinations report themselves instead (see [ReportVisibleRoute]), which keeps the bar in step
    // with the screen.
    var visibleRoute by remember { mutableStateOf(Routes.HOME) }

    // Destinations report themselves through this (see [ReportVisibleRoute]).
    val onVisibleRoute: (String) -> Unit = { route -> visibleRoute = route }

    // Header logout. It ends the real session and lets the gate in `App` swap this shell for the
    // login screen; no navigation happens here. `signOut()` is fire-and-forget by design: it owns an
    // application-scoped coroutine and reports failures through `AuthState.Error`, so the button has
    // nothing to await and no exception to handle.
    //
    // Without Supabase credentials there is no session to end, so the button is not offered at all
    // (`null` hides it) instead of sitting there doing nothing. Settings stays reachable through the
    // "Perfil" tab of the bottom bar.
    val onLogout: (() -> Unit)? = if (Secrets.hasSupabaseConfig) {
        { authStore.signOut() }
    } else {
        null
    }

    // Shared backdrop state. The content layer is marked as the Haze *source* and the floating bar
    // blurs it — the multiplatform equivalent of the reference app's native `BlurView`.
    val hazeState = rememberHazeState()

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
                .background(Background)
                // The canvas lives on the content layer (not on the outer Box) so the bar's blur
                // samples a fully painted backdrop instead of a transparent layer.
                .hazeSource(hazeState),
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
                SettingsScreen(onBack = { navController.popBackStack() })
            }
        }

        if (visibleRoute in BottomBarRoutes) {
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

/**
 * Declares a screen of the shell.
 *
 * Both the tabs and the pushed screens use it: they differ only in the route they declare, which is
 * what decides whether the bottom bar stays visible ([BottomBarRoutes]).
 *
 * The canvas is painted here, per destination, instead of only on the `NavHost`. `AnimatedContent`
 * keeps the screen being left composed under the one arriving for a few hundred milliseconds while
 * it settles, and the screens themselves are transparent `Column`s — without an opaque canvas of its
 * own, every gap of the arriving screen (a loading skeleton, for instance) would show the screen
 * that is leaving through it, which reads as "both screens at once".
 */
private fun NavGraphBuilder.shellComposable(
    route: String,
    currentRoute: String,
    onVisibleRoute: (String) -> Unit,
    content: @Composable () -> Unit,
) {
    composable(route = route) {
        ReportVisibleRoute(route, currentRoute, onVisibleRoute)
        Box(modifier = Modifier.fillMaxSize().background(Background)) {
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
