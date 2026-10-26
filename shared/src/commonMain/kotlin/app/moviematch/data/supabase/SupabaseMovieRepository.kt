package app.moviematch.data.supabase

import app.moviematch.domain.MovieRepository
import app.moviematch.domain.model.ContentItem
import app.moviematch.domain.model.ContentType
import app.moviematch.domain.model.Group
import app.moviematch.domain.model.Profile
import app.moviematch.domain.model.ProfileName
import app.moviematch.domain.model.SearchResult
import app.moviematch.domain.model.WatchStatus
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.exception.PostgrestRestException
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.CancellationException
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock

/** `watch_status` value stored for a title that is still queued. */
private const val PENDING_STATUS = "PENDING"

/** PostgreSQL `unique_violation`, raised by `sessions`/`series_sessions` when a title is added twice. */
private const val UNIQUE_VIOLATION_CODE = "23505"

/** `ratings` unique key, used as the `on_conflict` target of the upsert. */
private const val RATING_CONFLICT = "session_id,profile_id"

/** `series_ratings` unique key, used as the `on_conflict` target of the upsert. */
private const val SERIES_RATING_CONFLICT = "series_session_id,profile_id"

/** Copy shown when the same title is added to the group twice. */
private const val ALREADY_ON_LIST_MESSAGE = "Este título já está na sua lista."

/** Copy shown when the signed-in account has no active group yet. */
private const val NO_GROUP_MESSAGE =
    "Você ainda não tem uma roda. Crie ou entre em uma para adicionar títulos."

/**
 * Supabase/Postgres implementation of [MovieRepository].
 *
 * It is the real-data counterpart of [app.moviematch.data.mock.MockMovieRepository] and mirrors the
 * queries of the reference web app (`hooks/useData.tsx` and the `screens` folder): same joins, same
 * orderings, same client-side derivations. Every behaviour that the mock established is preserved, so
 * no screen changes when this implementation replaces the mock.
 *
 * Three mapping decisions worth knowing before reading the code:
 * * **Type resolution.** The database keeps movies and series in two mirror tables and the domain id
 *   does not encode the type. Mutators that only receive an id therefore try `sessions` first and
 *   fall back to `series_sessions`; a missing id is a no-op, exactly like the mock.
 * * **Catalog writes.** `movies`/`series` have no INSERT grant and no write policy — the only door is
 *   the `upsert_movie`/`upsert_series` RPC (SECURITY DEFINER). `deleteContent` consequently removes
 *   only the group's session, never the shared catalog row (deliberate divergence from the reference,
 *   which also deleted the movie/series row).
 * * **Ratings.** `ratings`/`series_ratings` require a denormalized `group_id` that the reference app
 *   omitted; it is read from the session row here. `saveRating` upserts on
 *   `(session_id, profile_id)` / `(series_session_id, profile_id)` and only flips a pending session
 *   to watched when asked to.
 *
 * @param supabaseClient client with the Postgrest plugin installed.
 * @param tmdb TMDb client used by [search] and to fetch the details stored by the `upsert_*` RPCs.
 * @param currentUserId id of the signed-in Supabase user (the `profiles.id` used by `suggested_by`);
 *   a lambda instead of an auth lookup keeps the repository testable without the Auth plugin.
 */
internal class SupabaseMovieRepository(
    private val supabaseClient: SupabaseClient,
    private val tmdb: TmdbClient,
    private val currentUserId: () -> String?,
) : MovieRepository {

    override suspend fun group(): Group? = guarded("Não foi possível carregar o grupo.") {
        supabaseClient.from(GROUPS_TABLE)
            .select(Columns.raw(GROUP_SELECT)) { limit(1) }
            .decodeList<GroupRow>()
            .firstOrNull()
            ?.toGroup()
    }

    override suspend fun profiles(): List<Profile> = guarded("Não foi possível carregar os perfis.") {
        supabaseClient.from(PROFILES_TABLE)
            .select { order("created_at", Order.ASCENDING) }
            .decodeList<ProfileRow>()
            .map { it.toProfile() }
    }

    override suspend fun contentItems(): List<ContentItem> = guarded("Não foi possível carregar sua lista.") {
        val movies = supabaseClient.from(SESSIONS_TABLE)
            .select(Columns.raw(SESSIONS_SELECT)) { order("created_at", Order.DESCENDING) }
            .decodeList<SessionRow>()
            .map { it.toContentItem() }
        val series = supabaseClient.from(SERIES_SESSIONS_TABLE)
            .select(Columns.raw(SERIES_SESSIONS_SELECT)) { order("created_at", Order.DESCENDING) }
            .decodeList<SeriesSessionRow>()
            .map { it.toContentItem() }
        // The two collections arrive sorted individually; the merged list is re-sorted so the newest
        // title is first, whichever table it comes from (same rule as `useData`).
        (movies + series).sortedByDescending { it.createdAt }
    }

    override suspend fun contentItem(id: String): ContentItem? =
        guarded("Não foi possível carregar este título.") {
            val movie = supabaseClient.from(SESSIONS_TABLE)
                .select(Columns.raw(SESSIONS_SELECT)) {
                    filter { eq("id", id) }
                    limit(1)
                }
                .decodeSingleOrNull<SessionRow>()
            if (movie != null) return@guarded movie.toContentItem()

            supabaseClient.from(SERIES_SESSIONS_TABLE)
                .select(Columns.raw(SERIES_SESSIONS_SELECT)) {
                    filter { eq("id", id) }
                    limit(1)
                }
                .decodeSingleOrNull<SeriesSessionRow>()
                ?.toContentItem()
        }

    override suspend fun randomPending(): ContentItem? =
        contentItems().filter { it.status == WatchStatus.PENDING }.randomOrNull()

    override suspend fun search(query: String): List<SearchResult> = tmdb.search(query)

    override suspend fun addToWatchList(result: SearchResult): ContentItem =
        guarded("Não foi possível adicionar o título à sua lista.") {
            // Guiding copy instead of a crash: a signed-in member without a group must create or
            // join a roda before queueing titles (the screen shows the exception message).
            val groupId = group()?.id ?: throw MovieRepositoryException(NO_GROUP_MESSAGE)
            val userId = currentUserId()
            when (result.type) {
                ContentType.MOVIE -> {
                    val details = tmdb.movieDetails(result.id)
                    val movieId = supabaseClient.upsertCatalog("upsert_movie", UpsertMovieParams(details.toCatalogPayload()))
                    insertGuarded("Não foi possível adicionar o filme.") {
                        insertMovieSession(SessionInsert(groupId, movieId, PENDING_STATUS, userId))
                    }
                }

                ContentType.TV -> {
                    val details = tmdb.tvDetails(result.id)
                    val seriesId = supabaseClient.upsertCatalog("upsert_series", UpsertSeriesParams(details.toCatalogPayload()))
                    insertGuarded("Não foi possível adicionar a série.") {
                        insertSeriesSession(SeriesSessionInsert(groupId, seriesId, PENDING_STATUS, userId))
                    }
                }
            }
        }

    override suspend fun updateStatus(id: String, status: WatchStatus, watchedAt: String?) =
        guarded("Não foi possível atualizar o status.") {
            val session = sessionRefOf(id) ?: return@guarded
            supabaseClient.from(session.table).update(
                update = {
                    set("status", status.name)
                    if (watchedAt == null) setToNull("watched_at") else set("watched_at", watchedAt)
                },
                request = { filter { eq("id", id) } },
            )
        }

    override suspend fun saveRating(
        itemId: String,
        profile: ProfileName,
        score: Double,
        comment: String?,
        markWatched: Boolean,
    ) = guarded("Não foi possível salvar a nota.") {
        val session = sessionRefOf(itemId) ?: return@guarded
        val profileId = resolveProfileId(profile) ?: throw MovieRepositoryException(
            "Não foi possível identificar o perfil de ${profile.displayName} para salvar a nota.",
        )

        if (session.isSeries) {
            supabaseClient.from(SERIES_RATINGS_TABLE).upsert(
                SeriesRatingUpsert(itemId, session.groupId, profileId, score, comment),
            ) { onConflict = SERIES_RATING_CONFLICT }
        } else {
            supabaseClient.from(RATINGS_TABLE).upsert(
                RatingUpsert(itemId, session.groupId, profileId, score, comment),
            ) { onConflict = RATING_CONFLICT }
        }

        if (markWatched && session.status == WatchStatus.PENDING) {
            supabaseClient.from(session.table).update(
                update = {
                    set("status", WatchStatus.WATCHED.name)
                    set("watched_at", todayIso())
                },
                request = { filter { eq("id", itemId) } },
            )
        }
    }

    override suspend fun deleteContent(id: String) = guarded("Não foi possível apagar o título.") {
        val session = sessionRefOf(id) ?: return@guarded
        // Only the session row is removed; `ratings`/`series_ratings` cascade from it and the shared
        // `movies`/`series` catalog row is left untouched.
        supabaseClient.from(session.table).delete { filter { eq("id", id) } }
    }

    private suspend fun insertMovieSession(insert: SessionInsert): ContentItem {
        val row = supabaseClient.from(SESSIONS_TABLE)
            .insert(insert) { select(Columns.raw(SESSIONS_SELECT)) }
            .decodeSingle<SessionRow>()
        return row.toContentItem()
    }

    private suspend fun insertSeriesSession(insert: SeriesSessionInsert): ContentItem {
        val row = supabaseClient.from(SERIES_SESSIONS_TABLE)
            .insert(insert) { select(Columns.raw(SERIES_SESSIONS_SELECT)) }
            .decodeSingle<SeriesSessionRow>()
        return row.toContentItem()
    }

    /**
     * Locates the session behind [id] in either table, so callers can address the right table, group
     * and current status without knowing the content type. Returns `null` when the id is unknown.
     */
    private suspend fun sessionRefOf(id: String): SessionRef? {
        val movie = supabaseClient.from(SESSIONS_TABLE)
            .select(Columns.raw(SESSION_CORE_SELECT)) {
                filter { eq("id", id) }
                limit(1)
            }
            .decodeSingleOrNull<SessionCoreRow>()
        if (movie != null) {
            return SessionRef(SESSIONS_TABLE, movie.groupId, watchStatusOf(movie.status), isSeries = false)
        }

        val series = supabaseClient.from(SERIES_SESSIONS_TABLE)
            .select(Columns.raw(SESSION_CORE_SELECT)) {
                filter { eq("id", id) }
                limit(1)
            }
            .decodeSingleOrNull<SessionCoreRow>()
        return series?.let {
            SessionRef(SERIES_SESSIONS_TABLE, it.groupId, watchStatusOf(it.status), isSeries = true)
        }
    }

    /** Resolves the `profiles.id` of [profile] using the same substring match the UI uses. */
    private suspend fun resolveProfileId(profile: ProfileName): String? =
        profiles().firstOrNull { ProfileName.fromDisplayName(it.displayName) == profile }?.id

    private fun todayIso(): String = Clock.System.todayIn(TimeZone.currentSystemDefault()).toString()
}

/** Where a session lives and what the mutators need to know about it. */
private data class SessionRef(
    val table: String,
    val groupId: String,
    val status: WatchStatus,
    val isSeries: Boolean,
)

/**
 * Wraps a repository call, turning any failure into a [MovieRepositoryException] with the given
 * user-facing copy. Cancellations and already-mapped repository errors pass through untouched, so a
 * more specific message raised deeper in the call is never replaced by a generic one.
 */
private suspend fun <T> guarded(message: String, block: suspend () -> T): T = try {
    block()
} catch (e: CancellationException) {
    throw e
} catch (e: MovieRepositoryException) {
    throw e
} catch (e: Exception) {
    throw MovieRepositoryException(message, e)
}

/**
 * Variant of [guarded] for the session inserts: a `unique_violation` (the same title added twice)
 * maps to [ALREADY_ON_LIST_MESSAGE], every other failure keeps the caller's copy.
 */
private suspend fun <T> insertGuarded(message: String, block: suspend () -> T): T = try {
    block()
} catch (e: CancellationException) {
    throw e
} catch (e: PostgrestRestException) {
    if (e.code == UNIQUE_VIOLATION_CODE) throw MovieRepositoryException(ALREADY_ON_LIST_MESSAGE, e)
    throw MovieRepositoryException(message, e)
}

/** Calls the `upsert_movie`/`upsert_series` RPC and returns the catalog row UUID it resolves. */
private suspend inline fun <reified P : Any> SupabaseClient.upsertCatalog(function: String, params: P): String =
    postgrest.rpc(function, params).decodeAs<String>()

/**
 * Raised when a Supabase/TMDb call fails; the message is already user-facing Portuguese copy, so the
 * screens can render it directly. Modelled after
 * [app.moviematch.data.supabase.ProfileWriteException].
 */
internal class MovieRepositoryException(message: String, cause: Throwable? = null) : Exception(message, cause)
