package app.moviematch.presentation.group

import app.moviematch.auth.AuthState
import app.moviematch.auth.AuthStore
import app.moviematch.data.mock.MockData
import app.moviematch.data.supabase.GroupRepositoryException
import app.moviematch.presentation.common.CurrentProfileStore
import app.moviematch.presentation.common.UiState
import app.moviematch.presentation.common.dataOrNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Covers the app-scoped [GroupStore] with an in-memory [FakeGroupDataSource].
 *
 * The store owns the shared state that fixes the stale "Minha roda" card, so these tests pin the
 * two things that matter: the state flips to "no group" after leaving (defect 1 + S3/S4 -> S1) and
 * an offline store degrades to a typed error instead of throwing out of the Koin graph.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GroupStoreTest {

    private fun signedIn(profile: CurrentProfileStore): AuthStore = AuthStore(
        supabaseClient = null,
        currentProfile = profile,
        initialState = AuthState.SignedIn(profile = profile.profile.value, email = null),
    )

    private fun TestScope.store(
        data: FakeGroupDataSource?,
        profile: CurrentProfileStore,
        signedIn: Boolean = true,
    ): GroupStore {
        val auth = if (signedIn) signedIn(profile) else AuthStore(supabaseClient = null, currentProfile = profile)
        return GroupStore(
            repository = data,
            currentProfile = profile,
            authStore = auth,
            // Unconfined so the initial `refresh()` triggered by the seeded SignedIn state has
            // already completed when the test body starts.
            scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler)),
        )
    }

    @Test
    fun refreshPublishesGroupAndInvitesWhenSignedIn() = runTest {
        val profile = CurrentProfileStore(MockData.maria)

        val withGroup = FakeGroupDataSource(group = sampleGroup())
        val groupStore = store(withGroup, profile)
        advanceUntilIdle()

        assertEquals("group-1", groupStore.groupState.value.dataOrNull()?.id)
        assertEquals(
            emptyList(),
            groupStore.invites.value,
            "a member already in a group does not need the guest invite list",
        )
        assertTrue("pendingInvites" !in withGroup.calls)

        val withoutGroup = FakeGroupDataSource(group = null, invites = listOf(sampleInvite()))
        val invitesStore = store(withoutGroup, profile)
        advanceUntilIdle()

        assertNull(invitesStore.groupState.value.dataOrNull())
        assertEquals(listOf("invite-1"), invitesStore.invites.value.map { it.id })
    }

    @Test
    fun refreshPublishesNullGroupWhenThereIsNoActiveOne() = runTest {
        val profile = CurrentProfileStore(MockData.maria)
        val groupStore = store(FakeGroupDataSource(group = null), profile)
        advanceUntilIdle()

        assertNull(groupStore.groupState.value.dataOrNull())
        assertEquals(emptyList(), groupStore.invites.value)
    }

    @Test
    fun offlineStorePublishesTypedErrorInsteadOfThrowing() = runTest {
        val profile = CurrentProfileStore(MockData.maria)
        val groupStore = store(data = null, profile = profile)
        advanceUntilIdle()

        val error = assertIs<UiState.Error>(groupStore.groupState.value)
        assertTrue("Supabase não configurado" in error.message, error.message)

        // A manual retry (the StateError button, for instance) must stay safe too.
        groupStore.refresh()
        assertIs<UiState.Error>(groupStore.groupState.value)
    }

    @Test
    fun offlineWritesFailWithTheTypedGroupError() = runTest {
        val groupStore = store(data = null, profile = CurrentProfileStore(MockData.maria))

        val failure = assertFailsWith<GroupRepositoryException> {
            groupStore.leaveGroup("group-1")
        }
        assertTrue("Supabase não configurado" in failure.message.orEmpty(), failure.message.orEmpty())
    }

    @Test
    fun leaveGroupThenRefreshFlipsStateToNoGroup() = runTest {
        val profile = CurrentProfileStore(MockData.maria)
        val data = FakeGroupDataSource(group = sampleGroup())
        val groupStore = store(data, profile)
        advanceUntilIdle()

        assertEquals("group-1", groupStore.groupState.value.dataOrNull()?.id)

        groupStore.leaveGroup("group-1")
        groupStore.refresh()

        assertNull(groupStore.groupState.value.dataOrNull(), "leaving flips S3/S4 back to S1")
        assertTrue("leaveGroup:group-1" in data.calls, "calls=${data.calls}")
    }

    @Test
    fun acceptingInvitePublishesTheGroupAndHidesInvites() = runTest {
        val profile = CurrentProfileStore(MockData.maria)
        val data = FakeGroupDataSource(group = null, invites = listOf(sampleInvite()))
        val groupStore = store(data, profile)
        advanceUntilIdle()

        assertNull(groupStore.groupState.value.dataOrNull())
        assertEquals(listOf("invite-1"), groupStore.invites.value.map { it.id })

        groupStore.acceptInvite("invite-1")
        groupStore.refresh()

        assertEquals("group-1", groupStore.groupState.value.dataOrNull()?.id)
        assertEquals(emptyList(), groupStore.invites.value, "a member must not see the guest invite list")
        assertTrue("acceptInvite:invite-1" in data.calls, "calls=${data.calls}")
        assertEquals(
            2,
            data.calls.count { it == "activeGroup" },
            "the store must refresh the group after accepting: calls=${data.calls}",
        )
    }

    /**
     * Regression for the reported bug: dissolving the group used to keep the caller's
     * `group_members.left_at = null`, so the next `accept_invite` failed with
     * `already_in_active_group` and the screen fell back to "Não foi possível aceitar o convite.".
     * The store now observes "no group" after dissolving and still accepts a pending invite.
     */
    @Test
    fun anInviteCanBeAcceptedAfterDissolvingThePreviousGroup() = runTest {
        val profile = CurrentProfileStore(MockData.maria)
        val data = FakeGroupDataSource(group = sampleGroup(), invites = listOf(sampleInvite()))
        val groupStore = store(data, profile)
        advanceUntilIdle()

        assertEquals("group-1", groupStore.groupState.value.dataOrNull()?.id)

        groupStore.dissolveGroup("group-1")
        groupStore.refresh()

        assertNull(groupStore.groupState.value.dataOrNull(), "dissolving must release the membership")
        assertEquals(listOf("invite-1"), groupStore.invites.value.map { it.id })

        groupStore.acceptInvite("invite-1")
        groupStore.refresh()

        assertEquals("group-1", groupStore.groupState.value.dataOrNull()?.id, "the old membership must not block the invite")
        assertEquals(emptyList(), groupStore.invites.value)
        assertTrue("dissolveGroup:group-1" in data.calls, "calls=${data.calls}")
        assertTrue("acceptInvite:invite-1" in data.calls, "calls=${data.calls}")
    }
}
