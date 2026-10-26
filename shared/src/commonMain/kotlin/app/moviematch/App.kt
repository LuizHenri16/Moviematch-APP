package app.moviematch

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.moviematch.auth.AuthState
import app.moviematch.auth.AuthStore
import app.moviematch.config.Secrets
import app.moviematch.presentation.common.InstallAppImageLoader
import app.moviematch.presentation.login.LoginScreen
import app.moviematch.presentation.navigation.AppShell
import app.moviematch.presentation.theme.MovieMatchTheme
import org.koin.compose.koinInject

/**
 * Shared UI entry point, rendered by every platform:
 * * Android -> `MainActivity.setContent { App() }`
 * * iOS     -> `MainViewController()` (SwiftUI wraps it via `UIViewControllerRepresentable`)
 *
 * It installs the theme and the image loader, then hands the screen over to one of two layers:
 * * the **login gate**, which is the door of the app while there is no session;
 * * [AppShell], which hosts the tabs and the group screen (reachable from Settings).
 *
 * The gate is skipped entirely when `.env` has no Supabase credentials: a clean clone or a CI build
 * cannot sign in, and trapping the app on a login screen that cannot possibly succeed would be worse
 * than the offline behaviour (Home with the seeded profile) it has today.
 */
@Composable
fun App() {
    InstallAppImageLoader()

    MovieMatchTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            if (Secrets.hasSupabaseConfig) {
                SessionGate()
            } else {
                AppShell()
            }
        }
    }
}

/**
 * Decides between the login screen and the app.
 *
 * `SignedOut` / `SigningIn` / `Error` mean "no session", so [LoginScreen] is the whole app;
 * `SignedIn` hands over to [AppShell]. Nothing else has to navigate: signing out from anywhere
 * drops the state back to `SignedOut`, and this gate swaps the shell for the login screen on its own.
 *
 * The group onboarding is not a gate anymore: a signed-in member lands on [AppShell] and reaches the
 * "roda" from Settings. The shared group state lives in the application-scoped `GroupStore`.
 */
@Composable
private fun SessionGate() {
    val authStore = koinInject<AuthStore>()
    val authState by authStore.state.collectAsStateWithLifecycle()

    if (authState is AuthState.SignedIn) {
        AppShell()
    } else {
        LoginScreen()
    }
}
