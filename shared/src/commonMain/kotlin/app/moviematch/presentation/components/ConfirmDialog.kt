package app.moviematch.presentation.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * Confirmation dialog for an action that cannot be undone.
 *
 * A stock Material 3 [AlertDialog]: 28dp extra-large corners, `surfaceContainerHigh` container,
 * centred icon/title/text and text buttons. The dialog stays open (and cannot be dismissed) while
 * [busy] is true, so the person always sees the outcome before the screen reacts.
 *
 * @param title short question, e.g. "Apagar filme?".
 * @param message consequence of the action.
 * @param confirmLabel label of the confirming button.
 * @param busy `true` while the action is in flight; locks the dialog and shows a spinner.
 * @param danger `true` paints the action in the `error` role; `false` uses `primary`.
 */
@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    busy: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    danger: Boolean = true,
) {
    val icon: ImageVector = if (danger) Icons.Default.Delete else Icons.Default.Check
    val iconColor = if (danger) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.primary
    }

    val confirmContentColor = if (danger) {
        MaterialTheme.colorScheme.onError
    } else {
        MaterialTheme.colorScheme.onPrimary
    }

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(24.dp),
            )
        },
        title = { Text(text = title) },
        text = { Text(text = message) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !busy,
                shape = MaterialTheme.shapes.extraLarge,
                colors = if (danger) {
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    )
                } else {
                    ButtonDefaults.buttonColors()
                },
            ) {
                if (busy) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = confirmContentColor,
                    )
                } else {
                    Text(text = confirmLabel)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !busy) {
                Text(text = "Cancelar")
            }
        },
    )
}

/**
 * Dialog action of the rating and confirmation flows.
 *
 * Kept as a thin wrapper over the MD3 [Button] so callers can keep choosing a semantic container
 * (`error`, `primary`, `secondaryContainer`). While [loading] the label is replaced by a spinner
 * without changing the button width.
 */
@Composable
internal fun DialogButton(
    label: String,
    containerColor: Color,
    contentColor: Color,
    borderColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    icon: ImageVector? = null,
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = MaterialTheme.shapes.extraLarge,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor.takeOrElse { MaterialTheme.colorScheme.primary },
            contentColor = contentColor.takeOrElse { MaterialTheme.colorScheme.onPrimary },
        ),
    ) {
        when {
            loading -> CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = contentColor.takeOrElse { MaterialTheme.colorScheme.onPrimary },
            )

            else -> {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    androidx.compose.foundation.layout.Spacer(Modifier.size(8.dp))
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                )
            }
        }
    }
}
