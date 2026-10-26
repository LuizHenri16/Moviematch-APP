package app.moviematch.di

import app.moviematch.auth.AuthStore
import app.moviematch.config.Secrets
import app.moviematch.data.mock.MockData
import app.moviematch.data.mock.MockMovieRepository
import app.moviematch.data.supabase.GroupRepository
import app.moviematch.data.supabase.ProfileRepository
import app.moviematch.data.supabase.SupabaseMovieRepository
import app.moviematch.data.supabase.TmdbClient
import app.moviematch.data.supabase.createAppSupabaseClient
import app.moviematch.data.supabase.createHttpEngine
import app.moviematch.data.supabase.createSettings
import app.moviematch.data.supabase.createTmdbHttpClient
import app.moviematch.domain.GroupDataSource
import app.moviematch.domain.MovieRepository
import app.moviematch.presentation.addmovie.AddMovieViewModel
import app.moviematch.presentation.common.ContentSelectionStore
import app.moviematch.presentation.common.CurrentProfileStore
import app.moviematch.presentation.details.MovieDetailsViewModel
import app.moviematch.presentation.group.GroupStore
import app.moviematch.presentation.group.GroupViewModel
import app.moviematch.presentation.home.HomeViewModel
import app.moviematch.presentation.login.LoginViewModel
import app.moviematch.presentation.movies.MoviesViewModel
import app.moviematch.presentation.settings.SettingsViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * Infrastructure layer.
 *
 * [MovieRepository] is bound to the real [SupabaseMovieRepository] as soon as the `.env` carries
 * Supabase credentials, and to the in-memory [MockMovieRepository] otherwise (clean clone, CI without
 * secrets). The choice is made here only — no screen or `ViewModel` knows which implementation it is
 * talking to, which is the reason the contract lives in `domain/`.
 *
 * [TmdbClient] is the catalogue HTTP client of the "Adicionar" tab. It is registered unconditionally
 * but only instantiated when the real repository is, since the offline path never asks for it.
 */
val dataModule = module {
    single {
        TmdbClient(
            httpClient = createTmdbHttpClient(createHttpEngine()),
            apiKey = Secrets.TMDB_API_KEY,
        )
    }

    single<MovieRepository> {
        if (Secrets.hasSupabaseConfig) {
            val client = get<SupabaseClient>()
            SupabaseMovieRepository(
                supabaseClient = client,
                tmdb = get(),
                currentUserId = { client.auth.currentUserOrNull()?.id },
            )
        } else {
            MockMovieRepository()
        }
    }

    /**
     * Group creation/invite flow. It is bound through [GroupDataSource] so the screens depend on the
     * contract (and can be tested with a fake), and it is only registered when Supabase is
     * configured. `GroupStore` resolves it with `getOrNull()` and turns the absence into a typed
     * error instead of a `NoDefinitionFoundException` when Settings/Group are opened offline.
     */
    if (Secrets.hasSupabaseConfig) {
        single<GroupDataSource> { GroupRepository(supabaseClient = get()) }
    }
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

    /**
     * The single `public.profiles` write path, used by the avatar form of the settings screen.
     * Resolves to a `null` client when Supabase is not configured, which makes it keep the avatar
     * in memory only.
     */
    single { ProfileRepository(supabaseClient = getOrNull<SupabaseClient>(), currentProfile = get()) }
}

/** Application services that are not tied to a single screen. */
val appModule = module {
    /** Which member is using the device; seeded with Maria until a session is started. */
    single { CurrentProfileStore(initial = MockData.maria) }

    /** Stand-in for the navigation arguments of the details screen. */
    single { ContentSelectionStore() }

    /**
     * Single source of truth of the active "roda", shared by Settings and the group screen.
     * `getOrNull()` keeps the graph resolvable when Supabase is not configured.
     */
    single {
        GroupStore(
            repository = getOrNull<GroupDataSource>(),
            currentProfile = get(),
            authStore = get(),
        )
    }
}

/** Screen level `ViewModel`s. */
val viewModelModule = module {
    viewModel { HomeViewModel(get(), get()) }
    viewModel { MoviesViewModel(get()) }
    viewModel { AddMovieViewModel(get()) }
    viewModel { MovieDetailsViewModel(get(), get(), get()) }
    viewModel { SettingsViewModel(get(), get(), get(), get(), get()) }
    viewModel { LoginViewModel(get()) }
    viewModel { GroupViewModel(get(), get(), get()) }
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
