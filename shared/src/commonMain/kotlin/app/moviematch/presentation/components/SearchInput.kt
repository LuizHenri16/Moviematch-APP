package app.moviematch.presentation.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import app.moviematch.presentation.theme.Dimens

/**
 * Search field of the catalogue screens.
 *
 * A MD3 [OutlinedTextField] with the search icon, a single line, a 56dp minimum height and a
 * pill-shaped outline. [trailing] is where the debounce spinner or the clear button goes; the field
 * itself owns focus, error and disabled colours through [OutlinedTextFieldDefaults].
 */
@Composable
fun SearchInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    iconColor: Color = Color.Unspecified,
    trailing: @Composable (() -> Unit)? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = Dimens.SearchInputHeight),
        placeholder = {
            if (placeholder.isNotEmpty()) {
                Text(
                    text = placeholder,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = iconColor.takeOrElse { MaterialTheme.colorScheme.onSurfaceVariant },
            )
        },
        trailingIcon = trailing,
        singleLine = true,
        shape = CircleShape,
        textStyle = MaterialTheme.typography.bodyLarge,
        colors = OutlinedTextFieldDefaults.colors(),
    )
}
