package app.moviematch.presentation.details

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.moviematch.domain.ratingVerdict
import app.moviematch.domain.round1
import app.moviematch.domain.model.ProfileName
import app.moviematch.presentation.common.formatDecimals1
import app.moviematch.presentation.common.formatDecimals2
import app.moviematch.presentation.components.AppButton
import app.moviematch.presentation.components.Avatar
import app.moviematch.presentation.components.BentoTile
import app.moviematch.presentation.components.icons.IconFrown
import app.moviematch.presentation.components.icons.IconSize
import app.moviematch.presentation.components.icons.iconSize
import app.moviematch.presentation.theme.Accent
import app.moviematch.presentation.theme.Border
import app.moviematch.presentation.theme.Dimens
import app.moviematch.presentation.theme.Outline
import app.moviematch.presentation.theme.Surface
import app.moviematch.presentation.theme.SurfaceDeep
import app.moviematch.presentation.theme.TextMuted
import app.moviematch.presentation.theme.TextOnAccent
import app.moviematch.presentation.theme.TextPrimary
import app.moviematch.presentation.theme.TextSecondary
import app.moviematch.presentation.theme.TextSoft

/**
 * Rating dialog, ported from the reference `RatingModal`.
 *
 * Only the score of the member currently using the device is editable (the other one is shown for
 * context), the "Já assistiram juntos?" switch decides whether the title is also marked as watched,
 * and the footer previews the group average plus its verdict seal.
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

    BasicAlertDialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 520.dp)
                .clip(RoundedCornerShape(Dimens.RadiusCard))
                .background(Surface)
                .border(1.dp, Border, RoundedCornerShape(Dimens.RadiusCard))
                .padding(Dimens.Spacing24)
                .verticalScroll(rememberScrollState()),
        ) {
            TogetherRow(
                watchedTogether = watchedTogether,
                onToggle = { watchedTogether = it },
            )

            Spacer(Modifier.height(Dimens.Spacing16))
            Divider()
            Spacer(Modifier.height(Dimens.Spacing16))

            RatingSliderCard(
                profile = currentProfile,
                score = score,
                onScoreChange = { raw -> score = round1(raw) },
            )

            Spacer(Modifier.height(14.dp))

            Text(
                text = "Deixe um comentário",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextSecondary,
            )
            Spacer(Modifier.height(Dimens.Spacing8))

            CommentField(
                value = comment,
                onValueChange = { comment = it },
            )

            Spacer(Modifier.height(14.dp))

            AverageFooter(average = average)

            Spacer(Modifier.height(Dimens.Spacing16))

            AppButton(
                label = "Salvar Avaliação",
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
                modifier = Modifier.fillMaxWidth(),
                enabled = score != null,
            )
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
            Box(
                modifier = Modifier
                    .size(Dimens.IconBadgeMedium)
                    .clip(CircleShape)
                    .background(Surface),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp),
                )
            }

            Column {
                Text(
                    text = "Já assistiram juntos?",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                )
                Text(
                    text = if (watchedTogether) {
                        "Sim! Avaliar a sessão agora"
                    } else {
                        "Não, assistiram separados"
                    },
                    fontSize = 12.sp,
                    color = TextSecondary,
                )
            }
        }

        Switch(
            checked = watchedTogether,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = TextOnAccent,
                checkedTrackColor = Accent,
                uncheckedThumbColor = TextSecondary,
                uncheckedTrackColor = Outline,
            ),
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
        containerColor = Surface,
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
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                    )
                }

                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = score?.formatDecimals1() ?: "—",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary,
                    )
                    Spacer(Modifier.height(1.dp))
                    Text(
                        text = " /10",
                        fontSize = 12.sp,
                        color = TextMuted,
                    )
                }
            }

            Spacer(Modifier.height(Dimens.Spacing8))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                IconFrown(modifier = Modifier.iconSize(IconSize.Small), color = TextMuted)

                Slider(
                    value = (score ?: 0.0).toFloat(),
                    onValueChange = { raw -> onScoreChange(raw.toDouble()) },
                    modifier = Modifier.weight(1f),
                    enabled = true,
                    valueRange = 0f..10f,
                    steps = 99,
                )

                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = TextPrimary,
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
                        fontSize = 11.sp,
                        color = TextMuted,
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
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 88.dp)
            .clip(shape)
            .background(Surface, shape)
            .border(1.dp, Border, shape)
            .padding(horizontal = Dimens.Spacing16, vertical = Dimens.Spacing12),
    ) {
        if (value.isEmpty()) {
            Text(
                text = "Como foi a sessão? Registre sua opinião sobre o filme…",
                fontSize = 14.sp,
                color = TextMuted,
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            textStyle = TextStyle(
                color = TextPrimary,
                fontSize = 14.sp,
                lineHeight = 20.sp,
            ),
            cursorBrush = SolidColor(TextPrimary),
        )
    }
}

@Composable
private fun AverageFooter(average: Double?) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(SurfaceDeep, shape)
            .border(1.dp, Border, shape)
            .padding(horizontal = Dimens.Spacing16, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = TextPrimary,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = "Média da Galera",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = average?.formatDecimals2() ?: "—",
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary,
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Surface)
                    .padding(horizontal = Dimens.Spacing12, vertical = 6.dp),
            ) {
                Text(
                    text = ratingVerdict(average),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSoft,
                )
            }
        }
    }
}

/** Hairline separator between the toggle and the score editor. */
@Composable
private fun Divider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Border),
    )
}
