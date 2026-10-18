package app.moviematch.presentation.login

import androidx.lifecycle.ViewModel
import app.moviematch.auth.AuthState
import app.moviematch.auth.AuthStore
import app.moviematch.auth.GoogleSignInProvider
import kotlinx.coroutines.flow.StateFlow

/**
 * Drives the login screen: a single Google sign-in entry point.
 *
 * The session itself belongs to [AuthStore]; this `ViewModel` only forwards the state the screen
 * renders, so the screen never talks to Supabase directly. There is deliberately nothing else here:
 * the screen is the door of the app and nothing more (the profile photo lives in Settings).
 */
class LoginViewModel(
    private val authStore: AuthStore,
) : ViewModel() {

    /** Session state, forwarded from [AuthStore] so the screen only talks to its `ViewModel`. */
    val authState: StateFlow<AuthState> = authStore.state

    /** Starts the native Google flow; the provider comes from `rememberGoogleSignInProvider()`. */
    fun signInWithGoogle(googleSignIn: GoogleSignInProvider) {
        authStore.signInWithGoogle(googleSignIn)
    }
}
