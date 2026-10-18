package app.moviematch.presentation.movies

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.moviematch.domain.MovieRepository
import app.moviematch.domain.contentToMovie
import app.moviematch.domain.model.ContentItem
import app.moviematch.domain.model.ContentType
import app.moviematch.domain.model.MovieSummary
import app.moviematch.domain.model.WatchStatus
import app.moviematch.presentation.common.UiState
import app.moviematch.presentation.common.map
import app.moviematch.presentation.common.uiStateOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Filter chips shown above the list. */
enum class MoviesFilter { ALL, WATCHLIST, WATCHED }

/** A list row: the raw item plus its card projection. */
data class MovieEntry(
    val item: ContentItem,
    val summary: MovieSummary,
    val type: ContentType,
)

/**
 * Drives the "Filmes e Séries" catalogue: loads the list, owns the search query/filter chips and the
 * status toggle used by the checkmark on each card.
 */
class MoviesViewModel(
    private val repository: MovieRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<List<MovieEntry>>>(UiState.Loading)
    val state: StateFlow<UiState<List<MovieEntry>>> = _state.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _filter = MutableStateFlow(MoviesFilter.ALL)
    val filter: StateFlow<MoviesFilter> = _filter.asStateFlow()

    /** The rows that must be visible, recomputed whenever the data, the query or the chip changes. */
    val visible: StateFlow<List<MovieEntry>> =
        combine(_state, _query, _filter) { state, query, filter ->
            val entries = when (state) {
                is UiState.Content -> state.data
                else -> emptyList()
            }
            entries.filter { entry -> entry.matches(filter, query) }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            _state.value = uiStateOf("Não foi possível carregar sua lista.") {
                repository.contentItems()
            }.map { items ->
                items.map { item ->
                    MovieEntry(item = item, summary = contentToMovie(item), type = item.type)
                }
            }
        }
    }

    fun onQueryChange(value: String) {
        _query.value = value
    }

    fun onFilterChange(value: MoviesFilter) {
        _filter.value = value
    }

    /** Flips a title between "para assistir" and "assistido". */
    fun toggleWatched(entry: MovieEntry) {
        val next = if (entry.item.status == WatchStatus.WATCHED) WatchStatus.PENDING else WatchStatus.WATCHED
        viewModelScope.launch {
            repository.updateStatus(
                id = entry.item.id,
                status = next,
                watchedAt = if (next == WatchStatus.WATCHED) TODAY else null,
            )
            load()
        }
    }

    private companion object {
        /**
         * Date the session is stamped with when marked as watched. A real clock arrives with the
         * backend (`kotlinx-datetime` is already on the classpath).
         */
        const val TODAY = "2026-10-18"
    }
}

private fun MovieEntry.matches(filter: MoviesFilter, query: String): Boolean {
    val matchesFilter = when (filter) {
        MoviesFilter.ALL -> true
        MoviesFilter.WATCHED -> summary.isWatched
        MoviesFilter.WATCHLIST -> !summary.isWatched
    }
    if (!matchesFilter) return false

    val term = query.trim().lowercase()
    if (term.isEmpty()) return true
    return summary.title.lowercase().contains(term) ||
        summary.year?.toString()?.contains(term) == true ||
        summary.genres.any { it.lowercase().contains(term) }
}
