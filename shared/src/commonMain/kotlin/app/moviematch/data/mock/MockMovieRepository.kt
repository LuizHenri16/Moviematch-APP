package app.moviematch.data.mock

import app.moviematch.domain.MovieRepository
import app.moviematch.domain.model.ContentItem
import app.moviematch.domain.model.Group
import app.moviematch.domain.model.Profile
import app.moviematch.domain.model.ProfileName
import app.moviematch.domain.model.ProfileRef
import app.moviematch.domain.model.RatingWithProfile
import app.moviematch.domain.model.SearchResult
import app.moviematch.domain.model.WatchStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update

/**
 * In-memory [MovieRepository] used until the Supabase client is wired.
 *
 * It behaves like the real thing: mutations are persisted in a [StateFlow], so changes made from one
 * screen (rating a movie, toggling "assistido", adding a title) are visible everywhere immediately.
 * A small [SIMULATED_LATENCY_MS] keeps the loading/skeleton states honest.
 */
class MockMovieRepository(
    private val latencyMs: Long = SIMULATED_LATENCY_MS,
) : MovieRepository {

    private val itemsFlow = MutableStateFlow(MockData.items())

    /** Observable list — `MovieDetailsViewModel`/`MoviesViewModel` collect it. */
    val items: StateFlow<List<ContentItem>> = itemsFlow.asStateFlow()

    override suspend fun group(): Group = MockData.group

    override suspend fun profiles(): List<Profile> = listOf(MockData.maria, MockData.luiz)

    override suspend fun contentItems(): List<ContentItem> {
        simulateLatency()
        return itemsFlow.first()
    }

    override suspend fun contentItem(id: String): ContentItem? {
        simulateLatency()
        return itemsFlow.first().firstOrNull { it.id == id }
    }

    override suspend fun randomPending(): ContentItem? {
        val pending = itemsFlow.first().filter { it.status == WatchStatus.PENDING }
        if (pending.isEmpty()) return null
        return pending.random()
    }

    override suspend fun search(query: String): List<SearchResult> {
        simulateLatency()
        return MockData.searchCatalog(query)
    }

    override suspend fun addToWatchList(result: SearchResult): ContentItem {
        val created = ContentItem(
            id = "session-${result.id}",
            type = result.type,
            status = WatchStatus.PENDING,
            createdAt = "2026-10-18T12:00:00Z",
            suggestedBy = ProfileRef(displayName = MockData.luiz.displayName),
            title = result.title,
            year = result.year,
            duration = null,
            overview = result.overview,
            posterUrl = result.posterUrl,
            genres = listOf("Outro"),
            badgeCategory = "PENDENTE",
        )
        itemsFlow.update { current -> if (current.any { it.id == created.id }) current else current + created }
        return created
    }

    override suspend fun updateStatus(id: String, status: WatchStatus, watchedAt: String?) {
        itemsFlow.update { current ->
            current.map { item ->
                if (item.id != id) {
                    item
                } else {
                    item.copy(
                        status = status,
                        watchedAt = watchedAt,
                        badgeCategory = if (status == WatchStatus.WATCHED) {
                            item.genres.firstOrNull().orEmpty().uppercase()
                        } else {
                            "PENDENTE"
                        },
                    )
                }
            }
        }
    }

    override suspend fun saveRating(
        itemId: String,
        profile: ProfileName,
        score: Double,
        comment: String?,
        markWatched: Boolean,
    ) {
        itemsFlow.update { current ->
            current.map { item ->
                if (item.id != itemId) return@map item

                val others = item.ratings.filter { existing ->
                    ProfileName.fromDisplayName(existing.profile?.displayName) != profile
                }
                val updated = others + RatingWithProfile(
                    score = score,
                    comment = comment,
                    createdAt = "2026-10-18T12:00:00Z",
                    profile = ProfileRef(displayName = profile.displayName),
                )
                val nextStatus = if (markWatched) WatchStatus.WATCHED else item.status
                item.copy(
                    ratings = updated,
                    status = nextStatus,
                    watchedAt = if (markWatched) "2026-10-18" else item.watchedAt,
                    badgeCategory = if (nextStatus == WatchStatus.WATCHED) {
                        item.genres.firstOrNull().orEmpty().uppercase()
                    } else {
                        "PENDENTE"
                    },
                )
            }
        }
    }

    override suspend fun deleteContent(id: String) {
        itemsFlow.update { current -> current.filterNot { it.id == id } }
    }

    private suspend fun simulateLatency() {
        if (latencyMs > 0) delay(latencyMs)
    }

    private companion object {
        /** Long enough to render the skeletons, short enough to not feel broken. */
        const val SIMULATED_LATENCY_MS = 450L
    }
}
