package app.moviematch.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.moviematch.auth.AuthState
import app.moviematch.auth.rememberGoogleSignInProvider
import app.moviematch.domain.formatDateShort
import app.moviematch.domain.model.Profile
import app.moviematch.presentation.common.dataOrNull
import app.moviematch.presentation.common.errorOrNull
import app.moviematch.presentation.common.isLoading
import app.moviematch.presentation.components.AppButton
import app.moviematch.presentation.components.Avatar
import app.moviematch.presentation.components.BentoCard
import app.moviematch.presentation.components.HeaderIconButton
import app.moviematch.presentation.components.StateError
import app.moviematch.presentation.components.icons.IconChevronLeft
import app.moviematch.presentation.components.icons.IconLink
import app.moviematch.presentation.components.icons.iconSize
import app.moviematch.presentation.components.skeletons.SettingsSkeleton
import app.moviematch.presentation.theme.Accent
import app.moviematch.presentation.theme.Border
import app.moviematch.presentation.theme.Danger
import app.moviematch.presentation.theme.Dimens
import app.moviematch.presentation.theme.SurfaceDeep
import app.moviematch.presentation.theme.TextMuted
import app.moviematch.presentation.theme.TextPrimary
import app.moviematch.presentation.theme.TextSecondary
import org.koin.compose.viewmodel.koinViewModel

/**
 * Settings screen: the signed-in member, the Google account entry point and the avatar URL form.
 *
 * Ported from the reference `SettingsScreen`. The reference also shows the account e-mail read from
 * Supabase auth; that line shows since when the member is part of the group instead, and the e-mail
 * appears once a session exists. Everything else — the preview, the validation copy, the
 * save/remove buttons — keeps the same behaviour.
 */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val urlInput by viewModel.urlInput.collectAsStateWithLifecycle()
    val fieldError by viewModel.fieldError.collectAsStateWithLifecycle()
    val justSaved by viewModel.justSaved.collectAsStateWithLifecycle()
    val authState by viewModel.authState.collectAsStateWithLifecycle()

    // Activity-backed on Android (Credential Manager requirement), a documented stub on iOS.
    val googleSignIn = rememberGoogleSignInProvider()

    val profile = state.dataOrNull()

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.ScreenPadding, vertical = Dimens.Spacing12),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            HeaderIconButton(onClick = onBack) {
                IconChevronLeft(modifier = Modifier.iconSize(18.dp), color = TextPrimary)
            }
            Text(
                text = "Configurações",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
            )
            Spacer(Modifier.size(Dimens.IconButtonSize))
        }

        when {
            state.isLoading -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Dimens.ScreenPadding, vertical = Dimens.Spacing8),
            ) {
                SettingsSkeleton()
            }

            profile == null -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                StateError(
                    message = state.errorOrNull() ?: "Não foi possível carregar seu perfil.",
                    onRetry = viewModel::load,
                )
            }

            else -> SettingsContent(
                profile = profile,
                urlInput = urlInput,
                fieldError = fieldError,
                justSaved = justSaved,
                authState = authState,
                onUrlChange = viewModel::onUrlChange,
                onSave = viewModel::save,
                onRemovePhoto = viewModel::removePhoto,
                onSignIn = { viewModel.signInWithGoogle(googleSignIn) },
                onSignOut = viewModel::signOut,
            )
        }
    }
}

@Composable
private fun SettingsContent(
    profile: Profile,
    urlInput: String,
    fieldError: String?,
    justSaved: Boolean,
    authState: AuthState,
    onUrlChange: (String) -> Unit,
    onSave: () -> Unit,
    onRemovePhoto: () -> Unit,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
) {
    val previewUrl = urlInput.trim().ifEmpty { null }
    val hasChanges = previewUrl != profile.avatarUrl

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                start = Dimens.ScreenPadding,
                end = Dimens.ScreenPadding,
                top = Dimens.Spacing8,
                bottom = Dimens.Spacing24,
            ),
    ) {
        BentoCard(padding = Dimens.Spacing16) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing16),
            ) {
                Avatar(
                    name = profile.displayName,
                    url = previewUrl,
                    size = Dimens.AvatarLarge,
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = profile.displayName,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                    )
                    Text(
                        text = "Na roda desde ${formatDateShort(profile.createdAt)}",
                        fontSize = 13.sp,
                        color = TextSecondary,
                    )
                }
            }
        }

        Spacer(Modifier.height(Dimens.Spacing24))

        GoogleAccountCard(
            state = authState,
            onSignIn = onSignIn,
            onSignOut = onSignOut,
        )

        Spacer(Modifier.height(Dimens.Spacing24))

        Text(
            text = "Foto de perfil",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
        )
        Spacer(Modifier.height(Dimens.Spacing4))
        Text(
            text = "Cole o link de uma imagem (http/https). Ela aparece nas suas notas e no Duelo " +
                "de Críticos.",
            fontSize = 12.sp,
            lineHeight = 18.sp,
            color = TextSecondary,
        )

        Spacer(Modifier.height(Dimens.Spacing12))

        AvatarUrlField(value = urlInput, onValueChange = onUrlChange)

        if (fieldError != null) {
            Spacer(Modifier.height(Dimens.Spacing8))
            Text(
                text = fieldError,
                fontSize = 12.sp,
                color = Danger,
            )
        }

        if (justSaved) {
            Spacer(Modifier.height(Dimens.Spacing8))
            Text(
                text = "Avatar atualizado!",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Accent,
            )
        }

        Spacer(Modifier.height(Dimens.Spacing16))

        AppButton(
            label = "Salvar",
            onClick = onSave,
            modifier = Modifier.fillMaxWidth(),
            enabled = hasChanges,
        )

        if (profile.avatarUrl != null) {
            Spacer(Modifier.height(Dimens.Spacing16))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Dimens.RadiusPill))
                    .clickable(onClick = onRemovePhoto)
                    .padding(vertical = Dimens.Spacing8),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = Danger,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.size(Dimens.Spacing8))
                Text(
                    text = "Remover foto",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Danger,
                )
            }
        }
    }
}

@Composable
private fun AvatarUrlField(
    value: String,
    onValueChange: (String) -> Unit,
) {
    val shape = RoundedCornerShape(Dimens.RadiusTile)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(SurfaceDeep, shape)
            .border(1.dp, Border, shape)
            .padding(horizontal = Dimens.Spacing16, vertical = Dimens.Spacing4),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing8),
    ) {
        IconLink(modifier = Modifier.iconSize(16.dp), color = TextMuted)

        Box(modifier = Modifier.weight(1f)) {
            if (value.isEmpty()) {
                Text(
                    text = "https://exemplo.com/foto.jpg",
                    modifier = Modifier.padding(vertical = Dimens.Spacing12),
                    fontSize = 14.sp,
                    color = TextMuted,
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = Dimens.Spacing12),
                singleLine = true,
                textStyle = TextStyle(color = TextPrimary, fontSize = 14.sp),
                cursorBrush = SolidColor(TextPrimary),
            )
        }
    }
}
