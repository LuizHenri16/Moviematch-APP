package app.moviematch.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/** Shown when somebody taps "Entrar com Google" on iOS. */
private const val IOS_NOT_WIRED_MESSAGE =
    "O login com Google ainda não está ligado no iOS deste app."

/**
 * iOS stub: Google Sign-In is **not** wired yet.
 *
 * What is missing, in order:
 * 1. The [GoogleSignIn-iOS](https://github.com/google/GoogleSignIn-iOS) SDK as a Swift Package
 *    dependency of `iosApp` (there is no Kotlin Multiplatform artifact for it), plus the
 *    `CFBundleURLTypes` entry with the reversed client id in `Info.plist`.
 * 2. A C-interop binding (`expect`/`actual` over the Objective-C API) exposing
 *    `GIDSignIn.sharedInstance.signIn` and `user.idToken.tokenString`.
 * 3. A Mac: none of the above can be built or verified on Linux, which is why this actual only
 *    reports a clear error instead of pretending the flow exists.
 *
 * Everything else (session storage in `NSUserDefaults`, the Darwin HTTP engine, the Supabase
 * session handling in [AuthStore]) is already shared and works on iOS.
 */
@Composable
actual fun rememberGoogleSignInProvider(): GoogleSignInProvider = remember { IosGoogleSignInProvider }

private object IosGoogleSignInProvider : GoogleSignInProvider {

    override suspend fun requestIdToken(): GoogleIdToken = throw GoogleSignInException(
        "$IOS_NOT_WIRED_MESSAGE Falta integrar o SDK GoogleSignIn-iOS e rodar num Mac.",
    )
}
