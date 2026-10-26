package app.moviematch.presentation.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.moviematch.auth.AuthState
import app.moviematch.auth.rememberGoogleSignInProvider
import app.moviematch.domain.formatDateShort
import app.moviematch.domain.model.Group
import app.moviematch.domain.model.Profile
import app.moviematch.presentation.common.UiState
import app.moviematch.presentation.common.dataOrNull
import app.moviematch.presentation.common.errorOrNull
import app.moviematch.presentation.common.isLoading
import app.moviematch.presentation.components.AppButton
import app.moviematch.presentation.components.AppSmallButton
import app.moviematch.presentation.components.Avatar
import app.moviematch.presentation.components.AvatarUrlField
import app.moviematch.presentation.components.BentoCard
import app.moviematch.presentation.components.ConfirmDialog
import app.moviematch.presentation.components.HeaderIconButton
import app.moviematch.presentation.components.ScreenTopBar
import app.moviematch.presentation.components.Skeleton
import app.moviematch.presentation.components.SkeletonSquare
import app.moviematch.presentation.components.StateError
import app.moviematch.presentation.components.icons.IconChevronLeft
import app.moviematch.presentation.components.icons.IconChevronRight
import app.moviematch.presentation.components.icons.iconSize
import app.moviematch.presentation.components.skeletons.SettingsSkeleton
import app.moviematch.presentation.theme.CardMetaStyle
import app.moviematch.presentation.theme.DialogBodyStyle
import app.moviematch.presentation.theme.Dimens
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
    onOpenGroup: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val urlInput by viewModel.urlInput.collectAsStateWithLifecycle()
    val fieldError by viewModel.fieldError.collectAsStateWithLifecycle()
    val justSaved by viewModel.justSaved.collectAsStateWithLifecycle()
    val saving by viewModel.saving.collectAsStateWithLifecycle()
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val groupState by viewModel.groupState.collectAsStateWithLifecycle()

    // Activity-backed on Android (Credential Manager requirement), a documented stub on iOS.
    val googleSignIn = rememberGoogleSignInProvider()

    val profile = state.dataOrNull()

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        ScreenTopBar(
            title = "Configurações",
            navigationIcon = {
                HeaderIconButton(
                    onClick = onBack,
                    contentDescription = "Voltar",
                ) {
                    IconChevronLeft(
                        modifier = Modifier.iconSize(18.dp),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            },
        )

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
                saving = saving,
                authState = authState,
                groupState = groupState,
                onUrlChange = viewModel::onUrlChange,
                onSave = viewModel::save,
                onRemovePhoto = viewModel::removePhoto,
                onSignIn = { viewModel.signInWithGoogle(googleSignIn) },
                onSignOut = viewModel::signOut,
                onOpenGroup = onOpenGroup,
                onRetryGroup = viewModel::reloadGroup,
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
    saving: Boolean,
    authState: AuthState,
    groupState: UiState<Group?>,
    onUrlChange: (String) -> Unit,
    onSave: () -> Unit,
    onRemovePhoto: () -> Unit,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
    onOpenGroup: () -> Unit,
    onRetryGroup: () -> Unit,
) {
    val previewUrl = urlInput.trim().ifEmpty { null }
    val hasChanges = previewUrl != profile.avatarUrl
    var showRemoveConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
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
                    verticalArrangement = Arrangement.spacedBy(Dimens.Spacing4),
                ) {
                    Text(
                        text = profile.displayName,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Na roda desde ${formatDateShort(profile.createdAt)}",
                        style = DialogBodyStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Spacer(Modifier.height(Dimens.Spacing24))

        GroupCard(
            groupState = groupState,
            profile = profile,
            onOpenGroup = onOpenGroup,
            onRetry = onRetryGroup,
        )

        Spacer(Modifier.height(Dimens.Spacing24))

        GoogleAccountCard(
            state = authState,
            onSignIn = onSignIn,
            onSignOut = onSignOut,
        )

        Spacer(Modifier.height(Dimens.Spacing24))

        Text(
            text = "Foto de perfil",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(Dimens.Spacing4))
        Text(
            text = "Cole o link de uma imagem (http/https). Ela aparece nas suas notas e no Duelo " +
                "de Críticos.",
            style = CardMetaStyle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(Dimens.Spacing12))

        AvatarUrlField(
            value = urlInput,
            onValueChange = onUrlChange,
            enabled = !saving,
        )

        if (fieldError != null) {
            Spacer(Modifier.height(Dimens.Spacing8))
            Text(
                text = fieldError,
                style = CardMetaStyle,
                color = MaterialTheme.colorScheme.error,
            )
        }

        if (justSaved) {
            Spacer(Modifier.height(Dimens.Spacing8))
            Text(
                text = "Avatar atualizado!",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.tertiary,
            )
        }

        Spacer(Modifier.height(Dimens.Spacing16))

        AppButton(
            label = if (saving) "Salvando…" else "Salvar",
            onClick = onSave,
            modifier = Modifier.fillMaxWidth(),
            enabled = hasChanges && !saving,
        )

        if (profile.avatarUrl != null) {
            Spacer(Modifier.height(Dimens.Spacing16))
            OutlinedButton(
                onClick = { showRemoveConfirm = true },
                modifier = Modifier.fillMaxWidth(),
                shape = CircleShape,
                border = BorderStroke(
                    width = Dimens.BorderWidth,
                    color = MaterialTheme.colorScheme.outlineVariant,
                ),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(Dimens.Spacing8))
                Text(
                    text = "Remover foto",
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }

    if (showRemoveConfirm) {
        ConfirmDialog(
            title = "Remover foto?",
            message = "Sua foto de perfil será removida da roda. Você pode adicionar outra depois.",
            confirmLabel = "Remover",
            busy = saving,
            onConfirm = {
                showRemoveConfirm = false
                onRemovePhoto()
            },
            onDismiss = { showRemoveConfirm = false },
        )
    }
}

/**
 * "Minha roda" card: renders each [UiState] explicitly so the loading state never falls back to the
 * onboarding CTA, and a failed load offers a retry instead of pretending the member has no group.
 */
@Composable
private fun GroupCard(
    groupState: UiState<Group?>,
    profile: Profile,
    onOpenGroup: () -> Unit,
    onRetry: () -> Unit,
) {
    when (groupState) {
        is UiState.Loading -> GroupCardSkeleton()

        is UiState.Error -> BentoCard(padding = Dimens.Spacing16) {
            Text(
                text = "Minha roda",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(Dimens.Spacing4))
            Text(
                text = groupState.message,
                style = CardMetaStyle,
                color = MaterialTheme.colorScheme.error,
            )
            Spacer(Modifier.height(Dimens.Spacing12))
            AppSmallButton(
                label = "Tentar novamente",
                onClick = onRetry,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        is UiState.Content -> {
            val group = groupState.data
            if (group == null) {
                GroupCtaCard(onOpenGroup = onOpenGroup)
            } else {
                GroupDetailCard(group = group, profile = profile, onOpenGroup = onOpenGroup)
            }
        }
    }
}

@Composable
private fun GroupCardSkeleton() {
    BentoCard(padding = Dimens.Spacing16) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Skeleton(width = 110.dp, height = Dimens.SkeletonLineHeight, borderRadius = Dimens.SkeletonRadius)
            Skeleton(width = 56.dp, height = Dimens.SkeletonLineHeight, borderRadius = Dimens.SkeletonRadius)
        }
        Skeleton(
            modifier = Modifier.padding(top = Dimens.Spacing8),
            height = Dimens.SkeletonLineHeight,
            borderRadius = Dimens.SkeletonRadius,
        )
        Row(
            modifier = Modifier.padding(top = Dimens.Spacing12),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing12),
        ) {
            SkeletonSquare(size = Dimens.AvatarSmall, circular = true)
            Skeleton(width = 140.dp, height = Dimens.SkeletonLineHeight, borderRadius = Dimens.SkeletonRadius)
        }
    }
}

@Composable
private fun GroupDetailCard(group: Group, profile: Profile, onOpenGroup: () -> Unit) {
    BentoCard(
        padding = Dimens.Spacing16,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenGroup),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Dimens.Spacing8),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Minha roda",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing8),
                ) {
                    Text(
                        text = group.inviteCode ?: "—",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    IconChevronRight(
                        modifier = Modifier.iconSize(18.dp),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            Text(
                text = "Código do convite — compartilhe com a pessoa para ela entrar na roda.",
                style = CardMetaStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            group.members.firstOrNull { it.id != profile.id }?.let { partner ->
                Spacer(Modifier.height(Dimens.Spacing8))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing12),
                ) {
                    Avatar(
                        name = partner.displayName,
                        url = partner.avatarUrl,
                        size = Dimens.AvatarSmall,
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(Dimens.Spacing4),
                    ) {
                        Text(
                            text = "Parceiro: ${partner.displayName}",
                            style = DialogBodyStyle,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "Na roda desde ${formatDateShort(partner.createdAt)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } ?: run {
                Spacer(Modifier.height(Dimens.Spacing8))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Dimens.Spacing8),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = "Aguardando a outra pessoa entrar…",
                        style = DialogBodyStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun GroupCtaCard(onOpenGroup: () -> Unit) {
    BentoCard(
        padding = Dimens.Spacing16,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenGroup),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Minha roda",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing8),
            ) {
                Text(
                    text = "Criar ou entrar",
                    style = DialogBodyStyle,
                    color = MaterialTheme.colorScheme.tertiary,
                )
                IconChevronRight(
                    modifier = Modifier.iconSize(18.dp),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        Spacer(Modifier.height(Dimens.Spacing4))
        Text(
            text = "Monte sua dupla para escolher, assistir e dar nota junto.",
            style = CardMetaStyle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
