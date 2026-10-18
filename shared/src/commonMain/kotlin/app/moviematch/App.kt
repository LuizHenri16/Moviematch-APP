package app.moviematch

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.moviematch.presentation.navigation.AppShell
import app.moviematch.presentation.theme.MovieMatchTheme

/**
 * Shared UI entry point, rendered by every platform:
 * * Android -> `MainActivity.setContent { App() }`
 * * iOS     -> `MainViewController()` (SwiftUI wraps it via `UIViewControllerRepresentable`)
 *
 * It only installs the theme and hands the screen over to [AppShell], which owns the navigation
 * graph and the floating bottom bar.
 */
@Composable
fun App() {
    MovieMatchTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            AppShell()
        }
    }
}
