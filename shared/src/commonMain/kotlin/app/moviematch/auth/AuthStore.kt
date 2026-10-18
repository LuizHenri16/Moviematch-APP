package app.moviematch.auth

import app.moviematch.config.Secrets
import app.moviematch.data.supabase.profileById
import app.moviematch.domain.model.Profile
import app.moviematch.presentation.common.CurrentProfileStore
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.auth.user.UserSession
import io.github.jan.supabase.exceptions.HttpRequestException
import io.github.jan.supabase.exceptions.RestException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/** Shown when `Secrets.WEB_CLIENT_ID` is empty; also the guard used before opening the native UI. */
internal const val MISSING_GOOGLE_CLIENT_ID_MESSAGE =
    "Login com Google indisponível: WEB_CLIENT_ID não configurado."

/** Shown when the `.env` has no Supabase credentials at all. */
private const val MISSING_SUPABASE_MESSAGE =
    "Supabase não configurado: preencha SUPABASE_URL e SUPABASE_ANON_KEY no .env."

private const val SIGN_IN_FAILED_MESSAGE = "Não foi possível entrar. Tente novamente."
private const val SESSION_EXPIRED_MESSAGE = "Sua sessão expirou. Entre de novo para continuar."
private const val SIGN_OUT_FAILED_MESSAGE =
    "Você saiu deste aparelho, mas o servidor não confirmou o logout."

/**
 * Owns the Supabase session of the app.
 *
 * It is the only place that knows about `auth.users`:
 * * [signInWithGoogle] exchanges a Google ID token for a Supabase session.
 * * A collector on `auth.sessionStatus` mirrors the session into [AuthState] and, while signed in,
 *   replaces [CurrentProfileStore] with the real `public.profiles` row (falling back to the ID
 *   token claims when that row cannot be read).
 * * [signOut] clears the session and puts the seeded profile back, so no screen has to deal with
 *   a "no profile" situation.
 *
 * The store is an application wide singleton, hence its own [CoroutineScope]: it outlives every
 * screen and is never cancelled.
 *
 * @param supabaseClient `null` when `.env` has no Supabase credentials. Every entry point then
 *   moves to [AuthState.Error] with an explicit message instead of crashing.
 */
class AuthStore(
    private val supabaseClient: SupabaseClient?,
    private val currentProfile: CurrentProfileStore,
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _state = MutableStateFlow<AuthState>(
        if (supabaseClient == null) AuthState.Error(MISSING_SUPABASE_MESSAGE) else AuthState.SignedOut,
    )
    val state: StateFlow<AuthState> = _state.asStateFlow()

    init {
        if (supabaseClient != null) {
            scope.launch { observeSession() }
        }
    }

    /**
     * Runs the platform "Sign in with Google" UI and turns the resulting ID token into a session.
     *
     * The nonce is generated and hashed by the platform implementation: Google receives the SHA-256
     * hash, Supabase receives the raw value (it hashes it itself before comparing).
     *
     * @param googleSignIn provider returned by [rememberGoogleSignInProvider].
     */
    fun signInWithGoogle(googleSignIn: GoogleSignInProvider) {
        val client = supabaseClient
        if (client == null) {
            _state.value = AuthState.Error(MISSING_SUPABASE_MESSAGE)
            return
        }
        if (!Secrets.hasGoogleSignInConfig) {
            _state.value = AuthState.Error(MISSING_GOOGLE_CLIENT_ID_MESSAGE)
            return
        }
        if (_state.value == AuthState.SigningIn) return

        _state.value = AuthState.SigningIn
        scope.launch {
            try {
                val token = googleSignIn.requestIdToken()
                client.auth.signInWith(IDToken) {
                    idToken = token.idToken
                    provider = Google
                    nonce = token.rawNonce
                }
                // `signInWith` imports the session, and the collector below reacts to it.
            } catch (e: GoogleSignInException) {
                _state.value = AuthState.Error(e.message ?: SIGN_IN_FAILED_MESSAGE)
            } catch (e: RestException) {
                _state.value = AuthState.Error(
                    "O Supabase recusou a entrada (HTTP ${e.statusCode}): ${e.error}",
                )
            } catch (e: HttpRequestException) {
                _state.value = AuthState.Error(
                    "Não foi possível falar com o Supabase. Verifique sua conexão.",
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = AuthState.Error(SIGN_IN_FAILED_MESSAGE)
            }
        }
    }

    /**
     * Ends the session. Supabase clears the stored session locally even if the server call fails,
     * so the app always ends up signed out; the collector restores the seeded profile.
     */
    fun signOut() {
        val client = supabaseClient ?: return
        scope.launch {
            try {
                client.auth.signOut()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = AuthState.Error(SIGN_OUT_FAILED_MESSAGE)
            }
        }
    }

    /** Mirrors `auth.sessionStatus` into [AuthState] and [CurrentProfileStore]. */
    private suspend fun observeSession() {
        supabaseClient ?: return
        supabaseClient.auth.sessionStatus.collect { status ->
            when (status) {
                SessionStatus.Initializing -> Unit

                is SessionStatus.Authenticated -> onAuthenticated(status.session)

                is SessionStatus.NotAuthenticated -> {
                    currentProfile.reset()
                    _state.value = AuthState.SignedOut
                }

                is SessionStatus.RefreshFailure -> _state.value = AuthState.Error(SESSION_EXPIRED_MESSAGE)
            }
        }
    }

    private suspend fun onAuthenticated(session: UserSession) {
        // `UserSession.user` is nullable: a session restored without its user payload keeps the
        // profile the app already had instead of blanking it out.
        val user = session.user
        val profile = user?.let { loadProfile(it) } ?: currentProfile.profile.value
        currentProfile.set(profile)
        _state.value = AuthState.SignedIn(profile = profile, email = user?.email)
    }

    /**
     * Reads `public.profiles` for the signed-in user.
     *
     * When the row is absent (or the read is rejected while the RLS policies are still being
     * deployed) the ID token claims are used instead: the person stays signed in and the screens
     * keep rendering a name and an avatar.
     */
    private suspend fun loadProfile(user: UserInfo): Profile {
        val stored = try {
            supabaseClient?.profileById(user.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
        return stored ?: user.toFallbackProfile()
    }
}

/**
 * Builds a [Profile] out of the OIDC claims, mirroring `public.handle_new_user()` from
 * `0001_init.sql` so the fallback looks exactly like what the database trigger would have written.
 */
private fun UserInfo.toFallbackProfile(): Profile = Profile(
    id = id,
    displayName = userMetadata?.string("full_name")
        ?: userMetadata?.string("name")
        ?: email?.substringBefore('@')
        ?: "Sem nome",
    avatarUrl = userMetadata?.string("avatar_url") ?: userMetadata?.string("picture"),
    createdAt = createdAt?.toString().orEmpty(),
)

/** `null` for a missing key, a JSON null or a non-primitive value. */
private fun JsonObject.string(key: String): String? =
    (this[key] as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }
