package app.moviematch.presentation.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import app.moviematch.presentation.components.icons.IconFilm
import app.moviematch.presentation.components.icons.IconSize
import app.moviematch.presentation.components.icons.iconSize
import app.moviematch.presentation.theme.Border
import app.moviematch.presentation.theme.Dimens
import app.moviematch.presentation.theme.TextOnAccent
import app.moviematch.presentation.theme.TextPrimary
import app.moviematch.presentation.theme.White

/**
 * Floating bottom navigation.
 *
 * Same vocabulary as the reference `BottomBar`: a dark pill floating above the content with the
 * active destination filled in white. The reference app draws it with a native `BlurView` plus a
 * translucent tint; the multiplatform equivalent is Haze ([hazeEffect]), which blurs the content
 * layer on every supported target (Android and iOS). When the platform cannot blur, the effect
 * falls back to the opaque [BarFallbackTint] so the bar never becomes a see-through pane. A soft
 * drop shadow ([BarElevation]) keeps the pill floating.
 *
 * Home is intentionally highlighted on **every** route: the reference `BottomBar` always renders
 * the Home pill with `activeTabButton`, so when `Adicionar` or `Filmes` is selected both Home and
 * the pressed tab appear white. That is the reference behaviour, not a bug. The other destinations
 * (`Adicionar`, `Filmes`) stay active only on their own route, and `Perfil` is never active.
 *
 * @param hazeState Backdrop state shared with the content layer, which marks itself as the blur
 *   source (`Modifier.hazeSource`). See `AppShell`.
 */
@Composable
fun BottomBar(
    activeRoute: String,
    hazeState: HazeState,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(Dimens.RadiusBottomBar)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = Dimens.ScreenPadding, vertical = Dimens.Spacing12)
            .shadow(
                elevation = BarElevation,
                shape = shape,
                clip = false,
                ambientColor = BarShadowColor,
                spotColor = BarShadowColor,
            )
            .clip(shape)
            .hazeEffect(state = hazeState) {
                blurRadius = BarBlurRadius
                noiseFactor = 0f
                backgroundColor = BarScrim
                tints = listOf(HazeTint(BarTint))
                fallbackTint = HazeTint(BarFallbackTint)
            }
            .border(1.dp, Border, shape)
            .padding(horizontal = Dimens.Spacing12, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing12),
    ) {
        // Always active by design: the reference app keeps Home highlighted on every route.
        HomeTab(onClick = { onNavigate(Routes.HOME) })

        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconTab(
                active = activeRoute == Routes.ADD,
                onClick = { onNavigate(Routes.ADD) },
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Adicionar",
                    tint = if (activeRoute == Routes.ADD) TextOnAccent else TextPrimary,
                    modifier = Modifier.iconSize(IconSize.Large),
                )
            }

            IconTab(
                active = activeRoute == Routes.MOVIES,
                onClick = { onNavigate(Routes.MOVIES) },
            ) {
                IconFilm(
                    modifier = Modifier.iconSize(IconSize.Large),
                    color = if (activeRoute == Routes.MOVIES) TextOnAccent else TextPrimary,
                )
            }

            // Profile is a pushed destination, never a bar tab, so it is never active.
            IconTab(
                active = false,
                onClick = { onNavigate(Routes.SETTINGS) },
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Perfil",
                    tint = TextPrimary,
                    modifier = Modifier.iconSize(IconSize.Large),
                )
            }
        }
    }
}

@Composable
private fun RowScope.HomeTab(onClick: () -> Unit) {
    val shape = RoundedCornerShape(Dimens.RadiusPill)
    Row(
        modifier = Modifier
            .weight(1f)
            .height(52.dp)
            .clip(shape)
            .background(White, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = Dimens.Spacing16),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing8),
    ) {
        Icon(
            imageVector = Icons.Default.Home,
            contentDescription = null,
            tint = TextOnAccent,
            modifier = Modifier.iconSize(IconSize.Large),
        )
        Text(
            text = "Home",
            style = MaterialTheme.typography.titleSmall,
            color = TextOnAccent,
        )
    }
}

@Composable
private fun RowScope.IconTab(
    active: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(Dimens.RadiusTile)
    Box(
        modifier = Modifier
            .size(Dimens.IconBadgeMedium)
            .clip(shape)
            .background(if (active) White else Color.Transparent, shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

/** Elevation of the floating bar — mirrors `elevation 10 / shadowRadius 12` of the reference. */
private val BarElevation = 10.dp

/** Shadow colour (`rgba(0, 0, 0, 0.28)`), matching the reference `shadowOpacity 0.28`. */
private val BarShadowColor = Color.Black.copy(alpha = 0.28f)

/** Backdrop blur radius — roughly the intensity of the reference `BlurView`. */
private val BarBlurRadius = 24.dp

/** Scrim drawn behind the blurred backdrop — `rgba(30, 30, 30, 0.55)` of the reference container. */
private val BarScrim = Color(0x8C1E1E1E)

/** Dark tint composited over the blurred backdrop — `rgba(35, 35, 35, 0.68)` of the reference. */
private val BarTint = Color(0xAD232323)

/**
 * Opaque tint used when the platform cannot render the blur, so the bar never turns into a
 * see-through pane (`#21262D` — the solid fill the bar used before the blur was wired up).
 */
private val BarFallbackTint = Color(0xE621262D)
