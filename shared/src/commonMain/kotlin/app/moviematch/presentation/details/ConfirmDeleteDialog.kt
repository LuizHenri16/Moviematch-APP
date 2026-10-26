package app.moviematch.presentation.details

import androidx.compose.runtime.Composable
import app.moviematch.presentation.components.ConfirmDialog

/**
 * Destructive confirmation of "delete a title", built on the reusable [ConfirmDialog].
 *
 * It keeps its original call site (`MovieDetailsScreen`) and copy, while the layout, palette and
 * busy behaviour live in the shared component — a stock Material 3 `AlertDialog` in the `error`
 * role, so there is nothing Material-3-specific left to set here.
 */
@Composable
fun ConfirmDeleteDialog(
    movieTitle: String?,
    deleting: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    ConfirmDialog(
        title = "Apagar filme?",
        message = movieTitle?.let { "\"$it\" será removido da lista da roda. " } +
            "Essa ação não pode ser desfeita.",
        confirmLabel = "Apagar",
        busy = deleting,
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        danger = true,
    )
}
