package app.moviematch.presentation.common

/**
 * The three states every screen can be in.
 *
 * Keeping this hierarchy explicit is what allows each screen to render real skeletons while loading
 * and a retry affordance on failure, instead of a blank frame.
 */
sealed interface UiState<out T> {

    /** Data is being fetched; screens render their skeleton shape. */
    data object Loading : UiState<Nothing>

    /** Fetch failed; [message] is already user-facing Portuguese copy. */
    data class Error(val message: String) : UiState<Nothing>

    /** Data is available and can be rendered. */
    data class Content<T>(val data: T) : UiState<T>
}

/** The payload when the state is [UiState.Content], `null` otherwise. */
fun <T> UiState<T>.dataOrNull(): T? = (this as? UiState.Content<T>)?.data

/** The error message when the state is [UiState.Error], `null` otherwise. */
fun <T> UiState<T>.errorOrNull(): String? = (this as? UiState.Error)?.message

/** `true` while the first load is in flight. */
val UiState<*>.isLoading: Boolean get() = this is UiState.Loading

/** Transforms the payload of a [UiState.Content], leaving loading/error states untouched. */
fun <T, R> UiState<T>.map(transform: (T) -> R): UiState<R> = when (this) {
    is UiState.Loading -> UiState.Loading
    is UiState.Error -> this
    is UiState.Content -> UiState.Content(transform(data))
}

/**
 * Runs [block] and wraps the outcome in a [UiState], turning any throwable into user-facing copy.
 *
 * Every `ViewModel` funnels its repository calls through here so error handling is uniform.
 */
suspend fun <T> uiStateOf(
    failureMessage: String = "Não foi possível carregar. Tente novamente.",
    block: suspend () -> T,
): UiState<T> = try {
    UiState.Content(block())
} catch (cancellation: kotlinx.coroutines.CancellationException) {
    throw cancellation
} catch (error: Throwable) {
    UiState.Error(error.message?.takeIf { it.isNotBlank() } ?: failureMessage)
}
