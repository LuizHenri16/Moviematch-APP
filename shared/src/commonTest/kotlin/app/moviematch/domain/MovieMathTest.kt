package app.moviematch.domain

import app.moviematch.data.mock.MockData
import app.moviematch.domain.model.ContentItem
import app.moviematch.domain.model.ContentType
import app.moviematch.domain.model.Movie
import app.moviematch.domain.model.ProfileName
import app.moviematch.domain.model.ProfileRef
import app.moviematch.domain.model.RatingWithProfile
import app.moviematch.domain.model.SessionWithMovie
import app.moviematch.domain.model.WatchStatus
import app.moviematch.presentation.common.formatDecimals1
import app.moviematch.presentation.common.formatDecimals2
import app.moviematch.presentation.common.formatDecimals2Trimmed
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Covers every pure rule the UI relies on: the flattening helpers shared by the cards, the verdict
 * bands/seals and the Home aggregations computed from the mock watch list.
 *
 * Runs on every target (Android/iOS) because it only depends on `commonMain` code.
 */
class MovieMathTest {

    private fun rating(name: String?, score: Double, comment: String? = null) = RatingWithProfile(
        score = score,
        comment = comment,
        createdAt = "2026-01-01T20:00:00Z",
        profile = name?.let { ProfileRef(displayName = it) },
    )

    // --- formatRuntime ---------------------------------------------------------------------

    @Test
    fun formatRuntimeUsesHoursAndPaddedMinutes() {
        assertEquals("2h 46m", formatRuntime(166))
        assertEquals("2h 21m", formatRuntime(141))
        assertEquals("3h 00m", formatRuntime(180))
        assertEquals("1h 00m", formatRuntime(60))
        assertEquals("1h 25m", formatRuntime(85))
        assertEquals("45m", formatRuntime(45))
    }

    @Test
    fun formatRuntimeTreatsMissingValuesAsAbsent() {
        assertNull(formatRuntime(null))
        assertNull(formatRuntime(0))
        assertNull(formatRuntime(-10))
    }

    // --- formatDateShort -------------------------------------------------------------------

    @Test
    fun formatDateShortPrintsPortugueseMonthAbbreviation() {
        assertEquals("14 fev", formatDateShort("2026-02-14"))
        assertEquals("4 mar", formatDateShort("2026-03-04T12:00:00Z"))
        assertEquals("1 jan", formatDateShort("2026-01-01"))
        assertEquals("4 fev", formatDateShort("2026-2-4"))
    }

    @Test
    fun formatDateShortFailsSafeOnBadInput() {
        assertEquals("—", formatDateShort(null))
        assertEquals("—", formatDateShort(""))
        assertEquals("—", formatDateShort("not-a-date"))
        assertEquals("—", formatDateShort("2026-02"))
        assertEquals("—", formatDateShort("2026-13-01"))
        assertEquals("—", formatDateShort("2026-00-10"))
        assertEquals("—", formatDateShort("2026-02-32"))
    }

    // --- round1 ----------------------------------------------------------------------------

    @Test
    fun round1KeepsOneDecimal() {
        assertEquals(1.4, round1(1.4))
        assertEquals(9.3, round1(9.25))
        assertEquals(5.8, round1(5.75))
        assertEquals(8.6, round1(8.6))
        assertEquals(-1.4, round1(-1.4))
    }

    // --- posterUrl -------------------------------------------------------------------------

    @Test
    fun posterUrlBuildsTmdbPathAndRejectsBlanks() {
        assertEquals("https://image.tmdb.org/t/p/w500/abc.jpg", posterUrl("/abc.jpg"))
        assertEquals("https://image.tmdb.org/t/p/w185/abc.jpg", posterUrl("/abc.jpg", size = "w185"))
        assertNull(posterUrl(null))
        assertNull(posterUrl(""))
        assertNull(posterUrl("   "))
    }

    // --- scoreByProfile / averageOf --------------------------------------------------------

    @Test
    fun scoreByProfileMatchesOnDisplayName() {
        val scores = scoreByProfile(listOf(rating("Maria", 9.5), rating("Luiz", 9.0)))
        assertEquals(9.5, scores.maria)
        assertEquals(9.0, scores.luiz)
    }

    @Test
    fun scoreByProfileIgnoresUnknownAuthorsAndNulls() {
        assertEquals(ProfileScores(), scoreByProfile(null))
        assertEquals(ProfileScores(), scoreByProfile(emptyList()))
        assertEquals(ProfileScores(), scoreByProfile(listOf(rating("Ana", 5.0))))
        assertEquals(ProfileScores(), scoreByProfile(listOf(rating(null, 5.0))))

        val partial = scoreByProfile(listOf(rating("Maria", 8.0), rating("Visitante", 1.0)))
        assertEquals(8.0, partial.maria)
        assertNull(partial.luiz)
    }

    @Test
    fun averageOfReturnsArithmeticMeanOrNull() {
        assertNull(averageOf(null))
        assertNull(averageOf(emptyList()))
        assertEquals(9.25, averageOf(listOf(rating("Maria", 9.5), rating("Luiz", 9.0))))
        assertEquals(10.0, averageOf(listOf(rating("Maria", 10.0))))
    }

    @Test
    fun profileNameParsesCaseInsensitiveSubstrings() {
        assertEquals(ProfileName.MARIA, ProfileName.fromDisplayName("Maria"))
        assertEquals(ProfileName.MARIA, ProfileName.fromDisplayName("maria silva"))
        assertEquals(ProfileName.LUIZ, ProfileName.fromDisplayName("LUIZ"))
        assertNull(ProfileName.fromDisplayName("Ana"))
        assertNull(ProfileName.fromDisplayName(null))
    }

    // --- toMovie ---------------------------------------------------------------------------

    @Test
    fun toMovieFlattensWatchedSession() {
        val session = SessionWithMovie(
            id = "session-1",
            status = WatchStatus.WATCHED,
            watchedAt = "2026-02-14",
            movie = Movie(
                id = "movie-1",
                title = "Duna: Parte Dois",
                releaseYear = 2024,
                runtimeMinutes = 166,
                mainGenre = "Ficção Científica",
                subgenres = listOf("Aventura"),
            ),
            ratings = listOf(rating("Maria", 9.5), rating("Luiz", 9.0)),
        )

        val summary = toMovie(session)
        assertEquals("session-1", summary.id)
        assertEquals("movie-1", summary.movieId)
        assertEquals("Duna: Parte Dois", summary.title)
        assertEquals(2024, summary.year)
        assertEquals("2h 46m", summary.duration)
        assertNull(summary.posterUrl)
        assertEquals(listOf("Ficção Científica", "Aventura"), summary.genres)
        assertEquals("FICÇÃO CIENTÍFICA", summary.badgeCategory)
        assertTrue(summary.isWatched)
        assertEquals(9.5, summary.mariaRating)
        assertEquals(9.0, summary.luizRating)
        assertEquals(9.25, summary.averageRating)
        assertEquals(2, summary.totalRatings)
    }

    @Test
    fun toMovieHandlesPendingSessionWithoutMovie() {
        val summary = toMovie(SessionWithMovie(id = "session-2", status = WatchStatus.PENDING))
        assertEquals("session-2", summary.id)
        assertEquals("", summary.movieId)
        assertEquals("Filme sem título", summary.title)
        assertNull(summary.year)
        assertNull(summary.duration)
        assertEquals("PENDENTE", summary.badgeCategory)
        assertFalse(summary.isWatched)
        assertNull(summary.averageRating)
        assertEquals(0, summary.totalRatings)
    }

    // --- contentToMovie --------------------------------------------------------------------

    @Test
    fun contentToMovieBadgesPendingAndWatchedRows() {
        val watched = contentToMovie(
            ContentItem(
                id = "c-1",
                type = ContentType.MOVIE,
                status = WatchStatus.WATCHED,
                title = "A Substância",
                year = 2024,
                duration = "2h 21m",
                genres = listOf("Terror", "Ficção Científica"),
                badgeCategory = "TERROR",
                ratings = listOf(rating("Maria", 7.5), rating("Luiz", 4.0)),
            ),
        )
        assertEquals("TERROR", watched.badgeCategory)
        assertTrue(watched.isWatched)
        assertEquals(5.75, watched.averageRating)
        assertEquals(2, watched.totalRatings)

        val pending = contentToMovie(
            ContentItem(
                id = "c-2",
                type = ContentType.TV,
                status = WatchStatus.PENDING,
                title = "",
                genres = listOf("Ficção Científica"),
                badgeCategory = "PENDENTE",
            ),
        )
        assertEquals("Série sem título", pending.title)
        assertEquals("PENDENTE", pending.badgeCategory)
        assertFalse(pending.isWatched)
        assertEquals("c-2", pending.movieId)
    }

    // --- verdictBand / harmonyCopy ---------------------------------------------------------

    @Test
    fun verdictBandSplitsAtFiveAndSeven() {
        assertEquals(2, verdictBand(10.0))
        assertEquals(2, verdictBand(7.0))
        assertEquals(1, verdictBand(6.99))
        assertEquals(1, verdictBand(5.0))
        assertEquals(0, verdictBand(4.99))
        assertEquals(0, verdictBand(0.0))
    }

    @Test
    fun harmonyCopyFollowsPercentageBands() {
        val building = harmonyCopy(percentage = 0, sessionsBoth = 0)
        assertEquals("Conhecendo os Gostos…", building.title)
        assertEquals("Sintonia em Construção", building.badge)

        assertEquals("Sintonia Quase Perfeita!", harmonyCopy(85, 4).title)
        assertEquals("Sintonia Estelar ✨", harmonyCopy(100, 4).badge)

        assertEquals("Boa Sintonia!", harmonyCopy(60, 4).title)
        assertEquals("Sintonia Boa", harmonyCopy(84, 4).badge)

        assertEquals("Sintonia em Evolução…", harmonyCopy(35, 4).title)
        assertEquals("Sintonia Morna", harmonyCopy(59, 4).badge)

        assertEquals("Vereditos Divergentes!", harmonyCopy(34, 4).title)
        assertEquals("Sintonia em Conflito", harmonyCopy(0, 4).badge)
    }

    // --- approvedLabel / ratingVerdict -----------------------------------------------------

    @Test
    fun approvedLabelDescribesTheGroupVerdict() {
        assertEquals("Aguardando as notas da roda", approvedLabel(average = null, totalRatings = 0))
        assertEquals("Aguardando as notas da roda", approvedLabel(average = 8.0, totalRatings = 0))
        assertEquals("Selo 'Aprovado pela Maria!'", approvedLabel(average = 7.0, totalRatings = 2))
        assertEquals("Veredito morno", approvedLabel(average = 5.0, totalRatings = 2))
        assertEquals("Selo 'Quem quer ver de novo?'", approvedLabel(average = 4.9, totalRatings = 2))
    }

    @Test
    fun ratingVerdictFollowsTheScoreBands() {
        assertEquals("Aguardando nota", ratingVerdict(null))
        assertEquals("Obra-prima!", ratingVerdict(9.0))
        assertEquals("Obra-prima!", ratingVerdict(10.0))
        assertEquals("Excelente!", ratingVerdict(8.0))
        assertEquals("Muito Bom!", ratingVerdict(7.0))
        assertEquals("Regular", ratingVerdict(5.0))
        assertEquals("Fraco", ratingVerdict(4.9))
    }

    // --- genreIconType ---------------------------------------------------------------------

    @Test
    fun genreIconTypeMapsGenresToIconFamilies() {
        assertEquals(GenreIcon.GHOST, genreIconType("Terror"))
        assertEquals(GenreIcon.GHOST, genreIconType("Suspense"))
        assertEquals(GenreIcon.SMILE, genreIconType("Comédia"))
        assertEquals(GenreIcon.SMILE, genreIconType("Animação"))
        assertEquals(GenreIcon.COMPASS, genreIconType("Ficção Científica"))
        assertEquals(GenreIcon.COMPASS, genreIconType("Aventura"))
        assertEquals(GenreIcon.HEART, genreIconType("Drama"))
        assertEquals(GenreIcon.HEART, genreIconType("Romance"))
    }

    // --- homeStats -------------------------------------------------------------------------

    @Test
    fun homeStatsAggregatesTheMockWatchList() {
        val stats = homeStats(MockData.items())

        assertEquals(9, stats.totalList)
        assertEquals(5, stats.watched)
        assertEquals(4, stats.queue)
        assertEquals(56, stats.watchedPercentage)

        // Índice de Sintonia: 3 of the 5 shared sessions land in the same verdict band.
        assertEquals(5, stats.bothRatedCount)
        assertEquals(60, stats.harmonyPercentage)
        assertEquals(8.6, stats.agreementAvg)
        assertEquals("Boa Sintonia!", stats.harmonyTitle)
        assertEquals("Sintonia Boa", stats.harmonyBadge)
    }

    @Test
    fun homeStatsComputesTheCriticsDuel() {
        val stats = homeStats(MockData.items())

        assertEquals(8.8, stats.mariaAvg)
        assertEquals(5, stats.mariaRatingsCount)
        assertEquals(7.4, stats.luizAvg)
        assertEquals(5, stats.luizRatingsCount)
        assertEquals(10, stats.evaluationsCount)
        assertEquals(1.4, stats.diffPoints)
    }

    @Test
    fun homeStatsRanksTheTopThreeByAverage() {
        val consensus = homeStats(MockData.items()).consensus

        assertEquals(3, consensus.size)
        assertEquals(listOf("Ruptura", "Duna: Parte Dois", "Oppenheimer"), consensus.map { it.title })
        assertEquals(listOf(1, 2, 3), consensus.map { it.rank })
        // The reference rounds the stored average to one decimal (`round1`), even though the
        // ranking itself is sorted by the raw average.
        assertEquals(9.8, consensus[0].averageRating)
        assertEquals(9.3, consensus[1].averageRating)
        assertEquals(8.8, consensus[2].averageRating)
        assertEquals(10.0, consensus[0].mariaRating)
        assertEquals(9.5, consensus[0].luizRating)
    }

    @Test
    fun homeStatsCountsGenresFromThePrimaryGenre() {
        val genres = homeStats(MockData.items()).genres

        assertEquals(listOf("Ficção Científica", "Drama", "Terror", "Fantasia", "Comédia", "Animação"), genres.map { it.name })
        assertEquals(listOf(2, 2, 2, 1, 1, 1), genres.map { it.count })
        assertEquals(22, genres.first().percentage)
        assertEquals(GenreIcon.COMPASS, genres.first().iconType)
        assertEquals(GenreIcon.HEART, genres.first { it.name == "Drama" }.iconType)
        assertEquals(GenreIcon.GHOST, genres.first { it.name == "Terror" }.iconType)
    }

    @Test
    fun homeStatsIsEmptySafe() {
        val stats = homeStats(emptyList())
        assertEquals(0, stats.totalList)
        assertEquals(0, stats.watchedPercentage)
        assertEquals(0, stats.harmonyPercentage)
        assertEquals(0.0, stats.agreementAvg)
        assertNull(stats.mariaAvg)
        assertNull(stats.luizAvg)
        assertNull(stats.diffPoints)
        assertTrue(stats.consensus.isEmpty())
        assertTrue(stats.genres.isEmpty())
    }

    // --- number formatting -----------------------------------------------------------------

    @Test
    fun formatDecimalsMatchesToFixed() {
        assertEquals("8.8", 8.8.formatDecimals1())
        assertEquals("9.0", 9.0.formatDecimals1())
        assertEquals("7.4", 7.44.formatDecimals1())
        assertEquals("7.5", 7.46.formatDecimals1())
        assertEquals("-1.5", (-1.5).formatDecimals1())

        assertEquals("9.75", 9.75.formatDecimals2())
        assertEquals("8.00", 8.0.formatDecimals2())
    }

    @Test
    fun formatDecimals2TrimmedDropsWholeCents() {
        assertEquals("9.75", 9.75.formatDecimals2Trimmed())
        assertEquals("8", 8.0.formatDecimals2Trimmed())
        assertEquals("8.50", 8.5.formatDecimals2Trimmed())
    }
}
