package app.moviematch.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import app.moviematch.auth.AuthState
import app.moviematch.presentation.components.AppButton
import app.moviematch.presentation.components.BentoCard
import app.moviematch.presentation.components.ButtonVariant
import app.moviematch.presentation.theme.Danger
import app.moviematch.presentation.theme.Dimens
import app.moviematch.presentation.theme.TextPrimary
import app.moviematch.presentation.theme.TextSecondary

/**
 * "Conta Google" block of the settings screen: the entry point of the Supabase session.
 *
 * It is additive by design — the rest of the app keeps working with the seeded profile while nobody
 * is signed in.
 *
 * TODO(product): decide whether the screens should require a session (today they only *prefer* one),
 * and whether a signed-in person should be able to switch back to the offline profile.
 */
@Composable
internal fun GoogleAccountCard(
    state: AuthState,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
) {
    BentoCard(
        modifier = Modifier.fillMaxWidth(),
        padding = Dimens.Spacing16,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.Spacing8)) {
            Text(
                text = "Conta Google",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
            )

            when (state) {
                AuthState.SigningIn -> {
                    Caption("Abrindo o Google…")
                    AppButton(
                        label = "Entrando…",
                        onClick = {},
                        modifier = Modifier.fillMaxWidth(),
                        enabled = false,
                    )
                }

                is AuthState.SignedIn -> {
                    Caption(state.email ?: "Sessão ativa")
                    AppButton(
                        label = "Sair",
                        onClick = onSignOut,
                        modifier = Modifier.fillMaxWidth(),
                        variant = ButtonVariant.Outline,
                    )
                }

                AuthState.SignedOut, is AuthState.Error -> {
                    val error = state as? AuthState.Error
                    Caption(
                        text = error?.message ?: "Entre para manter a roda sincronizada.",
                        color = if (error != null) Danger else TextSecondary,
                    )
                    AppButton(
                        label = "Entrar com Google",
                        onClick = onSignIn,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun Caption(text: String, color: Color = TextSecondary) {
    Text(
        text = text,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        color = color,
    )
}
