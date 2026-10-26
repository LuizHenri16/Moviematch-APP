package app.moviematch.data.supabase

import app.moviematch.domain.model.Group
import app.moviematch.domain.model.Profile
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** PostgREST resource name of the group table (see `supabase/0001_init.sql`). */
internal const val GROUPS_TABLE = "groups"

/**
 * Fallback name when the group has no readable members.
 *
 * `public.groups` has no `name` column (only `id`, `status`, `invite_code`, `pair_key`, ...), so the
 * display name is derived from the members, exactly like the seeded "A Roda" of the mock.
 */
internal const val DEFAULT_GROUP_NAME = "A Roda"

/**
 * `groups` joined with its members and their profiles.
 *
 * `left_at` is selected so the mapper can drop members that already left (soft-delete) and the
 * `slot` orders the pair. The profile embed reuses [ProfileRow], which is the same subset the
 * settings screen reads.
 */
internal const val GROUP_SELECT =
    "id,created_at,invite_code,created_by," +
        "group_members(slot,left_at,profile:profiles(id,display_name,avatar_url,created_at))"

/** One row of `public.group_members` with the embedded profile. */
@Serializable
internal data class GroupMemberRow(
    val slot: Int,
    @SerialName("left_at") val leftAt: String? = null,
    val profile: ProfileRow? = null,
)

/** One row of `public.groups` with its members. */
@Serializable
internal data class GroupRow(
    val id: String,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("invite_code") val inviteCode: String? = null,
    @SerialName("created_by") val createdBy: String? = null,
    @SerialName("group_members") val members: List<GroupMemberRow> = emptyList(),
)

/**
 * Builds the domain [Group].
 *
 * Members with `left_at` set are ignored and the rest are ordered by `slot`, so the pair comes out in
 * its canonical 1-2 order regardless of how PostgREST sorted the nested rows. The name joins the
 * member names with `" & "` and falls back to [DEFAULT_GROUP_NAME] when nobody could be read.
 */
internal fun GroupRow.toGroup(): Group {
    val activeMembers = members
        .filter { it.leftAt == null }
        .sortedBy { it.slot }
        .mapNotNull { it.profile?.toProfile() }
        .distinctBy { it.id }
    val name = activeMembers
        .map(Profile::displayName)
        .filter { it.isNotBlank() }
        .joinToString(" & ")
        .ifBlank { DEFAULT_GROUP_NAME }
    return Group(
        id = id,
        name = name,
        members = activeMembers,
        createdAt = createdAt.orEmpty(),
        inviteCode = inviteCode?.takeIf { it.isNotBlank() },
        createdBy = createdBy?.takeIf { it.isNotBlank() },
    )
}
