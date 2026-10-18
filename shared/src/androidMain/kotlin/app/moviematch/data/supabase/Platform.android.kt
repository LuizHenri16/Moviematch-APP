package app.moviematch.data.supabase

import android.content.Context
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import org.koin.mp.KoinPlatform

/** `SharedPreferences` file holding the Supabase session (auth tokens + user). */
private const val SETTINGS_STORE = "app.moviematch.session"

/**
 * The session store lives in [SharedPreferencesSettings], so it survives every app restart.
 *
 * `Context` is taken from Koin, which the Android app registers in `initKoin(application)`
 * (`androidContext(application)`). That indirection is what keeps `commonMain` free of Android
 * types while still giving this function something to write with.
 */
internal actual fun createSettings(): Settings = SharedPreferencesSettings(
    appContext().getSharedPreferences(SETTINGS_STORE, Context.MODE_PRIVATE),
)

internal actual fun createHttpEngine(): HttpClientEngine = OkHttp.create()

/** The application `Context` registered by the Android entry point. */
private fun appContext(): Context = KoinPlatform.getKoin().get()
