package app.moviematch.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.moviematch.presentation.theme.Dimens

/**
 * Material 3 button roles of the app.
 *
 * * [ButtonVariant.Primary] — filled button (`primary` / `onPrimary`): the main call to action.
 * * [ButtonVariant.Accent]  — filled button in the `secondary` role: action of a secondary flow.
 * * [ButtonVariant.Outline] — outlined button: alternative action next to a filled one.
 * * [ButtonVariant.Danger]  — filled button in the `error` role: destructive confirmation.
 *
 * All four are 40dp tall with the MD3 full corner, a state layer (pressed/focus/hover) supplied by
 * the component itself and a minimum 48dp touch target supplied by the layout around them.
 */
enum class ButtonVariant { Primary, Accent, Outline, Danger }

@Composable
fun AppButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ButtonVariant = ButtonVariant.Primary,
    enabled: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null,
) {
    val shape = RoundedCornerShape(Dimens.RadiusPill)
    val contentPadding = PaddingValues(horizontal = Dimens.Spacing24, vertical = Dimens.Spacing8)
    val content: @Composable () -> Unit = {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing8),
        ) {
            leadingIcon?.invoke()
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
            )
        }
    }

    val buttonModifier = modifier.heightIn(min = Dimens.ButtonHeight)

    when (variant) {
        ButtonVariant.Outline -> OutlinedButton(
            onClick = onClick,
            modifier = buttonModifier,
            enabled = enabled,
            shape = shape,
            colors = ButtonDefaults.outlinedButtonColors(),
            contentPadding = contentPadding,
            content = { content() },
        )

        else -> Button(
            onClick = onClick,
            modifier = buttonModifier,
            enabled = enabled,
            shape = shape,
            colors = when (variant) {
                ButtonVariant.Accent -> ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary,
                )

                ButtonVariant.Danger -> ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                )

                else -> ButtonDefaults.buttonColors()
            },
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
            contentPadding = contentPadding,
            content = { content() },
        )
    }
}

/**
 * Compact action of a card header ("Sortear Filme", "Tentar novamente").
 *
 * A MD3 [FilledTonalButton] (`secondaryContainer` / `onSecondaryContainer`) by default. Callers that
 * need another role pass [containerColor] and [contentColor]; leaving them [Color.Unspecified] keeps
 * the tonal defaults.
 */
@Composable
fun AppSmallButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = Color.Unspecified,
    contentColor: Color = Color.Unspecified,
    enabled: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null,
) {
    val colors = if (containerColor == Color.Unspecified && contentColor == Color.Unspecified) {
        ButtonDefaults.filledTonalButtonColors()
    } else {
        ButtonDefaults.filledTonalButtonColors(
            containerColor = containerColor.takeOrElse { MaterialTheme.colorScheme.secondaryContainer },
            contentColor = contentColor.takeOrElse { MaterialTheme.colorScheme.onSecondaryContainer },
        )
    }

    FilledTonalButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = Dimens.ButtonHeight),
        enabled = enabled,
        shape = RoundedCornerShape(Dimens.RadiusPill),
        colors = colors,
        contentPadding = PaddingValues(horizontal = Dimens.Spacing16, vertical = Dimens.Spacing8),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing8),
        ) {
            leadingIcon?.invoke()
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
            )
        }
    }
}
