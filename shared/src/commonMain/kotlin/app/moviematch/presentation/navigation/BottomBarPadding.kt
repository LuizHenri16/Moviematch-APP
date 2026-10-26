package app.moviematch.presentation.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import app.moviematch.presentation.theme.Dimens

/**
 * Bottom padding a scrollable screen must reserve so its last item is not hidden behind the
 * [BottomBar].
 *
 * The MD3 navigation bar is edge-to-edge and consumes the system navigation bar inset inside the
 * component, so the reserved space is exactly: bar height + the window inset.
 */
@Composable
fun rememberBottomBarBottomPadding(): Dp {
    val systemBars = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    return Dimens.BottomBarContentHeight + systemBars
}

/** Ready-to-use `contentPadding` for a scrollable screen that lives under the bottom bar. */
@Composable
fun bottomBarContentPadding(): PaddingValues = PaddingValues(bottom = rememberBottomBarBottomPadding())
