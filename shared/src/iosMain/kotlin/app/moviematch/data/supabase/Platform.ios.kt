package app.moviematch.data.supabase

import com.russhwolf.settings.NSUserDefaultsSettings
import com.russhwolf.settings.Settings
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin
import platform.Foundation.NSUserDefaults

/**
 * iOS session store: `NSUserDefaults` — no context needed, unlike Android, so Koin plays no part
 * here.
 */
internal actual fun createSettings(): Settings =
    NSUserDefaultsSettings(NSUserDefaults.standardUserDefaults)

internal actual fun createHttpEngine(): HttpClientEngine = Darwin.create()
