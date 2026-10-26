package app.moviematch.data.supabase

import app.moviematch.domain.GroupDataSource
import app.moviematch.domain.model.Group
import app.moviematch.domain.model.GroupInvite
import app.moviematch.domain.model.ProfileSummary
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.exception.PostgrestRestException
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** PostgREST resource of the invites (see `supabase/0001_init.sql`). */
internal const val GROUP_INVITES_TABLE = "group_invites"

/** The only columns an invite exposes to the guest; names of the pair stay behind the RLS policies. */
internal const val GROUP_INVITE_SELECT = "id,group_id,created_at,expires_at"

/** `join_group_by_code` RPC added by `supabase/0002_group_join_by_code.sql`. */
private const val JOIN_GROUP_BY_CODE_FUNCTION = "join_group_by_code"

private const val CREATE_GROUP_FUNCTION = "create_group"
private const val SEARCH_PROFILES_FUNCTION = "search_profiles"
private const val INVITE_BY_PROFILE_FUNCTION = "invite_by_profile"
private const val INVITE_BY_HANDLE_FUNCTION = "invite_by_handle"
private const val ACCEPT_INVITE_FUNCTION = "accept_invite"
private const val DECLINE_INVITE_FUNCTION = "decline_invite"
private const val LEAVE_GROUP_FUNCTION = "leave_group"
private const val DISSOLVE_GROUP_FUNCTION = "dissolve_group"
private const val REMOVE_MEMBER_FUNCTION = "remove_member"

/** `invite_status` value the invite list filters on. */
private const val PENDING_INVITE_STATUS = "pending"

/** PostgreSQL `unique_violation`, raised on duplicate keys (invites and group memberships). */
private const val UNIQUE_VIOLATION_CODE = "23505"

/** Copy shown when the invitee already has a pending invite from this group. */
private const val DUPLICATE_INVITE_MESSAGE = "Já existe um convite pendente para essa pessoa."

/** `group_members` table and its unique objects (partial indexes from `0004`, legacy constraints from `0001`). */
private const val GROUP_MEMBERS_TABLE = "group_members"
private const val GROUP_MEMBERS_SLOT_INDEX = "group_members_active_group_slot_uidx"
private const val GROUP_MEMBERS_SLOT_CONSTRAINT = "group_members_group_id_slot_key"
private const val GROUP_MEMBERS_PROFILE_INDEX = "group_members_active_group_profile_uidx"
private const val GROUP_MEMBERS_PROFILE_CONSTRAINT = "group_members_group_id_profile_id_key"

/** Copy used when a `23505` on `group_members` means the second slot is already taken. */
private const val GROUP_FULL_MESSAGE = "Esta roda já está completa."

/** Copy used when a `23505` on `group_members` means the caller is already a member. */
private const val ALREADY_IN_GROUP_MESSAGE = "Você já está nesta roda."

/** One row of `public.group_invites` as the guest reads it. */
@Serializable
internal data class GroupInviteRow(
    val id: String,
    @SerialName("group_id") val groupId: String,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("expires_at") val expiresAt: String? = null,
)

/** Maps the invite row onto the domain model. */
internal fun GroupInviteRow.toGroupInvite(): GroupInvite = GroupInvite(
    id = id,
    groupId = groupId,
    createdAt = createdAt.orEmpty(),
    expiresAt = expiresAt,
)

/** One row of the `search_profiles` RPC result set. */
@Serializable
internal data class ProfileSummaryRow(
    val id: String,
    val handle: String? = null,
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
)

/** Maps a search hit, reusing the same blank/`"Sem nome"` rules as [ProfileRow.toProfile]. */
internal fun ProfileSummaryRow.toProfileSummary(): ProfileSummary = ProfileSummary(
    id = id,
    displayName = displayName?.takeIf { it.isNotBlank() } ?: "Sem nome",
    handle = handle?.trim()?.takeIf { it.isNotBlank() },
    avatarUrl = avatarUrl?.takeIf { it.isNotBlank() },
)

// --- RPC payloads (one per function, named after the SQL parameters) ------------------------------

@Serializable
private data class JoinByCodeParams(@SerialName("p_code") val code: String)

@Serializable
private data class SearchProfilesParams(@SerialName("p_query") val query: String)

@Serializable
private data class InviteByProfileParams(
    @SerialName("p_group_id") val groupId: String,
    @SerialName("p_profile_id") val profileId: String,
)

@Serializable
private data class InviteByHandleParams(
    @SerialName("p_group_id") val groupId: String,
    @SerialName("p_handle") val handle: String,
)

@Serializable
private data class InviteIdParams(@SerialName("p_invite_id") val inviteId: String)

/** `leave_group` / `dissolve_group` both receive the target group as `p_group_id`. */
@Serializable
private data class GroupIdParams(@SerialName("p_group_id") val groupId: String)

@Serializable
private data class RemoveMemberParams(
    @SerialName("p_group_id") val groupId: String,
    @SerialName("p_profile_id") val profileId: String,
)

/**
 * Group and invite flow against the real backend.
 *
 * It owns every write of the "roda" lifecycle the app performs, always through the `SECURITY DEFINER`
 * RPCs of `supabase/0001_init.sql` (+ `0002_group_join_by_code.sql` and
 * `0004_fix_group_lifecycle.sql`): `groups`, `group_members` and `group_invites` deliberately have
 * **no** INSERT/UPDATE policy, so writing them directly is impossible and not attempted here.
 *
 * Three decisions worth knowing before reading the code:
 * * **Reads respect RLS.** [activeGroup] and [pendingInvites] rely entirely on the policies
 *   (`groups_select_member`, `group_invites_select_target_or_member`): a member sees their pair, a
 *   guest sees only the invites aimed at them. That is also why a pending invite shows no group name —
 *   the group and the inviter's profile are not readable before the invite is accepted.
 * * **Mutations do not decode their return.** `create_group`/`accept_invite`/`join_group_by_code`
 *   answer with the group row and `invite_by_*` with the invite row, but every caller immediately
 *   re-reads [activeGroup] (which also brings the members and the `invite_code` the RPC return may
 *   omit), so the repository only cares whether the call raised.
 * * **Typed errors.** Every failure is a [GroupRepositoryException] whose message is ready for the
 *   screen; the stable `raise exception '<code>'` identifiers of the SQL are mapped by
 *   [groupErrorMessage].
 *
 * @param supabaseClient client with the Postgrest plugin installed and an authenticated session; the
 *   Koin `dataModule` only registers this repository when the `.env` carries Supabase credentials.
 */
class GroupRepository(
    private val supabaseClient: SupabaseClient,
) : GroupDataSource {

    /**
     * The group the signed-in member belongs to, or `null` when they are not in one yet.
     *
     * The `groups_select_member` policy returns rows only to active members, so "no group" and "group
     * of somebody else" both come back as an empty list.
     */
    override suspend fun activeGroup(): Group? = guarded("Não foi possível carregar sua roda.") {
        supabaseClient.from(GROUPS_TABLE)
            .select(Columns.raw(GROUP_SELECT)) { limit(1) }
            .decodeList<GroupRow>()
            .firstOrNull()
            ?.toGroup()
    }

    /**
     * Invites the signed-in member can answer, newest first.
     *
     * No client-side target filter is needed: the `group_invites_select_target_or_member` policy
     * already narrows the rows to the invites addressed to the caller (by profile id or by handle).
     * The screen is only shown while the caller has no active group, so the "member" arm of that
     * policy never widens this result.
     */
    override suspend fun pendingInvites(): List<GroupInvite> = guarded("Não foi possível carregar seus convites.") {
        supabaseClient.from(GROUP_INVITES_TABLE)
            .select(Columns.raw(GROUP_INVITE_SELECT)) {
                filter { eq("status", PENDING_INVITE_STATUS) }
                order("created_at", Order.DESCENDING)
            }
            .decodeList<GroupInviteRow>()
            .map { it.toGroupInvite() }
    }

    /**
     * Creates an `active` group with a fresh `invite_code` and puts the caller in `slot = 1`.
     *
     * @throws GroupRepositoryException `already_in_active_group` when the member is already in one.
     */
    override suspend fun createGroup() {
        guarded("Não foi possível criar a roda.") {
            supabaseClient.postgrest.rpc(CREATE_GROUP_FUNCTION)
        }
    }

    /**
     * Joins the group that owns [code] (the "convite por código"), through the `join_group_by_code`
     * RPC added by `supabase/0002_group_join_by_code.sql`.
     *
     * The RPC's return shape is not decoded on purpose: the screen always re-reads [activeGroup]
     * afterwards, so a scalar/`jsonb`/row response all work the same and only a raised error matters.
     *
     * @throws GroupRepositoryException `invalid_invite_code`, `already_in_active_group` or
     *   `group_full` when the RPC rejects the code.
     */
    override suspend fun joinGroupByCode(code: String) {
        guarded("Não foi possível entrar com esse código.") {
            supabaseClient.postgrest.rpc(JOIN_GROUP_BY_CODE_FUNCTION, JoinByCodeParams(code.trim()))
        }
    }

    /**
     * Searches profiles by `handle`/`display_name` through the `search_profiles` RPC.
     *
     * The RPC itself ignores queries shorter than 2 characters and caps the answer at 20 rows; the
     * `ViewModel` applies the same minimum before calling, so the field never fires a useless request.
     */
    override suspend fun searchProfiles(query: String): List<ProfileSummary> =
        guarded("Não foi possível buscar pessoas agora.") {
            supabaseClient.postgrest.rpc(SEARCH_PROFILES_FUNCTION, SearchProfilesParams(query.trim()))
                .decodeList<ProfileSummaryRow>()
                .map { it.toProfileSummary() }
        }

    /**
     * Invites an existing profile to [groupId] (`invite_by_profile`).
     *
     * @throws GroupRepositoryException `cannot_invite_self`, `already_a_member`, `group_full`,
     *   `profile_not_found`, `not_a_member` or a duplicate-pending-invite message.
     */
    override suspend fun inviteByProfile(groupId: String, profileId: String) {
        guarded("Não foi possível enviar o convite.") {
            supabaseClient.postgrest.rpc(
                INVITE_BY_PROFILE_FUNCTION,
                InviteByProfileParams(groupId, profileId),
            )
        }
    }

    /**
     * Invites by `handle` (`invite_by_handle`) without searching first — the path that also reaches
     * somebody whose account does not exist yet.
     *
     * @throws GroupRepositoryException `handle_required`, `cannot_invite_self`, `group_full` or
     *   `not_a_member`.
     */
    override suspend fun inviteByHandle(groupId: String, handle: String) {
        guarded("Não foi possível enviar o convite por handle.") {
            supabaseClient.postgrest.rpc(INVITE_BY_HANDLE_FUNCTION, InviteByHandleParams(groupId, handle.trim()))
        }
    }

    /**
     * Accepts an invite: the caller takes `slot = 2`, the pair key is written and the invite becomes
     * `accepted`.
     *
     * @throws GroupRepositoryException `invite_expired`, `not_invited`, `invite_not_pending`,
     *   `already_in_active_group`, `group_not_active`, `group_full` or `already_a_member`.
     */
    override suspend fun acceptInvite(inviteId: String) {
        guarded("Não foi possível aceitar o convite.") {
            supabaseClient.postgrest.rpc(ACCEPT_INVITE_FUNCTION, InviteIdParams(inviteId))
        }
    }

    /**
     * Declines an invite (`decline_invite`), leaving the group untouched.
     *
     * @throws GroupRepositoryException when the invite is not pending or not addressed to the caller.
     */
    override suspend fun declineInvite(inviteId: String) {
        guarded("Não foi possível recusar o convite.") {
            supabaseClient.postgrest.rpc(DECLINE_INVITE_FUNCTION, InviteIdParams(inviteId))
        }
    }

    /**
     * Leaves [groupId] (`leave_group`).
     *
     * The `p_group_id` argument is mandatory: the SQL signature is `leave_group(p_group_id uuid)`,
     * so calling the RPC without it always fails. The caller's membership is soft-deleted
     * (`left_at = now()`); if the group becomes empty it is dissolved and if only one member remains
     * the `pair_key` is cleared.
     *
     * @throws GroupRepositoryException `not_authenticated`, `not_a_member`.
     */
    override suspend fun leaveGroup(groupId: String) {
        guarded("Não foi possível sair da roda.") {
            supabaseClient.postgrest.rpc(LEAVE_GROUP_FUNCTION, GroupIdParams(groupId))
        }
    }

    /**
     * Dissolves [groupId] (`dissolve_group`).
     *
     * Like [leaveGroup], the `p_group_id` argument is mandatory (`dissolve_group(p_group_id uuid)`).
     * Soft-deletes the group (`status = 'dissolved'`), preserving all data but making it invisible
     * via RLS. Only an active member can call this.
     *
     * @throws GroupRepositoryException `not_authenticated`, `not_a_member`, `group_not_active`.
     */
    override suspend fun dissolveGroup(groupId: String) {
        guarded("Não foi possível dissolver a roda.") {
            supabaseClient.postgrest.rpc(DISSOLVE_GROUP_FUNCTION, GroupIdParams(groupId))
        }
    }

    /**
     * Removes another member from the group (`remove_member`).
     *
     * Only the group creator (slot = 1) can remove the other member (slot = 2).
     * The removed member's membership is soft-deleted. If the group becomes empty,
     * it is dissolved. If only one member remains, the pair_key is cleared.
     *
     * @throws GroupRepositoryException `not_authenticated`, `not_a_member`,
     *   `not_group_creator`, `cannot_remove_self`, `member_not_found`,
     *   `cannot_remove_creator`, `group_not_active`.
     */
    override suspend fun removeMember(groupId: String, profileId: String) {
        guarded("Não foi possível remover o membro.") {
            supabaseClient.postgrest.rpc(REMOVE_MEMBER_FUNCTION, RemoveMemberParams(groupId, profileId))
        }
    }
}

/**
 * User-facing copy of every stable `raise exception '<code>'` of the group RPCs, keyed by the
 * identifier the database sends as the PostgREST message (see `0001_init.sql` / `0002`).
 */
private val GROUP_ERROR_MESSAGES: Map<String, String> = mapOf(
    "not_authenticated" to "Sua sessão expirou. Entre de novo para continuar.",
    "already_in_active_group" to "Você já está em uma roda ativa.",
    "group_full" to "Esta roda já está completa.",
    "group_not_active" to "Esta roda não está mais ativa.",
    "invalid_invite_code" to "Código de convite inválido. Confira e tente de novo.",
    "invite_expired" to "Este convite expirou. Peça um convite novo.",
    "not_invited" to "Este convite não é para você.",
    "invite_not_pending" to "Este convite não está mais pendente.",
    "invite_not_found_or_not_pending" to "Este convite não está mais disponível.",
    "invite_not_found" to "Convite não encontrado.",
    "handle_required" to "Digite um @handle para convidar.",
    "cannot_invite_self" to "Você não pode convidar você mesmo.",
    "already_a_member" to "Essa pessoa já está na sua roda.",
    "not_a_member" to "Sua conta não faz parte desta roda.",
    "profile_not_found" to "Perfil não encontrado.",
    "not_group_creator" to "Apenas quem criou a roda pode remover membros.",
    "cannot_remove_self" to "Você não pode se remover. Use \"Sair da roda\".",
    "member_not_found" to "Essa pessoa não está na sua roda.",
    "cannot_remove_creator" to "Não é possível remover quem criou a roda.",
)

/**
 * Turns a failed call into the pt-BR copy the screen shows.
 *
 * Only a [PostgrestRestException] carries a database identifier, so anything else (a network failure,
 * for instance) degrades to the caller's [fallback] instead of an English transport message. Two
 * errors have no stable identifier because Postgres reports the generic
 * `duplicate key value violates unique constraint "<name>"` with SQLSTATE `23505`: a duplicate
 * pending invite and a duplicate/occupied `group_members` slot or profile. Both are matched by the
 * unique object name (new partial indexes or legacy constraints), after the stable identifiers above
 * have had their chance.
 */
internal fun groupErrorMessage(error: Throwable, fallback: String): String {
    val response = error as? PostgrestRestException ?: return fallback
    GROUP_ERROR_MESSAGES[response.error]?.let { return it }
    if (response.code != UNIQUE_VIOLATION_CODE) return fallback

    // The unique object name may appear in several fields of the PostgREST envelope depending on the
    // client version — check every plausible string field, including the wrapped cause.
    val haystack = buildString {
        append(response.error)
        append(response.message)
        response.details?.let { append(it) }
        response.hint?.let { append(it) }
        response.cause?.message?.let { append(it) }
        append(response.toString())
    }

    return when {
        GROUP_INVITES_TABLE in haystack -> DUPLICATE_INVITE_MESSAGE

        // Slot 2 taken: either the new partial index or the legacy non-partial constraint.
        GROUP_MEMBERS_SLOT_INDEX in haystack ||
            GROUP_MEMBERS_SLOT_CONSTRAINT in haystack ||
            (GROUP_MEMBERS_TABLE in haystack && "slot" in haystack) -> GROUP_FULL_MESSAGE

        // Same profile already has a (possibly soft-deleted) row in this group.
        GROUP_MEMBERS_PROFILE_INDEX in haystack ||
            GROUP_MEMBERS_PROFILE_CONSTRAINT in haystack ||
            (GROUP_MEMBERS_TABLE in haystack && "profile" in haystack) -> ALREADY_IN_GROUP_MESSAGE

        else -> fallback
    }
}

/**
 * Wraps a repository call, turning any failure into a [GroupRepositoryException] ready for the UI.
 * Cancellations and already-mapped errors pass through untouched, so a more specific message raised
 * deeper in the call is never replaced by a generic one.
 */
private suspend fun <T> guarded(message: String, block: suspend () -> T): T = try {
    block()
} catch (e: CancellationException) {
    throw e
} catch (e: GroupRepositoryException) {
    throw e
} catch (e: Exception) {
    throw GroupRepositoryException(groupErrorMessage(e, message), e)
}

/** Raised when a group RPC fails; the message is already user-facing Portuguese copy. */
class GroupRepositoryException(message: String, cause: Throwable? = null) : Exception(message, cause)
