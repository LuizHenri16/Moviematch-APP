package app.moviematch.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.moviematch.domain.HomeStats
import app.moviematch.domain.MovieRepository
import app.moviematch.domain.homeStats
import app.moviematch.domain.model.ContentItem
import app.moviematch.domain.model.Group
import app.moviematch.presentation.common.UiState
import app.moviematch.presentation.common.dataOrNull
import app.moviematch.presentation.common.uiStateOf
import app.moviematch.presentation.group.GroupStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
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
    private val groupStore: GroupStore,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<HomeStats>>(UiState.Loading)
    val state: StateFlow<UiState<HomeStats>> = _state.asStateFlow()

    private val _pickResult = MutableStateFlow<RandomPick>(RandomPick.Idle)
    val pickResult: StateFlow<RandomPick> = _pickResult.asStateFlow()

    /**
     * Names of the pair in slot order, or `null` when the member is not in an active group.
     *
     * The second name is blank while the group still has a single member ("aguardando a outra
     * pessoa"). There is deliberately no seeded "Maria"/"Luiz": the critics duel is only shown when
     * the group actually exists, and it must name the real participants.
     */
    private val _memberNames = MutableStateFlow<Pair<String, String>?>(null)
    val memberNames: StateFlow<Pair<String, String>?> = _memberNames.asStateFlow()

    init {
        load()
        // The group lives in the app-scoped store, so creating/joining/leaving it (in Settings or in
        // the group screen) pushes the new pair to Home without recreating this ViewModel.
        viewModelScope.launch {
            groupStore.groupState
                .map { it.dataOrNull() }
                .map(::participantNames)
                .distinctUntilChanged()
                .collect { names ->
                    if (names != _memberNames.value) {
                        _memberNames.value = names
                        // The stats carry the per-member averages, so they depend on these names.
                        load()
                    }
                }
        }
    }

    fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            _state.value = uiStateOf("Não foi possível carregar sua lista.") {
                val names = _memberNames.value
                homeStats(
                    items = repository.contentItems(),
                    member1Name = names?.first.orEmpty(),
                    member2Name = names?.second.orEmpty(),
                )
            }
        }
    }

    /** The two active members in slot order; `null` when there is no group to show. */
    private fun participantNames(group: Group?): Pair<String, String>? {
        if (group == null || group.members.isEmpty()) return null
        return group.members.getOrNull(0)?.displayName.orEmpty() to
            group.members.getOrNull(1)?.displayName.orEmpty()
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
