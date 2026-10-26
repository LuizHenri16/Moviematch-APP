package app.moviematch.data.supabase

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** JSON response headers shared by every fake backend of this file. */
private val jsonResponseHeaders = headersOf(HttpHeaders.ContentType, "application/json")

/**
 * Covers [GroupRepository] against a Ktor [MockEngine].
 *
 * The engine answers the real PostgREST URLs the repository builds (`/rest/v1/<table>`,
 * `/rest/v1/rpc/<fn>`) and the assertions look at the outgoing requests — path, method, query
 * parameters and JSON body — so the contract with `supabase/0001_init.sql` (and with the
 * `join_group_by_code` RPC of `supabase/0002_group_join_by_code.sql`) is checked without a database.
 *
 * The error tests reply with the exact envelopes PostgREST sends for a `raise exception`: the
 * identifier the SQL raises lands in `message`, and the SQLSTATE in `code`. That is what lets the
 * repository tell `group_full` apart from `already_in_active_group`, which share SQLSTATE `23505`.
 */
class GroupRepositoryTest {
    // --- harness ---------------------------------------------------------------------------

    /** A response double: `status to body`, or `null` when the route is not expected. */
    private class Backend(private val route: (HttpRequestData) -> Pair<HttpStatusCode, String>?) {

        val engine = MockEngine { request ->
            val (status, body) = route(request) ?: error("Unexpected request: ${request.method.value} ${request.url}")
            respond(content = body, status = status, headers = jsonResponseHeaders)
        }

        val requests: List<HttpRequestData> get() = engine.requestHistory

        fun single(pathSuffix: String, method: HttpMethod? = null): HttpRequestData =
            requests.single { request ->
                request.url.encodedPath.endsWith(pathSuffix) && (method == null || request.method == method)
            }
    }

    private class Harness(val repository: GroupRepository, val backend: Backend)

    private fun harness(route: (HttpRequestData) -> Pair<HttpStatusCode, String>?): Harness {
        val backend = Backend(route)
        val client = createSupabaseClient(
            supabaseUrl = "https://project.supabase.co",
            supabaseKey = "anon-key",
        ) {
            httpEngine = backend.engine
            install(Postgrest)
        }
        return Harness(GroupRepository(supabaseClient = client), backend)
    }

    /** A repository whose only backend answers [identifier] as a raised database error. */
    private fun failingRepository(identifier: String, code: String): GroupRepository =
        harness { HttpStatusCode.BadRequest to errorBody(identifier, code) }.repository

    // --- fixtures --------------------------------------------------------------------------

    private companion object {
        const val JOINED_GROUP = """
            [
              {
                "id": "group-1",
                "created_at": "2026-09-01T10:00:00.000Z",
                "invite_code": "3F9A20C7",
                "created_by": "profile-luiz",
                "group_members": [
                  { "slot": 2, "left_at": null, "profile": { "id": "profile-maria", "display_name": "Maria", "avatar_url": null, "created_at": "2026-09-01T00:00:00.000Z" } },
                  { "slot": 1, "left_at": null, "profile": { "id": "profile-luiz", "display_name": "Luiz", "avatar_url": "https://cdn.test/luiz.jpg", "created_at": "2026-09-01T00:00:00.000Z" } },
                  { "slot": 3, "left_at": "2026-10-01T00:00:00.000Z", "profile": { "id": "profile-former", "display_name": "Ex", "avatar_url": null, "created_at": "2026-09-01T00:00:00.000Z" } }
                ]
              }
            ]
        """

        /** Same pair, but the creator never got an invite code back (null and blank both count). */
        const val GROUP_WITHOUT_CODE = """
            [
              {
                "id": "group-2",
                "created_at": "2026-09-01T10:00:00.000Z",
                "invite_code": "   ",
                "group_members": [
                  { "slot": 1, "left_at": null, "profile": { "id": "profile-luiz", "display_name": "Luiz", "avatar_url": null, "created_at": "2026-09-01T00:00:00.000Z" } }
                ]
              }
            ]
        """

        const val PENDING_INVITES = """
            [
              { "id": "invite-2", "group_id": "group-9", "created_at": "2026-10-02T00:00:00.000Z", "expires_at": "2026-10-09T00:00:00.000Z" },
              { "id": "invite-1", "group_id": "group-8", "created_at": "2026-10-01T00:00:00.000Z", "expires_at": null }
            ]
        """

        const val SEARCH_HITS = """
            [
              { "id": "profile-maria", "handle": "maria", "display_name": "Maria", "avatar_url": null },
              { "id": "profile-nameless", "handle": null, "display_name": "   ", "avatar_url": "https://cdn.test/x.jpg" },
              { "id": "profile-joao", "handle": "  joao  ", "display_name": "João", "avatar_url": null }
            ]
        """

        fun errorBody(message: String, code: String): String =
            """{"code":"$code","message":"$message","details":null,"hint":null}"""
    }

    // --- reads -----------------------------------------------------------------------------

    @Test
    fun activeGroupReadsThePairInSlotOrderAndTheInviteCode() = runTest {
        val test = harness { HttpStatusCode.OK to JOINED_GROUP }

        val group = test.repository.activeGroup()

        assertEquals("group-1", group?.id)
        assertEquals("Luiz & Maria", group?.name, "members come out in slot order, whoever replied first")
        assertEquals(listOf("profile-luiz", "profile-maria"), group?.members?.map { it.id })
        assertEquals("https://cdn.test/luiz.jpg", group?.members?.first()?.avatarUrl)
        assertEquals("3F9A20C7", group?.inviteCode)
        assertEquals("profile-luiz", group?.createdBy, "the creator identity gates remove_member")
    }

    @Test
    fun activeGroupAsksForTheInviteCodeAndTheMembersInOneRoundTrip() = runTest {
        val test = harness { HttpStatusCode.OK to JOINED_GROUP }

        test.repository.activeGroup()

        val request = test.backend.single("/rest/v1/groups")
        val select = request.url.parameters["select"].orEmpty()
        assertTrue("invite_code" in select, "the shareable code comes with the group: $select")
        assertTrue("created_by" in select, "the creator identity comes with the group: $select")
        assertTrue(
            "group_members(slot,left_at,profile:profiles(id,display_name,avatar_url,created_at))" in select,
            "the embed is untouched so the existing mapper keeps working: $select",
        )
        assertTrue(" " !in select, "PostgREST rejects a select string with spaces: $select")
        assertEquals("1", request.url.parameters["limit"], "one active group per member")
    }

    @Test
    fun aBlankInviteCodeIsReadAsAbsent() = runTest {
        val test = harness { HttpStatusCode.OK to GROUP_WITHOUT_CODE }

        assertNull(test.repository.activeGroup()?.inviteCode)
    }

    @Test
    fun thereIsNoActiveGroupWhenTheMemberBelongsToNone() = runTest {
        val test = harness { HttpStatusCode.OK to "[]" }

        assertNull(test.repository.activeGroup(), "RLS hides the groups of other people, so none means none")
    }

    @Test
    fun pendingInvitesOnlyAskForPendingOnesNewestFirst() = runTest {
        val test = harness { HttpStatusCode.OK to PENDING_INVITES }

        val invites = test.repository.pendingInvites()

        assertEquals(listOf("invite-2", "invite-1"), invites.map { it.id })
        assertEquals("group-9", invites.first().groupId)
        assertEquals("2026-10-09T00:00:00.000Z", invites.first().expiresAt)
        assertNull(invites.last().expiresAt, "an invite without a deadline stays nullable")

        val request = test.backend.single("/rest/v1/group_invites")
        assertEquals("eq.pending", request.url.parameters["status"], "answered invites must not come back")
        // PostgREST appends .nullslast to descending orders.
        assertEquals("created_at.desc.nullslast", request.url.parameters["order"])
        assertEquals(
            "id,group_id,created_at,expires_at",
            request.url.parameters["select"],
            "a guest may only read these four columns",
        )
    }

    // --- writes ----------------------------------------------------------------------------

    @Test
    fun createGroupCallsTheRpc() = runTest {
        val test = harness { HttpStatusCode.OK to JOINED_GROUP }

        test.repository.createGroup()

        test.backend.single("/rpc/create_group", HttpMethod.Post)
    }

    @Test
    fun joinGroupByCodeSendsTheCodeAsTheRpcArgument() = runTest {
        val test = harness { HttpStatusCode.OK to JOINED_GROUP }

        test.repository.joinGroupByCode("  3F9A20C7  ")

        val rpc = test.backend.single("/rpc/join_group_by_code", HttpMethod.Post)
        val body = rpc.body.asText()
        assertTrue("\"p_code\":\"3F9A20C7\"" in body, "the code is trimmed before it is sent: $body")
    }

    @Test
    fun joinGroupByCodeMapsEveryDocumentedError() = runTest {
        val documented = listOf(
            "invalid_invite_code" to "Código de convite inválido",
            "already_in_active_group" to "Você já está em uma roda ativa.",
            "group_full" to "Esta roda já está completa.",
            "not_authenticated" to "Sua sessão expirou",
        )

        documented.forEach { (identifier, fragment) ->
            // `group_full` and `already_in_active_group` share SQLSTATE 23505, hence the table.
            val failure = assertFailsWith<GroupRepositoryException> {
                failingRepository(identifier, code = "23505").joinGroupByCode("3F9A20C7")
            }
            val message = failure.message.orEmpty()
            assertTrue(fragment in message, "$identifier should read as \"$fragment\", got \"$message\"")
        }
    }

    @Test
    fun anUnrecognisedDatabaseErrorFallsBackToTheGenericCopy() = runTest {
        val failure = assertFailsWith<GroupRepositoryException> {
            failingRepository("permission denied for table groups", code = "42501").createGroup()
        }

        assertEquals("Não foi possível criar a roda.", failure.message)
    }

    @Test
    fun aFailureWithoutADatabaseEnvelopeFallsBackToTheGenericCopy() {
        assertEquals(
            "Não foi possível carregar sua roda.",
            groupErrorMessage(IllegalStateException("Connection refused"), "Não foi possível carregar sua roda."),
            "a transport error must never leak an English message to the screen",
        )
    }

    @Test
    fun searchProfilesSendsTheQueryAndMapsTheHits() = runTest {
        val test = harness { HttpStatusCode.OK to SEARCH_HITS }

        val hits = test.repository.searchProfiles("  ma  ")

        assertEquals("ma", test.backend.single("/rpc/search_profiles", HttpMethod.Post).body.asText()
            .substringAfter("\"p_query\":\"").substringBefore('"'))
        assertEquals(listOf("profile-maria", "profile-nameless", "profile-joao"), hits.map { it.id })
        assertEquals("Maria", hits.first().displayName)
        assertEquals("maria", hits.first().handle)
        assertEquals("Sem nome", hits[1].displayName, "a blank display_name falls back like ProfileRow does")
        assertNull(hits[1].handle)
        assertEquals("joao", hits[2].handle, "the handle is trimmed")
    }

    @Test
    fun inviteByProfileSendsTheGroupAndTheProfile() = runTest {
        val test = harness { HttpStatusCode.OK to "[]" }

        test.repository.inviteByProfile(groupId = "group-1", profileId = "profile-joao")

        val body = test.backend.single("/rpc/invite_by_profile", HttpMethod.Post).body.asText()
        assertTrue("\"p_group_id\":\"group-1\"" in body, body)
        assertTrue("\"p_profile_id\":\"profile-joao\"" in body, body)
    }

    @Test
    fun inviteByProfileMapsTheFullAndSelfInviteErrors() = runTest {
        val documented = listOf(
            "group_full" to "Esta roda já está completa.",
            "cannot_invite_self" to "Você não pode convidar você mesmo.",
            "already_a_member" to "Essa pessoa já está na sua roda.",
            "not_a_member" to "Sua conta não faz parte desta roda.",
        )

        documented.forEach { (identifier, expected) ->
            val failure = assertFailsWith<GroupRepositoryException> {
                failingRepository(identifier, code = "P0001").inviteByProfile("group-1", "profile-joao")
            }
            assertEquals(expected, failure.message, identifier)
        }
    }

    @Test
    fun theDuplicatePendingInviteIsRecognisedByItsUniqueViolation() = runTest {
        // The message must be JSON-escaped because `errorBody` interpolates it into a JSON string.
        val duplicateConstraint =
            "duplicate key value violates unique constraint \\\"group_invites_pending_profile_uidx\\\""

        val failure = assertFailsWith<GroupRepositoryException> {
            failingRepository(duplicateConstraint, code = "23505").inviteByProfile("group-1", "profile-joao")
        }

        assertEquals("Já existe um convite pendente para essa pessoa.", failure.message)
    }

    @Test
    fun inviteByHandleSendsTheHandleAndMapsTheMissingOne() = runTest {
        val test = harness { HttpStatusCode.OK to "[]" }

        test.repository.inviteByHandle(groupId = "group-1", handle = "  @joao  ")

        val body = test.backend.single("/rpc/invite_by_handle", HttpMethod.Post).body.asText()
        assertTrue("\"p_group_id\":\"group-1\"" in body, body)
        assertTrue("\"p_handle\":\"@joao\"" in body, "the handle is trimmed: $body")

        val failure = assertFailsWith<GroupRepositoryException> {
            failingRepository("handle_required", code = "P0001").inviteByHandle("group-1", "@joao")
        }
        assertEquals("Digite um @handle para convidar.", failure.message)
    }

    @Test
    fun acceptInviteSendsTheInviteAndMapsTheExpiredOne() = runTest {
        val test = harness { HttpStatusCode.OK to JOINED_GROUP }

        test.repository.acceptInvite("invite-2")

        val body = test.backend.single("/rpc/accept_invite", HttpMethod.Post).body.asText()
        assertTrue("\"p_invite_id\":\"invite-2\"" in body, body)

        val expired = assertFailsWith<GroupRepositoryException> {
            failingRepository("invite_expired", code = "P0001").acceptInvite("invite-2")
        }
        assertEquals("Este convite expirou. Peça um convite novo.", expired.message)

        val taken = assertFailsWith<GroupRepositoryException> {
            failingRepository("already_in_active_group", code = "23505").acceptInvite("invite-2")
        }
        assertEquals("Você já está em uma roda ativa.", taken.message)
    }

    @Test
    fun acceptInviteMapsGroupMemberUniqueViolationsInsteadOfTheGenericFallback() = runTest {
        // Both the partial indexes added by 0004 and the legacy non-partial constraints of 0001
        // must read as domain errors; the generic "Não foi possível aceitar o convite." is the bug.
        val slotObjects = listOf(
            "group_members_active_group_slot_uidx",
            "group_members_group_id_slot_key",
        )
        slotObjects.forEach { uniqueObject ->
            val duplicate = "duplicate key value violates unique constraint \\\"$uniqueObject\\\""
            val failure = assertFailsWith<GroupRepositoryException> {
                failingRepository(duplicate, code = "23505").acceptInvite("invite-2")
            }
            assertEquals("Esta roda já está completa.", failure.message, uniqueObject)
        }

        val profileObjects = listOf(
            "group_members_active_group_profile_uidx",
            "group_members_group_id_profile_id_key",
        )
        profileObjects.forEach { uniqueObject ->
            val duplicate = "duplicate key value violates unique constraint \\\"$uniqueObject\\\""
            val failure = assertFailsWith<GroupRepositoryException> {
                failingRepository(duplicate, code = "23505").acceptInvite("invite-2")
            }
            assertEquals("Você já está nesta roda.", failure.message, uniqueObject)
        }
    }

    @Test
    fun aDuplicatePendingInviteStillWinsOverTheGroupMemberMapping() = runTest {
        val duplicate =
            "duplicate key value violates unique constraint \\\"group_invites_pending_profile_uidx\\\""

        val failure = assertFailsWith<GroupRepositoryException> {
            failingRepository(duplicate, code = "23505").acceptInvite("invite-2")
        }

        assertEquals("Já existe um convite pendente para essa pessoa.", failure.message)
    }

    @Test
    fun declineInviteSendsTheInviteAndMapsTheAnsweredOne() = runTest {
        val test = harness { HttpStatusCode.OK to "[]" }

        test.repository.declineInvite("invite-2")

        val body = test.backend.single("/rpc/decline_invite", HttpMethod.Post).body.asText()
        assertTrue("\"p_invite_id\":\"invite-2\"" in body, body)

        val failure = assertFailsWith<GroupRepositoryException> {
            failingRepository("invite_not_pending", code = "P0001").declineInvite("invite-2")
        }
        assertEquals("Este convite não está mais pendente.", failure.message)
    }

    @Test
    fun leaveGroupSendsTheGroupId() = runTest {
        val test = harness { HttpStatusCode.OK to "[]" }

        test.repository.leaveGroup("group-1")

        val rpc = test.backend.single("/rpc/leave_group", HttpMethod.Post)
        val body = rpc.body.asText()
        assertTrue("\"p_group_id\":\"group-1\"" in body, "leave_group requires p_group_id: $body")
    }

    @Test
    fun dissolveGroupSendsTheGroupId() = runTest {
        val test = harness { HttpStatusCode.OK to "[]" }

        test.repository.dissolveGroup("group-1")

        val rpc = test.backend.single("/rpc/dissolve_group", HttpMethod.Post)
        val body = rpc.body.asText()
        assertTrue("\"p_group_id\":\"group-1\"" in body, "dissolve_group requires p_group_id: $body")
    }

    @Test
    fun removeMemberSendsGroupAndProfile() = runTest {
        val test = harness { HttpStatusCode.OK to "[]" }

        test.repository.removeMember(groupId = "group-1", profileId = "profile-maria")

        val rpc = test.backend.single("/rpc/remove_member", HttpMethod.Post)
        val body = rpc.body.asText()
        assertTrue("\"p_group_id\":\"group-1\"" in body, body)
        assertTrue("\"p_profile_id\":\"profile-maria\"" in body, body)
    }

    @Test
    fun leaveDissolveAndRemoveMapEveryDocumentedError() = runTest {
        suspend fun messageOf(identifier: String, call: suspend (GroupRepository) -> Unit): String =
            assertFailsWith<GroupRepositoryException> {
                call(failingRepository(identifier, code = "P0001"))
            }.message.orEmpty()

        assertEquals(
            "Sua conta não faz parte desta roda.",
            messageOf("not_a_member") { it.leaveGroup("group-1") },
        )
        assertEquals(
            "Esta roda não está mais ativa.",
            messageOf("group_not_active") { it.dissolveGroup("group-1") },
        )
        assertEquals(
            "Apenas quem criou a roda pode remover membros.",
            messageOf("not_group_creator") { it.removeMember("group-1", "profile-maria") },
        )
        assertEquals(
            "Você não pode se remover. Use \"Sair da roda\".",
            messageOf("cannot_remove_self") { it.removeMember("group-1", "profile-luiz") },
        )
        assertEquals(
            "Essa pessoa não está na sua roda.",
            messageOf("member_not_found") { it.removeMember("group-1", "profile-ghost") },
        )
        assertEquals(
            "Não é possível remover quem criou a roda.",
            messageOf("cannot_remove_creator") { it.removeMember("group-1", "profile-maria") },
        )
    }

    @Test
    fun everyRaisedIdentifierOfTheFlowHasPortugueseCopy() = runTest {
        val documented = listOf(
            "not_authenticated",
            "already_in_active_group",
            "group_full",
            "group_not_active",
            "invalid_invite_code",
            "invite_expired",
            "not_invited",
            "invite_not_pending",
            "invite_not_found_or_not_pending",
            "invite_not_found",
            "handle_required",
            "cannot_invite_self",
            "already_a_member",
            "not_a_member",
            "profile_not_found",
            "not_group_creator",
            "cannot_remove_self",
            "member_not_found",
            "cannot_remove_creator",
        )

        documented.forEach { identifier ->
            val failure = assertFailsWith<GroupRepositoryException> {
                failingRepository(identifier, code = "P0001").createGroup()
            }
            assertTrue(
                failure.message.orEmpty() != "Não foi possível criar a roda.",
                "$identifier is not translated and would fall back to the generic copy",
            )
        }
    }

    @Test
    fun aUniqueViolationOnAnotherTableIsNotReportedAsADuplicateInvite() = runTest {
        val failure = assertFailsWith<GroupRepositoryException> {
            failingRepository("duplicate key value violates unique constraint \"groups_invite_code_key\"", code = "23505")
                .createGroup()
        }

        assertEquals("Não foi possível criar a roda.", failure.message)
    }
}

/** Reads the JSON body of an outgoing request, whatever concrete [OutgoingContent] Ktor produced. */
private fun OutgoingContent.asText(): String = when (this) {
    is TextContent -> text
    is OutgoingContent.ByteArrayContent -> bytes().decodeToString()
    else -> error("Unsupported request body: $this")
}
