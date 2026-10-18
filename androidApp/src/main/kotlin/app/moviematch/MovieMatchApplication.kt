package app.moviematch

import android.app.Application
import app.moviematch.di.initKoin

/**
 * Registers the process-wide `Application` so Koin is started before any UI is created.
 */
class MovieMatchApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // The Android overload also hands Koin the application `Context`, used by the Supabase
        // session store (`SharedPreferences`).
        initKoin(application = this)
    }
}
