package app.moviematch.presentation.details

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import app.moviematch.domain.ratingVerdict
import app.moviematch.domain.round1
import app.moviematch.domain.model.ProfileName
import app.moviematch.presentation.common.formatDecimals1
import app.moviematch.presentation.common.formatDecimals2
import app.moviematch.presentation.components.Avatar
import app.moviematch.presentation.components.BentoTile
import app.moviematch.presentation.components.DialogButton
import app.moviematch.presentation.components.IconBadge
import app.moviematch.presentation.components.icons.IconFrown
import app.moviematch.presentation.components.icons.IconSize
import app.moviematch.presentation.components.icons.iconSize
import app.moviematch.presentation.theme.CardMetaStyle
import app.moviematch.presentation.theme.Dimens

/**
 * Rating dialog, ported from the reference `RatingModal`.
 *
 * Only the score of the member currently using the device is editable (the other one is shown for
 * context), the "Já assistiram juntos?" switch decides whether the title is also marked as watched,
 * and the footer previews the group average plus its verdict seal.
 *
 * Material 3: the sheet is a [Surface] in `surfaceContainerHigh` with the extra-large (28dp) corner,
 * floating over a `scrim` overlay. [BasicAlertDialog] is used because the form is taller than a
 * stock [androidx.compose.material3.AlertDialog] and needs to scroll, so it also asks for the full
 * platform width and caps itself at the MD3 dialog maximum (560dp).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RatingDialog(
    currentProfile: ProfileName,
    mariaScore: Double?,
    luizScore: Double?,
    mariaComment: String?,
    luizComment: String?,
    initiallyWatched: Boolean,
    onSave: (profile: ProfileName, score: Double, comment: String?, watchedTogether: Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    val myScore = if (currentProfile == ProfileName.MARIA) mariaScore else luizScore
    val otherScore = if (currentProfile == ProfileName.MARIA) luizScore else mariaScore
    val myComment = if (currentProfile == ProfileName.MARIA) mariaComment else luizComment

    var score by remember { mutableStateOf(myScore) }
    var comment by remember { mutableStateOf(myComment.orEmpty()) }
    var watchedTogether by remember { mutableStateOf(initiallyWatched) }

    val rated = listOfNotNull(score, otherScore)
    val average = if (rated.isEmpty()) null else rated.sum() / rated.size

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                )
                .padding(Dimens.Spacing24),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp)
                    // Consume taps inside the sheet so they never reach the dismissal scrim, without
                    // turning the whole dialog into a "clickable" node for screen readers.
                    .pointerInput(Unit) { detectTapGestures { } },
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurface,
                shadowElevation = Dimens.ElevationHero,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .imePadding()
                        .padding(Dimens.Spacing24)
                        .verticalScroll(rememberScrollState()),
                ) {
                    TogetherRow(
                        watchedTogether = watchedTogether,
                        onToggle = { watchedTogether = it },
                    )

                    Spacer(Modifier.height(Dimens.Spacing16))
                    HorizontalDivider(
                        thickness = Dimens.DividerWidth,
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                    Spacer(Modifier.height(Dimens.Spacing16))

                    RatingSliderCard(
                        profile = currentProfile,
                        score = score,
                        onScoreChange = { raw -> score = round1(raw) },
                    )

                    Spacer(Modifier.height(Dimens.Spacing12))

                    CommentField(
                        value = comment,
                        onValueChange = { comment = it },
                    )

                    Spacer(Modifier.height(Dimens.Spacing12))

                    AverageFooter(average = average)

                    Spacer(Modifier.height(Dimens.Spacing16))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(Dimens.Spacing8),
                    ) {
                        DialogButton(
                            label = "Cancelar",
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            borderColor = MaterialTheme.colorScheme.outlineVariant,
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        DialogButton(
                            label = "Salvar Avaliação",
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            borderColor = MaterialTheme.colorScheme.outlineVariant,
                            onClick = {
                                val current = score
                                if (current != null) {
                                    onSave(
                                        currentProfile,
                                        current,
                                        comment.trim().ifEmpty { null },
                                        watchedTogether,
                                    )
                                }
                            },
                            enabled = score != null,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TogetherRow(
    watchedTogether: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Dimens.Spacing4),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing12),
        ) {
            IconBadge(size = Dimens.IconBadgeMedium) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
            }

            Column {
                Text(
                    text = "Já assistiram juntos?",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = if (watchedTogether) {
                        "Sim! Avaliar a sessão agora"
                    } else {
                        "Não, assistiram separados"
                    },
                    style = CardMetaStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Switch(
            checked = watchedTogether,
            onCheckedChange = onToggle,
        )
    }
}

@Composable
private fun RatingSliderCard(
    profile: ProfileName,
    score: Double?,
    onScoreChange: (Double) -> Unit,
) {
    BentoTile(
        modifier = Modifier.fillMaxWidth(),
        padding = Dimens.Spacing16,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing8),
                ) {
                    Avatar(name = profile.displayName, size = Dimens.AvatarMedium)
                    Text(
                        text = "Nota d${if (profile == ProfileName.MARIA) "a" else "o"} " +
                            profile.displayName,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = score?.formatDecimals1() ?: "—",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = " /10",
                        style = CardMetaStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(Dimens.Spacing8))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing12),
            ) {
                IconFrown(
                    modifier = Modifier.iconSize(IconSize.Small),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Slider(
                    value = (score ?: 0.0).toFloat(),
                    onValueChange = { raw -> onScoreChange(raw.toDouble()) },
                    modifier = Modifier
                        .weight(1f)
                        .semantics {
                            contentDescription = "Nota de ${profile.displayName}"
                        },
                    enabled = true,
                    valueRange = 0f..10f,
                    steps = 99,
                )

                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(IconSize.Small),
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.Spacing24),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                listOf("0", "2.5", "5.0", "7.5", "10.0").forEach { step ->
                    Text(
                        text = step,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun CommentField(
    value: String,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Comentário" },
        label = { Text(text = "Deixe um comentário") },
        placeholder = {
            Text(text = "Como foi a sessão? Registre sua opinião sobre o filme…")
        },
        shape = MaterialTheme.shapes.extraSmall,
        minLines = 3,
        textStyle = MaterialTheme.typography.bodyMedium,
    )
}

@Composable
private fun AverageFooter(average: Double?) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = Dimens.Spacing16,
                vertical = Dimens.Spacing12,
            ),
            verticalArrangement = Arrangement.spacedBy(Dimens.Spacing12),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing12),
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = "Média da Galera",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing12),
            ) {
                Text(
                    text = average?.formatDecimals2() ?: "—",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ) {
                    Text(
                        text = ratingVerdict(average),
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(
                            horizontal = Dimens.Spacing12,
                            vertical = Dimens.Spacing8,
                        ),
                    )
                }
            }
        }
    }
}
