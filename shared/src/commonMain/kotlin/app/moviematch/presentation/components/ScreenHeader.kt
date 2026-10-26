package app.moviematch.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import app.moviematch.presentation.theme.Dimens

/**
 * Screen header built on the Material 3 [TopAppBar].
 *
 * [navigationIcon] always sits on the leading edge (back/close) and [action] on the trailing edge, so
 * every pushed and tab screen shares the same geometry and the same MD3 content colours.
 *
 * The bar is transparent over the screen canvas; [floating] switches it to the `surfaceContainer`
 * tone (MD3 "on-scroll" app bar) for screens whose content scrolls underneath. [showRule] closes it
 * with the standard 1dp `outlineVariant` divider.
 *
 * @param subtitle optional supporting line under the title — MD3 has no subtitle slot, so it is part
 *   of the title slot with the `bodySmall` role.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    showRule: Boolean = false,
    floating: Boolean = false,
    navigationIcon: (@Composable () -> Unit)? = null,
    action: (@Composable () -> Unit)? = null,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        TopAppBar(
            title = {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            navigationIcon = { navigationIcon?.invoke() },
            actions = { action?.invoke() },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = if (floating) {
                    MaterialTheme.colorScheme.surfaceContainer
                } else {
                    Color.Transparent
                },
                titleContentColor = MaterialTheme.colorScheme.onSurface,
                navigationIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
            // Screens already consume the status bar inset (`Modifier.statusBarsPadding()`), so the
            // bar must not add it a second time.
            windowInsets = WindowInsets(0, 0, 0, 0),
        )

        // MD3 top app bars have a single title slot. The supporting line is rendered just below the
        // bar instead of inside it, so a long title never clips against the 64dp container.
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(
                    start = Dimens.ScreenPadding,
                    end = Dimens.ScreenPadding,
                    bottom = Dimens.Spacing12,
                ),
            )
        }

        if (showRule) {
            HorizontalDivider(
                thickness = Dimens.DividerWidth,
                color = MaterialTheme.colorScheme.outlineVariant,
            )
        }
    }
}
