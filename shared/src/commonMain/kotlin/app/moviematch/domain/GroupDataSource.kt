package app.moviematch.domain

import app.moviematch.domain.model.Group
import app.moviematch.domain.model.GroupInvite
import app.moviematch.domain.model.ProfileSummary

/**
 * Read/write contract of the "roda" (group) lifecycle.
 *
 * It mirrors the public surface of
 * [app.moviematch.data.supabase.GroupRepository] so the presentation layer can depend on an
 * interface instead of the Supabase implementation:
 * * `GroupStore` and the `ViewModel`s stay backend agnostic and testable with an in-memory fake.
 * * The Koin graph can resolve `null` when Supabase is not configured (`getOrNull()`), instead of
 *   failing to build the object graph while the app runs offline.
 *
 * Every mutation is addressed by the `groupId` explicitly: the SQL RPCs of `supabase/0001_init.sql`
 * (`leave_group`, `dissolve_group`) and `0003_remove_member.sql` receive `p_group_id`, so a call
 * without the id can never succeed.
 */
interface GroupDataSource {

    /** The group the signed-in member belongs to, or `null` when they are not in one yet. */
    suspend fun activeGroup(): Group?

    /** Invites the signed-in member can answer, newest first. */
    suspend fun pendingInvites(): List<GroupInvite>

    /** Creates an `active` group and puts the caller in `slot = 1`. */
    suspend fun createGroup()

    /** Joins the group that owns [code] (the "convite por código"). */
    suspend fun joinGroupByCode(code: String)

    /** Searches profiles by `handle`/`display_name` for the invite form. */
    suspend fun searchProfiles(query: String): List<ProfileSummary>

    /** Invites an existing profile to [groupId]. */
    suspend fun inviteByProfile(groupId: String, profileId: String)

    /** Invites by `handle`, the path that also reaches somebody without an account yet. */
    suspend fun inviteByHandle(groupId: String, handle: String)

    /** Accepts an invite, putting the caller in `slot = 2`. */
    suspend fun acceptInvite(inviteId: String)

    /** Declines an invite, leaving the group untouched. */
    suspend fun declineInvite(inviteId: String)

    /** Leaves [groupId] (soft-deletes the caller's membership). */
    suspend fun leaveGroup(groupId: String)

    /** Dissolves [groupId] (soft-deletes the whole group). */
    suspend fun dissolveGroup(groupId: String)

    /** Removes another member from [groupId]. Only the creator can do this. */
    suspend fun removeMember(groupId: String, profileId: String)
}
