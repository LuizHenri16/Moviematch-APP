package app.moviematch.data.supabase

import app.moviematch.domain.model.ContentType
import app.moviematch.domain.model.ProfileName
import app.moviematch.domain.model.SearchResult
import app.moviematch.domain.model.WatchStatus
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
 * Covers [SupabaseMovieRepository] against a Ktor [MockEngine].
 *
 * The engine answers the real PostgREST URLs the repository builds (`/rest/v1/<table>`,
 * `/rest/v1/rpc/<fn>`) and the assertions look at the outgoing requests — path, method, query
 * parameters and JSON body — so the contract with the schema in `supabase/0001_init.sql` is checked
 * without a database.
 */
class SupabaseMovieRepositoryTest {
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

    private class Harness(
        val repository: SupabaseMovieRepository,
        val backend: Backend,
        val tmdbRequests: List<HttpRequestData>,
    )

    private fun harness(
        tmdb: (HttpRequestData) -> Pair<HttpStatusCode, String>? = { null },
        backend: (HttpRequestData) -> Pair<HttpStatusCode, String>?,
    ): Harness {
        val backendDouble = Backend(backend)
        val tmdbEngine = MockEngine { request ->
            val (status, body) = tmdb(request)
                ?: error("Unexpected TMDb request: ${request.method.value} ${request.url}")
            respond(content = body, status = status, headers = jsonResponseHeaders)
        }

        val client = createSupabaseClient(
            supabaseUrl = "https://project.supabase.co",
            supabaseKey = "anon-key",
        ) {
            httpEngine = backendDouble.engine
            install(Postgrest)
        }

        val repository = SupabaseMovieRepository(
            supabaseClient = client,
            tmdb = TmdbClient(createTmdbHttpClient(tmdbEngine), apiKey = "tmdb-key"),
            currentUserId = { CURRENT_USER_ID },
        )
        return Harness(repository, backendDouble, tmdbEngine.requestHistory)
    }

    // --- fixtures --------------------------------------------------------------------------

    private companion object {
        const val CURRENT_USER_ID = "profile-luiz"

        const val DUPLICATE_KEY_ERROR =
            """{"code":"23505","message":"duplicate key value violates unique constraint","details":null,"hint":null}"""

        const val GROUP = """
            [
              {
                "id": "group-1",
                "created_at": "2026-09-01T10:00:00.000Z",
                "group_members": [
                  { "slot": 1, "left_at": null, "profile": { "id": "profile-maria", "display_name": "Maria" } },
                  { "slot": 2, "left_at": null, "profile": { "id": "profile-luiz", "display_name": "Luiz" } }
                ]
              }
            ]
        """

        const val MOVIE_DETAILS = """
            {
              "id": 603,
              "title": "Matrix",
              "release_date": "1999-03-31",
              "poster_path": "/matrix.jpg",
              "runtime": 136,
              "overview": "Um hacker descobre a verdade.",
              "genres": [{"id": 878, "name": "Ficção científica"}, {"id": 28, "name": "Ação"}]
            }
        """

        const val TV_DETAILS = """
            {
              "id": 1396,
              "name": "Breaking Bad",
              "first_air_date": "2008-01-20",
              "poster_path": "/bb.jpg",
              "episode_run_time": [47, 49],
              "genres": [{"id": 18, "name": "Drama"}],
              "number_of_seasons": 5,
              "number_of_episodes": 62
            }
        """

        const val MULTI_RESULTS = """
            {
              "results": [
                {"id": 603, "media_type": "movie", "title": "Matrix", "release_date": "1999-03-31", "poster_path": "/matrix.jpg"},
                {"id": 287, "media_type": "person", "name": "Brad Pitt"}
              ]
            }
        """

        fun arrayOf(vararg rows: String): String = rows.joinToString(separator = ",", prefix = "[", postfix = "]")

        fun movieSession(
            id: String,
            createdAt: String,
            status: String = "PENDING",
            posterPath: String? = "/$id.jpg",
        ) = """
            {
              "id": "$id",
              "status": "$status",
              "created_at": "$createdAt",
              "suggested_by": { "display_name": "Luiz" },
              "movie": {
                "id": "movie-$id",
                "title": "Filme $id",
                "release_year": 1999,
                "runtime_minutes": 136,
                "poster_path": ${posterPath?.let { "\"$it\"" } ?: "null"},
                "main_genre": "Drama",
                "subgenres": ["Crime"]
              },
              "ratings": []
            }
        """

        fun seriesSession(id: String, createdAt: String, status: String = "PENDING") = """
            {
              "id": "$id",
              "status": "$status",
              "created_at": "$createdAt",
              "series": {
                "id": "series-$id",
                "title": "Série $id",
                "release_year": 2008,
                "poster_path": "/$id.jpg",
                "main_genre": "Drama",
                "subgenres": [],
                "number_of_seasons": 5,
                "number_of_episodes": 62,
                "episode_run_time_minutes": 48
              },
              "series_ratings": []
            }
        """
    }

    // --- reads -----------------------------------------------------------------------------

    @Test
    fun contentItemsMergesBothTablesAndSortsThemNewestFirst() = runTest {
        val test = harness(backend = { request ->
            when (request.url.encodedPath) {
                "/rest/v1/sessions" -> HttpStatusCode.OK to arrayOf(
                    movieSession(id = "m-1", createdAt = "2026-10-01T00:00:00.000Z"),
                    movieSession(id = "m-2", createdAt = "2026-09-01T00:00:00.000Z"),
                )

                "/rest/v1/series_sessions" -> HttpStatusCode.OK to arrayOf(
                    seriesSession(id = "s-1", createdAt = "2026-10-05T00:00:00.000Z"),
                )

                else -> null
            }
        })

        val items = test.repository.contentItems()

        assertEquals(listOf("s-1", "m-1", "m-2"), items.map { it.id })
        assertEquals(listOf(ContentType.TV, ContentType.MOVIE, ContentType.MOVIE), items.map { it.type })
    }

    @Test
    fun contentItemsQueriesTheSameColumnsAndOrderingAsTheReference() = runTest {
        val test = harness(backend = { HttpStatusCode.OK to "[]" })

        test.repository.contentItems()

        val movies = test.backend.single("/rest/v1/sessions")
        assertTrue(
            movies.url.parameters["order"].orEmpty().startsWith("created_at.desc"),
            "newest first: ${movies.url.parameters["order"]}",
        )
        val select = movies.url.parameters["select"].orEmpty()
        assertTrue("suggested_by:profiles(display_name)" in select)
        assertTrue("movie:movies(" in select)
        assertTrue("ratings(score,comment,created_at,profile:profiles(display_name))" in select)
        // `Columns.raw` strips the whitespace of the multi-line literal.
        assertTrue(" " !in select, "the select list must not carry whitespace: $select")

        val series = test.backend.single("/rest/v1/series_sessions")
        val seriesSelect = series.url.parameters["select"].orEmpty()
        assertTrue("series_ratings(score,comment,created_at,profile:profiles(display_name))" in seriesSelect)
        assertTrue("number_of_seasons" in seriesSelect)
    }

    @Test
    fun contentItemFallsBackToTheSeriesTable() = runTest {
        val test = harness(backend = { request ->
            when (request.url.encodedPath) {
                "/rest/v1/sessions" -> HttpStatusCode.OK to "[]"
                "/rest/v1/series_sessions" -> HttpStatusCode.OK to arrayOf(
                    seriesSession(id = "s-1", createdAt = "2026-10-05T00:00:00.000Z"),
                )

                else -> null
            }
        })

        val item = test.repository.contentItem("s-1")

        assertEquals("s-1", item?.id)
        assertEquals(ContentType.TV, item?.type)
        assertEquals(5, item?.seasons)
        assertEquals("https://image.tmdb.org/t/p/w500/s-1.jpg", item?.posterUrl)
        assertEquals("s-1", test.backend.single("/rest/v1/sessions").url.parameters["id"]?.removePrefix("eq."))
        assertEquals("s-1", test.backend.single("/rest/v1/series_sessions").url.parameters["id"]?.removePrefix("eq."))
    }

    @Test
    fun contentItemReturnsNullForAnUnknownId() = runTest {
        val test = harness(backend = { HttpStatusCode.OK to "[]" })

        assertNull(test.repository.contentItem("missing"))
    }

    @Test
    fun groupReadsTheActiveMembersAndIsNullWithoutOne() = runTest {
        val withGroup = harness(backend = { HttpStatusCode.OK to GROUP })
        assertEquals("Maria & Luiz", withGroup.repository.group()?.name)
        assertEquals(listOf("profile-maria", "profile-luiz"), withGroup.repository.group()?.members?.map { it.id })

        val withoutGroup = harness(backend = { HttpStatusCode.OK to "[]" })
        assertNull(
            withoutGroup.repository.group(),
            "having no active group is a normal state, not a failure",
        )
    }

    @Test
    fun randomPendingPicksAQueuedTitleAndIsNullWhenEverythingWasWatched() = runTest {
        val pending = harness(backend = { request ->
            when (request.url.encodedPath) {
                "/rest/v1/sessions" -> HttpStatusCode.OK to arrayOf(
                    movieSession(id = "m-1", createdAt = "2026-10-01T00:00:00.000Z", status = "WATCHED"),
                    movieSession(id = "m-2", createdAt = "2026-09-01T00:00:00.000Z"),
                )

                "/rest/v1/series_sessions" -> HttpStatusCode.OK to "[]"
                else -> null
            }
        })
        assertEquals("m-2", pending.repository.randomPending()?.id)

        val allWatched = harness(backend = { request ->
            when (request.url.encodedPath) {
                "/rest/v1/sessions" -> HttpStatusCode.OK to arrayOf(
                    movieSession(id = "m-1", createdAt = "2026-10-01T00:00:00.000Z", status = "WATCHED"),
                )

                "/rest/v1/series_sessions" -> HttpStatusCode.OK to "[]"
                else -> null
            }
        })
        assertNull(allWatched.repository.randomPending())
    }

    // --- catalogue search ------------------------------------------------------------------

    @Test
    fun searchGoesThroughTheTmdbClient() = runTest {
        val test = harness(
            tmdb = { HttpStatusCode.OK to MULTI_RESULTS },
            backend = { null },
        )

        val results = test.repository.search("matrix")

        assertEquals(listOf(603), results.map { it.id })
        assertEquals(ContentType.MOVIE, results.single().type)
        assertEquals(1, test.tmdbRequests.size)
        assertTrue(test.backend.requests.isEmpty(), "search must not touch the database")
        assertEquals(emptyList(), test.repository.search(""))
    }

    // --- writes ----------------------------------------------------------------------------

    @Test
    fun addToWatchListStoresTheCatalogRowAndAPendingSession() = runTest {
        val test = harness(
            tmdb = { request ->
                assertEquals("/3/movie/603", request.url.encodedPath)
                HttpStatusCode.OK to MOVIE_DETAILS
            },
            backend = { request ->
                when {
                    request.url.encodedPath == "/rest/v1/groups" -> HttpStatusCode.OK to GROUP
                    request.url.encodedPath.endsWith("/rpc/upsert_movie") -> HttpStatusCode.OK to "\"catalog-uuid\""
                    request.url.encodedPath.endsWith("/sessions") -> HttpStatusCode.OK to arrayOf(
                        movieSession(id = "session-new", createdAt = "2026-10-18T12:00:00.000Z"),
                    )

                    else -> null
                }
            },
        )

        val created = test.repository.addToWatchList(
            SearchResult(id = 603, type = ContentType.MOVIE, title = "Matrix"),
        )

        assertEquals("session-new", created.id)
        assertEquals("Filme session-new", created.title)
        assertEquals(WatchStatus.PENDING, created.status)

        val rpc = test.backend.single("/rpc/upsert_movie", HttpMethod.Post)
        val rpcBody = rpc.body.asText()
        assertTrue("\"tmdb_id\":603" in rpcBody, "the RPC is keyed by tmdb_id: $rpcBody")
        assertTrue("\"p_tmdb\"" in rpcBody, "the RPC argument is named p_tmdb: $rpcBody")
        assertTrue("\"runtime_minutes\":136" in rpcBody, "the movie runtime travels in the payload")
        assertTrue("\"number_of_seasons\"" !in rpcBody, "movie payloads must not send series columns")

        val insert = test.backend.single("/rest/v1/sessions", HttpMethod.Post)
        val insertBody = insert.body.asText()
        assertTrue(
            "movie:movies(" in insert.url.parameters["select"].orEmpty(),
            "the insert asks for the same representation the reads use",
        )
        assertTrue("\"group_id\":\"group-1\"" in insertBody, insertBody)
        assertTrue("\"movie_id\":\"catalog-uuid\"" in insertBody, insertBody)
        assertTrue("\"status\":\"PENDING\"" in insertBody, insertBody)
        assertTrue("\"suggested_by\":\"$CURRENT_USER_ID\"" in insertBody, insertBody)
        assertTrue("return=representation" in insert.headers["Prefer"].orEmpty())
    }

    @Test
    fun addToWatchListUsesTheSeriesRpcForASeries() = runTest {
        val test = harness(
            tmdb = { request ->
                assertEquals("/3/tv/1396", request.url.encodedPath)
                HttpStatusCode.OK to TV_DETAILS
            },
            backend = { request ->
                when {
                    request.url.encodedPath == "/rest/v1/groups" -> HttpStatusCode.OK to GROUP
                    request.url.encodedPath.endsWith("/rpc/upsert_series") -> HttpStatusCode.OK to "\"series-uuid\""
                    request.url.encodedPath.endsWith("/series_sessions") -> HttpStatusCode.OK to arrayOf(
                        seriesSession(id = "series-session-new", createdAt = "2026-10-18T12:00:00.000Z"),
                    )

                    else -> null
                }
            },
        )

        val created = test.repository.addToWatchList(
            SearchResult(id = 1396, type = ContentType.TV, title = "Breaking Bad"),
        )

        assertEquals(ContentType.TV, created.type)
        assertEquals(48, created.runtimeMinutes)

        val rpcBody = test.backend.single("/rpc/upsert_series", HttpMethod.Post).body.asText()
        assertTrue("\"episode_run_time_minutes\":48" in rpcBody, "the average episode length is sent: $rpcBody")
        assertTrue("\"number_of_seasons\":5" in rpcBody, rpcBody)

        val insertBody = test.backend.single("/rest/v1/series_sessions", HttpMethod.Post).body.asText()
        assertTrue("\"series_id\":\"series-uuid\"" in insertBody, insertBody)
        assertTrue("\"status\":\"PENDING\"" in insertBody, insertBody)
    }

    @Test
    fun addToWatchListGuidesTheUserToCreateOrJoinARodaWhenThereIsNoGroup() = runTest {
        val test = harness(
            tmdb = { error("TMDb must not be called without a group") },
            backend = { request ->
                when (request.url.encodedPath) {
                    "/rest/v1/groups" -> HttpStatusCode.OK to "[]"
                    else -> null
                }
            },
        )

        val failure = assertFailsWith<MovieRepositoryException> {
            test.repository.addToWatchList(
                SearchResult(id = 603, type = ContentType.MOVIE, title = "Matrix"),
            )
        }

        assertEquals(
            "Você ainda não tem uma roda. Crie ou entre em uma para adicionar títulos.",
            failure.message,
        )
        assertTrue(
            test.backend.requests.none { it.method == HttpMethod.Post },
            "nothing is written when the member has no group",
        )
    }

    @Test
    fun addToWatchListExplainsADuplicatedTitle() = runTest {
        val test = harness(
            tmdb = { HttpStatusCode.OK to MOVIE_DETAILS },
            backend = { request ->
                when {
                    request.url.encodedPath == "/rest/v1/groups" -> HttpStatusCode.OK to GROUP
                    request.url.encodedPath.endsWith("/rpc/upsert_movie") -> HttpStatusCode.OK to "\"catalog-uuid\""
                    request.url.encodedPath.endsWith("/sessions") -> HttpStatusCode.Conflict to DUPLICATE_KEY_ERROR
                    else -> null
                }
            },
        )

        val failure = assertFailsWith<MovieRepositoryException> {
            test.repository.addToWatchList(SearchResult(id = 603, type = ContentType.MOVIE, title = "Matrix"))
        }

        assertEquals("Este título já está na sua lista.", failure.message)
    }

    @Test
    fun updateStatusPatchesTheMovieSession() = runTest {
        val test = harness(backend = { request ->
            when (request.method) {
                HttpMethod.Get -> HttpStatusCode.OK to """[{"id": "session-1", "group_id": "group-1", "status": "PENDING"}]"""
                HttpMethod.Patch -> HttpStatusCode.OK to "[]"
                else -> null
            }
        })

        test.repository.updateStatus("session-1", WatchStatus.WATCHED, "2026-10-18")

        val patch = test.backend.single("/rest/v1/sessions", HttpMethod.Patch)
        assertEquals("eq.session-1", patch.url.parameters["id"])
        val body = patch.body.asText()
        assertTrue("\"status\":\"WATCHED\"" in body, body)
        assertTrue("\"watched_at\":\"2026-10-18\"" in body, body)
    }

    @Test
    fun updateStatusPatchesTheSeriesSessionAndCanClearTheDate() = runTest {
        val test = harness(backend = { request ->
            when (request.method) {
                HttpMethod.Get -> HttpStatusCode.OK to "[]"
                HttpMethod.Patch -> HttpStatusCode.OK to "[]"
                else -> null
            }
        })
        val series = harness(backend = { request ->
            when {
                request.url.encodedPath.endsWith("/sessions") && request.method == HttpMethod.Get ->
                    HttpStatusCode.OK to "[]"

                request.url.encodedPath.endsWith("/series_sessions") && request.method == HttpMethod.Get ->
                    HttpStatusCode.OK to """[{"id": "series-session-1", "group_id": "group-1", "status": "WATCHED"}]"""

                request.method == HttpMethod.Patch -> HttpStatusCode.OK to "[]"
                else -> null
            }
        })

        // An unknown id is a no-op, exactly like the mock.
        test.repository.updateStatus("missing", WatchStatus.WATCHED, null)
        assertTrue(test.backend.requests.none { it.method == HttpMethod.Patch })

        series.repository.updateStatus("series-session-1", WatchStatus.PENDING, null)
        val body = series.backend.single("/rest/v1/series_sessions", HttpMethod.Patch).body.asText()
        assertTrue("\"status\":\"PENDING\"" in body, body)
        assertTrue("\"watched_at\":null" in body, body)
    }

    @Test
    fun saveRatingUpsertsOnTheSessionAndProfileUniquenessRule() = runTest {
        val test = harness(backend = { request ->
            when {
                request.url.encodedPath.endsWith("/sessions") && request.method == HttpMethod.Get ->
                    HttpStatusCode.OK to """[{"id": "session-1", "group_id": "group-1", "status": "PENDING"}]"""

                request.url.encodedPath.endsWith("/profiles") ->
                    HttpStatusCode.OK to """[{"id": "profile-maria", "display_name": "Maria"}]"""

                request.url.encodedPath.endsWith("/ratings") -> HttpStatusCode.Created to ""
                request.method == HttpMethod.Patch -> HttpStatusCode.OK to "[]"
                else -> null
            }
        })

        test.repository.saveRating(
            itemId = "session-1",
            profile = ProfileName.MARIA,
            score = 9.0,
            comment = "Clássico",
            markWatched = true,
        )

        val upsert = test.backend.single("/rest/v1/ratings", HttpMethod.Post)
        assertEquals("session_id,profile_id", upsert.url.parameters["on_conflict"])
        assertEquals("merge-duplicates", upsert.headers["Prefer"].orEmpty().substringAfter("resolution=").substringBefore(","))
        val body = upsert.body.asText()
        assertTrue("\"session_id\":\"session-1\"" in body, body)
        assertTrue("\"group_id\":\"group-1\"" in body, "ratings carry the denormalized group_id: $body")
        assertTrue("\"profile_id\":\"profile-maria\"" in body, body)
        assertTrue("\"score\":9.0" in body, body)
        assertTrue("\"comment\":\"Clássico\"" in body, body)

        val patch = test.backend.single("/rest/v1/sessions", HttpMethod.Patch).body.asText()
        assertTrue("\"status\":\"WATCHED\"" in patch, "marking a pending title as watched flips the status: $patch")
        assertTrue("\"watched_at\":\"" in patch, patch)
    }

    @Test
    fun saveRatingForASeriesUsesTheSeriesTables() = runTest {
        val test = harness(backend = { request ->
            when {
                request.url.encodedPath.endsWith("/sessions") -> HttpStatusCode.OK to "[]"
                request.url.encodedPath.endsWith("/series_sessions") && request.method == HttpMethod.Get ->
                    HttpStatusCode.OK to """[{"id": "series-session-1", "group_id": "group-1", "status": "WATCHED"}]"""

                request.url.encodedPath.endsWith("/profiles") ->
                    HttpStatusCode.OK to """[{"id": "profile-luiz", "display_name": "Luiz"}]"""

                request.url.encodedPath.endsWith("/series_ratings") -> HttpStatusCode.Created to ""
                else -> null
            }
        })

        test.repository.saveRating(
            itemId = "series-session-1",
            profile = ProfileName.LUIZ,
            score = 10.0,
            comment = null,
            markWatched = false,
        )

        val upsert = test.backend.single("/rest/v1/series_ratings", HttpMethod.Post)
        assertEquals("series_session_id,profile_id", upsert.url.parameters["on_conflict"])
        val body = upsert.body.asText()
        assertTrue("\"series_session_id\":\"series-session-1\"" in body, body)
        assertTrue("\"group_id\":\"group-1\"" in body, body)
        assertTrue("\"comment\":null" in body, "a rating without a comment sends an explicit null: $body")
        assertTrue(test.backend.requests.none { it.method == HttpMethod.Patch }, "nothing to mark as watched")
    }

    @Test
    fun saveRatingFailsClearlyWhenTheProfileCannotBeResolved() = runTest {
        val test = harness(backend = { request ->
            when {
                request.url.encodedPath.endsWith("/sessions") -> HttpStatusCode.OK to """[{"id": "session-1", "group_id": "group-1", "status": "PENDING"}]"""
                request.url.encodedPath.endsWith("/profiles") -> HttpStatusCode.OK to "[]"
                else -> null
            }
        })

        val failure = assertFailsWith<MovieRepositoryException> {
            test.repository.saveRating("session-1", ProfileName.MARIA, 8.0, null, false)
        }

        assertEquals("Não foi possível identificar o perfil de Maria para salvar a nota.", failure.message)
        assertTrue(test.backend.requests.none { it.method == HttpMethod.Post }, "no rating is written without a profile")
    }

    @Test
    fun deleteContentRemovesOnlyTheGroupSession() = runTest {
        val test = harness(backend = { request ->
            when (request.method) {
                HttpMethod.Get -> HttpStatusCode.OK to """[{"id": "session-1", "group_id": "group-1", "status": "WATCHED"}]"""
                HttpMethod.Delete -> HttpStatusCode.NoContent to ""
                else -> null
            }
        })

        test.repository.deleteContent("session-1")

        val delete = test.backend.single("/rest/v1/sessions", HttpMethod.Delete)
        assertEquals("eq.session-1", delete.url.parameters["id"])
        assertTrue(
            test.backend.requests.none { it.url.encodedPath.endsWith("/movies") },
            "the shared catalog row must survive: it has no DELETE grant and other groups may use it",
        )
    }
}

/** Reads the JSON body of an outgoing request, whatever concrete [OutgoingContent] Ktor produced. */
private fun OutgoingContent.asText(): String = when (this) {
    is TextContent -> text
    is OutgoingContent.ByteArrayContent -> bytes().decodeToString()
    else -> error("Unsupported request body: $this")
}
