package app.moviematch.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.moviematch.presentation.theme.Dimens
import app.moviematch.presentation.theme.ScreenSubtitleStyle
import app.moviematch.presentation.theme.ScreenTitleStyle
import app.moviematch.presentation.theme.TextMuted
import app.moviematch.presentation.theme.TextPrimary

/**
 * Screen title block used by the three tab screens: a 22sp title over a muted 11sp caption, with an
 * optional trailing action (the header icon button). Mirrors `topHeader` of the reference screens.
 */
@Composable
fun ScreenHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = Dimens.ScreenPadding,
                vertical = Dimens.Spacing16,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = ScreenTitleStyle,
                color = TextPrimary,
            )
            Text(
                text = subtitle,
                style = ScreenSubtitleStyle,
                color = TextMuted,
            )
        }

        action?.invoke()
    }
}
