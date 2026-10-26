package app.moviematch.presentation.group

import app.moviematch.auth.AuthState
import app.moviematch.auth.AuthStore
import app.moviematch.auth.MISSING_SUPABASE_MESSAGE
import app.moviematch.data.supabase.GroupRepositoryException
import app.moviematch.domain.GroupDataSource
import app.moviematch.domain.model.Group
import app.moviematch.domain.model.GroupInvite
import app.moviematch.domain.model.ProfileSummary
import app.moviematch.presentation.common.CurrentProfileStore
import app.moviematch.presentation.common.UiState
import app.moviematch.presentation.common.uiStateOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Application-scoped single source of truth for the active "roda".
 *
 * Both the settings screen ("Minha roda" card) and the group screen observe the same
 * [groupState]/[invites], so a mutation made in one place pushes the new value to the other without
 * a manual reload (the bug where the settings card stayed stale after leaving the group).
 *
 * The store also absorbs the offline case: [repository] is `null` when the `.env` has no Supabase
 * credentials, and every read/write then becomes a typed error instead of a Koin
 * `NoDefinitionFoundException` when `Routes.SETTINGS`/`Routes.GROUP` is opened.
 *
 * It owns its [scope] (like [AuthStore]) because it must survive every screen: it refreshes on
 * sign-in and on profile changes, no matter which screen is mounted at the time.
 *
 * @param repository `null` when Supabase is not configured (`getOrNull()` in the Koin graph).
 * @param scope application scope; injectable so tests can drive a deterministic dispatcher.
 */
class GroupStore(
    private val repository: GroupDataSource?,
    private val currentProfile: CurrentProfileStore,
    private val authStore: AuthStore,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {

    private val refreshMutex = Mutex()

    /** Active group; [UiState.Content] with `null` means "signed in, no group yet". */
    private val _groupState = MutableStateFlow<UiState<Group?>>(UiState.Loading)
    val groupState: StateFlow<UiState<Group?>> = _groupState.asStateFlow()

    /** Invites the user can answer while they have no active group. */
    private val _invites = MutableStateFlow<List<GroupInvite>>(emptyList())
    val invites: StateFlow<List<GroupInvite>> = _invites.asStateFlow()

    init {
        scope.launch {
            authStore.state
                // The profile id is what changes on a "second account on the same device"; a pair
                // keeps a single refresh per meaningful transition.
                .combine(currentProfile.profile) { auth, profile -> auth to profile.id }
                .distinctUntilChanged()
                .collect { (auth, _) ->
                    if (auth is AuthState.SignedIn) {
                        // `SignedIn` is emitted just before the token is hydrated; waiting for the
                        // same readiness flag the write actions use avoids one failing read at login.
                        authStore.sessionReady.first { it }
                        refresh()
                    } else {
                        clear()
                    }
                }
        }
    }

    /**
     * Re-reads the group and the pending invites.
     *
     * Called after every mutation and whenever the session/profile changes. Serialized so a login
     * followed by a profile switch cannot interleave two reads and publish an older result last.
     */
    suspend fun refresh() = refreshMutex.withLock {
        val repository = repository
        if (repository == null) {
            _groupState.value = UiState.Error(MISSING_SUPABASE_MESSAGE)
            _invites.value = emptyList()
            return@withLock
        }

        _groupState.value = uiStateOf("Não foi possível carregar sua roda.") {
            val group = repository.activeGroup()
            // A member already sees the outgoing invites through RLS, but the screen never shows
            // them while a group is active; keeping the list empty avoids a pointless read.
            _invites.value = if (group == null) repository.pendingInvites() else emptyList()
            group
        }
    }

    suspend fun createGroup() = requireRepository().createGroup()

    suspend fun joinGroupByCode(code: String) = requireRepository().joinGroupByCode(code)

    suspend fun searchProfiles(query: String): List<ProfileSummary> =
        requireRepository().searchProfiles(query)

    suspend fun inviteByProfile(groupId: String, profileId: String) =
        requireRepository().inviteByProfile(groupId, profileId)

    suspend fun inviteByHandle(groupId: String, handle: String) =
        requireRepository().inviteByHandle(groupId, handle)

    suspend fun acceptInvite(inviteId: String) = requireRepository().acceptInvite(inviteId)

    suspend fun declineInvite(inviteId: String) = requireRepository().declineInvite(inviteId)

    suspend fun leaveGroup(groupId: String) = requireRepository().leaveGroup(groupId)

    suspend fun dissolveGroup(groupId: String) = requireRepository().dissolveGroup(groupId)

    suspend fun removeMember(groupId: String, profileId: String) =
        requireRepository().removeMember(groupId, profileId)

    /** Publishes the "no session" state: onboarding online, a typed error when Supabase is absent. */
    private fun clear() {
        _groupState.value = if (repository == null) {
            UiState.Error(MISSING_SUPABASE_MESSAGE)
        } else {
            UiState.Content(null)
        }
        _invites.value = emptyList()
    }

    /** Offline writes fail with the same typed exception the screens already render. */
    private fun requireRepository(): GroupDataSource =
        repository ?: throw GroupRepositoryException(MISSING_SUPABASE_MESSAGE)
}
