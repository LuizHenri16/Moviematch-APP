package app.moviematch.presentation.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.moviematch.presentation.components.icons.IconLink
import app.moviematch.presentation.components.icons.iconSize
import androidx.compose.ui.unit.dp

/**
 * URL input of the avatar form.
 *
 * A MD3 [OutlinedTextField] with a leading link icon and a label, so the field explains itself
 * without a separate heading. Error and disabled styling come from the component defaults.
 */
@Composable
fun AvatarUrlField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        singleLine = true,
        label = { Text(text = "URL da foto") },
        placeholder = { Text(text = "https://exemplo.com/foto.jpg") },
        leadingIcon = {
            IconLink(
                modifier = Modifier.iconSize(20.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        // MD3 outlined text fields use the extra-small (4dp) shape. The pill corner is reserved for
        // the search bar, which is a different component.
        shape = MaterialTheme.shapes.extraSmall,
        textStyle = MaterialTheme.typography.bodyLarge,
        colors = OutlinedTextFieldDefaults.colors(),
    )
}
