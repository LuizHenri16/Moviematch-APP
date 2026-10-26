package app.moviematch.presentation.group

import app.moviematch.data.mock.MockData
import app.moviematch.domain.GroupDataSource
import app.moviematch.domain.model.Group
import app.moviematch.domain.model.GroupInvite
import app.moviematch.domain.model.Profile
import app.moviematch.domain.model.ProfileSummary

/**
 * In-memory [GroupDataSource] for `GroupStore`/`GroupViewModel` tests.
 *
 * It records every call as `"name:arg:arg"` and can be told to fail a given operation through
 * [failures] (keyed by the call-name prefix). Mutations update [group] so a following `refresh()`
 * observes the same state a real backend would.
 */
internal class FakeGroupDataSource(
    var group: Group? = null,
    var invites: List<GroupInvite> = emptyList(),
) : GroupDataSource {

    val calls = mutableListOf<String>()

    /** Exceptions keyed by call-name prefix (`"leaveGroup"`, `"removeMember"`, ...). */
    val failures = mutableMapOf<String, Exception>()

    private fun record(call: String) {
        calls += call
        failures.entries.firstOrNull { call.startsWith(it.key) }?.let { throw it.value }
    }

    override suspend fun activeGroup(): Group? {
        calls += "activeGroup"
        return group
    }

    override suspend fun pendingInvites(): List<GroupInvite> {
        calls += "pendingInvites"
        return invites
    }

    override suspend fun createGroup() = record("createGroup")

    override suspend fun joinGroupByCode(code: String) = record("joinGroupByCode:$code")

    override suspend fun searchProfiles(query: String): List<ProfileSummary> {
        calls += "searchProfiles:$query"
        return emptyList()
    }

    override suspend fun inviteByProfile(groupId: String, profileId: String) =
        record("inviteByProfile:$groupId:$profileId")

    override suspend fun inviteByHandle(groupId: String, handle: String) =
        record("inviteByHandle:$groupId:$handle")

    override suspend fun acceptInvite(inviteId: String) {
        record("acceptInvite:$inviteId")
        // Mirrors the backend: the invite is answered, the guest joins slot 2 and the
        // following refresh sees the group (and no longer sees the invite).
        invites = invites.filterNot { it.id == inviteId }
        group = group ?: sampleGroup()
    }

    override suspend fun declineInvite(inviteId: String) = record("declineInvite:$inviteId")

    override suspend fun leaveGroup(groupId: String) {
        record("leaveGroup:$groupId")
        group = null
    }

    override suspend fun dissolveGroup(groupId: String) {
        record("dissolveGroup:$groupId")
        group = null
    }

    override suspend fun removeMember(groupId: String, profileId: String) {
        record("removeMember:$groupId:$profileId")
        group = group?.let { current ->
            current.copy(members = current.members.filterNot { it.id == profileId })
        }
    }
}

/** A complete group used as the default state of the fakes. */
internal fun sampleGroup(
    id: String = "group-1",
    members: List<Profile> = listOf(MockData.maria, MockData.luiz),
    createdBy: String? = MockData.maria.id,
): Group = Group(
    id = id,
    name = members.joinToString(" & ") { it.displayName }.ifBlank { "A Roda" },
    members = members,
    createdAt = "2026-09-01T10:00:00.000Z",
    inviteCode = "3F9A20C7",
    createdBy = createdBy,
)

/** An invite the guest can answer. */
internal fun sampleInvite(id: String = "invite-1", groupId: String = "group-2"): GroupInvite =
    GroupInvite(id = id, groupId = groupId, createdAt = "2026-10-01T00:00:00.000Z")
