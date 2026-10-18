package app.moviematch.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import app.moviematch.presentation.components.icons.IconFilm
import app.moviematch.presentation.components.icons.IconSize
import app.moviematch.presentation.components.icons.iconSize
import app.moviematch.presentation.theme.Dimens
import app.moviematch.presentation.theme.SurfaceDeep
import app.moviematch.presentation.theme.TextMuted
import app.moviematch.presentation.theme.TextPrimary
import app.moviematch.presentation.theme.avatarColor
import app.moviematch.presentation.theme.avatarInitial
import coil3.compose.AsyncImage

/**
 * Round profile bubble: the downloaded avatar image when the profile has one, otherwise the coloured
 * initial (Maria in lavender, Luiz in sage). Mirrors `Avatar` from the reference design system.
 *
 * The initial is always painted *underneath* the image, so it doubles as the loading placeholder and
 * as the fallback: a link that resolves simply covers it, while a broken link (or no connection)
 * leaves the name on screen instead of an empty hole.
 */
@Composable
fun Avatar(
    name: String?,
    modifier: Modifier = Modifier,
    url: String? = null,
    size: Dp = Dimens.AvatarMedium,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(avatarColor(name), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = avatarInitial(name),
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * 0.42f).sp,
        )

        if (url != null) {
            var failed by remember(url) { mutableStateOf(false) }
            if (!failed) {
                AsyncImage(
                    model = url,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    onError = { failed = true },
                )
            }
        }
    }
}

/**
 * Poster thumbnail.
 *
 * Real TMDb paths arrive from the backend later; until then (and whenever a title has no artwork)
 * this renders a deterministic placeholder with the film glyph so the layout never collapses.
 */
@Composable
fun PosterImage(
    url: String?,
    modifier: Modifier = Modifier,
    containerColor: Color = SurfaceDeep,
) {
    val shape = RoundedCornerShape(Dimens.RadiusTile)
    Box(
        modifier = modifier.clip(shape),
        contentAlignment = Alignment.Center,
    ) {
        if (url != null) {
            AsyncImage(
                model = url,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                placeholder = null,
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(containerColor, shape),
                contentAlignment = Alignment.Center,
            ) {
                IconFilm(modifier = Modifier.iconSize(IconSize.Default), color = TextMuted)
            }
        }
    }
}
