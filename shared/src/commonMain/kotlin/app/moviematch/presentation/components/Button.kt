package app.moviematch.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.moviematch.presentation.theme.Accent
import app.moviematch.presentation.theme.Border
import app.moviematch.presentation.theme.Dimens
import app.moviematch.presentation.theme.TextOnAccent
import app.moviematch.presentation.theme.TextPrimary
import app.moviematch.presentation.theme.White

/**
 * Pill action button, mirroring the three variants of the reference `Button` component:
 * * [ButtonVariant.Primary] — solid white fill with deep slate text (the main call to action).
 * * [ButtonVariant.Accent]  — solid sage/mint fill (affirmative actions inside pastel bento blocks).
 * * [ButtonVariant.Outline] — dark container with a hairline border (secondary actions).
 */
enum class ButtonVariant { Primary, Accent, Outline }

@Composable
fun AppButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ButtonVariant = ButtonVariant.Primary,
    enabled: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null,
) {
    val contentColor = when (variant) {
        ButtonVariant.Primary, ButtonVariant.Accent -> TextOnAccent
        ButtonVariant.Outline -> TextPrimary
    }

    val content: @Composable RowScope.() -> Unit = {
        if (leadingIcon != null) {
            leadingIcon()
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = contentColor,
        )
    }

    when (variant) {
        ButtonVariant.Primary, ButtonVariant.Accent -> Button(
            onClick = onClick,
            modifier = modifier.height(Dimens.ButtonHeight),
            enabled = enabled,
            shape = MaterialTheme.shapes.extraLarge,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (variant == ButtonVariant.Primary) White else Accent,
                contentColor = contentColor,
                disabledContainerColor = (if (variant == ButtonVariant.Primary) White else Accent)
                    .copy(alpha = 0.4f),
                disabledContentColor = TextOnAccent.copy(alpha = 0.6f),
            ),
            contentPadding = PaddingValues(horizontal = Dimens.Spacing24),
            content = content,
        )

        ButtonVariant.Outline -> OutlinedButton(
            onClick = onClick,
            modifier = modifier.height(Dimens.ButtonHeight),
            enabled = enabled,
            shape = MaterialTheme.shapes.extraLarge,
            border = BorderStroke(1.dp, Border),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = contentColor),
            contentPadding = PaddingValues(horizontal = Dimens.Spacing24),
            content = content,
        )
    }
}

/** Compact pill used for inline actions such as "Sortear Filme" or "Tentar novamente". */
@Composable
fun AppSmallButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = White,
    contentColor: Color = TextOnAccent,
    leadingIcon: (@Composable () -> Unit)? = null,
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
        colors = ButtonDefaults.buttonColors(containerColor = containerColor, contentColor = contentColor),
        contentPadding = PaddingValues(horizontal = Dimens.Spacing16, vertical = Dimens.Spacing8),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing8),
        ) {
            if (leadingIcon != null) {
                leadingIcon()
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = contentColor,
            )
        }
    }
}
