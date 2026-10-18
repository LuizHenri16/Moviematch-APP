package app.moviematch.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.moviematch.presentation.theme.Accent
import app.moviematch.presentation.theme.Danger
import app.moviematch.presentation.theme.Dimens
import app.moviematch.presentation.theme.TextPrimary

/** Tone of a [StateMessage]: regular copy or an error. */
enum class StateTone { Default, Error }

/**
 * Centred feedback block used for the three non-content states.
 *
 * Mirrors `StateMessage` from the reference design system: an optional spinner (only for *actions*
 * in flight), a title and a message, plus a slot for a retry button.
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
                modifier = Modifier.padding(bottom = Dimens.Spacing4),
                color = Accent,
                strokeWidth = 2.dp,
            )
        }

        if (title != null) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
                textAlign = TextAlign.Center,
            )
        }

        if (message != null) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = if (tone == StateTone.Error) Danger else MaterialTheme.colorScheme.onSurfaceVariant,
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

/** Convenience wrapper for the empty state. */
@Composable
fun StateEmpty(title: String, message: String? = null) {
    StateMessage(title = title, message = message)
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
