package app.moviematch.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.moviematch.presentation.theme.Dimens

/** Tone of a [StateMessage]: regular copy or an error. */
enum class StateTone { Default, Error }

/**
 * Centred feedback block for the three non-content states (empty, loading, error).
 *
 * Follows the Material 3 semantics:
 * * **error** — an `errorContainer` surface with the `onErrorContainer` ink and a warning icon;
 * * **empty** — plain copy on the screen canvas with an optional action;
 * * **loading** — a small spinner plus copy, only while an action is in flight.
 *
 * ```kotlin
 * StateMessage(
 *     title = "Ops!",
 *     message = error,
 *     tone = StateTone.Error,
 *     action = { AppSmallButton("Tentar novamente", onClick = ::retry) },
 * )
 * ```
 */
@Composable
fun StateMessage(
    modifier: Modifier = Modifier,
    title: String? = null,
    message: String? = null,
    loading: Boolean = false,
    tone: StateTone = StateTone.Default,
    action: (@Composable () -> Unit)? = null,
) {
    if (tone == StateTone.Error) {
        Surface(
            modifier = modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ) {
            StateMessageBody(
                title = title,
                message = message,
                loading = loading,
                tone = tone,
                action = action,
            )
        }
    } else {
        StateMessageBody(
            modifier = modifier,
            title = title,
            message = message,
            loading = loading,
            tone = tone,
            action = action,
        )
    }
}

@Composable
private fun StateMessageBody(
    modifier: Modifier = Modifier,
    title: String? = null,
    message: String? = null,
    loading: Boolean = false,
    tone: StateTone = StateTone.Default,
    action: (@Composable () -> Unit)? = null,
) {
    val contentColor = when {
        loading -> MaterialTheme.colorScheme.onSurface
        tone == StateTone.Error -> MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.onSurface
    }
    val secondaryColor = when {
        tone == StateTone.Error -> MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = Dimens.Spacing24,
                vertical = Dimens.Spacing24 + Dimens.Spacing16,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Dimens.Spacing12),
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .padding(bottom = Dimens.Spacing4)
                    .size(28.dp),
                strokeWidth = 3.dp,
            )
        } else if (tone == StateTone.Error) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(28.dp),
            )
        }

        if (title != null) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = contentColor,
                textAlign = TextAlign.Center,
            )
        }

        if (message != null) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = secondaryColor,
                textAlign = TextAlign.Center,
            )
        }

        action?.invoke()
    }
}

/** Convenience wrapper for the loading state (spinner + copy). */
@Composable
fun StateLoading(message: String? = null) {
    StateMessage(loading = true, title = "Carregando…", message = message)
}

/** Convenience wrapper for the empty state, with an optional call to action. */
@Composable
fun StateEmpty(
    title: String,
    message: String? = null,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    StateMessage(
        modifier = modifier,
        title = title,
        message = message,
        action = action,
    )
}

/** Convenience wrapper for the error state, including the retry button. */
@Composable
fun StateError(message: String, onRetry: (() -> Unit)? = null) {
    StateMessage(
        title = "Ops!",
        message = message,
        tone = StateTone.Error,
        action = onRetry?.let { retry -> { AppSmallButton("Tentar novamente", onClick = retry) } },
    )
}
