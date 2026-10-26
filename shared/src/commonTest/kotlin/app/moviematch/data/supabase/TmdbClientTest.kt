package app.moviematch.data.supabase

import app.moviematch.domain.model.ContentType
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Exercises [TmdbClient] against a Ktor [MockEngine].
 *
 * There is no network involved, but the real request URL, query parameters, JSON decoding and the
 * typed error mapping all run, which is what the "Adicionar" screen depends on.
 */
class TmdbClientTest {

    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

    private fun engine(
        status: HttpStatusCode = HttpStatusCode.OK,
        body: String = "{}",
    ): MockEngine = MockEngine { request: HttpRequestData ->
        respond(content = body, status = status, headers = jsonHeaders)
    }

    private fun client(engine: MockEngine, apiKey: String = "tmdb-key"): TmdbClient =
        TmdbClient(httpClient = createTmdbHttpClient(engine), apiKey = apiKey)

    private companion object {
        const val MULTI_RESULTS = """
            {
              "results": [
                {
                  "id": 603,
                  "media_type": "movie",
                  "title": "Matrix",
                  "release_date": "1999-03-31",
                  "poster_path": "/matrix.jpg",
                  "overview": "Um hacker descobre a verdade.",
                  "vote_average": 8.2
                },
                {
                  "id": 1396,
                  "media_type": "tv",
                  "name": "Breaking Bad",
                  "first_air_date": "2008-01-20",
                  "poster_path": null,
                  "overview": "Um professor de química.",
                  "vote_average": 8.9
                },
                { "id": 287, "media_type": "person", "name": "Brad Pitt" },
                { "id": 42, "media_type": "collection", "name": "Coleção" }
              ]
            }
        """
    }

    // --- request shape ---------------------------------------------------------------------

    @Test
    fun searchBuildsTheSameRequestAsTheReference() = runTest {
        val engine = engine(body = MULTI_RESULTS)

        client(engine).search("  matrix  ")

        val request = engine.requestHistory.single()
        assertEquals("/3/search/multi", request.url.encodedPath)
        assertEquals("matrix", request.url.parameters["query"])
        assertEquals("1", request.url.parameters["page"])
        assertEquals("tmdb-key", request.url.parameters["api_key"])
        assertEquals("pt-BR", request.url.parameters["language"])
    }

    @Test
    fun searchDoesNotHitTheApiForABlankQuery() = runTest {
        val engine = engine(body = MULTI_RESULTS)

        assertEquals(emptyList(), client(engine).search("   "))
        assertTrue(engine.requestHistory.isEmpty(), "a blank query must not reach the network")
    }

    @Test
    fun searchWithoutAnApiKeyExplainsTheConfiguration() = runTest {
        val engine = engine(body = MULTI_RESULTS)

        val failure = assertFailsWith<MovieRepositoryException> { client(engine, apiKey = "").search("matrix") }

        assertEquals(MISSING_TMDB_KEY_MESSAGE, failure.message)
        assertTrue(engine.requestHistory.isEmpty(), "an unconfigured client must not reach the network")
    }

    // --- mapping ---------------------------------------------------------------------------

    @Test
    fun searchKeepsMoviesAndSeriesAndDropsPeople() = runTest {
        val results = client(engine(body = MULTI_RESULTS)).search("matrix")

        assertEquals(listOf(603, 1396), results.map { it.id })
        assertTrue(results.all { it.title.isNotBlank() })
    }

    @Test
    fun searchMapsPosterUrlYearAndVoteAverage() = runTest {
        val results = client(engine(body = MULTI_RESULTS)).search("matrix")

        val movie = results.first()
        assertEquals(ContentType.MOVIE, movie.type)
        assertEquals("Matrix", movie.title)
        assertEquals(1999, movie.year)
        assertEquals("https://image.tmdb.org/t/p/w185/matrix.jpg", movie.posterUrl)
        assertEquals(8.2, movie.voteAverage)
        assertEquals("Um hacker descobre a verdade.", movie.overview)

        val series = results[1]
        assertEquals(ContentType.TV, series.type)
        assertEquals("Breaking Bad", series.title)
        assertEquals(2008, series.year)
        assertNull(series.posterUrl, "a null poster_path maps to a null URL")
        assertEquals(8.9, series.voteAverage)
    }

    @Test
    fun searchReturnsAnEmptyListWhenTmdbHasNoMatches() = runTest {
        assertEquals(emptyList(), client(engine(body = """{"results":[]}""")).search("zzz"))
    }

    // --- error mapping ---------------------------------------------------------------------

    @Test
    fun unauthorizedSearchPointsAtTheApiKey() = runTest {
        val engine = engine(status = HttpStatusCode.Unauthorized, body = """{"status_message":"invalid key"}""")

        val failure = assertFailsWith<MovieRepositoryException> { client(engine).search("matrix") }

        assertEquals("Não foi possível autenticar no TMDb. Verifique se a chave é válida.", failure.message)
    }

    @Test
    fun missingDetailsReportANotFoundTitle() = runTest {
        val engine = engine(status = HttpStatusCode.NotFound)

        val failure = assertFailsWith<MovieRepositoryException> { client(engine).movieDetails(1) }

        assertEquals("Conteúdo não encontrado no TMDb.", failure.message)
        assertEquals("/3/movie/1", engine.requestHistory.single().url.encodedPath)
    }

    @Test
    fun aBrokenPayloadBecomesAReadableError() = runTest {
        val failure = assertFailsWith<MovieRepositoryException> {
            client(engine(body = "not json")).search("matrix")
        }

        assertEquals("O TMDb retornou uma resposta inválida.", failure.message)
    }

    @Test
    fun aFailingEngineBecomesANetworkError() = runTest {
        val engine = MockEngine { throw IllegalStateException("dns down") }

        val failure = assertFailsWith<MovieRepositoryException> { client(engine).search("matrix") }

        assertEquals("Não foi possível conectar ao TMDb. Verifique sua internet.", failure.message)
    }

    // --- detail endpoints ------------------------------------------------------------------

    @Test
    fun seriesDetailsReadTheEpisodeRunTimeAndSeasonCounts() = runTest {
        val body = """
            {
              "id": 1396,
              "name": "Breaking Bad",
              "first_air_date": "2008-01-20",
              "overview": "Um professor de química.",
              "poster_path": "/bb.jpg",
              "episode_run_time": [47, 49],
              "genres": [{"id": 18, "name": "Drama"}, {"id": 80, "name": "Crime"}],
              "number_of_seasons": 5,
              "number_of_episodes": 62
            }
        """
        val engine = engine(body = body)

        val payload = client(engine).tvDetails(1396).toCatalogPayload()

        assertEquals("/3/tv/1396", engine.requestHistory.single().url.encodedPath)
        assertEquals(48, payload.episodeRunTimeMinutes)
        assertEquals(5, payload.numberOfSeasons)
        assertEquals(62, payload.numberOfEpisodes)
        assertEquals("Drama", payload.mainGenre)
        assertEquals(listOf("Crime"), payload.subgenres)
        assertEquals("/bb.jpg", payload.posterPath, "the DB stores the raw poster path")
        assertEquals(2008, payload.releaseYear)
    }
}
