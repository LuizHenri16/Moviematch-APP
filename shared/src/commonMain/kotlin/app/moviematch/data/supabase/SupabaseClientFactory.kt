package app.moviematch.data.supabase

import app.moviematch.config.Secrets
import com.russhwolf.settings.Settings
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.SettingsCodeVerifierCache
import io.github.jan.supabase.auth.SettingsSessionManager
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest

/**
 * Key the session is stored under inside the platform [Settings] store.
 *
 * The Supabase SDK defaults to a URL derived key; a fixed one keeps the Android
 * `SharedPreferences` file readable and makes the store independent of the project URL.
 */
private const val SESSION_KEY = "moviematch.session"

/**
 * Key the PKCE code verifier is stored under inside the platform [Settings] store.
 *
 * Passed explicitly so the PKCE cache reuses the store injected into [createAppSupabaseClient]
 * instead of letting the SDK build its own default one.
 */
private const val CODE_VERIFIER_KEY = "moviematch.code_verifier"

/**
 * Builds the Supabase client used by the whole app.
 *
 * Installs only the two plugins the current scope needs:
 * * [Auth] with a [SettingsSessionManager], so the session survives an app restart on every
 *   platform (Android `SharedPreferences`, iOS `NSUserDefaults`).
 * * [Postgrest], used to read the signed-in member from `public.profiles`.
 *
 * The HTTP engine is provided by the platform, see [createHttpEngine].
 *
 * @param settings store handed to [SettingsSessionManager]. Kept as a parameter (instead of being
 *   resolved internally) so tests can pass an in-memory implementation.
 */
internal fun createAppSupabaseClient(settings: Settings): SupabaseClient = createSupabaseClient(
    supabaseUrl = Secrets.SUPABASE_URL,
    supabaseKey = Secrets.SUPABASE_ANON_KEY,
) {
    httpEngine = createHttpEngine()

    install(Auth) {
        sessionManager = SettingsSessionManager(
            settings = settings,
            key = SESSION_KEY,
        )
        codeVerifierCache = SettingsCodeVerifierCache(
            settings = settings,
            key = CODE_VERIFIER_KEY,
        )
    }

    install(Postgrest)
}
