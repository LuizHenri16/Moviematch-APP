package app.moviematch.data.supabase

import app.moviematch.domain.formatRuntime
import app.moviematch.domain.model.ContentItem
import app.moviematch.domain.model.ContentType
import app.moviematch.domain.model.ProfileRef
import app.moviematch.domain.model.RatingWithProfile
import app.moviematch.domain.model.WatchStatus
import app.moviematch.domain.posterUrl
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * PostgREST resource names of the movie/series session mirrors (see `supabase/0001_init.sql`).
 *
 * Movies and series live in two structurally identical tables (`sessions`/`series_sessions` and
 * `ratings`/`series_ratings`). Every read or write that does not know the type up front tries the
 * movie table first and falls back to the series one, so the names are declared once here.
 */
internal const val SESSIONS_TABLE = "sessions"
internal const val SERIES_SESSIONS_TABLE = "series_sessions"
internal const val RATINGS_TABLE = "ratings"
internal const val SERIES_RATINGS_TABLE = "series_ratings"

/**
 * Column list used by every session read, mirroring `SESSIONS_SELECT` of the reference
 * `hooks/useData.tsx` (same joins and same shape).
 *
 * It is a raw string because PostgREST resource embedding (`alias:relation(columns)`) cannot be
 * expressed with [io.github.jan.supabase.postgrest.query.Columns.list]. Whitespace is stripped by
 * `Columns.raw`, so the string is kept on one logical line per embedded resource for readability.
 */
internal const val SESSIONS_SELECT =
    "id,status,watched_at,streaming_provider,sofa_comment,created_at," +
        "suggested_by:profiles(display_name)," +
        "movie:movies(id,title,release_year,runtime_minutes,overview,poster_path,main_genre,subgenres)," +
        "ratings(score,comment,created_at,profile:profiles(display_name))"

/**
 * Series counterpart of [SESSIONS_SELECT].
 *
 * Deliberate divergence from the reference: the `suggested_by` column is embedded as
 * `profiles(display_name)` here too. The reference selects the raw UUID for series and resolves the
 * name client side from the loaded profiles; embedding keeps both session types symmetric and removes
 * that lookups table from the app.
 */
internal const val SERIES_SESSIONS_SELECT =
    "id,status,watched_at,streaming_provider,sofa_comment,created_at," +
        "suggested_by:profiles(display_name)," +
        "series:series(id,title,release_year,overview,poster_path,main_genre,subgenres," +
        "number_of_seasons,number_of_episodes,episode_run_time_minutes)," +
        "series_ratings(score,comment,created_at,profile:profiles(display_name))"

/** Minimal projection used by mutators that only need the table, the group and the status. */
internal const val SESSION_CORE_SELECT = "id,group_id,status"

/** Badge shown by the cards while a title is still queued (mirrors the mock/UI rule). */
internal const val BADGE_PENDING = "PENDENTE"

/** `public.movies`, as embedded inside a `sessions` row. */
@Serializable
internal data class MovieRow(
    val id: String,
    val title: String,
    @SerialName("release_year") val releaseYear: Int? = null,
    @SerialName("runtime_minutes") val runtimeMinutes: Int? = null,
    val overview: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("main_genre") val mainGenre: String? = null,
    val subgenres: List<String>? = null,
)

/** `public.series`, as embedded inside a `series_sessions` row. */
@Serializable
internal data class SeriesRow(
    val id: String,
    val title: String,
    @SerialName("release_year") val releaseYear: Int? = null,
    val overview: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("main_genre") val mainGenre: String? = null,
    val subgenres: List<String>? = null,
    @SerialName("number_of_seasons") val numberOfSeasons: Int? = null,
    @SerialName("number_of_episodes") val numberOfEpisodes: Int? = null,
    @SerialName("episode_run_time_minutes") val episodeRunTimeMinutes: Int? = null,
)

/** The only profile data a rating needs: who wrote it. */
@Serializable
internal data class ProfileRefRow(@SerialName("display_name") val displayName: String? = null)

/** `public.ratings`, as embedded inside a `sessions` row (and reused for `series_ratings`). */
@Serializable
internal data class RatingRow(
    val score: Double,
    val comment: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    val profile: ProfileRefRow? = null,
)

/** One row of `public.sessions` joined with its movie, suggestion author and ratings. */
@Serializable
internal data class SessionRow(
    val id: String,
    val status: String,
    @SerialName("watched_at") val watchedAt: String? = null,
    @SerialName("streaming_provider") val streamingProvider: String? = null,
    @SerialName("sofa_comment") val sofaComment: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("suggested_by") val suggestedBy: ProfileRefRow? = null,
    val movie: MovieRow? = null,
    val ratings: List<RatingRow> = emptyList(),
)

/** One row of `public.series_sessions` joined with its series, suggestion author and ratings. */
@Serializable
internal data class SeriesSessionRow(
    val id: String,
    val status: String,
    @SerialName("watched_at") val watchedAt: String? = null,
    @SerialName("streaming_provider") val streamingProvider: String? = null,
    @SerialName("sofa_comment") val sofaComment: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("suggested_by") val suggestedBy: ProfileRefRow? = null,
    val series: SeriesRow? = null,
    @SerialName("series_ratings") val ratings: List<RatingRow> = emptyList(),
)

/** `id, group_id, status` of a session, whatever its type. */
@Serializable
internal data class SessionCoreRow(
    val id: String,
    @SerialName("group_id") val groupId: String,
    val status: String,
)

/**
 * Translates the `watch_status` enum (returned as text) into the domain enum.
 *
 * Anything that is not the single known "watched" value is treated as pending, so an unexpected value
 * degrades to the queue instead of hiding the row.
 */
internal fun watchStatusOf(raw: String?): WatchStatus =
    if (raw == WatchStatus.WATCHED.name) WatchStatus.WATCHED else WatchStatus.PENDING

/**
 * Badge of the list cards: the leading genre uppercased once watched, `"PENDENTE"` otherwise.
 * Mirrors `MockData`/`MockMovieRepository` so the real backend renders exactly like the seed.
 */
internal fun badgeCategoryOf(status: WatchStatus, genres: List<String>): String =
    if (status == WatchStatus.WATCHED) genres.firstOrNull().orEmpty().uppercase() else BADGE_PENDING

/** `main_genre` first, then `subgenres`, dropping blanks — the order the tag rows and badges expect. */
private fun genresOf(mainGenre: String?, subgenres: List<String>?): List<String> =
    (listOf(mainGenre.orEmpty()) + subgenres.orEmpty()).filter { it.isNotBlank() }

/** A `suggested_by` embed only exists when the profile could be read and has a name. */
internal fun ProfileRefRow.toRef(): ProfileRef? =
    displayName?.takeIf { it.isNotBlank() }?.let { ProfileRef(displayName = it) }

private fun RatingRow.toRatingWithProfile(): RatingWithProfile = RatingWithProfile(
    score = score,
    comment = comment?.takeIf { it.isNotBlank() },
    createdAt = createdAt,
    profile = profile?.toRef(),
)

/**
 * Flattens a `sessions` row into the list/detail model.
 *
 * Movies carry their runtime through [formatRuntime] (the same `"2h 46m"` formatting the mock and the
 * detail screen use); the raw minutes stay in [ContentItem.runtimeMinutes] for the hero meta line.
 *
 * `poster_path` is stored raw (that is the DB convention and what `posterUrl(path)` documents), but
 * [ContentItem.posterUrl] is the ready-to-use URL: the poster surfaces hand it straight to Coil, and
 * `contentToMovie` copies it verbatim into `MovieSummary.posterUrl`.
 */
internal fun SessionRow.toContentItem(): ContentItem {
    val watchStatus = watchStatusOf(status)
    val genres = genresOf(movie?.mainGenre, movie?.subgenres)
    return ContentItem(
        id = id,
        type = ContentType.MOVIE,
        status = watchStatus,
        watchedAt = watchedAt,
        streamingProvider = streamingProvider,
        sofaComment = sofaComment,
        createdAt = createdAt.orEmpty(),
        suggestedBy = suggestedBy?.toRef(),
        title = movie?.title.orEmpty(),
        year = movie?.releaseYear,
        duration = formatRuntime(movie?.runtimeMinutes),
        overview = movie?.overview,
        posterUrl = posterUrl(movie?.posterPath),
        runtimeMinutes = movie?.runtimeMinutes,
        genres = genres,
        badgeCategory = badgeCategoryOf(watchStatus, genres),
        ratings = ratings.map { it.toRatingWithProfile() },
    )
}

/**
 * Flattens a `series_sessions` row into the list/detail model.
 *
 * Series have no single runtime, so [ContentItem.duration] stays `null` (matching the mock) and the
 * detail screen renders seasons/episodes instead. The typical episode length is still carried in
 * [ContentItem.runtimeMinutes] for callers that want it, and the poster follows the same
 * path-to-URL rule as the movie mapper.
 */
internal fun SeriesSessionRow.toContentItem(): ContentItem {
    val watchStatus = watchStatusOf(status)
    val genres = genresOf(series?.mainGenre, series?.subgenres)
    return ContentItem(
        id = id,
        type = ContentType.TV,
        status = watchStatus,
        watchedAt = watchedAt,
        streamingProvider = streamingProvider,
        sofaComment = sofaComment,
        createdAt = createdAt.orEmpty(),
        suggestedBy = suggestedBy?.toRef(),
        title = series?.title.orEmpty(),
        year = series?.releaseYear,
        duration = null,
        overview = series?.overview,
        posterUrl = posterUrl(series?.posterPath),
        runtimeMinutes = series?.episodeRunTimeMinutes,
        seasons = series?.numberOfSeasons,
        episodes = series?.numberOfEpisodes,
        genres = genres,
        badgeCategory = badgeCategoryOf(watchStatus, genres),
        ratings = ratings.map { it.toRatingWithProfile() },
    )
}
