package app.moviematch.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.moviematch.domain.HomeStats
import app.moviematch.domain.MovieRepository
import app.moviematch.domain.homeStats
import app.moviematch.domain.model.ContentItem
import app.moviematch.presentation.common.UiState
import app.moviematch.presentation.common.map
import app.moviematch.presentation.common.uiStateOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Outcome of the "Sortear Filme" action.
 *
 * It is modelled as a three-way result instead of a nullable title because an empty queue and a
 * still-running draw would both be `null`, and the screen must react differently to each.
 */
sealed interface RandomPick {
    data object Idle : RandomPick

    /** A title was drawn; the screen opens its details and calls [HomeViewModel.consumePick]. */
    data class Picked(val item: ContentItem) : RandomPick

    /** Every title is already watched — the screen shows the "fila vazia" notice. */
    data object EmptyQueue : RandomPick
}

/**
 * Drives the Home bento grid: loads the watch list, derives [HomeStats] and exposes the "Sortear
 * Filme" action.
 */
class HomeViewModel(
    private val repository: MovieRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<HomeStats>>(UiState.Loading)
    val state: StateFlow<UiState<HomeStats>> = _state.asStateFlow()

    private val _pickResult = MutableStateFlow<RandomPick>(RandomPick.Idle)
    val pickResult: StateFlow<RandomPick> = _pickResult.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            _state.value = uiStateOf("Não foi possível carregar sua lista.") {
                repository.contentItems()
            }.map(::homeStats)
        }
    }

    /** Draws one title from the "para assistir" queue. */
    fun pickRandom() {
        viewModelScope.launch {
            val item = repository.randomPending()
            _pickResult.value = if (item == null) RandomPick.EmptyQueue else RandomPick.Picked(item)
        }
    }

    /** Resets the draw result once the screen has reacted to it. */
    fun consumePick() {
        _pickResult.value = RandomPick.Idle
    }
}
