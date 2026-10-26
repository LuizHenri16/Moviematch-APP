package app.moviematch.presentation.group

import app.moviematch.auth.AuthState
import app.moviematch.auth.AuthStore
import app.moviematch.data.mock.MockData
import app.moviematch.data.supabase.GroupRepositoryException
import app.moviematch.domain.model.Profile
import app.moviematch.presentation.common.CurrentProfileStore
import app.moviematch.presentation.common.dataOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Covers [GroupViewModel] against a [GroupStore] backed by [FakeGroupDataSource].
 *
 * It pins the two defects the store migration was meant to fix: destructive actions carry the
 * current `group_id` (defect 1) and `isCreator` comes from `groups.created_by` (defect 6), plus the
 * typed-error path that must keep the group on screen.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GroupViewModelTest {

    private val mainDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(mainDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.newViewModel(
        data: FakeGroupDataSource,
        me: Profile = MockData.maria,
    ): GroupViewModel {
        val profile = CurrentProfileStore(me)
        val auth = AuthStore(
            supabaseClient = null,
            currentProfile = profile,
            initialState = AuthState.SignedIn(profile = me, email = null),
        )
        val store = GroupStore(data, profile, auth, backgroundScope)
        return GroupViewModel(store = store, currentProfile = profile, authStore = auth)
    }

    @Test
    fun leavingCallsStoreWithTheCurrentGroupId() = runTest(mainDispatcher) {
        val data = FakeGroupDataSource(group = sampleGroup())
        val viewModel = newViewModel(data)
        advanceUntilIdle()

        viewModel.onLeaveGroup()
        advanceUntilIdle()

        assertTrue("leaveGroup:group-1" in data.calls, "calls=${data.calls}")
    }

    @Test
    fun dissolvingCallsStoreWithTheCurrentGroupId() = runTest(mainDispatcher) {
        val data = FakeGroupDataSource(group = sampleGroup())
        val viewModel = newViewModel(data)
        advanceUntilIdle()

        viewModel.onDissolveGroup()
        advanceUntilIdle()

        assertTrue("dissolveGroup:group-1" in data.calls, "calls=${data.calls}")
    }

    @Test
    fun removingPartnerCallsStoreWithGroupAndProfile() = runTest(mainDispatcher) {
        val data = FakeGroupDataSource(group = sampleGroup())
        val viewModel = newViewModel(data)
        advanceUntilIdle()

        viewModel.onRemoveMember("profile-luiz")
        advanceUntilIdle()

        assertTrue("removeMember:group-1:profile-luiz" in data.calls, "calls=${data.calls}")
    }

    @Test
    fun overviewMarksIsCreatorFromCreatedBy() = runTest(mainDispatcher) {
        val data = FakeGroupDataSource(group = sampleGroup(createdBy = MockData.maria.id))
        val viewModel = newViewModel(data, me = MockData.maria)
        advanceUntilIdle()

        assertTrue(assertNotNull(viewModel.state.value.dataOrNull()).isCreator)

        // The creator left and the remaining member (slot 2) must lose the creator-only action.
        data.group = sampleGroup(createdBy = MockData.luiz.id)
        viewModel.load()
        advanceUntilIdle()

        assertFalse(assertNotNull(viewModel.state.value.dataOrNull()).isCreator)
    }

    @Test
    fun aFailedActionBecomesAnErrorMessageAndKeepsTheGroup() = runTest(mainDispatcher) {
        val data = FakeGroupDataSource(group = sampleGroup())
        data.failures["leaveGroup"] = GroupRepositoryException("Não foi possível sair da roda.")
        val viewModel = newViewModel(data)
        advanceUntilIdle()

        viewModel.onLeaveGroup()
        advanceUntilIdle()

        val message = assertNotNull(viewModel.message.value)
        assertTrue(message.isError)
        assertEquals("Não foi possível sair da roda.", message.text)
        assertEquals("group-1", assertNotNull(viewModel.state.value.dataOrNull()).group?.id)
    }

    @Test
    fun acceptingInviteCallsTheStoreAndRefreshesIntoTheGroupState() = runTest(mainDispatcher) {
        val data = FakeGroupDataSource(group = null, invites = listOf(sampleInvite()))
        val viewModel = newViewModel(data)
        advanceUntilIdle()

        val before = assertNotNull(viewModel.state.value.dataOrNull())
        assertNull(before.group)
        assertEquals(listOf("invite-1"), before.invites.map { it.id })

        viewModel.onAcceptInvite(before.invites.single())
        advanceUntilIdle()

        assertTrue("acceptInvite:invite-1" in data.calls, "calls=${data.calls}")
        val after = assertNotNull(viewModel.state.value.dataOrNull())
        assertEquals("group-1", after.group?.id, "the refresh must flip the screen to the group state")
        assertTrue(after.invites.isEmpty(), "the invites disappear once the member is in the group")
    }
}
