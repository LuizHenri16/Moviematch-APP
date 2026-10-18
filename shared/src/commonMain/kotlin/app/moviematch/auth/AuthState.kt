package app.moviematch.auth

import app.moviematch.domain.model.Profile

/**
 * Authentication state of the device, exposed by [AuthStore].
 *
 * `SignedOut` and `Error` both mean "no session": screens keep rendering the seeded profile, the
 * only difference is that `Error` carries a message to show next to the sign-in button.
 */
sealed interface AuthState {

    /** No session (fresh install, after signing out, or Supabase not configured). */
    data object SignedOut : AuthState

    /** The native sign-in UI is open, or the token is being exchanged for a session. */
    data object SigningIn : AuthState

    /**
     * A Supabase session is active.
     *
     * @param profile the member as stored in `public.profiles` (falls back to the ID token claims
     *   when the row cannot be read).
     * @param email the account e-mail, when the provider exposes one.
     */
    data class SignedIn(val profile: Profile, val email: String?) : AuthState

    /** Something went wrong; [message] is ready to be shown to the user (pt-BR). */
    data class Error(val message: String) : AuthState
}
