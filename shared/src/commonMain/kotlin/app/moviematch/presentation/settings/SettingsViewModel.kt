package app.moviematch.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.moviematch.auth.AuthState
import app.moviematch.auth.AuthStore
import app.moviematch.auth.GoogleSignInProvider
import app.moviematch.data.supabase.AVATAR_URL_FORMAT_ERROR
import app.moviematch.data.supabase.ProfileRepository
import app.moviematch.data.supabase.isAvatarUrlValid
import app.moviematch.domain.MovieRepository
import app.moviematch.domain.model.Profile
import app.moviematch.presentation.common.CurrentProfileStore
import app.moviematch.presentation.common.UiState
import app.moviematch.presentation.common.uiStateOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Settings screen: shows the signed-in profile, the Google account entry point and the avatar URL
 * form.
 *
 * The screen keeps working without a session: [CurrentProfileStore] still holds the seeded profile
 * and [authState] is the only extra thing it renders (the sign-in/sign-out button). Saving the photo
 * goes through [ProfileRepository], the single avatar write path, which persists to `public.profiles`
 * when there is a session and keeps the value in memory otherwise.
 */
class SettingsViewModel(
    private val repository: MovieRepository,
    private val currentProfile: CurrentProfileStore,
    private val authStore: AuthStore,
    private val profileRepository: ProfileRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<Profile>>(UiState.Loading)
    val state: StateFlow<UiState<Profile>> = _state.asStateFlow()

    private val _urlInput = MutableStateFlow("")
    val urlInput: StateFlow<String> = _urlInput.asStateFlow()

    private val _fieldError = MutableStateFlow<String?>(null)
    val fieldError: StateFlow<String?> = _fieldError.asStateFlow()

    private val _justSaved = MutableStateFlow(false)
    val justSaved: StateFlow<Boolean> = _justSaved.asStateFlow()

    /** `true` while the avatar URL is being written; disables the form. */
    private val _saving = MutableStateFlow(false)
    val saving: StateFlow<Boolean> = _saving.asStateFlow()

    /** Session state, forwarded from [AuthStore] so the screen only talks to its `ViewModel`. */
    val authState: StateFlow<AuthState> = authStore.state

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            _state.value = uiStateOf("Não foi possível carregar seu perfil.") {
                val current = currentProfile.profile.first()
                // [MovieRepository] is still the mock one, so a profile that came from Supabase
                // (a UUID) is simply not in it: the store is the source of truth and the repository
                // is only the offline seed.
                repository.profiles().firstOrNull { it.id == current.id } ?: current
            }.also { result ->
                val profile = (result as? UiState.Content)?.data
                _urlInput.value = profile?.avatarUrl.orEmpty()
            }
        }
    }

    /** Starts the native Google flow; the provider comes from `rememberGoogleSignInProvider()`. */
    fun signInWithGoogle(googleSignIn: GoogleSignInProvider) {
        authStore.signInWithGoogle(googleSignIn)
    }

    fun signOut() {
        authStore.signOut()
    }

    fun onUrlChange(value: String) {
        _urlInput.value = value
        _fieldError.value = null
        _justSaved.value = false
    }

    /**
     * Validates and persists the avatar URL through [ProfileRepository] (the single avatar write
     * path); empty input clears the photo.
     */
    fun save() {
        val trimmed = _urlInput.value.trim()
        if (trimmed.isNotEmpty() && !isAvatarUrlValid(trimmed)) {
            _fieldError.value = AVATAR_URL_FORMAT_ERROR
            return
        }

        viewModelScope.launch {
            _saving.value = true
            _fieldError.value = null
            _justSaved.value = false
            try {
                val updated = profileRepository.updateAvatarUrl(trimmed.ifEmpty { null })
                _state.value = UiState.Content(updated)
                _justSaved.value = true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Never swallow a write failure: the person must see why the photo did not stick.
                _fieldError.value = e.message ?: "Não foi possível salvar a foto. Tente novamente."
            } finally {
                _saving.value = false
            }
        }
    }

    /** Clears the avatar photo. */
    fun removePhoto() {
        _urlInput.value = ""
        save()
    }
}
