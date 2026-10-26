package app.moviematch.data.supabase

import app.moviematch.domain.model.ContentType
import app.moviematch.domain.model.SearchResult
import app.moviematch.domain.posterUrl
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.math.roundToInt

/**
 * TMDb wire models and their mappers.
 *
 * Field names follow the TMDb v3 JSON (snake_case); only the keys the app consumes are declared.
 * Mirrors `src/lib/tmdb.ts` of the reference implementation (same endpoints, same fields).
 */

/** `"Outro"` when TMDb returned no usable genre — same fallback used by the `upsert_*` RPCs. */
internal const val DEFAULT_GENRE = "Outro"

/** Envelope of `GET /search/multi`. */
@Serializable
internal data class TmdbSearchResponse(val results: List<TmdbSearchResult> = emptyList())

/**
 * One hit of `GET /search/multi`.
 *
 * Movies expose `title`/`release_date`, series expose `name`/`first_air_date`; `media_type` tells
 * them apart and may also be `"person"`, which is filtered out by [toSearchResult].
 */
@Serializable
internal data class TmdbSearchResult(
    val id: Int,
    @SerialName("media_type") val mediaType: String? = null,
    val title: String? = null,
    val name: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("first_air_date") val firstAirDate: String? = null,
    val overview: String? = null,
    @SerialName("vote_average") val voteAverage: Double? = null,
)

/** `GET /movie/{id}` (only the fields the watch list stores). */
@Serializable
internal data class TmdbMovieDetails(
    val id: Int,
    val title: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    val overview: String? = null,
    val runtime: Int? = null,
    val genres: List<TmdbGenre> = emptyList(),
)

/** `GET /tv/{id}` (only the fields the watch list stores). */
@Serializable
internal data class TmdbTvDetails(
    val id: Int,
    val name: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("first_air_date") val firstAirDate: String? = null,
    val overview: String? = null,
    @SerialName("episode_run_time") val episodeRunTime: List<Int> = emptyList(),
    val genres: List<TmdbGenre> = emptyList(),
    @SerialName("number_of_seasons") val numberOfSeasons: Int? = null,
    @SerialName("number_of_episodes") val numberOfEpisodes: Int? = null,
)

/** A genre as returned by the detail endpoints (`{"id": ..., "name": ...}`). */
@Serializable
internal data class TmdbGenre(val name: String = "")

/** `"movie"`/`"tv"` mapped to the domain enum; `person` and unknown values map to `null`. */
internal fun mediaTypeOf(raw: String?): ContentType? = when (raw) {
    "movie" -> ContentType.MOVIE
    "tv" -> ContentType.TV
    else -> null
}

/** `"2024-02-27"` -> `2024`; mirrors `releaseYear()` of the reference `lib/tmdb.ts`. */
internal fun releaseYearOf(date: String?): Int? = date?.take(4)?.toIntOrNull()?.takeIf { it > 0 }

/**
 * Poster size used by the search rows.
 *
 * The "Adicionar" list renders a small thumbnail and the reference selects `w185` for that surface
 * (`AddMovieScreen`: `posterUrl(result.poster_path, 'w185')`).
 */
internal const val SEARCH_POSTER_SIZE = "w185"

/**
 * Maps a `/search/multi` hit onto the domain [SearchResult], dropping people and anything that is
 * neither a movie nor a series (the same filter the reference applies).
 *
 * [SearchResult.posterUrl] is a ready-to-use image URL (`posterUrl(path, SEARCH_POSTER_SIZE)`), not
 * the raw `poster_path`, because the search row hands it straight to Coil. The raw path only travels
 * in the catalogue payloads, where the database column is `poster_path`.
 */
internal fun TmdbSearchResult.toSearchResult(): SearchResult? {
    val type = mediaTypeOf(mediaType) ?: return null
    return SearchResult(
        id = id,
        type = type,
        title = title ?: name.orEmpty(),
        year = releaseYearOf(releaseDate ?: firstAirDate),
        overview = overview.orEmpty(),
        posterUrl = posterUrl(posterPath, SEARCH_POSTER_SIZE),
        voteAverage = voteAverage,
    )
}

/**
 * `jsonb` payload of the `upsert_movie`/`upsert_series` RPCs (`p_tmdb`).
 *
 * Keys are exactly the ones the RPCs read with `->>`. The series-only keys stay at their `null`
 * default for movies (and vice versa); `encodeDefaults = false` omits them, and the RPCs resolve a
 * missing key to `null` anyway.
 */
@Serializable
internal data class TmdbCatalogPayload(
    @SerialName("tmdb_id") val tmdbId: Long,
    val title: String,
    @SerialName("release_year") val releaseYear: Int?,
    val overview: String?,
    @SerialName("poster_path") val posterPath: String?,
    @SerialName("main_genre") val mainGenre: String,
    val subgenres: List<String>,
    @SerialName("runtime_minutes") val runtimeMinutes: Int? = null,
    @SerialName("number_of_seasons") val numberOfSeasons: Int? = null,
    @SerialName("number_of_episodes") val numberOfEpisodes: Int? = null,
    @SerialName("episode_run_time_minutes") val episodeRunTimeMinutes: Int? = null,
)

/** Named argument of the `upsert_movie` RPC. */
@Serializable
internal data class UpsertMovieParams(@SerialName("p_tmdb") val tmdb: TmdbCatalogPayload)

/** Named argument of the `upsert_series` RPC. */
@Serializable
internal data class UpsertSeriesParams(@SerialName("p_tmdb") val tmdb: TmdbCatalogPayload)

/** Splits the TMDb genre list into `main_genre` + `subgenres`, dropping blanks. */
private fun mainGenreOf(genres: List<TmdbGenre>): Pair<String, List<String>> {
    val names = genres.map { it.name }.filter { it.isNotBlank() }
    return (names.firstOrNull() ?: DEFAULT_GENRE) to names.drop(1)
}

/**
 * Builds the catalog payload of a movie.
 *
 * `runtime` may be `null` for unreleased titles, which the RPC stores as `null` (the UI then shows no
 * duration).
 */
internal fun TmdbMovieDetails.toCatalogPayload(): TmdbCatalogPayload {
    val (mainGenre, subgenres) = mainGenreOf(genres)
    return TmdbCatalogPayload(
        tmdbId = id.toLong(),
        title = title.orEmpty(),
        releaseYear = releaseYearOf(releaseDate),
        overview = overview?.takeIf { it.isNotBlank() },
        posterPath = posterPath,
        mainGenre = mainGenre,
        subgenres = subgenres,
        runtimeMinutes = runtime,
    )
}

/**
 * Builds the catalog payload of a series.
 *
 * TMDb gives a list of typical episode lengths; the schema stores a single number, so the mean is
 * rounded — the same rule as the reference `findOrCreateSeries`.
 */
internal fun TmdbTvDetails.toCatalogPayload(): TmdbCatalogPayload {
    val (mainGenre, subgenres) = mainGenreOf(genres)
    return TmdbCatalogPayload(
        tmdbId = id.toLong(),
        title = name.orEmpty(),
        releaseYear = releaseYearOf(firstAirDate),
        overview = overview?.takeIf { it.isNotBlank() },
        posterPath = posterPath,
        mainGenre = mainGenre,
        subgenres = subgenres,
        numberOfSeasons = numberOfSeasons,
        numberOfEpisodes = numberOfEpisodes,
        episodeRunTimeMinutes = episodeRunTime
            .takeIf { it.isNotEmpty() }
            ?.let { lengths -> (lengths.sum().toDouble() / lengths.size).roundToInt() },
    )
}
