package app.moviematch.presentation.addmovie

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.moviematch.domain.MovieRepository
import app.moviematch.domain.model.ContentType
import app.moviematch.domain.model.SearchResult
import app.moviematch.presentation.common.UiState
import app.moviematch.presentation.common.uiStateOf
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Drives the "Adicionar" tab: a debounced search against the TMDb catalogue (through
 * `MovieRepository.search`) and the "add to watch list" mutation, mirroring the reference screen's
 * `requestId` de-duplication with a simple cancellable [Job].
 */
class AddMovieViewModel(
    private val repository: MovieRepository,
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _results = MutableStateFlow<UiState<List<SearchResult>>>(UiState.Content(emptyList()))
    val results: StateFlow<UiState<List<SearchResult>>> = _results.asStateFlow()

    /** `true` while the debounce/request is in flight — drives the spinner inside the search field. */
    private val _searching = MutableStateFlow(false)
    val searching: StateFlow<Boolean> = _searching.asStateFlow()

    /** Id (`"movie-123"`) of the row currently being added, so only that button shows a spinner. */
    private val _addingId = MutableStateFlow<String?>(null)
    val addingId: StateFlow<String?> = _addingId.asStateFlow()

    /** Set when the last mutation succeeded; the screen consumes it to switch back to the list. */
    private val _addedTitle = MutableStateFlow<String?>(null)
    val addedTitle: StateFlow<String?> = _addedTitle.asStateFlow()

    /** Set when the last mutation failed; rendered inside an error box above the results. */
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var searchJob: Job? = null

    fun onQueryChange(value: String) {
        _query.value = value
        searchJob?.cancel()

        if (value.isBlank()) {
            _results.value = UiState.Content(emptyList())
            _searching.value = false
            return
        }

        searchJob = viewModelScope.launch {
            _searching.value = true
            delay(DEBOUNCE_MS)
            _results.value = uiStateOf("Não foi possível buscar títulos agora.") {
                repository.search(value)
            }
            _searching.value = false
        }
    }

    fun add(result: SearchResult) {
        if (_addingId.value != null) return
        _addingId.value = keyOf(result)
        _error.value = null
        viewModelScope.launch {
            when (val outcome = uiStateOf("Não foi possível adicionar o título.") {
                repository.addToWatchList(result)
            }) {
                is UiState.Content -> {
                    _addedTitle.value = outcome.data.title
                    _query.value = ""
                    _results.value = UiState.Content(emptyList())
                }

                is UiState.Error -> _error.value = outcome.message
                UiState.Loading -> Unit
            }
            _addingId.value = null
        }
    }

    fun consumeAddedTitle() {
        _addedTitle.value = null
    }

    /** Stable key for a search hit, matching the reference `"${media_type}-${id}"` scheme. */
    fun keyOf(result: SearchResult): String =
        "${if (result.type == ContentType.TV) "tv" else "movie"}-${result.id}"

    private companion object {
        const val DEBOUNCE_MS = 400L
    }
}
