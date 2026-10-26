package app.moviematch.presentation.group

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.moviematch.domain.formatDateShort
import app.moviematch.domain.model.GroupInvite
import app.moviematch.domain.model.Profile
import app.moviematch.domain.model.ProfileSummary
import app.moviematch.presentation.common.dataOrNull
import app.moviematch.presentation.common.errorOrNull
import app.moviematch.presentation.common.isLoading
import app.moviematch.presentation.components.AppButton
import app.moviematch.presentation.components.AppSmallButton
import app.moviematch.presentation.components.Avatar
import app.moviematch.presentation.components.BentoCard
import app.moviematch.presentation.components.BentoTile
import app.moviematch.presentation.components.ButtonVariant
import app.moviematch.presentation.components.ConfirmDialog
import app.moviematch.presentation.components.HeaderIconButton
import app.moviematch.presentation.components.IconBadge
import app.moviematch.presentation.components.ScreenTopBar
import app.moviematch.presentation.components.SearchInput
import app.moviematch.presentation.components.Skeleton
import app.moviematch.presentation.components.SkeletonSquare
import app.moviematch.presentation.components.StateError
import app.moviematch.presentation.components.icons.IconChevronLeft
import app.moviematch.presentation.components.icons.IconSize
import app.moviematch.presentation.components.icons.IconTicket
import app.moviematch.presentation.components.icons.iconSize
import app.moviematch.presentation.theme.CardMetaStyle
import app.moviematch.presentation.theme.DialogBodyStyle
import app.moviematch.presentation.theme.Dimens
import app.moviematch.presentation.theme.NumericHeroStyle
import org.koin.compose.viewmodel.koinViewModel

/**
 * Group screen: creating the pair, joining by code, inviting the other person and
 * answering pending invites.
 *
 * This screen is now a pushed destination in the navigation graph (see [AppShell]).
 * It receives an [onBack] callback to close itself and return to the previous screen
 * (typically Settings). The same [GroupViewModel] handles both onboarding (no group)
 * and management (active group) modes via [GroupOverview.group].
 *
 * @param onBack called when the user taps the header back button.
 * @param viewModel injected via Koin (activity/navigation scoped).
 */
@Composable
fun GroupScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    viewModel: GroupViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val joinCode by viewModel.joinCode.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val resultsState by viewModel.results.collectAsStateWithLifecycle()
    val searching by viewModel.searching.collectAsStateWithLifecycle()
    val handleInput by viewModel.handleInput.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()

    val overview = state.dataOrNull()

    // The action waiting for confirmation; `null` means no dialog is open.
    var pendingAction by remember { mutableStateOf<PendingGroupAction?>(null) }

    // Keeps the last loaded overview around while a refresh runs, so the loading skeleton can match
    // the onboarding/management shape the member already saw instead of always showing onboarding.
    var lastOverview by remember { mutableStateOf<GroupOverview?>(null) }
    LaunchedEffect(overview) {
        if (overview != null) lastOverview = overview
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        ScreenTopBar(
            title = "Sua roda",
            subtitle = if (overview?.group != null) "Dupla de filmes e séries" else "Monte a sua dupla",
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(
                    start = Dimens.ScreenPadding,
                    end = Dimens.ScreenPadding,
                    bottom = Dimens.Spacing24,
                ),
            verticalArrangement = Arrangement.spacedBy(Dimens.Spacing16),
        ) {
            message?.let { NoticeBox(it) }

            when {
                state.isLoading -> GroupSkeleton(hasGroup = lastOverview?.group != null)

                overview == null -> StateError(
                    message = state.errorOrNull() ?: "Não foi possível carregar sua roda.",
                    onRetry = viewModel::load,
                )

                overview.group == null -> NoGroupContent(
                    joinCode = joinCode,
                    busy = busy,
                    invites = overview.invites,
                    onJoinCodeChange = viewModel::onJoinCodeChange,
                    onCreateGroup = viewModel::onCreateGroup,
                    onJoinWithCode = viewModel::onJoinWithCode,
                    onAcceptInvite = viewModel::onAcceptInvite,
                    onDeclineInvite = viewModel::onDeclineInvite,
                )

                else -> WithGroupContent(
                    overview = overview,
                    searchQuery = searchQuery,
                    results = resultsState.dataOrNull().orEmpty(),
                    searchError = resultsState.errorOrNull(),
                    searching = searching,
                    handleInput = handleInput,
                    busy = busy,
                    onSearchQueryChange = viewModel::onSearchQueryChange,
                    onHandleInputChange = viewModel::onHandleInputChange,
                    onInvite = viewModel::onInvite,
                    onInviteByHandle = viewModel::onInviteByHandle,
                    onLeaveGroup = { pendingAction = PendingGroupAction.Leave },
                    onDissolveGroup = { pendingAction = PendingGroupAction.Dissolve },
                    onRemoveMember = { profileId ->
                        pendingAction = PendingGroupAction.RemovePartner(
                            profileId = profileId,
                            displayName = overview.partner?.displayName.orEmpty(),
                        )
                    },
                )
            }
        }
    }

    pendingAction?.let { action ->
        val (title, message, confirmLabel) = action.toDialogCopy()
        ConfirmDialog(
            title = title,
            message = message,
            confirmLabel = confirmLabel,
            busy = busy,
            onConfirm = {
                pendingAction = null
                when (action) {
                    PendingGroupAction.Leave -> viewModel.onLeaveGroup()
                    PendingGroupAction.Dissolve -> viewModel.onDissolveGroup()
                    is PendingGroupAction.RemovePartner -> viewModel.onRemoveMember(action.profileId)
                }
            },
            onDismiss = { if (!busy) pendingAction = null },
        )
    }
}

/**
 * Destructive management action awaiting confirmation.
 *
 * Keeping the partner id/name in the pending action means the dialog is rendered from the same
 * values the button showed, even if the group state refreshes while it is open.
 */
private sealed interface PendingGroupAction {

    data object Leave : PendingGroupAction

    data object Dissolve : PendingGroupAction

    data class RemovePartner(val profileId: String, val displayName: String) : PendingGroupAction
}

/** Title, consequence and confirm label of [action], in the same pt-BR voice used across the app. */
private fun PendingGroupAction.toDialogCopy(): Triple<String, String, String> = when (this) {
    PendingGroupAction.Leave -> Triple(
        "Sair da roda?",
        "Você vai sair desta roda. Se ninguém ficar, ela é encerrada. " +
            "Depois você pode criar outra ou entrar com um código.",
        "Sair",
    )

    PendingGroupAction.Dissolve -> Triple(
        "Excluir a roda?",
        "A roda será encerrada para as duas pessoas. Essa ação não pode ser desfeita.",
        "Excluir",
    )

    is PendingGroupAction.RemovePartner -> Triple(
        "Remover parceiro?",
        "${displayName.ifBlank { "Seu parceiro" }} vai sair da roda e a vaga fica aberta para outra pessoa.",
        "Remover",
    )
}

// --- onboarding (no active group) ------------------------------------------------------------------

/**
 * "Sem grupo" state: create the pair, join one with a shared code, or answer the invites already
 * addressed to the caller.
 */
@Composable
private fun NoGroupContent(
    joinCode: String,
    busy: Boolean,
    invites: List<GroupInvite>,
    onJoinCodeChange: (String) -> Unit,
    onCreateGroup: () -> Unit,
    onJoinWithCode: () -> Unit,
    onAcceptInvite: (GroupInvite) -> Unit,
    onDeclineInvite: (GroupInvite) -> Unit,
) {
    SectionIntro(
        title = "Monte a sua roda",
        message = "A roda é só vocês dois: uma dupla para escolher, assistir e dar nota junto. " +
            "Crie a sua e convide a outra pessoa, ou entre com um código que alguém já compartilhou.",
    )

    BentoCard(padding = Dimens.Spacing16) {
        Text(
            text = "Criar uma roda",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(Dimens.Spacing4))
        Text(
            text = "Você vira o dono da roda e recebe um código para convidar a outra pessoa.",
            style = CardMetaStyle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Dimens.Spacing16))
        AppButton(
            label = if (busy) "Criando…" else "Criar grupo",
            onClick = onCreateGroup,
            enabled = !busy,
            modifier = Modifier.fillMaxWidth(),
        )
    }

    BentoCard(padding = Dimens.Spacing16) {
        Text(
            text = "Entrar com o código",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(Dimens.Spacing4))
        Text(
            text = "Cole o código do convite (ex.: 3F9A20C7) e entre na roda de uma vez.",
            style = CardMetaStyle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Dimens.Spacing12))
        SearchInput(
            value = joinCode,
            onValueChange = onJoinCodeChange,
            placeholder = "Código do convite",
        )
        Spacer(Modifier.height(Dimens.Spacing12))
        AppButton(
            label = if (busy) "Entrando…" else "Entrar com o código",
            onClick = onJoinWithCode,
            variant = ButtonVariant.Outline,
            enabled = joinCode.isNotBlank() && !busy,
            modifier = Modifier.fillMaxWidth(),
        )
    }

    PendingInvitesSection(
        invites = invites,
        busy = busy,
        onAcceptInvite = onAcceptInvite,
        onDeclineInvite = onDeclineInvite,
    )
}

/** List of invites the caller can answer; the inviter's name stays hidden by RLS. */
@Composable
private fun PendingInvitesSection(
    invites: List<GroupInvite>,
    busy: Boolean,
    onAcceptInvite: (GroupInvite) -> Unit,
    onDeclineInvite: (GroupInvite) -> Unit,
) {
    Text(
        text = "Convites pendentes",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurface,
    )

    if (invites.isEmpty()) {
        Text(
            text = "Nenhum convite por aqui ainda.",
            style = CardMetaStyle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }

    invites.forEach { invite ->
        BentoTile(padding = Dimens.Spacing12) {
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.Spacing12)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing12),
                ) {
                    IconBadge(size = Dimens.IconBadgeSmall) {
                        IconTicket(
                            modifier = Modifier.iconSize(IconSize.Small),
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Convite para uma roda",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = if (invite.expiresAt != null) {
                                "Expira em ${formatDateShort(invite.expiresAt)}"
                            } else {
                                "Sem prazo definido"
                            },
                            style = CardMetaStyle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing8)) {
                    AppSmallButton(
                        label = "Aceitar",
                        onClick = { onAcceptInvite(invite) },
                        enabled = !busy,
                        modifier = Modifier.weight(1f),
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    )
                    AppSmallButton(
                        label = "Recusar",
                        onClick = { onDeclineInvite(invite) },
                        enabled = !busy,
                        modifier = Modifier.weight(1f),
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

// --- with an active group --------------------------------------------------------------------------

/** "Com grupo" state: the pair, the invite code while the second slot is free, and the invite form. */
@Composable
private fun WithGroupContent(
    overview: GroupOverview,
    searchQuery: String,
    results: List<ProfileSummary>,
    searchError: String?,
    searching: Boolean,
    handleInput: String,
    busy: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onHandleInputChange: (String) -> Unit,
    onInvite: (ProfileSummary) -> Unit,
    onInviteByHandle: () -> Unit,
    onLeaveGroup: () -> Unit,
    onDissolveGroup: () -> Unit,
    onRemoveMember: (String) -> Unit,
) {
    PairCard(me = overview.me, partner = overview.partner)

    if (overview.partner != null) {
        BentoCard(padding = Dimens.Spacing16) {
            Text(
                text = "Roda completa",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(Dimens.Spacing4))
            Text(
                text = "Vocês dois já estão na roda. Boa sessão!",
                style = CardMetaStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    } else {
        InviteCodeCard(code = overview.group?.inviteCode)

        InviteForm(
            searchQuery = searchQuery,
            results = results,
            searchError = searchError,
            searching = searching,
            handleInput = handleInput,
            busy = busy,
            onSearchQueryChange = onSearchQueryChange,
            onHandleInputChange = onHandleInputChange,
            onInvite = onInvite,
            onInviteByHandle = onInviteByHandle,
        )
    }

    // Available in both states: a lone member (S3) must still be able to leave/dissolve and create
    // another group. "Remover parceiro" only appears when there is a partner and the caller created
    // the group, mirroring the backend rule of `remove_member`.
    GroupManagementCard(
        partner = overview.partner,
        isCreator = overview.isCreator,
        busy = busy,
        onLeaveGroup = onLeaveGroup,
        onDissolveGroup = onDissolveGroup,
        onRemoveMember = onRemoveMember,
    )
}

/** The two avatars of the pair, or the empty seat while the second member has not arrived. */
@Composable
private fun PairCard(me: Profile, partner: Profile?) {
    BentoCard(padding = Dimens.Spacing16) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing16),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Avatar(name = me.displayName, url = me.avatarUrl, size = Dimens.AvatarLarge)
                Spacer(Modifier.height(Dimens.Spacing8))
                Text(
                    text = me.displayName,
                    style = CardMetaStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
            }

            Text(
                text = "&",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                color = MaterialTheme.colorScheme.onSurface,
            )

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (partner != null) {
                    Avatar(name = partner.displayName, url = partner.avatarUrl, size = Dimens.AvatarLarge)
                    Spacer(Modifier.height(Dimens.Spacing8))
                    Text(
                        text = partner.displayName,
                        style = CardMetaStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                    )
                } else {
                    EmptySeat()
                    Spacer(Modifier.height(Dimens.Spacing8))
                    Text(
                        text = "Aguardando…",
                        style = CardMetaStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Spacer(Modifier.height(Dimens.Spacing16))

        Text(
            text = if (partner == null) {
                "Aguardando a outra pessoa entrar. Compartilhe o código ou envie um convite."
            } else {
                "Vocês dois estão na roda."
            },
            style = DialogBodyStyle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Dashed-free placeholder seat: a tonal disc with a plus, in the avatar footprint. */
@Composable
private fun EmptySeat() {
    Surface(
        modifier = Modifier.size(Dimens.AvatarLarge),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = "+",
                style = MaterialTheme.typography.headlineSmall,
            )
        }
    }
}

/** The shareable invite code, shown while the second seat is still free. */
@Composable
private fun InviteCodeCard(code: String?) {
    BentoCard(padding = Dimens.Spacing16) {
        Text(
            text = "Código do convite",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(Dimens.Spacing8))
        Text(
            text = code ?: "—",
            style = NumericHeroStyle,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(Dimens.Spacing8))
        Text(
            text = "Compartilhe este código: quem entrar com ele vira a outra pessoa da roda.",
            style = CardMetaStyle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Invite by searched profile or by raw handle, for someone without an account. */
@Composable
private fun InviteForm(
    searchQuery: String,
    results: List<ProfileSummary>,
    searchError: String?,
    searching: Boolean,
    handleInput: String,
    busy: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onHandleInputChange: (String) -> Unit,
    onInvite: (ProfileSummary) -> Unit,
    onInviteByHandle: () -> Unit,
) {
    BentoCard(padding = Dimens.Spacing16) {
        Text(
            text = "Convidar a outra pessoa",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(Dimens.Spacing4))
        Text(
            text = "Busque pelo nome ou @handle de quem você quer na roda.",
            style = CardMetaStyle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Dimens.Spacing12))

        SearchInput(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = "Buscar por nome ou @handle",
            trailing = {
                if (searching) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(IconSize.Small),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        strokeWidth = 2.dp,
                    )
                }
            },
        )

        val term = searchQuery.trim()
        when {
            searchError != null -> FormHint(searchError, isError = true)
            searching -> Unit
            term.length < 2 -> FormHint("Digite pelo menos 2 letras para buscar.")
            results.isEmpty() -> FormHint("Ninguém encontrado com esse nome.")
            else -> results.forEach { hit ->
                Spacer(Modifier.height(Dimens.Spacing8))
                ProfileHitRow(hit = hit, busy = busy, onInvite = { onInvite(hit) })
            }
        }

        Spacer(Modifier.height(Dimens.Spacing16))
        Text(
            text = "Ou convide por @handle",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Dimens.Spacing4))
        Text(
            text = "Funciona mesmo se a pessoa ainda não tem conta no app.",
            style = CardMetaStyle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Dimens.Spacing12))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing8),
        ) {
            Box(modifier = Modifier.weight(1f)) {
                SearchInput(
                    value = handleInput,
                    onValueChange = onHandleInputChange,
                    placeholder = "@handle",
                )
            }
            AppSmallButton(
                label = "Convidar",
                onClick = onInviteByHandle,
                enabled = handleInput.isNotBlank() && !busy,
            )
        }
    }
}

/** One search hit: avatar, name and handle, plus the invite action. */
@Composable
private fun ProfileHitRow(
    hit: ProfileSummary,
    busy: Boolean,
    onInvite: () -> Unit,
) {
    BentoTile(padding = Dimens.Spacing12) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing12),
        ) {
            Avatar(name = hit.displayName, url = hit.avatarUrl, size = Dimens.AvatarMedium)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = hit.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (hit.handle != null) {
                    Text(
                        text = "@${hit.handle}",
                        style = CardMetaStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            AppSmallButton(
                label = "Convidar",
                onClick = onInvite,
                enabled = !busy,
            )
        }
    }
}

// --- shared pieces ---------------------------------------------------------------------------------

/** Introductory block of the onboarding state. */
@Composable
private fun SectionIntro(title: String, message: String) {
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.Spacing4)) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = message,
            style = DialogBodyStyle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Small helper line under a form field. */
@Composable
private fun FormHint(message: String, isError: Boolean = false) {
    Text(
        text = message,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = Dimens.Spacing8),
        style = CardMetaStyle,
        color = if (isError) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        textAlign = TextAlign.Start,
    )
}

/** Feedback banner: the `tertiaryContainer` role for a success, the error container for a failure. */
@Composable
private fun NoticeBox(message: GroupMessage) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = if (message.isError) {
            MaterialTheme.colorScheme.errorContainer
        } else {
            MaterialTheme.colorScheme.tertiaryContainer
        },
        contentColor = if (message.isError) {
            MaterialTheme.colorScheme.onErrorContainer
        } else {
            MaterialTheme.colorScheme.onTertiaryContainer
        },
    ) {
        Text(
            text = message.text,
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.Spacing12),
            style = DialogBodyStyle,
            textAlign = TextAlign.Center,
        )
    }
}

/*** Management actions of the roda: leave/dissolve for every member and remove-partner for the creator. */
@Composable
private fun GroupManagementCard(
    partner: Profile?,
    isCreator: Boolean,
    busy: Boolean,
    onLeaveGroup: () -> Unit,
    onDissolveGroup: () -> Unit,
    onRemoveMember: (String) -> Unit,
) {
    BentoCard(padding = Dimens.Spacing16) {
        Text(
            text = "Gerenciar a roda",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(Dimens.Spacing4))
        Text(
            text = if (isCreator && partner != null) {
                "Você criou a roda: pode remover o parceiro, sair ou excluí-la."
            } else {
                "Qualquer pessoa da roda pode sair ou excluí-la."
            },
            style = CardMetaStyle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Dimens.Spacing12))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing8),
        ) {
            AppSmallButton(
                label = "Sair da roda",
                onClick = onLeaveGroup,
                enabled = !busy,
                modifier = Modifier.weight(1f),
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                contentColor = MaterialTheme.colorScheme.onSurface,
            )
            AppSmallButton(
                label = "Excluir roda",
                onClick = onDissolveGroup,
                enabled = !busy,
                modifier = Modifier.weight(1f),
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
            )
        }

        // The backend only lets the creator remove the other member, and there is nothing to remove
        // while the second seat is still free (S3).
        if (isCreator && partner != null) {
            Spacer(Modifier.height(Dimens.Spacing8))

            AppButton(
                label = "Remover parceiro",
                onClick = { onRemoveMember(partner.id) },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth(),
                variant = ButtonVariant.Outline,
            )
        }
    }
}

/**
 * Skeleton of the group screen. [hasGroup] selects the management shape (pair + management card)
 * instead of the onboarding shape (create/join forms).
 */
@Composable
private fun GroupSkeleton(hasGroup: Boolean = false) {
    if (hasGroup) {
        PairCardSkeleton()
        ManagementCardSkeleton()
    } else {
        BentoCard(padding = Dimens.Spacing16) {
            Skeleton(
                width = 180.dp,
                height = Dimens.SkeletonTitleHeight,
                borderRadius = Dimens.SkeletonRadius,
            )
            Skeleton(
                modifier = Modifier.padding(top = Dimens.Spacing8),
                height = Dimens.SkeletonLineHeight,
                borderRadius = Dimens.SkeletonRadius,
            )
            Skeleton(
                modifier = Modifier.padding(top = Dimens.Spacing16),
                height = Dimens.SearchInputHeight,
                borderRadius = Dimens.RadiusPill,
            )
        }

        PairCardSkeleton()
    }
}

@Composable
private fun PairCardSkeleton() {
    BentoCard(padding = Dimens.Spacing16) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing16),
        ) {
            SkeletonSquare(size = Dimens.AvatarLarge, circular = true)
            Skeleton(width = 24.dp, height = Dimens.SkeletonLineHeight, borderRadius = Dimens.SkeletonRadius)
            SkeletonSquare(size = Dimens.AvatarLarge, circular = true)
        }
        Skeleton(
            modifier = Modifier.padding(top = Dimens.Spacing16),
            height = Dimens.SkeletonLineHeight,
            borderRadius = Dimens.SkeletonRadius,
        )
    }
}

@Composable
private fun ManagementCardSkeleton() {
    BentoCard(padding = Dimens.Spacing16) {
        Skeleton(
            width = 150.dp,
            height = Dimens.SkeletonTitleHeight,
            borderRadius = Dimens.SkeletonRadius,
        )
        Skeleton(
            modifier = Modifier.padding(top = Dimens.Spacing8),
            width = 220.dp,
            height = Dimens.SkeletonLineHeight,
            borderRadius = Dimens.SkeletonRadius,
        )
        Row(
            modifier = Modifier.padding(top = Dimens.Spacing16),
            horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing8),
        ) {
            repeat(2) {
                Skeleton(
                    modifier = Modifier.weight(1f),
                    height = Dimens.IconButtonSize,
                    borderRadius = Dimens.RadiusPill,
                )
            }
        }
    }
}
