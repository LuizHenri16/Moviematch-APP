package app.moviematch.presentation.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.moviematch.domain.MovieRepository
import app.moviematch.domain.model.ContentItem
import app.moviematch.domain.model.Profile
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

/** Outcome of saving a rating, so the UI can show success/error feedback instead of swallowing it. */
sealed interface SaveRatingResult {
    data object Idle : SaveRatingResult
    data object Saving : SaveRatingResult
    data object Saved : SaveRatingResult
    data class Failed(val message: String) : SaveRatingResult
}

/**
 * Detail screen of one title: loads it by id, saves/edits both ratings and handles "reassistir"
 * and deletion.
 *
 * The id comes from [ContentSelectionStore] instead of a navigation argument — see that class for
 * the rationale. The member whose rating is edited is identified by [profileId] (the `profiles.id`
 * from [CurrentProfileStore]), never by a display-name substring — this is what keeps ratings
 * attached to the right person when real Google names are used.
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

    private val _saveRatingResult = MutableStateFlow<SaveRatingResult>(SaveRatingResult.Idle)
    val saveRatingResult: StateFlow<SaveRatingResult> = _saveRatingResult.asStateFlow()

    /** The profile of the person using the device (name + id). */
    private val _currentProfile = MutableStateFlow<Profile?>(null)
    val currentProfile: StateFlow<Profile?> = _currentProfile.asStateFlow()

    /** The id of the profile whose rating the dialog edits. */
    private val _profileId = MutableStateFlow<String?>(null)
    val profileId: StateFlow<String?> = _profileId.asStateFlow()

    init {
        viewModelScope.launch {
            currentProfileStore.profile.collect { profile ->
                _currentProfile.value = profile
                _profileId.value = profile.id
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
     *
     * Any failure is surfaced through [saveRatingResult] so the UI can show an error message
     * instead of silently swallowing it. The dialog is only closed after a successful save.
     */
    fun saveRating(
        profileId: String,
        score: Double,
        comment: String?,
        watchedTogether: Boolean,
    ) {
        val item = (_state.value as? UiState.Content)?.data ?: return
        val pid = this._profileId.value
        if (pid.isNullOrBlank()) {
            _saveRatingResult.value = SaveRatingResult.Failed("Perfil não identificado.")
            return
        }
        _saveRatingResult.value = SaveRatingResult.Saving
        viewModelScope.launch {
            try {
                repository.saveRating(
                    itemId = item.id,
                    profileId = pid,
                    score = score,
                    comment = comment?.takeIf { it.isNotBlank() },
                    markWatched = watchedTogether,
                )
                load(item.id)
                _saveRatingResult.value = SaveRatingResult.Saved
            } catch (e: Exception) {
                _saveRatingResult.value = SaveRatingResult.Failed(
                    e.message?.takeIf { it.isNotBlank() } ?: "Não foi possível salvar a avaliação.",
                )
            } finally {
                _ratingDialogOpen.value = false
            }
        }
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

    fun consumeSaveRatingResult() {
        _saveRatingResult.value = SaveRatingResult.Idle
    }
}
