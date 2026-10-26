package app.moviematch.domain

import app.moviematch.domain.model.ContentItem
import app.moviematch.domain.model.MovieSummary
import app.moviematch.domain.model.ProfileName
import app.moviematch.domain.model.RatingWithProfile
import app.moviematch.domain.model.SessionWithMovie
import app.moviematch.domain.model.WatchStatus
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Pure business rules of the app, ported 1:1 from the reference implementation
 * (`src/lib/movies.ts` plus the aggregations that used to live inside `HomeScreen`).
 *
 * Everything in this file is a pure function so it can be unit tested in `commonTest` and reused by
 * every platform without touching Compose.
 */

private const val IMAGE_BASE_URL = "https://image.tmdb.org/t/p"

/** Builds a TMDb image URL — mirrors `posterUrl()` from the reference `lib/tmdb.ts`. */
fun posterUrl(path: String?, size: String = "w500"): String? =
    path?.takeIf { it.isNotBlank() }?.let { "$IMAGE_BASE_URL/$size$it" }

/** `125` -> `"2h 05m"`, `45` -> `"45m"`, null/0 -> `null`. */
fun formatRuntime(minutes: Int?): String? {
    if (minutes == null || minutes <= 0) return null
    val hours = minutes / 60
    val rest = minutes % 60
    return if (hours > 0) "${hours}h ${rest.toString().padStart(2, '0')}m" else "${rest}m"
}

private val MONTHS = listOf("jan", "fev", "mar", "abr", "mai", "jun", "jul", "ago", "set", "out", "nov", "dez")

/** `"2026-03-04T12:00:00Z"` -> `"4 mar"`. String-only on purpose: no clock/date dependency yet. */
fun formatDateShort(iso: String?): String {
    if (iso.isNullOrBlank()) return "—"
    val datePart = iso.substringBefore('T')
    val parts = datePart.split('-')
    if (parts.size < 3) return "—"
    val year = parts[0].toIntOrNull() ?: return "—"
    val month = parts[1].toIntOrNull() ?: return "—"
    val day = parts[2].toIntOrNull() ?: return "—"
    if (year <= 0 || month !in 1..12 || day !in 1..31) return "—"
    return "$day ${MONTHS[month - 1]}"
}

/** Rounds to one decimal place the same way the reference `round1` helper does. */
fun round1(value: Double): Double = (value * 10.0).roundToInt() / 10.0

/** The two individual scores of a session, one per member of the group. */
data class ProfileScores(val maria: Double? = null, val luiz: Double? = null)

/** Maps each rating to its author using "Maria"/"Luiz" substring matching (legacy). */
fun scoreByProfile(ratings: List<RatingWithProfile>?): ProfileScores {
    var maria: Double? = null
    var luiz: Double? = null
    ratings.orEmpty().forEach { rating ->
        when (ProfileName.fromDisplayName(rating.profile?.displayName)) {
            ProfileName.MARIA -> maria = rating.score
            ProfileName.LUIZ -> luiz = rating.score
            null -> Unit
        }
    }
    return ProfileScores(maria = maria, luiz = luiz)
}

/** Maps each rating to its author by exact display name matching. */
fun scoreByProfileNames(
    ratings: List<RatingWithProfile>?,
    name1: String,
    name2: String,
): Pair<Double?, Double?> {
    var score1: Double? = null
    var score2: Double? = null
    ratings.orEmpty().forEach { rating ->
        val displayName = rating.profile?.displayName?.lowercase() ?: ""
        when {
            displayName == name1.lowercase() -> score1 = rating.score
            displayName == name2.lowercase() -> score2 = rating.score
            else -> Unit
        }
    }
    return score1 to score2
}

/** Arithmetic mean of every rating, or `null` when there is none. */
fun averageOf(ratings: List<RatingWithProfile>?): Double? {
    val list = ratings.orEmpty()
    if (list.isEmpty()) return null
    return list.sumOf { it.score } / list.size
}

/** Flattens a watch session into the card/detail projection. Mirrors `toMovie()`. */
fun toMovie(session: SessionWithMovie): MovieSummary {
    val movie = session.movie
    val scores = scoreByProfile(session.ratings)
    val isWatched = session.status == WatchStatus.WATCHED
    return MovieSummary(
        id = session.id,
        movieId = movie?.id.orEmpty(),
        title = movie?.title ?: "Filme sem título",
        year = movie?.releaseYear,
        duration = formatRuntime(movie?.runtimeMinutes),
        posterUrl = posterUrl(movie?.posterPath),
        genres = movie?.genres.orEmpty(),
        badgeCategory = if (isWatched) movie?.mainGenre.orEmpty().uppercase() else "PENDENTE",
        isWatched = isWatched,
        mariaRating = scores.maria,
        luizRating = scores.luiz,
        averageRating = averageOf(session.ratings),
        totalRatings = session.ratings.size,
    )
}

/** Flattens a list row (movie *or* series) into the card projection. Mirrors `contentToMovie()`. */
fun contentToMovie(item: ContentItem): MovieSummary {
    val scores = scoreByProfile(item.ratings)
    val fallbackTitle = if (item.type == app.moviematch.domain.model.ContentType.TV) "Série sem título" else "Filme sem título"
    return MovieSummary(
        id = item.id,
        movieId = item.id,
        title = item.title.ifBlank { fallbackTitle },
        year = item.year,
        duration = item.duration,
        posterUrl = item.posterUrl,
        genres = item.genres,
        badgeCategory = if (item.isWatched) item.genres.firstOrNull().orEmpty().uppercase() else "PENDENTE",
        isWatched = item.isWatched,
        mariaRating = scores.maria,
        luizRating = scores.luiz,
        averageRating = averageOf(item.ratings),
        totalRatings = item.ratings.size,
    )
}

/** 0 (divergent), 1 (morno), 2 (approved) — the band used to decide whether two people agreed. */
fun verdictBand(score: Double): Int = when {
    score >= 7 -> 2
    score >= 5 -> 1
    else -> 0
}

data class HarmonyCopy(val title: String, val badge: String)

/** Copy shown by the "Índice de Sintonia" card. Mirrors `harmonyCopy()` from the reference Home. */
fun harmonyCopy(percentage: Int, sessionsBoth: Int): HarmonyCopy = when {
    sessionsBoth == 0 -> HarmonyCopy("Conhecendo os Gostos…", "Sintonia em Construção")
    percentage >= 85 -> HarmonyCopy("Sintonia Quase Perfeita!", "Sintonia Estelar ✨")
    percentage >= 60 -> HarmonyCopy("Boa Sintonia!", "Sintonia Boa")
    percentage >= 35 -> HarmonyCopy("Sintonia em Evolução…", "Sintonia Morna")
    else -> HarmonyCopy("Vereditos Divergentes!", "Sintonia em Conflito")
}

/** Verdict seal shown on the detail screen. Mirrors `approvedLabel()`. */
fun approvedLabel(average: Double?, totalRatings: Int): String = when {
    totalRatings == 0 || average == null -> "Aguardando as notas da roda"
    average >= 7 -> "Selo 'Aprovado pela roda!'"
    average >= 5 -> "Veredito morno"
    else -> "Selo 'Quem quer ver de novo?'"
}

/** Badge of the rating dialog footer. Mirrors `ratingVerdict()` from the reference `RatingModal`. */
fun ratingVerdict(average: Double?): String = when {
    average == null -> "Aguardando nota"
    average >= 9 -> "Obra-prima!"
    average >= 8 -> "Excelente!"
    average >= 7 -> "Muito Bom!"
    average >= 5 -> "Regular"
    else -> "Fraco"
}

enum class GenreIcon { GHOST, SMILE, COMPASS, HEART }

/** Maps a genre name to the icon family used by the "Gêneros Favoritos" card. */
fun genreIconType(name: String): GenreIcon {
    val n = name.lowercase()
    if (listOf("terror", "horror", "suspense", "thriller", "misterio", "mystery", "crime").any { n.contains(it) }) {
        return GenreIcon.GHOST
    }
    if (listOf("comédia", "comedia", "comedy", "animação", "animacao", "animation").any { n.contains(it) }) {
        return GenreIcon.SMILE
    }
    if (listOf("aventura", "adventure", "ação", "acao", "action", "sci-fi", "ficção", "ficcao", "fantasia", "fantasy")
            .any { n.contains(it) }
    ) {
        return GenreIcon.COMPASS
    }
    return GenreIcon.HEART
}

// --- Home aggregations ------------------------------------------------------------------

data class ConsensusEntry(
    val rank: Int,
    val title: String,
    val year: Int?,
    val mariaRating: Double?,
    val luizRating: Double?,
    val averageRating: Double,
)

data class GenreStat(
    val name: String,
    val count: Int,
    val percentage: Int,
    val iconType: GenreIcon,
)

/** Everything the Home bento grid renders, derived from the watch list. */
data class HomeStats(
    val totalList: Int = 0,
    val watched: Int = 0,
    val queue: Int = 0,
    val watchedPercentage: Int = 0,
    val bothRatedCount: Int = 0,
    val harmonyPercentage: Int = 0,
    val agreementAvg: Double = 0.0,
    val harmonyTitle: String = "",
    val harmonyBadge: String = "",
    val member1Name: String = "",
    val member1Avg: Double? = null,
    val member1RatingsCount: Int = 0,
    val member2Name: String = "",
    val member2Avg: Double? = null,
    val member2RatingsCount: Int = 0,
    val evaluationsCount: Int = 0,
    val diffPoints: Double? = null,
    val consensus: List<ConsensusEntry> = emptyList(),
    val genres: List<GenreStat> = emptyList(),
)

/**
 * Single pass over the watch list producing every number/copy the Home screen needs.
 * This is the Kotlin port of the `useMemo` block of the reference `HomeScreen`.
 */
fun homeStats(
    items: List<ContentItem>,
    member1Name: String = "",
    member2Name: String = "",
): HomeStats {
    val totalList = items.size
    val watched = items.count { it.isWatched }
    val queue = totalList - watched
    val watchedPercentage = if (totalList > 0) percentage(watched, totalList) else 0

    // --- Índice de Sintonia: how often both members land in the same verdict band. ---
    val bothRated = items.filter { item ->
        val (score1, score2) = scoreByProfileNames(item.ratings, member1Name, member2Name)
        score1 != null && score2 != null
    }
    val agreed = bothRated.count { item ->
        val (score1, score2) = scoreByProfileNames(item.ratings, member1Name, member2Name)
        verdictBand(score1!!) == verdictBand(score2!!)
    }
    val harmonyPercentage = if (bothRated.isNotEmpty()) percentage(agreed, bothRated.size) else 0
    val meanDiff = if (bothRated.isNotEmpty()) {
        bothRated.sumOf { item ->
            val (score1, score2) = scoreByProfileNames(item.ratings, member1Name, member2Name)
            abs(score1!! - score2!!)
        } / bothRated.size
    } else {
        0.0
    }
    val agreementAvg = if (bothRated.isNotEmpty()) round1(10 - meanDiff) else 0.0
    val harmony = harmonyCopy(harmonyPercentage, bothRated.size)

    // --- Duelo de Críticos: individual averages. ---
    var member1Sum = 0.0
    var member1Count = 0
    var member2Sum = 0.0
    var member2Count = 0
    items.forEach { item ->
        item.ratings.forEach { rating ->
            val displayName = rating.profile?.displayName?.lowercase() ?: ""
            when {
                displayName == member1Name.lowercase() -> {
                    member1Sum += rating.score
                    member1Count++
                }
                displayName == member2Name.lowercase() -> {
                    member2Sum += rating.score
                    member2Count++
                }
                else -> Unit
            }
        }
    }
    val member1Avg = if (member1Count > 0) round1(member1Sum / member1Count) else null
    val member2Avg = if (member2Count > 0) round1(member2Sum / member2Count) else null
    val diffPoints = if (member1Avg != null && member2Avg != null) round1(abs(member1Avg - member2Avg)) else null

    // --- Consenso do Grupo: the three best rated titles. ---
    val consensus = items
        .mapNotNull { item ->
            val average = averageOf(item.ratings) ?: return@mapNotNull null
            val scores = scoreByProfile(item.ratings)
            Pair(item, Pair(scores, average))
        }
        .sortedByDescending { (_, value) -> value.second }
        .take(3)
        .mapIndexed { index, (item, value) ->
            val (scores, average) = value
            ConsensusEntry(
                rank = index + 1,
                title = item.title.ifBlank {
                    if (item.type == app.moviematch.domain.model.ContentType.TV) "Série sem título" else "Filme sem título"
                },
                year = item.year,
                mariaRating = scores.maria,
                luizRating = scores.luiz,
                averageRating = round1(average),
            )
        }

    // --- Gêneros Favoritos: distribution of the main genre. ---
    val genreCounts = mutableListOf<Pair<String, Int>>()
    items.forEach { item ->
        val genre = item.genres.firstOrNull() ?: item.badgeCategory
        if (genre.isNotBlank() && genre != "Outro") {
            val index = genreCounts.indexOfFirst { it.first == genre }
            if (index >= 0) {
                genreCounts[index] = genreCounts[index].copy(second = genreCounts[index].second + 1)
            } else {
                genreCounts += genre to 1
            }
        }
    }
    val genres = genreCounts
        .map { (name, count) ->
            GenreStat(
                name = name,
                count = count,
                percentage = if (totalList > 0) percentage(count, totalList) else 0,
                iconType = genreIconType(name),
            )
        }
        .sortedByDescending { it.count }

    return HomeStats(
        totalList = totalList,
        watched = watched,
        queue = queue,
        watchedPercentage = watchedPercentage,
        bothRatedCount = bothRated.size,
        harmonyPercentage = harmonyPercentage,
        agreementAvg = agreementAvg,
        harmonyTitle = harmony.title,
        harmonyBadge = harmony.badge,
        member1Name = member1Name,
        member1Avg = member1Avg,
        member1RatingsCount = member1Count,
        member2Name = member2Name,
        member2Avg = member2Avg,
        member2RatingsCount = member2Count,
        evaluationsCount = member1Count + member2Count,
        diffPoints = diffPoints,
        consensus = consensus,
        genres = genres,
    )
}

private fun percentage(part: Int, total: Int): Int = ((part.toDouble() / total) * 100).roundToInt()
