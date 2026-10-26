package app.moviematch.data.supabase

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Insert payloads of the two session tables and their ratings.
 *
 * Every field is declared without a default on purpose: supabase-kt serializes with
 * `encodeDefaults = false`, so a defaulted field whose value equals the default would simply be
 * dropped from the JSON body. All of these columns must always reach PostgREST (including explicit
 * `null`s, e.g. a rating without a comment), so none of them has a default.
 */

/** Row inserted into `sessions` when a movie joins the watch list. */
@Serializable
internal data class SessionInsert(
    @SerialName("group_id") val groupId: String,
    @SerialName("movie_id") val movieId: String,
    val status: String,
    @SerialName("suggested_by") val suggestedBy: String?,
)

/** Row inserted into `series_sessions` when a series joins the watch list. */
@Serializable
internal data class SeriesSessionInsert(
    @SerialName("group_id") val groupId: String,
    @SerialName("series_id") val seriesId: String,
    val status: String,
    @SerialName("suggested_by") val suggestedBy: String?,
)

/**
 * Row upserted into `ratings`.
 *
 * `group_id` is denormalized from the session row and is required by the table: the reference
 * implementation omitted it, which would violate the `not null` constraint, so this is a deliberate
 * correction rather than a divergence in behaviour.
 */
@Serializable
internal data class RatingUpsert(
    @SerialName("session_id") val sessionId: String,
    @SerialName("group_id") val groupId: String,
    @SerialName("profile_id") val profileId: String,
    val score: Double,
    val comment: String?,
)

/** Mirror of [RatingUpsert] for `series_ratings`. */
@Serializable
internal data class SeriesRatingUpsert(
    @SerialName("series_session_id") val seriesSessionId: String,
    @SerialName("group_id") val groupId: String,
    @SerialName("profile_id") val profileId: String,
    val score: Double,
    val comment: String?,
)
