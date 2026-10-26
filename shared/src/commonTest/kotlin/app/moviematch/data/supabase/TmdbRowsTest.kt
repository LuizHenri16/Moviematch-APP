package app.moviematch.data.supabase

import app.moviematch.domain.model.ContentType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Covers the pure TMDb payload mappers and the wire shape of the `upsert_*` payload. */
class TmdbRowsTest {

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Same configuration supabase-kt's serializer uses: `encodeDefaults = false` (so a defaulted
     * field is dropped) and `explicitNulls = true` (so a nullable field without a default is sent as
     * an explicit `null`).
     */
    private val wire = Json { ignoreUnknownKeys = true }

    private fun searchResult(raw: String) = json.decodeFromString<TmdbSearchResult>(raw).toSearchResult()

    private fun movieDetails(raw: String) = json.decodeFromString<TmdbMovieDetails>(raw)

    private fun tvDetails(raw: String) = json.decodeFromString<TmdbTvDetails>(raw)

    // --- /search/multi ---------------------------------------------------------------------

    @Test
    fun peopleAndUnknownMediaTypesAreDropped() {
        assertNull(searchResult("""{"id": 287, "media_type": "person", "name": "Brad Pitt"}"""))
        assertNull(searchResult("""{"id": 42, "media_type": "collection", "name": "Coleção"}"""))
        assertNull(searchResult("""{"id": 42}"""))
    }

    @Test
    fun moviesUseTitleAndReleaseDate() {
        val result = searchResult(
            """{"id": 603, "media_type": "movie", "title": "Matrix", "release_date": "1999-03-31", "poster_path": "/matrix.jpg", "vote_average": 8.2, "overview": "Um hacker."}""",
        )

        assertEquals(ContentType.MOVIE, result?.type)
        assertEquals("Matrix", result?.title)
        assertEquals(1999, result?.year)
        assertEquals("Um hacker.", result?.overview)
    }

    @Test
    fun seriesUseNameAndFirstAirDate() {
        val result = searchResult(
            """{"id": 1396, "media_type": "tv", "name": "Breaking Bad", "first_air_date": "2008-01-20"}""",
        )

        assertEquals(ContentType.TV, result?.type)
        assertEquals("Breaking Bad", result?.title)
        assertEquals(2008, result?.year)
        assertEquals("", result?.overview, "a missing overview becomes an empty string")
    }

    @Test
    fun theSearchRowGetsAnImageUrlOnTheThumbnailSize() {
        val result = searchResult("""{"id": 603, "media_type": "movie", "title": "Matrix", "poster_path": "/matrix.jpg"}""")

        assertEquals("https://image.tmdb.org/t/p/w185/matrix.jpg", result?.posterUrl)
    }

    @Test
    fun aMissingPosterStaysNull() {
        assertNull(searchResult("""{"id": 603, "media_type": "movie", "title": "Matrix"}""")?.posterUrl)
        assertNull(searchResult("""{"id": 603, "media_type": "movie", "title": "Matrix", "poster_path": ""}""")?.posterUrl)
    }

    @Test
    fun releaseYearIgnoresEmptyAndInvalidDates() {
        assertNull(releaseYearOf(null))
        assertNull(releaseYearOf(""))
        assertNull(releaseYearOf("0000-00-00"))
        assertNull(releaseYearOf("sem data"))
        assertEquals(2026, releaseYearOf("2026-10-18"))
    }

    // --- catalogue payloads ----------------------------------------------------------------

    @Test
    fun moviePayloadCarriesTheRuntimeAndSplitsTheGenres() {
        val payload = movieDetails(
            """{"id": 603, "title": "Matrix", "release_date": "1999-03-31", "poster_path": "/matrix.jpg", "runtime": 136, "overview": "Um hacker.", "genres": [{"id": 878, "name": "Ficção científica"}, {"id": 28, "name": "Ação"}]}""",
        ).toCatalogPayload()

        assertEquals(603L, payload.tmdbId)
        assertEquals("Matrix", payload.title)
        assertEquals(1999, payload.releaseYear)
        assertEquals("/matrix.jpg", payload.posterPath, "the DB column is the raw poster path")
        assertEquals(136, payload.runtimeMinutes)
        assertEquals("Ficção científica", payload.mainGenre)
        assertEquals(listOf("Ação"), payload.subgenres)
        assertNull(payload.numberOfSeasons)
    }

    @Test
    fun moviePayloadFallsBackToTheOtherGenreWhenTmdbSendsNone() {
        val payload = movieDetails("""{"id": 1, "title": "Sem gênero", "genres": []}""").toCatalogPayload()

        assertEquals(DEFAULT_GENRE, payload.mainGenre)
        assertTrue(payload.subgenres.isEmpty())
        assertNull(payload.releaseYear)
        assertNull(payload.runtimeMinutes, "some unreleased titles have no runtime")
    }

    @Test
    fun seriesPayloadAveragesTheEpisodeRunTime() {
        val payload = tvDetails(
            """{"id": 1396, "name": "Breaking Bad", "first_air_date": "2008-01-20", "episode_run_time": [47, 49], "number_of_seasons": 5, "number_of_episodes": 62, "genres": [{"id": 18, "name": "Drama"}]}""",
        ).toCatalogPayload()

        assertEquals("Breaking Bad", payload.title)
        assertEquals(2008, payload.releaseYear)
        assertEquals(48, payload.episodeRunTimeMinutes)
        assertEquals(5, payload.numberOfSeasons)
        assertEquals(62, payload.numberOfEpisodes)
        assertEquals("Drama", payload.mainGenre)
    }

    @Test
    fun seriesPayloadWithoutEpisodeRunTimesLeavesTheFieldNull() {
        val payload = tvDetails("""{"id": 1396, "name": "Breaking Bad", "episode_run_time": []}""").toCatalogPayload()

        assertNull(payload.episodeRunTimeMinutes)
    }

    @Test
    fun theWirePayloadKeepsTheKeysTheRpcReads() {
        val payload = movieDetails(
            """{"id": 603, "title": "Matrix", "release_date": "1999-03-31", "poster_path": null, "runtime": 136, "overview": "", "genres": []}""",
        ).toCatalogPayload()

        val body = wire.encodeToJsonElement(payload).jsonObject

        // supabase-kt serializes with `encodeDefaults = false`, so the series-only keys of the payload
        // are dropped for a movie...
        assertTrue("number_of_seasons" !in body, "movie payloads must not send series columns")
        assertTrue("number_of_episodes" !in body)
        assertTrue("episode_run_time_minutes" !in body)
        // ...while the keys the RPC actually reads are always present.
        assertEquals(603, body.getValue("tmdb_id").jsonPrimitive.int)
        assertEquals("Matrix", body.getValue("title").jsonPrimitive.content)
        assertEquals(136, body.getValue("runtime_minutes").jsonPrimitive.int)
        assertEquals(1999, body.getValue("release_year").jsonPrimitive.int)
        assertEquals(DEFAULT_GENRE, body.getValue("main_genre").jsonPrimitive.content)
        assertEquals(0, body.getValue("subgenres").jsonArray.size)
        // A nullable column without a Kotlin default is sent as an explicit null, which the RPC turns
        // into a NULL column value through `nullif(->> 'poster_path', '')`.
        assertEquals(JsonNull, body.getValue("poster_path"))
    }

    @Test
    fun theSeriesPayloadSendsTheEpisodeRunTime() {
        val payload = tvDetails(
            """{"id": 1396, "name": "Breaking Bad", "episode_run_time": [48], "number_of_seasons": 5, "number_of_episodes": 62}""",
        ).toCatalogPayload()

        val body = wire.encodeToJsonElement(payload).jsonObject

        assertTrue("runtime_minutes" !in body, "series payloads must not send the movie runtime")
        assertEquals(48, body.getValue("episode_run_time_minutes").jsonPrimitive.int)
        assertEquals(5, body.getValue("number_of_seasons").jsonPrimitive.int)
        assertEquals(62, body.getValue("number_of_episodes").jsonPrimitive.int)
    }
}
