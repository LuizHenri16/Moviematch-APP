package app.moviematch.data.supabase

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Covers `groups` + `group_members` -> [app.moviematch.domain.model.Group]. */
class GroupRowsTest {

    private val json = Json { ignoreUnknownKeys = true }

    private fun group(raw: String) = json.decodeFromString<List<GroupRow>>(raw).single().toGroup()

    @Test
    fun membersAreOrderedBySlotAndNameTheGroup() {
        val raw = """
            [
              {
                "id": "group-1",
                "created_at": "2026-09-01T10:00:00.000Z",
                "group_members": [
                  { "slot": 2, "left_at": null, "profile": { "id": "p-2", "display_name": "Luiz", "avatar_url": null, "created_at": "2026-08-02T10:00:00.000Z" } },
                  { "slot": 1, "left_at": null, "profile": { "id": "p-1", "display_name": "Maria", "avatar_url": "https://cdn/avatar.png", "created_at": "2026-08-01T10:00:00.000Z" } }
                ]
              }
            ]
        """

        val group = group(raw)

        assertEquals("group-1", group.id)
        assertEquals("2026-09-01T10:00:00.000Z", group.createdAt)
        assertEquals("Maria & Luiz", group.name)
        assertEquals(listOf("p-1", "p-2"), group.members.map { it.id })
        assertEquals("https://cdn/avatar.png", group.members.first().avatarUrl)
    }

    @Test
    fun membersThatLeftAreIgnored() {
        val raw = """
            [
              {
                "id": "group-1",
                "group_members": [
                  { "slot": 1, "left_at": null, "profile": { "id": "p-1", "display_name": "Maria" } },
                  { "slot": 2, "left_at": "2026-09-20T10:00:00.000Z", "profile": { "id": "p-9", "display_name": "Antigo" } }
                ]
              }
            ]
        """

        val group = group(raw)

        assertEquals("Maria", group.name)
        assertEquals(listOf("p-1"), group.members.map { it.id })
    }

    @Test
    fun anEmptyGroupFallsBackToTheHouseName() {
        val raw = """[{"id": "group-1", "group_members": []}]"""

        val group = group(raw)

        assertEquals("A Roda", group.name)
        assertTrue(group.members.isEmpty())
        assertEquals("", group.createdAt)
    }

    @Test
    fun aGroupWithoutReadableMembersAlsoFallsBack() {
        val raw = """[{"id": "group-1", "group_members": [{"slot": 1, "left_at": null, "profile": null}]}]"""

        assertEquals("A Roda", group(raw).name)
    }

    @Test
    fun aRepeatedMemberRowIsCountedOnce() {
        val raw = """
            [
              {
                "id": "group-1",
                "group_members": [
                  { "slot": 1, "left_at": null, "profile": { "id": "p-1", "display_name": "Maria" } },
                  { "slot": 1, "left_at": null, "profile": { "id": "p-1", "display_name": "Maria" } }
                ]
              }
            ]
        """

        val group = group(raw)

        assertEquals("Maria", group.name)
        assertEquals(1, group.members.size)
    }

    @Test
    fun aProfileWithoutADisplayNameUsesTheSharedFallbackName() {
        val raw = """[{"id": "group-1", "group_members": [{"slot": 1, "left_at": null, "profile": {"id": "p-1", "display_name": "   "}}]}]"""

        val group = group(raw)

        assertEquals("Sem nome", group.name, "the same fallback `profiles()` uses")
        assertEquals("Sem nome", group.members.single().displayName)
    }
}
