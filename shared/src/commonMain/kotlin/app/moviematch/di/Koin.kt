package app.moviematch.di

import app.moviematch.auth.AuthStore
import app.moviematch.config.Secrets
import app.moviematch.data.mock.MockData
import app.moviematch.data.mock.MockMovieRepository
import app.moviematch.data.supabase.createAppSupabaseClient
import app.moviematch.data.supabase.createSettings
import app.moviematch.domain.MovieRepository
import app.moviematch.presentation.addmovie.AddMovieViewModel
import app.moviematch.presentation.common.ContentSelectionStore
import app.moviematch.presentation.common.CurrentProfileStore
import app.moviematch.presentation.details.MovieDetailsViewModel
import app.moviematch.presentation.home.HomeViewModel
import app.moviematch.presentation.movies.MoviesViewModel
import app.moviematch.presentation.settings.SettingsViewModel
import io.github.jan.supabase.SupabaseClient
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * Infrastructure layer.
 *
 * The Supabase client is wired only for auth (`AuthStore` + `public.profiles`), so [MovieRepository]
 * is still bound to the in-memory [MockMovieRepository]. Swapping it for the real implementation is
 * a one-line change here — no screen or `ViewModel` knows which one it is talking to.
 */
val dataModule = module {
    single<MovieRepository> { MockMovieRepository() }
}

/**
 * Supabase session infrastructure.
 *
 * When [Secrets.hasSupabaseConfig] is `false` (clean clone, CI without `.env`) the client is not
 * registered at all and [AuthStore] starts in an explicit error state: the app keeps running with
 * the seeded profile instead of crashing on a blank URL.
 */
val supabaseModule = module {
    if (Secrets.hasSupabaseConfig) {
        single<SupabaseClient> { createAppSupabaseClient(settings = createSettings()) }
    }

    single { AuthStore(supabaseClient = getOrNull<SupabaseClient>(), currentProfile = get()) }
}

/** Application services that are not tied to a single screen. */
val appModule = module {
    /** Which member is using the device; seeded with Maria until a session is started. */
    single { CurrentProfileStore(initial = MockData.maria) }

    /** Stand-in for the navigation arguments of the details screen. */
    single { ContentSelectionStore() }
}

/** Screen level `ViewModel`s. */
val viewModelModule = module {
    viewModel { HomeViewModel(get()) }
    viewModel { MoviesViewModel(get()) }
    viewModel { AddMovieViewModel(get()) }
    viewModel { MovieDetailsViewModel(get(), get(), get()) }
    viewModel { SettingsViewModel(get(), get(), get()) }
}

/** Everything the app needs, in start order. Shared by the two platform entry points. */
val appModules: List<Module> = listOf(
    dataModule,
    supabaseModule,
    appModule,
    viewModelModule,
)

/**
 * Starts Koin exactly once, from each platform entry point.
 *
 * Android uses the overload in `Koin.android.kt` instead, because it also has to register the
 * `Context` that `createSettings()` resolves; iOS calls this one from `MovieMatchApp.init`.
 */
fun initKoin() {
    startKoin {
        modules(appModules)
    }
}
