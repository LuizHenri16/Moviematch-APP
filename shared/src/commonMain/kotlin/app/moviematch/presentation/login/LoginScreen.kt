package app.moviematch.presentation.login

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.moviematch.auth.AuthState
import app.moviematch.auth.rememberGoogleSignInProvider
import app.moviematch.presentation.components.Avatar
import app.moviematch.presentation.components.IconBadge
import app.moviematch.presentation.components.icons.IconChevronRight
import app.moviematch.presentation.components.icons.IconLock
import app.moviematch.presentation.components.icons.IconSize
import app.moviematch.presentation.components.icons.IconTicket
import app.moviematch.presentation.components.icons.iconSize
import app.moviematch.presentation.theme.Dimens
import org.koin.compose.viewmodel.koinViewModel

/**
 * Login screen: the door of the app.
 *
 * Ported from the reference `ProfilePicker`, with one deliberate difference: the reference offered a
 * card per member backed by a fake password login, while here the person who enters *is* the owner of
 * the Google account. So the screen shows a single card, which runs the real Google flow.
 *
 * Showing this screen or the app shell is [app.moviematch.App]'s decision, not this screen's: it
 * renders the session state it is given and nothing else.
 */
@Composable
fun LoginScreen(
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = koinViewModel(),
) {
    val authState by viewModel.authState.collectAsStateWithLifecycle()

    // Activity-backed on Android (Credential Manager requirement), a documented stub on iOS.
    val googleSignIn = rememberGoogleSignInProvider()

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(
                    start = Dimens.ScreenPadding,
                    end = Dimens.ScreenPadding,
                    top = Dimens.Spacing24 + Dimens.Spacing16,
                    bottom = Dimens.Spacing24,
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            BrandMark()

            Spacer(Modifier.height(Dimens.Spacing24 + Dimens.Spacing16))

            HeaderText(
                title = "Quem está entrando?",
                subtitle = "Entre com a sua conta Google para abrir a lista da roda.\n" +
                    "Só vocês dois têm acesso, então cada um entra na sua :)\n" +
                    "sem senha, direto pelo Google.",
            )

            Spacer(Modifier.height(Dimens.Spacing24 + Dimens.Spacing12))

            val error = (authState as? AuthState.Error)?.message
            if (error != null) {
                ErrorBox(error)
                Spacer(Modifier.height(Dimens.Spacing24))
            }

            SignInCard(
                signingIn = authState == AuthState.SigningIn,
                onSignIn = { viewModel.signInWithGoogle(googleSignIn) },
            )
        }
    }
}

/** Brand lockup: the ticket glyph plus the wordmark of the reference header. */
@Composable
private fun BrandMark() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconTicket(
            modifier = Modifier.iconSize(IconSize.Large),
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.width(Dimens.Spacing8))
        Text(
            text = "MovieMatch",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** Centred title + subtitle pair, mirroring the reference header block. */
@Composable
private fun HeaderText(title: String, subtitle: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Dimens.Spacing16),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/** Error container of the design system (`errorContainer` surface + `error` border). */
@Composable
private fun ErrorBox(message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        border = BorderStroke(
            width = Dimens.BorderWidth,
            color = MaterialTheme.colorScheme.error,
        ),
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.Spacing16),
        )
    }
}

/**
 * The one card of the screen, shaped exactly like the reference profile rows: avatar with a padlock
 * badge, label, and a disc that holds either the chevron or the in-flight spinner.
 *
 * The avatar shows the "G" of the Google account because, unlike the reference, there is no fixed
 * pair of members to choose from — whoever owns the account is the one entering.
 */
@Composable
private fun SignInCard(
    signingIn: Boolean,
    onSignIn: () -> Unit,
) {
    Card(
        onClick = onSignIn,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { role = Role.Button },
        enabled = !signingIn,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = Dimens.ElevationCard),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.Spacing24),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box {
                Avatar(name = "Google", size = Dimens.AvatarLarge)
                LockBadge(modifier = Modifier.align(Alignment.BottomEnd))
            }

            Spacer(Modifier.width(Dimens.Spacing24))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (signingIn) "Entrando…" else "Entrar com Google",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(Dimens.Spacing4))
                Text(
                    text = "Use a conta que já está na roda.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.width(Dimens.Spacing16))

            IconBadge(
                size = Dimens.IconBadgeMedium,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                if (signingIn) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                    )
                } else {
                    IconChevronRight(
                        modifier = Modifier.iconSize(IconSize.Default),
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
        }
    }
}

/** 20dp padlock pinned to the avatar, in the `secondaryContainer` role. */
@Composable
private fun BoxScope.LockBadge(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.size(20.dp),
        shape = MaterialTheme.shapes.extraSmall,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        border = BorderStroke(
            width = Dimens.BorderWidth,
            color = MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Box(contentAlignment = Alignment.Center) {
            IconLock(
                modifier = Modifier.size(10.dp),
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}
