package app.moviematch.auth

import androidx.compose.runtime.Composable

/**
 * A Google ID token (the OIDC JWT minted by Google) plus the nonce it was minted for.
 *
 * The token goes to Supabase as-is; the **raw** nonce (never the hashed one) is what
 * `auth.signInWith(IDToken)` expects, because Supabase hashes it again before comparing it with the
 * `nonce` claim of the token.
 */
data class GoogleIdToken(
    val idToken: String,
    val rawNonce: String,
)

/** Raised when the native "Sign in with Google" UI could not produce an ID token. */
class GoogleSignInException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)

/**
 * Bridge to the native "Sign in with Google" UI of each platform.
 *
 * Only the *token acquisition* is platform specific: exchanging it for a Supabase session happens
 * in [AuthStore], in shared code.
 */
interface GoogleSignInProvider {

    /**
     * Shows the platform sign-in UI and returns the ID token of the chosen account.
     *
     * @throws GoogleSignInException when the flow is unavailable, cancelled or returns something
     *   other than a Google ID token; [Throwable.message] is already user facing (pt-BR).
     */
    suspend fun requestIdToken(): GoogleIdToken
}

/**
 * Returns the [GoogleSignInProvider] of the running platform.
 *
 * A `@Composable` factory because the Android Credential Manager API requires an **Activity
 * scoped** `Context` to present its UI: on Android the implementation is built from
 * `LocalContext.current` (which, inside `MainActivity.setContent { App() }`, *is* the Activity).
 * An application `Context` makes the flow fail with "failed to launch the selector UI".
 *
 * iOS is not wired yet, see `GoogleSignIn.ios.kt`.
 */
@Composable
expect fun rememberGoogleSignInProvider(): GoogleSignInProvider
