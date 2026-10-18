package app.moviematch.domain.model

/**
 * Domain model of the MovieMatch product: two people keep a shared watch list, watch titles
 * together and rate them from 0 to 10.
 *
 * The shapes intentionally mirror the Supabase tables described in `docs/native-app/ER-DIAGRAM.md`
 * of the reference app (`movies`, `series`, `sessions`, `ratings`, `profiles`), so wiring the real
 * backend later is a matter of mapping rows onto these types — no UI change required.
 *
 * Everything here is backend agnostic: no Supabase/HTTP dependency yet.
 */

enum class WatchStatus { PENDING, WATCHED }

enum class ContentType { MOVIE, TV }

/** The two fixed members of the group ("roda"). */
enum class ProfileName(val displayName: String) {
    MARIA("Maria"),
    LUIZ("Luiz"),
    ;

    companion object {
        /** Mirrors `profileNameOf` from the reference app: matches on a substring, case-insensitive. */
        fun fromDisplayName(displayName: String?): ProfileName? {
            val name = displayName?.lowercase() ?: return null
            return when {
                name.contains("maria") -> MARIA
                name.contains("luiz") -> LUIZ
                else -> null
            }
        }
    }
}

data class Profile(
    val id: String,
    val displayName: String,
    val avatarUrl: String? = null,
    val createdAt: String = "",
)

data class Group(
    val id: String,
    val name: String,
    val members: List<Profile> = emptyList(),
    val createdAt: String = "",
)

data class Movie(
    val id: String,
    val tmdbId: Int? = null,
    val title: String,
    val releaseYear: Int? = null,
    val runtimeMinutes: Int? = null,
    val overview: String? = null,
    val posterPath: String? = null,
    val mainGenre: String = "Outro",
    val subgenres: List<String> = emptyList(),
    val createdBy: String? = null,
    val createdAt: String = "",
) {
    /** `main_genre` + `subgenres`, without blanks — used for the tag rows. */
    val genres: List<String> get() = (listOf(mainGenre) + subgenres).filter { it.isNotBlank() }
}

data class Series(
    val id: String,
    val tmdbId: Int? = null,
    val title: String,
    val releaseYear: Int? = null,
    val overview: String? = null,
    val posterPath: String? = null,
    val mainGenre: String = "Outro",
    val subgenres: List<String> = emptyList(),
    val numberOfSeasons: Int? = null,
    val numberOfEpisodes: Int? = null,
    val episodeRunTimeMinutes: Int? = null,
    val createdBy: String? = null,
    val createdAt: String = "",
) {
    val genres: List<String> get() = (listOf(mainGenre) + subgenres).filter { it.isNotBlank() }
}

data class Session(
    val id: String,
    val movieId: String,
    val status: WatchStatus = WatchStatus.PENDING,
    val watchedAt: String? = null,
    val streamingProvider: String? = null,
    val suggestedBy: String? = null,
    val sofaComment: String? = null,
    val createdAt: String = "",
)

data class Rating(
    val id: String,
    val sessionId: String,
    val profileId: String,
    val score: Double,
    val comment: String? = null,
    val createdAt: String = "",
)

/** A rating plus the display name of who wrote it (the only profile data the UI needs). */
data class RatingWithProfile(
    val score: Double,
    val comment: String? = null,
    val createdAt: String? = null,
    val profile: ProfileRef? = null,
)

data class ProfileRef(val displayName: String)

/** A watch session joined with its movie, the shape the detail screen consumes. */
data class SessionWithMovie(
    val id: String,
    val status: WatchStatus,
    val watchedAt: String? = null,
    val streamingProvider: String? = null,
    val sofaComment: String? = null,
    val createdAt: String = "",
    val suggestedBy: ProfileRef? = null,
    val movie: Movie? = null,
    val ratings: List<RatingWithProfile> = emptyList(),
)

/**
 * A list row: either a movie or a series, already flattened into everything the cards and detail
 * screens need. Ported from the reference `ContentItem` type.
 */
data class ContentItem(
    val id: String,
    val type: ContentType,
    val status: WatchStatus,
    val watchedAt: String? = null,
    val streamingProvider: String? = null,
    val sofaComment: String? = null,
    val createdAt: String = "",
    val suggestedBy: ProfileRef? = null,
    val title: String,
    val year: Int? = null,
    val duration: String? = null,
    val overview: String? = null,
    val posterUrl: String? = null,
    val runtimeMinutes: Int? = null,
    val seasons: Int? = null,
    val episodes: Int? = null,
    val genres: List<String> = emptyList(),
    val badgeCategory: String = "",
    val ratings: List<RatingWithProfile> = emptyList(),
) {
    val isWatched: Boolean get() = status == WatchStatus.WATCHED
}

/**
 * Compact projection used by every "list" surface (`MovieCard`, consensus ranking, random pick).
 * Ported from the reference `Movie` type returned by `toMovie`/`contentToMovie`.
 */
data class MovieSummary(
    val id: String,
    val movieId: String,
    val title: String,
    val year: Int? = null,
    val duration: String? = null,
    val posterUrl: String? = null,
    val genres: List<String> = emptyList(),
    val badgeCategory: String = "",
    val isWatched: Boolean = false,
    val mariaRating: Double? = null,
    val luizRating: Double? = null,
    val averageRating: Double? = null,
    val totalRatings: Int = 0,
)

/** TMDb-shaped search hit used by the "Adicionar" screen (mocked for now). */
data class SearchResult(
    val id: Int,
    val type: ContentType,
    val title: String,
    val year: Int? = null,
    val overview: String = "",
    val posterUrl: String? = null,
    val voteAverage: Double? = null,
)
