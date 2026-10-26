package app.moviematch.data.mock

import app.moviematch.domain.formatRuntime
import app.moviematch.domain.model.ContentItem
import app.moviematch.domain.model.ContentType
import app.moviematch.domain.model.Group
import app.moviematch.domain.model.Profile
import app.moviematch.domain.model.ProfileRef
import app.moviematch.domain.model.RatingWithProfile
import app.moviematch.domain.model.SearchResult
import app.moviematch.domain.model.WatchStatus

/**
 * Example data backing [MockMovieRepository], the offline fallback used when the `.env` has no
 * Supabase credentials (with a configured backend the Koin `dataModule` binds
 * [app.moviematch.data.supabase.SupabaseMovieRepository] instead).
 *
 * The numbers were chosen so every bento card has something meaningful to show: the harmony index
 * sits at 60%, the critics' average differs by 1.4 points and four titles are still queued.
 *
 * Poster URLs are intentionally `null`: the seed must render fully offline, and every poster surface
 * falls back to a deterministic placeholder. The real backend fills the same field with a full
 * `https://image.tmdb.org/t/p/...` URL coming from the TMDb `poster_path`.
 */
object MockData {

    const val GROUP_ID = "grupo-roda"

    val maria = Profile(id = "profile-maria", displayName = "Maria", createdAt = "2025-11-02T10:00:00Z")
    val luiz = Profile(id = "profile-luiz", displayName = "Luiz", createdAt = "2025-11-02T10:05:00Z")

    val group = Group(
        id = GROUP_ID,
        name = "A Roda",
        members = listOf(maria, luiz),
        createdAt = "2025-11-02T10:00:00Z",
    )

    /** Stable ids so navigation/deep links stay reproducible between launches. */
    fun items(): List<ContentItem> = listOf(
        movie(
            id = "session-duna-2",
            title = "Duna: Parte Dois",
            year = 2024,
            runtimeMinutes = 166,
            mainGenre = "Ficção Científica",
            subgenres = listOf("Aventura"),
            overview = "Paul Atreides se une aos Fremen para lutar contra a Casa Harkonnen enquanto " +
                "tenta evitar o futuro terrível que só ele pode prever.",
            status = WatchStatus.WATCHED,
            watchedAt = "2026-02-14",
            suggestedBy = "Luiz",
            ratings = listOf(rating("Maria", 9.5, "Melhor sessão do ano, a fotografia é absurda."), rating("Luiz", 9.0, null)),
        ),
        movie(
            id = "session-pobres-criaturas",
            title = "Pobres Criaturas",
            year = 2023,
            runtimeMinutes = 141,
            mainGenre = "Fantasia",
            subgenres = listOf("Drama", "Comédia"),
            overview = "A fantástica evolução de Bella Baxter, uma jovem trazida de volta à vida por um " +
                "cientista pouco ortodoxo.",
            status = WatchStatus.WATCHED,
            watchedAt = "2026-01-20",
            suggestedBy = "Maria",
            ratings = listOf(rating("Maria", 8.0, "Visualmente deslumbrante."), rating("Luiz", 6.0, "Cansativo no terceiro ato.")),
        ),
        movie(
            id = "session-oppenheimer",
            title = "Oppenheimer",
            year = 2023,
            runtimeMinutes = 180,
            mainGenre = "Drama",
            subgenres = listOf("História"),
            overview = "A história do físico teórico J. Robert Oppenheimer e seu papel no desenvolvimento da " +
                "primeira bomba atômica.",
            status = WatchStatus.WATCHED,
            watchedAt = "2025-12-05",
            suggestedBy = "Luiz",
            ratings = listOf(rating("Maria", 9.0, null), rating("Luiz", 8.5, "Três horas que passam voando.")),
        ),
        movie(
            id = "session-a-substancia",
            title = "A Substância",
            year = 2024,
            runtimeMinutes = 141,
            mainGenre = "Terror",
            subgenres = listOf("Ficção Científica"),
            overview = "Uma celebridade em declínio decide usar uma droga de mercado negro que gera uma versão " +
                "mais jovem de si mesma.",
            status = WatchStatus.WATCHED,
            watchedAt = "2026-03-01",
            suggestedBy = "Maria",
            ratings = listOf(rating("Maria", 7.5, "Corajoso."), rating("Luiz", 4.0, "Exagerou na reta final.")),
        ),
        series(
            id = "session-ruptura",
            title = "Ruptura",
            year = 2022,
            seasons = 2,
            episodes = 19,
            mainGenre = "Drama",
            subgenres = listOf("Mistério", "Ficção Científica"),
            overview = "Funcionários de uma empresa de biotecnologia passam por um procedimento que separa " +
                "suas memórias do trabalho das memórias pessoais.",
            status = WatchStatus.WATCHED,
            watchedAt = "2026-03-22",
            suggestedBy = "Luiz",
            ratings = listOf(rating("Maria", 10.0, "Sem defeitos."), rating("Luiz", 9.5, null)),
        ),
        series(
            id = "session-fallout",
            title = "Fallout",
            year = 2024,
            seasons = 1,
            episodes = 8,
            mainGenre = "Ficção Científica",
            subgenres = listOf("Ação", "Aventura"),
            overview = "Duzentos anos depois do apocalipse, uma moradora de um abrigo subterrâneo é forçada a " +
                "voltar para a superfície devastada.",
            status = WatchStatus.PENDING,
            suggestedBy = "Maria",
        ),
        movie(
            id = "session-anora",
            title = "Anora",
            year = 2024,
            runtimeMinutes = 139,
            mainGenre = "Comédia",
            subgenres = listOf("Drama", "Romance"),
            overview = "Uma stripper de Brighton Beach conhece o filho de um oligarca russo e se casa por " +
                "impulso, até que a família dele descobre.",
            status = WatchStatus.PENDING,
            suggestedBy = "Luiz",
        ),
        movie(
            id = "session-nosferatu",
            title = "Nosferatu",
            year = 2024,
            runtimeMinutes = 132,
            mainGenre = "Terror",
            subgenres = listOf("Fantasia"),
            overview = "Uma história gótica de obsessão entre uma jovem atormentada e um vampiro milenar.",
            status = WatchStatus.PENDING,
            suggestedBy = "Maria",
        ),
        movie(
            id = "session-flow",
            title = "Flow",
            year = 2024,
            runtimeMinutes = 85,
            mainGenre = "Animação",
            subgenres = listOf("Aventura"),
            overview = "Um gato solitário precisa sobreviver a uma inundação que engoliu o mundo, em um barco " +
                "compartilhado com outras espécies.",
            status = WatchStatus.PENDING,
            suggestedBy = "Luiz",
        ),
    )

    /** Fake TMDb catalogue queried by the "Adicionar" screen, keyed by a substring match. */
    private val catalog = listOf(
        SearchResult(
            id = 693134,
            type = ContentType.MOVIE,
            title = "Duna: Parte Dois",
            year = 2024,
            overview = "Paul Atreides se une aos Fremen para uma guerra de vingança contra os conspiradores.",
            voteAverage = 8.2,
        ),
        SearchResult(
            id = 872585,
            type = ContentType.MOVIE,
            title = "Oppenheimer",
            year = 2023,
            overview = "A história do cientista que liderou o Projeto Manhattan.",
            voteAverage = 8.1,
        ),
        SearchResult(
            id = 1029575,
            type = ContentType.MOVIE,
            title = "Ainda Estou Aqui",
            year = 2024,
            overview = "Rio de Janeiro, 1971: a vida de uma família é interrompida por uma ação do regime militar.",
            voteAverage = 8.0,
        ),
        SearchResult(
            id = 157336,
            type = ContentType.MOVIE,
            title = "Interestelar",
            year = 2014,
            overview = "Exploradores viajam por um buraco de minhoca em busca de um novo lar para a humanidade.",
            voteAverage = 8.4,
        ),
        SearchResult(
            id = 95396,
            type = ContentType.TV,
            title = "Ruptura",
            year = 2022,
            overview = "Funcionários separam cirurgicamente suas memórias de trabalho e pessoais.",
            voteAverage = 8.7,
        ),
        SearchResult(
            id = 1396,
            type = ContentType.TV,
            title = "Breaking Bad",
            year = 2008,
            overview = "Um professor de química se torna fabricante de metanfetamina após um diagnóstico fatal.",
            voteAverage = 8.9,
        ),
        SearchResult(
            id = 94605,
            type = ContentType.TV,
            title = "Arcane",
            year = 2021,
            overview = "Duas irmãs em lados opostos de uma guerra entre cidades rivais.",
            voteAverage = 8.7,
        ),
    )

    /**
     * Offline stand-in for the TMDb lookup used by [MockMovieRepository.search]: a blank query
     * returns nothing and the rest is a case-insensitive substring match, the same contract as the
     * real client.
     */
    fun searchCatalog(query: String): List<SearchResult> {
        val term = query.trim().lowercase()
        if (term.isEmpty()) return emptyList()
        return catalog.filter { it.title.lowercase().contains(term) }
    }

    // --- Builders ------------------------------------------------------------------------

    private fun rating(profile: String, score: Double, comment: String?): RatingWithProfile =
        RatingWithProfile(
            score = score,
            comment = comment,
            createdAt = "2026-01-01T20:00:00Z",
            profile = ProfileRef(displayName = profile),
        )

    private fun movie(
        id: String,
        title: String,
        year: Int,
        runtimeMinutes: Int,
        mainGenre: String,
        subgenres: List<String>,
        overview: String,
        status: WatchStatus,
        suggestedBy: String,
        watchedAt: String? = null,
        ratings: List<RatingWithProfile> = emptyList(),
    ): ContentItem = ContentItem(
        id = id,
        type = ContentType.MOVIE,
        status = status,
        watchedAt = watchedAt,
        streamingProvider = null,
        sofaComment = null,
        createdAt = "2025-12-01T19:00:00Z",
        suggestedBy = ProfileRef(displayName = suggestedBy),
        title = title,
        year = year,
        duration = formatRuntime(runtimeMinutes),
        overview = overview,
        posterUrl = null,
        runtimeMinutes = runtimeMinutes,
        genres = (listOf(mainGenre) + subgenres).filter { it.isNotBlank() },
        badgeCategory = if (status == WatchStatus.WATCHED) mainGenre.uppercase() else "PENDENTE",
        ratings = ratings,
    )

    private fun series(
        id: String,
        title: String,
        year: Int,
        seasons: Int,
        episodes: Int,
        mainGenre: String,
        subgenres: List<String>,
        overview: String,
        status: WatchStatus,
        suggestedBy: String,
        watchedAt: String? = null,
        ratings: List<RatingWithProfile> = emptyList(),
    ): ContentItem = ContentItem(
        id = id,
        type = ContentType.TV,
        status = status,
        watchedAt = watchedAt,
        streamingProvider = null,
        sofaComment = null,
        createdAt = "2025-12-01T19:00:00Z",
        suggestedBy = ProfileRef(displayName = suggestedBy),
        title = title,
        year = year,
        duration = null,
        overview = overview,
        posterUrl = null,
        runtimeMinutes = null,
        seasons = seasons,
        episodes = episodes,
        genres = (listOf(mainGenre) + subgenres).filter { it.isNotBlank() },
        badgeCategory = if (status == WatchStatus.WATCHED) mainGenre.uppercase() else "PENDENTE",
        ratings = ratings,
    )
}
