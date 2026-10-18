package app.moviematch.auth

import android.content.Context
import android.util.Base64
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialInterruptedException
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import androidx.credentials.exceptions.GetCredentialUnsupportedException
import androidx.credentials.exceptions.NoCredentialException
import app.moviematch.config.Secrets
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import java.security.MessageDigest
import java.security.SecureRandom

/** How many random bytes the raw nonce is built from (32 bytes -> 43 Base64 chars). */
private const val NONCE_BYTES = 32

private const val HEX_DIGITS = "0123456789abcdef"

private const val NO_ACCOUNT_MESSAGE = "Nenhuma conta Google disponível neste aparelho."
private const val CANCELLED_MESSAGE = "Login cancelado."
private const val INTERRUPTED_MESSAGE = "O login foi interrompido. Tente de novo."
private const val UNSUPPORTED_MESSAGE = "Este aparelho não consegue concluir o login com Google."
private const val UNEXPECTED_CREDENTIAL_MESSAGE =
    "O Google devolveu um tipo de credencial inesperado. Tente de novo."
private const val PARSING_MESSAGE = "O Google devolveu um token inválido. Tente de novo."
private const val FAILED_MESSAGE = "Não foi possível entrar com o Google."

/**
 * Android implementation of the native Google Sign-In flow (Credential Manager).
 *
 * `LocalContext.current` is used on purpose: Credential Manager refuses an application context
 * ("failed to launch the selector UI: ensure the context parameter is an Activity-based context").
 * Inside `MainActivity.setContent { App() }` that `Context` *is* the Activity, and the provider is
 * re-created whenever the Activity changes.
 */
@Composable
actual fun rememberGoogleSignInProvider(): GoogleSignInProvider {
    val context = LocalContext.current
    return remember(context) { AndroidGoogleSignInProvider(context) }
}

/**
 * Mirrors the Supabase "Sign in with Google on Android" guide: one [GetGoogleIdOption] request,
 * a SHA-256 hashed nonce handed to Google, and the raw nonce kept for Supabase.
 */
private class AndroidGoogleSignInProvider(
    private val context: Context,
    private val credentialManager: CredentialManager = CredentialManager.create(context),
) : GoogleSignInProvider {

    override suspend fun requestIdToken(): GoogleIdToken {
        if (!Secrets.hasGoogleSignInConfig) {
            // Never touch Credential Manager without a server client id: the request would fail
            // deep inside Play Services with a much harder to read error.
            throw GoogleSignInException(MISSING_GOOGLE_CLIENT_ID_MESSAGE)
        }

        val rawNonce = generateRawNonce()
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(Secrets.WEB_CLIENT_ID)
            .setNonce(sha256Hex(rawNonce))
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val credential = try {
            credentialManager.getCredential(request = request, context = context).credential
        } catch (e: GetCredentialException) {
            throw GoogleSignInException(e.toUserMessage(), e)
        }

        if (credential !is CustomCredential ||
            credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            throw GoogleSignInException(UNEXPECTED_CREDENTIAL_MESSAGE)
        }

        val idToken = try {
            GoogleIdTokenCredential.createFrom(credential.data).idToken
        } catch (e: GoogleIdTokenParsingException) {
            throw GoogleSignInException(PARSING_MESSAGE, e)
        }

        return GoogleIdToken(idToken = idToken, rawNonce = rawNonce)
    }
}

/** Turns the Credential Manager exceptions into something the person can act on. */
private fun GetCredentialException.toUserMessage(): String = when (this) {
    is GetCredentialCancellationException -> CANCELLED_MESSAGE
    is NoCredentialException -> NO_ACCOUNT_MESSAGE
    is GetCredentialInterruptedException -> INTERRUPTED_MESSAGE
    is GetCredentialProviderConfigurationException -> UNSUPPORTED_MESSAGE
    is GetCredentialUnsupportedException -> UNSUPPORTED_MESSAGE
    else -> message?.let { "$FAILED_MESSAGE ($it)" } ?: FAILED_MESSAGE
}

/**
 * Random nonce, Base64 (URL safe, unpadded) like Google's own sample.
 *
 * Uses `android.util.Base64` on purpose: `java.util.Base64` only exists from API 26, and this
 * project targets `minSdk = 24` without core library desugaring, so it would crash with
 * `NoClassDefFoundError` on Android 7.0/7.1. The flags reproduce exactly the
 * `getUrlEncoder().withoutPadding()` output: URL-safe alphabet, no line breaks, no `=`.
 *
 * Only the SHA-256 hash goes to Google; Supabase hashes the raw value itself before comparing it
 * with the `nonce` claim of the ID token.
 */
private fun generateRawNonce(): String {
    val bytes = ByteArray(NONCE_BYTES).also { SecureRandom().nextBytes(it) }
    return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
}

/** Lowercase hexadecimal SHA-256 digest, the representation Google expects for a nonce. */
private fun sha256Hex(value: String): String =
    MessageDigest.getInstance("SHA-256").digest(value.encodeToByteArray()).toHex()

private fun ByteArray.toHex(): String = buildString(size * 2) {
    for (byte in this@toHex) {
        val value = byte.toInt() and 0xFF
        append(HEX_DIGITS[value shr 4])
        append(HEX_DIGITS[value and 0x0F])
    }
}
