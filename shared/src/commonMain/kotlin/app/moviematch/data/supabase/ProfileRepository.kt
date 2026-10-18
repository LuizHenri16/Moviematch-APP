package app.moviematch.data.supabase

import app.moviematch.domain.model.Profile
import app.moviematch.presentation.common.CurrentProfileStore
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.CancellationException

/**
 * Copy shown when the typed link is not an absolute `http`/`https` URL.
 *
 * Lives next to the write path on purpose: the avatar form of the settings screen owns the only
 * field that takes a link, so the rejection wording is defined once.
 */
internal const val AVATAR_URL_FORMAT_ERROR = "Use um link que comece com http:// ou https://"

/** `true` when a *non-empty* avatar URL is absolute and can be requested by an image loader. */
internal fun isAvatarUrlValid(value: String): Boolean =
    value.startsWith("http://") || value.startsWith("https://")

/**
 * Writes to `public.profiles`.
 *
 * This is the single place in the app that mutates a profile row: the avatar form of the settings
 * screen goes through [updateAvatarUrl], so there is only one behaviour to reason about (and one
 * place to fix).
 *
 * The RLS policy `profiles_update_self` only allows the row whose `id = auth.uid()`, which is exactly
 * the row targeted here.
 *
 * @param supabaseClient `null` when `.env` has no Supabase credentials (clean clone, CI); see
 *   [updateAvatarUrl] for how that degrades.
 * @param currentProfile in-memory mirror of the signed-in member, kept in sync by [updateAvatarUrl].
 */
class ProfileRepository(
    private val supabaseClient: SupabaseClient?,
    private val currentProfile: CurrentProfileStore,
) {

    /**
     * Persists [avatarUrl] (`null` clears the photo) and mirrors the result in [CurrentProfileStore]
     * so every screen reacts immediately.
     *
     * When there is no session to write with — a clean clone without `.env`, or the offline mode the
     * settings screen still offers — the value is only kept in memory. That is the behaviour the
     * offline app had before Supabase existed, and it keeps the screen usable instead of failing on
     * a backend that is not there.
     *
     * @return the updated [Profile], already published to [CurrentProfileStore].
     * @throws ProfileWriteException when the server rejected the update; [Throwable.message] is
     *   already user facing (pt-BR).
     */
    suspend fun updateAvatarUrl(avatarUrl: String?): Profile {
        val userId = supabaseClient?.auth?.currentUserOrNull()?.id
        if (supabaseClient != null && userId != null) {
            try {
                supabaseClient.from(PROFILES_TABLE).update(
                    update = {
                        if (avatarUrl == null) setToNull("avatar_url") else set("avatar_url", avatarUrl)
                    },
                    request = {
                        filter { eq("id", userId) }
                    },
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                throw ProfileWriteException(
                    message = e.message?.takeIf { it.isNotBlank() }
                        ?.let { "Não foi possível salvar a foto: $it" }
                        ?: "Não foi possível salvar a foto. Tente novamente.",
                    cause = e,
                )
            }
        }

        val updated = currentProfile.profile.value.copy(avatarUrl = avatarUrl)
        currentProfile.set(updated)
        return updated
    }
}

/** Raised when `public.profiles` could not be updated; the message is ready to be shown to the person. */
class ProfileWriteException(message: String, cause: Throwable? = null) : Exception(message, cause)
