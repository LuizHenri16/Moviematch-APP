package app.moviematch.presentation.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.moviematch.domain.MovieRepository
import app.moviematch.domain.model.ContentItem
import app.moviematch.domain.model.ProfileName
import app.moviematch.presentation.common.ContentSelectionStore
import app.moviematch.presentation.common.CurrentProfileStore
import app.moviematch.presentation.common.UiState
import app.moviematch.presentation.common.uiStateOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Result of a destructive action, so the screen can navigate back only after it succeeded. */
sealed interface DeleteResult {
    data object Idle : DeleteResult
    data object Deleting : DeleteResult
    data object Deleted : DeleteResult
    data class Failed(val message: String) : DeleteResult
}

/**
 * Detail screen of one title: loads it by id, saves/edits both ratings and handles "reassistir"
 * (back to the queue) and deletion.
 *
 * The id comes from [ContentSelectionStore] instead of a navigation argument — see that class for
 * the rationale. The member a rating belongs to comes from [CurrentProfileStore]: the rating dialog
 * only ever edits the profile that is using the device, exactly like the reference implementation.
 */
class MovieDetailsViewModel(
    private val repository: MovieRepository,
    private val selectionStore: ContentSelectionStore,
    currentProfileStore: CurrentProfileStore,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<ContentItem>>(UiState.Loading)
    val state: StateFlow<UiState<ContentItem>> = _state.asStateFlow()

    private val _deleteResult = MutableStateFlow<DeleteResult>(DeleteResult.Idle)
    val deleteResult: StateFlow<DeleteResult> = _deleteResult.asStateFlow()

    private val _ratingDialogOpen = MutableStateFlow(false)
    val ratingDialogOpen: StateFlow<Boolean> = _ratingDialogOpen.asStateFlow()

    /** Member whose score the rating dialog edits. */
    private val _currentProfile = MutableStateFlow(
        ProfileName.fromDisplayName(currentProfileStore.profile.value.displayName) ?: ProfileName.MARIA,
    )
    val currentProfile: StateFlow<ProfileName> = _currentProfile.asStateFlow()

    init {
        viewModelScope.launch {
            currentProfileStore.profile.collect { profile ->
                _currentProfile.value =
                    ProfileName.fromDisplayName(profile.displayName) ?: ProfileName.MARIA
            }
        }
        viewModelScope.launch {
            selectionStore.selected.collect { selection ->
                if (selection != null) {
                    load(selection.id)
                }
            }
        }
    }

    fun load(id: String) {
        viewModelScope.launch {
            _state.value = UiState.Loading
            _state.value = uiStateOf("Não foi possível carregar este título.") {
                repository.contentItem(id) ?: error("Título não encontrado na sua lista.")
            }
        }
    }

    /** Re-reads the current selection (used by the retry button). */
    fun reload() {
        selectionStore.selected.value?.let { load(it.id) }
    }

    fun openRatingDialog() {
        _ratingDialogOpen.value = true
    }

    fun closeRatingDialog() {
        _ratingDialogOpen.value = false
    }

    /**
     * Creates or replaces one member's rating. When [watchedTogether] is checked and the title is
     * still pending, it is also marked as watched — the same rule as the reference implementation.
     */
    fun saveRating(
        profile: ProfileName,
        score: Double,
        comment: String?,
        watchedTogether: Boolean,
    ) {
        val item = (_state.value as? UiState.Content)?.data ?: return
        viewModelScope.launch {
            repository.saveRating(
                itemId = item.id,
                profile = profile,
                score = score,
                comment = comment?.takeIf { it.isNotBlank() },
                markWatched = watchedTogether,
            )
            load(item.id)
        }
        _ratingDialogOpen.value = false
    }

    /** Moves a watched title back to the queue. */
    fun moveBackToQueue() {
        val item = (_state.value as? UiState.Content)?.data ?: return
        viewModelScope.launch {
            repository.updateStatus(item.id, app.moviematch.domain.model.WatchStatus.PENDING, null)
            load(item.id)
        }
    }

    fun delete() {
        val item = (_state.value as? UiState.Content)?.data ?: return
        _deleteResult.value = DeleteResult.Deleting
        viewModelScope.launch {
            _deleteResult.value = uiStateOf("Não foi possível apagar o título.") {
                repository.deleteContent(item.id)
            }.let { outcome ->
                when (outcome) {
                    is UiState.Content -> DeleteResult.Deleted
                    is UiState.Error -> DeleteResult.Failed(outcome.message)
                    UiState.Loading -> DeleteResult.Idle
                }
            }
        }
    }

    fun consumeDeleteResult() {
        _deleteResult.value = DeleteResult.Idle
    }
}
