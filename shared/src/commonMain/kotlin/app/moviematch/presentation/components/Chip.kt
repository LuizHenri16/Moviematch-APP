package app.moviematch.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import app.moviematch.presentation.theme.Dimens

/** Compact chip size: 24dp micro labels and badges over imagery, 32dp for filters. */
enum class ChipSize { Small, Default }

/** Semantic tone of a [Chip]. */
enum class ChipTone {
    /** `surfaceContainerHigh` with `onSurfaceVariant` — genres and generic labels. */
    Neutral,

    /** `tertiaryContainer` — media type / data family. */
    Lavender,

    /** `primaryContainer` — state and emphasis. */
    Accent,

    /** `errorContainer` — destructive state. */
    Danger,

    /** `surfaceContainerHighest` — badge pinned on top of a poster. */
    Category,
}

private data class ChipColors(val container: Color, val content: Color, val border: BorderStroke?)

private val ChipTone.colors: ChipColors
    @Composable get() = when (this) {
        ChipTone.Neutral -> ChipColors(
            container = MaterialTheme.colorScheme.surfaceContainerHigh,
            content = MaterialTheme.colorScheme.onSurfaceVariant,
            border = null,
        )

        ChipTone.Lavender -> ChipColors(
            container = MaterialTheme.colorScheme.tertiaryContainer,
            content = MaterialTheme.colorScheme.onTertiaryContainer,
            border = null,
        )

        ChipTone.Accent -> ChipColors(
            container = MaterialTheme.colorScheme.primaryContainer,
            content = MaterialTheme.colorScheme.onPrimaryContainer,
            border = null,
        )

        ChipTone.Danger -> ChipColors(
            container = MaterialTheme.colorScheme.errorContainer,
            content = MaterialTheme.colorScheme.onErrorContainer,
            border = null,
        )

        ChipTone.Category -> ChipColors(
            container = MaterialTheme.colorScheme.surfaceContainerHighest,
            content = MaterialTheme.colorScheme.onSurface,
            border = BorderStroke(Dimens.BorderWidth, MaterialTheme.colorScheme.outlineVariant),
        )
    }

/**
 * Material 3 chip.
 *
 * Unselected chips use a tonal container (`surfaceContainerHigh`); [selected] switches them to the
 * `secondaryContainer` role that MD3 FilterChip uses, with a heavier label. The chip is **not**
 * clickable on its own: selection semantics belong to the caller (`Modifier.selectable` /
 * `Modifier.toggleable`), which also supplies the ripple and the 48dp touch target.
 *
 * Uppercase is reserved for [ChipTone.Category]; every other label stays in sentence case.
 */
@Composable
fun Chip(
    label: String,
    modifier: Modifier = Modifier,
    size: ChipSize = ChipSize.Default,
    tone: ChipTone = ChipTone.Neutral,
    selected: Boolean = false,
    leadingIcon: (@Composable () -> Unit)? = null,
    @Suppress("UNUSED_PARAMETER") interactionSource: MutableInteractionSource? = null,
) {
    val palette = if (selected) {
        ChipColors(
            container = MaterialTheme.colorScheme.secondaryContainer,
            content = MaterialTheme.colorScheme.onSecondaryContainer,
            border = null,
        )
    } else {
        tone.colors
    }

    val height = if (size == ChipSize.Small) Dimens.ChipHeightSmall else Dimens.ChipHeightDefault
    val horizontalPadding = if (size == ChipSize.Small) Dimens.Spacing8 else Dimens.Spacing12
    val textStyle = if (size == ChipSize.Small) {
        MaterialTheme.typography.labelMedium
    } else {
        MaterialTheme.typography.labelLarge
    }
    val isCategory = tone == ChipTone.Category && !selected

    Surface(
        modifier = modifier.heightIn(min = height),
        shape = MaterialTheme.shapes.small,
        color = palette.container,
        contentColor = palette.content,
        border = palette.border,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = horizontalPadding, vertical = Dimens.Spacing4),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(
                space = Dimens.Spacing4,
                alignment = Alignment.CenterHorizontally,
            ),
        ) {
            leadingIcon?.invoke()
            Text(
                text = if (isCategory) label.uppercase() else label,
                style = textStyle.copy(
                    fontWeight = if (selected) FontWeight.SemiBold else textStyle.fontWeight,
                ),
                color = palette.content,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
