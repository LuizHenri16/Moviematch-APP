package app.moviematch.data.supabase

import com.russhwolf.settings.Settings
import io.ktor.client.engine.HttpClientEngine

/**
 * Platform pieces the Supabase client needs, declared as `expect`/`actual` so `commonMain` stays
 * free of Android/iOS types.
 */

/**
 * Key/value store used by the Supabase session manager (see
 * [io.github.jan.supabase.auth.SettingsSessionManager] usage in [createAppSupabaseClient]).
 *
 * * Android -> `SharedPreferences` (the `Context` is the one the app registers with Koin).
 * * iOS     -> `NSUserDefaults`.
 *
 * On Android the context comes from Koin, which is why the platform entry point must start Koin
 * with `androidContext(application)` (see `initKoin(application)` in `Koin.android.kt`).
 */
internal expect fun createSettings(): Settings

/**
 * Ktor engine backing every Supabase HTTP request.
 *
 * * Android -> OkHttp (`ktor-client-okhttp`).
 * * iOS     -> Darwin (`ktor-client-darwin`).
 *
 * Declared explicitly instead of relying on Ktor's classpath discovery so the engine of each
 * platform is a compile-time decision.
 */
internal expect fun createHttpEngine(): HttpClientEngine
