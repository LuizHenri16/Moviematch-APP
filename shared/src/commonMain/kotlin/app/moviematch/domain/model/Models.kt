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
    /** Shareable short code of the group (`groups.invite_code`), used to invite the other half. */
    val inviteCode: String? = null,
    /**
     * `groups.created_by`, the profile that created the group.
     *
     * It is the identity the backend uses in `remove_member` (`only the creator can remove the
     * partner`). It is kept nullable because the column may be absent from an old row/response.
     */
    val createdBy: String? = null,
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

/**
 * A profile hit of the invite search (`search_profiles` RPC).
 *
 * It carries the `handle` because that is what the invite rows show next to the name; the rest of the
 * app only ever needs a [Profile].
 */
data class ProfileSummary(
    val id: String,
    val displayName: String,
    val handle: String? = null,
    val avatarUrl: String? = null,
)

/**
 * An invite the signed-in member can answer, as read from `public.group_invites`.
 *
 * Only these four fields travel to the app: the RLS policies hide the inviter's profile and the group
 * row from someone who is not a member yet, so the screen cannot name who is inviting — it can only
 * offer to accept or decline (see `data/supabase/GroupRepository.kt`).
 */
data class GroupInvite(
    val id: String,
    val groupId: String,
    val createdAt: String = "",
    val expiresAt: String? = null,
)

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
