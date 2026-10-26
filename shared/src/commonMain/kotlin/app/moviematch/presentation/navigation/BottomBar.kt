package app.moviematch.presentation.navigation

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.moviematch.presentation.components.icons.IconFilm
import app.moviematch.presentation.components.icons.IconSize
import app.moviematch.presentation.components.icons.iconSize
import app.moviematch.presentation.theme.Dimens

/**
 * Primary navigation, built on the Material 3 [NavigationBar].
 *
 * Each destination is a [NavigationBarItem] with an icon and a label, which gives the bar the MD3
 * indicator (a `secondaryContainer` pill behind the active icon), the MD3 state layers and the
 * mandatory 48dp touch targets. `Perfil` opens the pushed settings destination, so it never becomes
 * the selected item.
 */
@Composable
fun BottomBar(
    activeRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBar(
        modifier = modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        NavigationBarItem(
            selected = activeRoute == Routes.HOME,
            onClick = { onNavigate(Routes.HOME) },
            icon = {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = null,
                    modifier = Modifier.iconSize(IconSize.Large),
                )
            },
            label = { Text(text = "Home") },
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
            ),
        )

        NavigationBarItem(
            selected = activeRoute == Routes.ADD,
            onClick = { onNavigate(Routes.ADD) },
            icon = {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.iconSize(IconSize.Large),
                )
            },
            label = { Text(text = "Adicionar") },
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
            ),
        )

        NavigationBarItem(
            selected = activeRoute == Routes.MOVIES,
            onClick = { onNavigate(Routes.MOVIES) },
            icon = {
                // No explicit colour: the icon inherits the item content colour, so the selected
                // destination gets `onSecondaryContainer` like the Material icons do.
                IconFilm(modifier = Modifier.iconSize(IconSize.Large))
            },
            label = { Text(text = "Filmes") },
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
            ),
        )

        // Pushed destination: never selected, but still a first-class navigation entry.
        NavigationBarItem(
            selected = false,
            onClick = { onNavigate(Routes.SETTINGS) },
            icon = {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.iconSize(IconSize.Large),
                )
            },
            label = { Text(text = "Perfil") },
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
            ),
        )
    }
}
