package app.moviematch.presentation.details

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.moviematch.presentation.theme.Border
import app.moviematch.presentation.theme.Danger
import app.moviematch.presentation.theme.DangerBorder
import app.moviematch.presentation.theme.DangerStrong
import app.moviematch.presentation.theme.DangerSurface
import app.moviematch.presentation.theme.Dimens
import app.moviematch.presentation.theme.Surface
import app.moviematch.presentation.theme.SurfaceRaised
import app.moviematch.presentation.theme.TextPrimary
import app.moviematch.presentation.theme.TextSecondary
import app.moviematch.presentation.theme.White

/**
 * Destructive confirmation, ported from the reference `ConfirmDeleteModal`.
 *
 * The dialog stays open (and cannot be dismissed) while [deleting] is true, so the person always
 * sees the outcome of the action before the screen navigates back.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfirmDeleteDialog(
    movieTitle: String?,
    deleting: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    BasicAlertDialog(onDismissRequest = { if (!deleting) onDismiss() }) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Dimens.RadiusCard))
                .background(Surface)
                .border(1.dp, Border, RoundedCornerShape(Dimens.RadiusCard))
                .padding(Dimens.Spacing24),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(DangerSurface, CircleShape)
                    .border(1.dp, DangerBorder, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = Danger,
                    modifier = Modifier.size(22.dp),
                )
            }

            Spacer(Modifier.height(Dimens.Spacing16))

            Text(
                text = "Apagar filme?",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(Dimens.Spacing8))

            Text(
                text = movieTitle?.let { "\"$it\" será removido da lista da roda. " } +
                    "Essa ação não pode ser desfeita.",
                fontSize = 13.sp,
                lineHeight = 20.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(Dimens.Spacing24))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing12),
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(Dimens.RadiusPill))
                        .background(SurfaceRaised, RoundedCornerShape(Dimens.RadiusPill))
                        .border(1.dp, Border, RoundedCornerShape(Dimens.RadiusPill))
                        .clickable(enabled = !deleting, onClick = onDismiss)
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Cancelar",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                    )
                }

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(Dimens.RadiusPill))
                        .background(DangerStrong, RoundedCornerShape(Dimens.RadiusPill))
                        .clickable(enabled = !deleting, onClick = onConfirm)
                        .padding(vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    if (deleting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = White,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            tint = White,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.size(Dimens.Spacing8))
                        Text(
                            text = "Apagar",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = White,
                        )
                    }
                }
            }
        }
    }
}
