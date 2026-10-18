package app.moviematch.presentation.login

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.moviematch.auth.AuthState
import app.moviematch.auth.rememberGoogleSignInProvider
import app.moviematch.presentation.components.Avatar
import app.moviematch.presentation.components.icons.IconChevronRight
import app.moviematch.presentation.components.icons.IconLock
import app.moviematch.presentation.components.icons.IconSize
import app.moviematch.presentation.components.icons.IconTicket
import app.moviematch.presentation.components.icons.iconSize
import app.moviematch.presentation.theme.Accent
import app.moviematch.presentation.theme.Border
import app.moviematch.presentation.theme.Danger
import app.moviematch.presentation.theme.DangerBorder
import app.moviematch.presentation.theme.DangerSurface
import app.moviematch.presentation.theme.Dimens
import app.moviematch.presentation.theme.SurfaceCard
import app.moviematch.presentation.theme.SurfaceCardElevated
import app.moviematch.presentation.theme.TextPrimary
import app.moviematch.presentation.theme.TextSecondary
import app.moviematch.presentation.theme.TextSoft
import org.koin.compose.viewmodel.koinViewModel

/** `#323538` — the disc behind the padlock badge in the reference profile card. */
private val LockBadgeSurface = Color(0xFF323538)

/** `24dp` — curvature of the reference profile card (between [Dimens.RadiusTile] and [Dimens.RadiusCard]). */
private val ProfileCardShape = RoundedCornerShape(24.dp)

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

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(
                start = Dimens.ScreenPadding,
                end = Dimens.ScreenPadding,
                top = Dimens.Spacing24 + Dimens.Spacing16,
                bottom = Dimens.Spacing24,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
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

/** Brand lockup: the ticket glyph plus the letter-spaced wordmark of the reference header. */
@Composable
private fun BrandMark() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconTicket(modifier = Modifier.iconSize(IconSize.Large), color = Accent)
        Spacer(Modifier.width(Dimens.Spacing8))
        Text(
            text = "Movie Match",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
            color = TextSoft,
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
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            color = TextPrimary,
            textAlign = TextAlign.Center,
        )
        Text(
            text = subtitle,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center,
        )
    }
}

/** Error container of the reference design system (`dangerSurface` + `dangerBorder`). */
@Composable
private fun ErrorBox(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DangerSurface)
            .border(1.dp, DangerBorder, RoundedCornerShape(16.dp))
            .padding(Dimens.Spacing16),
    ) {
        Text(
            text = message,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            color = Danger,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(ProfileCardShape)
            .background(SurfaceCard)
            .border(1.dp, Border, ProfileCardShape)
            .clickable(enabled = !signingIn, onClick = onSignIn)
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
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
            )
            Spacer(Modifier.height(Dimens.Spacing4))
            Text(
                text = "Use a conta que já está na roda.",
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = TextSecondary,
            )
        }

        Spacer(Modifier.width(Dimens.Spacing16))

        Box(
            modifier = Modifier
                .size(Dimens.IconButtonSize)
                .clip(CircleShape)
                .background(SurfaceCardElevated, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (signingIn) {
                CircularProgressIndicator(
                    modifier = Modifier.size(Dimens.AvatarSmall),
                    color = Accent,
                    strokeWidth = 2.dp,
                )
            } else {
                IconChevronRight(modifier = Modifier.iconSize(IconSize.Default), color = TextSecondary)
            }
        }
    }
}

/** 20dp padlock disc pinned to the avatar, with a hairline matching the card it sits on. */
@Composable
private fun BoxScope.LockBadge(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(20.dp)
            .clip(CircleShape)
            .background(LockBadgeSurface, CircleShape)
            .border(2.dp, SurfaceCard, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        IconLock(modifier = Modifier.size(10.dp), color = TextPrimary)
    }
}
