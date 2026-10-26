package app.moviematch.data.supabase

import app.moviematch.domain.model.SearchResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json

/**
 * TMDb v3 HTTP client used by the "Adicionar" tab.
 *
 * This is the only place that talks to `api.themoviedb.org`. It mirrors `src/lib/tmdb.ts` of the
 * reference implementation: same base URL, same `language=pt-BR`, same 12s timeout and the same
 * typed error copy (with the env var name adapted to this project's `TMDB_API_KEY`).
 *
 * The client is deliberately free of supabase-kt: search and details are read-only HTTP calls, while
 * the writing of the catalogue is done by the `upsert_movie`/`upsert_series` RPCs inside
 * [SupabaseMovieRepository]. Failures are raised as [MovieRepositoryException] so the UI shows the
 * same Portuguese copy no matter which layer failed.
 *
 * @param httpClient engine-configured client (see [createTmdbHttpClient]); injected so tests can pass
 *   a Ktor `MockEngine`.
 * @param apiKey `Secrets.TMDB_API_KEY`; an empty value makes [search] and the detail calls throw
 *   [MISSING_TMDB_KEY_MESSAGE] instead of hitting a key-less endpoint.
 */
internal class TmdbClient(
    private val httpClient: HttpClient,
    private val apiKey: String,
) {

    /**
     * `GET /search/multi`, filtered to movies and series.
     *
     * An empty query returns an empty list without a request, exactly like `searchMulti` in the
     * reference.
     */
    suspend fun search(query: String): List<SearchResult> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return emptyList()
        val response = get<TmdbSearchResponse>("/search/multi", mapOf("query" to trimmed, "page" to "1"))
        return response.results.mapNotNull { it.toSearchResult() }
    }

    /** `GET /movie/{id}`; used to build the `upsert_movie` payload. */
    suspend fun movieDetails(tmdbId: Int): TmdbMovieDetails = get("/movie/$tmdbId")

    /** `GET /tv/{id}`; used to build the `upsert_series` payload. */
    suspend fun tvDetails(tmdbId: Int): TmdbTvDetails = get("/tv/$tmdbId")

    private suspend inline fun <reified T> get(path: String, params: Map<String, String> = emptyMap()): T =
        tmdbRequest(httpClient = httpClient, apiKey = apiKey, path = path, params = params)
}

/** Shown when the `.env` has no `TMDB_API_KEY`. */
internal const val MISSING_TMDB_KEY_MESSAGE =
    "Chave do TMDb não configurada. Defina TMDB_API_KEY no arquivo .env."

/** TMDb v3 REST base URL (mirrors `BASE_URL` of the reference `lib/tmdb.ts`). */
private const val TMDB_BASE_URL = "https://api.themoviedb.org/3"

/** Language of every lookup (mirrors the reference `language=pt-BR`). */
private const val TMDB_LANGUAGE = "pt-BR"

/** Search/detail lookups are interactive, so they get a tighter budget (`TMDB_TIMEOUT_MS`). */
private const val TMDB_TIMEOUT_MS = 12_000L

/**
 * Builds the [HttpClient] of [TmdbClient]: JSON content negotiation plus the 12s request timeout.
 *
 * Kept separate from the Supabase client (which the app also uses) so the two backends have their own
 * base URL, headers and timeout and neither can leak into the other.
 */
internal fun createTmdbHttpClient(engine: HttpClientEngine): HttpClient = HttpClient(engine) {
    expectSuccess = false
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true })
    }
    install(HttpTimeout) {
        requestTimeoutMillis = TMDB_TIMEOUT_MS
    }
}

/**
 * Runs one TMDb request and decodes the JSON body, translating transport and HTTP failures into
 * [MovieRepositoryException] with user-facing copy.
 */
private suspend inline fun <reified T> tmdbRequest(
    httpClient: HttpClient,
    apiKey: String,
    path: String,
    params: Map<String, String>,
): T {
    if (apiKey.isBlank()) throw MovieRepositoryException(MISSING_TMDB_KEY_MESSAGE)

    val response = try {
        httpClient.get("$TMDB_BASE_URL$path") {
            parameter("api_key", apiKey)
            parameter("language", TMDB_LANGUAGE)
            params.forEach { (key, value) -> parameter(key, value) }
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        // Anything the engine throws (DNS, refused connection, timeout) is a network problem from the
        // user's point of view, so it gets the same single message the reference shows.
        throw MovieRepositoryException("Não foi possível conectar ao TMDb. Verifique sua internet.", e)
    }

    if (!response.status.isSuccess()) throw tmdbError(response.status.value)

    return try {
        response.body<T>()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        throw MovieRepositoryException("O TMDb retornou uma resposta inválida.", e)
    }
}

/** Maps a non-2xx TMDb status to the same typed copy the reference `tmdbResponseError` uses. */
private fun tmdbError(status: Int): MovieRepositoryException = when {
    status == 401 -> MovieRepositoryException(
        "Não foi possível autenticar no TMDb. Verifique se a chave é válida.",
    )
    status == 404 -> MovieRepositoryException("Conteúdo não encontrado no TMDb.")
    status == 429 -> MovieRepositoryException(
        "Muitas consultas ao TMDb em pouco tempo. Aguarde alguns segundos e tente novamente.",
    )
    status >= 500 -> MovieRepositoryException(
        "O TMDb está indisponível no momento. Tente novamente em instantes.",
    )
    else -> MovieRepositoryException("O TMDb respondeu com um erro inesperado ($status).")
}
