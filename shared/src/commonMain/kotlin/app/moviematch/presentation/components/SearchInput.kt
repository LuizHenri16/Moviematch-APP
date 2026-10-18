package app.moviematch.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import app.moviematch.presentation.components.icons.IconSize
import app.moviematch.presentation.theme.Border
import app.moviematch.presentation.theme.Dimens
import app.moviematch.presentation.theme.Surface
import app.moviematch.presentation.theme.TextMuted
import app.moviematch.presentation.theme.TextPrimary

/**
 * Pill search field (48dp, dark surface, leading magnifier) mirroring `SearchInput` from the
 * reference design system. [trailing] is where the debounce spinner or a clear button goes.
 */
@Composable
fun SearchInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    iconColor: Color = TextMuted,
    trailing: @Composable (() -> Unit)? = null,
) {
    val shape = MaterialTheme.shapes.extraLarge
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(Dimens.SearchInputHeight)
            .background(Surface, shape)
            .border(1.dp, Border, shape)
            .padding(horizontal = Dimens.Spacing16),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing8),
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(IconSize.Small + 2.dp),
        )

        Box {
            if (value.isEmpty() && placeholder.isNotEmpty()) {
                Text(
                    text = placeholder,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted,
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = LocalTextStyle.current.merge(
                    MaterialTheme.typography.bodyMedium.copy(color = TextPrimary),
                ),
                cursorBrush = SolidColor(TextPrimary),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (trailing != null) {
            trailing()
        }
    }
}
