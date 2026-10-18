package app.moviematch.domain

import app.moviematch.domain.model.ContentItem
import app.moviematch.domain.model.Group
import app.moviematch.domain.model.Profile
import app.moviematch.domain.model.ProfileName
import app.moviematch.domain.model.SearchResult
import app.moviematch.domain.model.WatchStatus

/**
 * Data access contract consumed by the `ViewModel`s.
 *
 * The only implementation today is [app.moviematch.data.mock.MockMovieRepository]; the Supabase one
 * will be dropped in later without any screen change, which is the whole point of keeping this
 * interface free of framework types.
 */
interface MovieRepository {

    /** The two people of the group plus the group itself. */
    suspend fun group(): Group

    suspend fun profiles(): List<Profile>

    /** Every movie/series on the shared watch list, newest first. */
    suspend fun contentItems(): List<ContentItem>

    suspend fun contentItem(id: String): ContentItem?

    /** Titles marked as watched, used by "Sortear Filme" to pick from the queue. */
    suspend fun randomPending(): ContentItem?

    /** TMDb-style search used by the "Adicionar" screen. */
    suspend fun search(query: String): List<SearchResult>

    /** Adds one of the [SearchResult]s to the watch list and returns the created row. */
    suspend fun addToWatchList(result: SearchResult): ContentItem

    suspend fun updateStatus(id: String, status: WatchStatus, watchedAt: String? = null)

    /**
     * Creates or replaces the rating of [profile] for [itemId].
     * When [markWatched] is true and the title is still pending, it moves to [WatchStatus.WATCHED].
     */
    suspend fun saveRating(
        itemId: String,
        profile: ProfileName,
        score: Double,
        comment: String?,
        markWatched: Boolean,
    )

    /** Removes the title (and its ratings) from the list. */
    suspend fun deleteContent(id: String)
}
