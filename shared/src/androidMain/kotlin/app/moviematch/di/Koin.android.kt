package app.moviematch.di

import android.app.Application
import app.moviematch.data.supabase.createSettings
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

/**
 * Android entry point, called by the `Application` before any UI exists.
 *
 * Same modules as the shared [initKoin], plus the application `Context`: Koin is where
 * [createSettings] picks it up from to open the `SharedPreferences` file that stores the Supabase
 * session. An application context is enough for storage — the *Activity* context Credential Manager
 * needs is taken from `LocalContext.current` inside `rememberGoogleSignInProvider`.
 */
fun initKoin(application: Application) {
    startKoin {
        androidContext(application)
        modules(appModules)
    }
}
