package app.moviematch.presentation.group

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.moviematch.auth.AuthStore
import app.moviematch.domain.model.Group
import app.moviematch.domain.model.GroupInvite
import app.moviematch.domain.model.Profile
import app.moviematch.domain.model.ProfileSummary
import app.moviematch.presentation.common.CurrentProfileStore
import app.moviematch.presentation.common.UiState
import app.moviematch.presentation.common.dataOrNull
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

/** Feedback shown above the group content; [isError] selects the danger tone. */
data class GroupMessage(val text: String, val isError: Boolean = false)

/**
 * Everything the group screen renders once loaded.
 *
 * [group] is `null` while the signed-in member has no active group (the onboarding state); [partner]
 * is `null` while the group only has one member ("aguardando a outra pessoa"). [isCreator] mirrors
 * `groups.created_by` and gates the "Remover parceiro" action, the only one restricted to the
 * creator by the backend.
 */
data class GroupOverview(
    val group: Group?,
    val me: Profile,
    val partner: Profile? = null,
    val invites: List<GroupInvite> = emptyList(),
    val isCreator: Boolean = false,
)

/**
 * Drives the group screen: the pair state (or its absence), the invite code, the search used to
 * invite someone, the pending invites and the "entrar com o código" action.
 *
 * The group itself lives in the application-scoped [GroupStore], the same instance the settings
 * screen observes. This `ViewModel` only projects that shared state onto a [GroupOverview] and runs
 * the mutations, so leaving/dissolving the group updates both screens at once.
 *
 * Every successful mutation refreshes the store, which is how accepting an invite or joining by code
 * flips the screen to the "com grupo" state without any manual navigation.
 */
class GroupViewModel(
    private val store: GroupStore,
    private val currentProfile: CurrentProfileStore,
    private val authStore: AuthStore,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<GroupOverview>>(UiState.Loading)
    val state: StateFlow<UiState<GroupOverview>> = _state.asStateFlow()

    /** Text of the "entrar com o código" field. */
    private val _joinCode = MutableStateFlow("")
    val joinCode: StateFlow<String> = _joinCode.asStateFlow()

    /** Text of the "convidar pelo nome" search field. */
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _results = MutableStateFlow<UiState<List<ProfileSummary>>>(UiState.Content(emptyList()))
    val results: StateFlow<UiState<List<ProfileSummary>>> = _results.asStateFlow()

    /** `true` while the debounced profile search is in flight (drives the field spinner). */
    private val _searching = MutableStateFlow(false)
    val searching: StateFlow<Boolean> = _searching.asStateFlow()

    /** Text of the "convidar por handle" field, for someone whose account does not exist yet. */
    private val _handleInput = MutableStateFlow("")
    val handleInput: StateFlow<String> = _handleInput.asStateFlow()

    /** `true` while a mutation is in flight; disables every action of the screen. */
    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _message = MutableStateFlow<GroupMessage?>(null)
    val message: StateFlow<GroupMessage?> = _message.asStateFlow()

    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            // The store already refreshes on sign-in/profile changes; the ViewModel only mirrors it.
            combine(store.groupState, store.invites) { groupState, invites -> groupState to invites }
                .collect { (groupState, invites) -> _state.value = project(groupState, invites) }
        }
    }

    /** Reloads the shared group state (retry of the error state, for instance). */
    fun load() {
        viewModelScope.launch { store.refresh() }
    }

    fun onJoinCodeChange(value: String) {
        _joinCode.value = value
        clearMessage()
    }

    /** Creates the pair and puts the caller in `slot = 1`. */
    fun onCreateGroup() {
        launchAction("Não foi possível criar a roda.") {
            // Ensure Supabase session is fully ready (token hydrated) before RPC
            authStore.sessionReady.firstOrNull { it } ?: return@launchAction
            store.createGroup()
        }
    }

    /** Joins the group behind [joinCode] ("entrar com o código"). */
    fun onJoinWithCode() {
        if (_joinCode.value.isBlank()) return
        launchAction("Não foi possível entrar com esse código.") {
            authStore.sessionReady.firstOrNull { it } ?: return@launchAction
            store.joinGroupByCode(_joinCode.value)
        }
    }

    /** Debounced `search_profiles`, mirroring the "Adicionar" tab search. */
    fun onSearchQueryChange(value: String) {
        _searchQuery.value = value
        searchJob?.cancel()

        val term = value.trim()
        if (term.length < MIN_SEARCH_LENGTH) {
            _results.value = UiState.Content(emptyList())
            _searching.value = false
            return
        }

        searchJob = viewModelScope.launch {
            _searching.value = true
            delay(SEARCH_DEBOUNCE_MS)
            _results.value = try {
                UiState.Content(store.searchProfiles(term))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                UiState.Error(e.message?.takeIf { it.isNotBlank() } ?: "Não foi possível buscar pessoas agora.")
            }
            _searching.value = false
        }
    }

    /** Invites a search hit by its profile id. */
    fun onInvite(profile: ProfileSummary) {
        val groupId = stateGroupId() ?: return
        launchAction(
            failureFallback = "Não foi possível convidar ${profile.displayName}.",
            successMessage = GroupMessage("Convite enviado para ${profile.displayName}!"),
            onSuccess = ::clearSearch,
        ) {
            authStore.sessionReady.firstOrNull { it } ?: return@launchAction
            store.inviteByProfile(groupId, profile.id)
        }
    }

    fun onHandleInputChange(value: String) {
        _handleInput.value = value
        clearMessage()
    }

    /** Invites by handle, the path that also reaches somebody without an account. */
    fun onInviteByHandle() {
        val groupId = stateGroupId() ?: return
        val handle = _handleInput.value.trim()
        if (handle.isBlank()) return
        launchAction(
            failureFallback = "Não foi possível convidar @$handle.",
            successMessage = GroupMessage("Convite enviado para @$handle!"),
            onSuccess = ::clearSearch,
        ) {
            authStore.sessionReady.firstOrNull { it } ?: return@launchAction
            store.inviteByHandle(groupId, handle)
        }
    }

    /** Accepts a pending invite; the refresh that follows flips the screen to the group state. */
    fun onAcceptInvite(invite: GroupInvite) {
        launchAction("Não foi possível aceitar o convite.") {
            authStore.sessionReady.firstOrNull { it } ?: return@launchAction
            store.acceptInvite(invite.id)
        }
    }

    fun onDeclineInvite(invite: GroupInvite) {
        launchAction(
            failureFallback = "Não foi possível recusar o convite.",
            successMessage = GroupMessage("Convite recusado."),
        ) {
            authStore.sessionReady.firstOrNull { it } ?: return@launchAction
            store.declineInvite(invite.id)
        }
    }

    /** Leaves the current group (soft-deletes membership). */
    fun onLeaveGroup() {
        val groupId = stateGroupId() ?: return
        launchAction("Não foi possível sair da roda.") {
            authStore.sessionReady.firstOrNull { it } ?: return@launchAction
            store.leaveGroup(groupId)
        }
    }

    /** Dissolves the group (soft-delete). Any active member can do this. */
    fun onDissolveGroup() {
        val groupId = stateGroupId() ?: return
        launchAction("Não foi possível dissolver a roda.") {
            authStore.sessionReady.firstOrNull { it } ?: return@launchAction
            store.dissolveGroup(groupId)
        }
    }

    /** Removes a member from the group. Only the group creator can do this. */
    fun onRemoveMember(profileId: String) {
        val groupId = stateGroupId() ?: return
        launchAction("Não foi possível remover o membro.") {
            authStore.sessionReady.firstOrNull { it } ?: return@launchAction
            store.removeMember(groupId, profileId)
        }
    }

    /** Ends the session; the gate then swaps this screen for the login screen. */
    fun signOut() {
        authStore.signOut()
    }

    /** Hides the current feedback message. */
    fun clearMessage() {
        _message.value = null
    }

    /**
     * Runs a mutation, then refreshes the shared state and reports the outcome.
     *
     * [onSuccess] replaces the refresh when the action changed something the reload would otherwise
     * clear (the search, for instance). A failure never throws to the UI: it becomes a [GroupMessage]
     * so "grupo cheio", "convite expirado" and friends land on the screen as copy.
     */
    private fun launchAction(
        failureFallback: String,
        successMessage: GroupMessage? = null,
        onSuccess: (() -> Unit)? = null,
        block: suspend () -> Unit,
    ) {
        if (_busy.value) return
        _busy.value = true
        clearMessage()
        viewModelScope.launch {
            try {
                block()
                store.refresh()
                onSuccess?.invoke()
                if (successMessage != null) _message.value = successMessage
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _message.value = GroupMessage(
                    text = e.message?.takeIf { it.isNotBlank() } ?: failureFallback,
                    isError = true,
                )
            } finally {
                _busy.value = false
            }
        }
    }

    /** Projects the shared store state into the screen model, hiding invites once a group exists. */
    private fun project(
        groupState: UiState<Group?>,
        invites: List<GroupInvite>,
    ): UiState<GroupOverview> = when (groupState) {
        is UiState.Loading -> UiState.Loading
        is UiState.Error -> UiState.Error(groupState.message)
        is UiState.Content -> UiState.Content(overview(groupState.data, invites))
    }

    private fun overview(group: Group?, invites: List<GroupInvite>): GroupOverview {
        val me = currentProfile.profile.value
        return GroupOverview(
            group = group,
            me = me,
            partner = group?.members?.firstOrNull { it.id != me.id },
            invites = if (group == null) invites else emptyList(),
            isCreator = group?.createdBy == me.id,
        )
    }

    private fun stateGroupId(): String? = _state.value.dataOrNull()?.group?.id

    private fun clearSearch() {
        searchJob?.cancel()
        _searchQuery.value = ""
        _handleInput.value = ""
        _results.value = UiState.Content(emptyList())
        _searching.value = false
    }

    private companion object {
        /** `search_profiles` ignores shorter queries; the same floor avoids a useless request. */
        const val MIN_SEARCH_LENGTH = 2

        /** Matches the "Adicionar" tab debounce. */
        const val SEARCH_DEBOUNCE_MS = 400L
    }
}
