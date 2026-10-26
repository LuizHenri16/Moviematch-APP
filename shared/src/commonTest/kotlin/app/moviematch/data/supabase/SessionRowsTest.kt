package app.moviematch.data.supabase

import app.moviematch.domain.model.ContentType
import app.moviematch.domain.model.WatchStatus
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Covers the PostgREST payload -> domain mapping of the two session tables.
 *
 * The fixtures are decoded from JSON (not built as objects) so the `@SerialName` column names are
 * exercised too: a typo in `SESSIONS_SELECT`/`SERIES_SESSIONS_SELECT` would make a field arrive null
 * and fail these assertions.
 */
class SessionRowsTest {

    private val json = Json { ignoreUnknownKeys = true }

    private fun movieSession(raw: String) = json.decodeFromString<List<SessionRow>>(raw).single()

    private fun seriesSession(raw: String) = json.decodeFromString<List<SeriesSessionRow>>(raw).single()

    private companion object {
        const val MOVIE_SESSION = """
            [
              {
                "id": "session-1",
                "status": "PENDING",
                "watched_at": null,
                "streaming_provider": null,
                "sofa_comment": null,
                "created_at": "2026-10-01T12:00:00.000Z",
                "suggested_by": { "display_name": "Luiz" },
                "movie": {
                  "id": "movie-1",
                  "title": "Matrix",
                  "release_year": 1999,
                  "runtime_minutes": 136,
                  "overview": "Um hacker descobre a verdade.",
                  "poster_path": "/matrix.jpg",
                  "main_genre": "Ficção científica",
                  "subgenres": ["Ação", "  "]
                },
                "ratings": [
                  {
                    "score": 9.0,
                    "comment": "Clássico",
                    "created_at": "2026-10-02T12:00:00.000Z",
                    "profile": { "display_name": "Maria" }
                  },
                  { "score": 7.5, "comment": "   ", "created_at": null, "profile": null }
                ]
              }
            ]
        """

        const val SERIES_SESSION = """
            [
              {
                "id": "series-session-1",
                "status": "WATCHED",
                "watched_at": "2026-10-10",
                "streaming_provider": "Netflix",
                "sofa_comment": "Melhor que o filme.",
                "created_at": "2026-10-05T12:00:00.000Z",
                "suggested_by": { "display_name": "Maria" },
                "series": {
                  "id": "series-1",
                  "title": "Breaking Bad",
                  "release_year": 2008,
                  "overview": "Um professor de química.",
                  "poster_path": "/bb.jpg",
                  "main_genre": "Drama",
                  "subgenres": ["Crime"],
                  "number_of_seasons": 5,
                  "number_of_episodes": 62,
                  "episode_run_time_minutes": 48
                },
                "series_ratings": [
                  { "score": 10.0, "comment": null, "created_at": "2026-10-11T12:00:00.000Z", "profile": { "display_name": "Luiz" } }
                ]
              }
            ]
        """
    }

    @Test
    fun movieSessionMapsEveryListField() {
        val item = movieSession(MOVIE_SESSION).toContentItem()

        assertEquals("session-1", item.id)
        assertEquals(ContentType.MOVIE, item.type)
        assertEquals(WatchStatus.PENDING, item.status)
        assertEquals("2026-10-01T12:00:00.000Z", item.createdAt)
        assertEquals("Luiz", item.suggestedBy?.displayName)
        assertEquals("Matrix", item.title)
        assertEquals(1999, item.year)
        assertEquals("2h 16m", item.duration)
        assertEquals(136, item.runtimeMinutes)
        assertEquals("Um hacker descobre a verdade.", item.overview)
        assertNull(item.watchedAt)
        assertNull(item.streamingProvider)
        assertNull(item.sofaComment)
    }

    @Test
    fun movieSessionTurnsThePosterPathIntoAFullUrl() {
        val item = movieSession(MOVIE_SESSION).toContentItem()

        assertEquals("https://image.tmdb.org/t/p/w500/matrix.jpg", item.posterUrl)
    }

    @Test
    fun genresKeepTheMainGenreFirstAndDropBlanks() {
        val item = movieSession(MOVIE_SESSION).toContentItem()

        assertEquals(listOf("Ficção científica", "Ação"), item.genres)
        assertEquals("PENDENTE", item.badgeCategory)
    }

    @Test
    fun ratingsAreFlattenedAndBlankCommentsBecomeNull() {
        val item = movieSession(MOVIE_SESSION).toContentItem()

        assertEquals(2, item.ratings.size)
        assertEquals(9.0, item.ratings[0].score)
        assertEquals("Clássico", item.ratings[0].comment)
        assertEquals("Maria", item.ratings[0].profile?.displayName)
        assertEquals("2026-10-02T12:00:00.000Z", item.ratings[0].createdAt)
        assertNull(item.ratings[1].comment, "a comment with only spaces carries no information")
        assertNull(item.ratings[1].profile)
    }

    @Test
    fun watchedMovieUsesTheLeadingGenreAsBadge() {
        val raw = MOVIE_SESSION.replace("\"status\": \"PENDING\"", "\"status\": \"WATCHED\"")

        val item = movieSession(raw).toContentItem()

        assertEquals(WatchStatus.WATCHED, item.status)
        assertEquals("FICÇÃO CIENTÍFICA", item.badgeCategory)
    }

    @Test
    fun anUnknownStatusDegradesToPending() {
        val raw = MOVIE_SESSION.replace("\"status\": \"PENDING\"", "\"status\": \"ARCHIVED\"")

        assertEquals(WatchStatus.PENDING, movieSession(raw).toContentItem().status)
    }

    @Test
    fun seriesSessionKeepsDurationNullAndCarriesSeasonsAndEpisodes() {
        val item = seriesSession(SERIES_SESSION).toContentItem()

        assertEquals(ContentType.TV, item.type)
        assertEquals(WatchStatus.WATCHED, item.status)
        assertEquals("2026-10-10", item.watchedAt)
        assertEquals("Netflix", item.streamingProvider)
        assertEquals("Melhor que o filme.", item.sofaComment)
        assertEquals("Breaking Bad", item.title)
        assertEquals(2008, item.year)
        assertNull(item.duration, "a series has no single runtime")
        assertEquals(48, item.runtimeMinutes)
        assertEquals(5, item.seasons)
        assertEquals(62, item.episodes)
        assertEquals("https://image.tmdb.org/t/p/w500/bb.jpg", item.posterUrl)
        assertEquals(listOf("Drama", "Crime"), item.genres)
        assertEquals("DRAMA", item.badgeCategory)
        assertEquals("Maria", item.suggestedBy?.displayName)
        assertEquals(1, item.ratings.size)
        assertEquals(10.0, item.ratings.single().score)
    }

    @Test
    fun aMissingEmbedDegradesToAnEmptyItem() {
        val raw = """[{"id": "session-2", "status": "PENDING"}]"""

        val item = movieSession(raw).toContentItem()

        assertEquals("", item.title)
        assertNull(item.year)
        assertNull(item.duration)
        assertNull(item.posterUrl)
        assertNull(item.suggestedBy)
        assertEquals(emptyList(), item.genres)
        assertEquals(emptyList(), item.ratings)
        assertEquals("PENDENTE", item.badgeCategory)
        assertEquals("", item.createdAt)
        assertNull(item.overview)
    }
}
