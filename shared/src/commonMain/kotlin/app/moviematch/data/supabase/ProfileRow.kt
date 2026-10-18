package app.moviematch.data.supabase

import app.moviematch.domain.model.Profile
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** `public.profiles`, the 1:1 mirror of `auth.users` (see `docs/native-app/supabase/0001_init.sql`). */
internal const val PROFILES_TABLE = "profiles"

/**
 * Subset of `public.profiles` the app reads.
 *
 * Only the columns the domain [Profile] needs are declared: `handle`, `updated_at` and the RLS
 * helpers stay server side.
 */
@Serializable
internal data class ProfileRow(
    val id: String,
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
)

/** Maps a `public.profiles` row onto the domain model. */
internal fun ProfileRow.toProfile(): Profile = Profile(
    id = id,
    displayName = displayName?.takeIf { it.isNotBlank() } ?: "Sem nome",
    avatarUrl = avatarUrl?.takeIf { it.isNotBlank() },
    createdAt = createdAt.orEmpty(),
)

/**
 * Reads the profile of [id] (the Supabase auth user id, `profiles.id = auth.uid()`).
 *
 * Returns `null` when the row does not exist. The RLS policy `profiles_select_self_or_pair` only
 * exposes the signed-in member and their active group partner, so a foreign id resolves to nothing.
 *
 * @throws io.github.jan.supabase.exceptions.RestException on a failed request (for example
 *   `42501` when the session is not accepted by the database).
 */
internal suspend fun SupabaseClient.profileById(id: String): Profile? = from(PROFILES_TABLE)
    .select {
        filter { eq("id", id) }
        limit(1)
    }
    .decodeSingleOrNull<ProfileRow>()
    ?.toProfile()
