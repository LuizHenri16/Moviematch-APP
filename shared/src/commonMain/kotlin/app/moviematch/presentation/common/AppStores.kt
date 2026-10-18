package app.moviematch.presentation.common

import app.moviematch.domain.model.ContentType
import app.moviematch.domain.model.Profile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Which profile is using the device.
 *
 * Seeded with one of the two members while nobody is signed in (the app must keep working offline);
 * [app.moviematch.auth.AuthStore] replaces it with the real Supabase profile once a session exists
 * and drops back to the seeded one with [reset] when the session ends.
 */
class CurrentProfileStore(private val initial: Profile) {

    private val _profile = MutableStateFlow(initial)

    val profile: StateFlow<Profile> = _profile.asStateFlow()

    fun set(profile: Profile) {
        _profile.value = profile
    }

    /** Restores the profile the app was seeded with. */
    fun reset() {
        _profile.value = initial
    }
}

/** Identifies the title a detail screen must display. */
data class ContentSelection(val id: String, val type: ContentType)

/**
 * Stand-in for navigation arguments.
 *
 * `navigation-compose` arguments are typed as `SavedState`/`Bundle` per platform, which is not
 * available from `commonMain` without platform-specific code. Until the real navigation graph
 * (with typed routes) is introduced, the details screen reads the current selection from this tiny
 * store, which is exactly as reactive as a nav argument and keeps the navigation layer *common*.
 */
class ContentSelectionStore {

    private val _selected = MutableStateFlow<ContentSelection?>(null)

    val selected: StateFlow<ContentSelection?> = _selected.asStateFlow()

    fun select(id: String, type: ContentType) {
        _selected.value = ContentSelection(id, type)
    }
}
