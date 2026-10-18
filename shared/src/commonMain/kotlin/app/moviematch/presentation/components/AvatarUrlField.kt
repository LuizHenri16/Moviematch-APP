package app.moviematch.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.moviematch.presentation.components.icons.IconLink
import app.moviematch.presentation.components.icons.iconSize
import app.moviematch.presentation.theme.Border
import app.moviematch.presentation.theme.Dimens
import app.moviematch.presentation.theme.SurfaceDeep
import app.moviematch.presentation.theme.TextMuted
import app.moviematch.presentation.theme.TextPrimary

/**
 * Pill-shaped URL input used to type an avatar image link.
 *
 * Used by the avatar form of the settings screen, which is the only place that takes a link: the
 * photo is typed here, saved through `ProfileRepository` and rendered by [Avatar]. Rendering is a
 * [BasicTextField] with a hand-made placeholder, mirroring the reference design system.
 */
@Composable
fun AvatarUrlField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val shape = RoundedCornerShape(Dimens.RadiusTile)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(SurfaceDeep, shape)
            .border(1.dp, Border, shape)
            .padding(horizontal = Dimens.Spacing16, vertical = Dimens.Spacing4),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing8),
    ) {
        IconLink(modifier = Modifier.iconSize(16.dp), color = TextMuted)

        Box(modifier = Modifier.weight(1f)) {
            if (value.isEmpty()) {
                Text(
                    text = "https://exemplo.com/foto.jpg",
                    modifier = Modifier.padding(vertical = Dimens.Spacing12),
                    fontSize = 14.sp,
                    color = TextMuted,
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = Dimens.Spacing12),
                enabled = enabled,
                singleLine = true,
                textStyle = TextStyle(color = TextPrimary, fontSize = 14.sp),
                cursorBrush = SolidColor(TextPrimary),
            )
        }
    }
}
