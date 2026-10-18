package app.moviematch.presentation.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Spacing, radius and component-size tokens.
 *
 * Using named tokens instead of raw `dp` values keeps every screen consistent and makes a future
 * redesign a one-file change. The scale mirrors `src/theme/index.ts` of the reference app.
 */
object Dimens {

    // --- Spacing scale (4 / 8 / 12 / 16 / 24) ------------------------------------------
    val Spacing4: Dp = 4.dp
    val Spacing8: Dp = 8.dp
    val Spacing12: Dp = 12.dp
    val Spacing16: Dp = 16.dp
    val Spacing24: Dp = 24.dp

    /** Outer margin / gutter used by the bento grid. */
    val ScreenPadding: Dp = Spacing16

    // --- Corner radius -----------------------------------------------------------------
    /** Micro radius (skeleton blocks, inner rows). */
    val RadiusSmall: Dp = 8.dp
    val RadiusTile: Dp = 18.dp
    val RadiusCard: Dp = 28.dp

    /** Floating bottom bar radius. */
    val RadiusBottomBar: Dp = 42.dp

    /** Job of the details hero container — same curvature as the bottom bar. */
    val RadiusHero: Dp = 42.dp

    /** Fully rounded ("pill") shape: large enough to always exceed half of any component. */
    val RadiusPill: Dp = 999.dp

    // --- Component sizes ---------------------------------------------------------------
    val ButtonHeight: Dp = 52.dp
    val SearchInputHeight: Dp = 48.dp
    val IconButtonSize: Dp = 40.dp
    val AvatarSmall: Dp = 22.dp
    val AvatarMedium: Dp = 28.dp
    val AvatarLarge: Dp = 72.dp
    val IconBadgeSmall: Dp = 32.dp
    val IconBadgeMedium: Dp = 36.dp
    val IconBadgeLarge: Dp = 44.dp
    val BottomBarContentHeight: Dp = 96.dp
    val PosterWidth: Dp = 80.dp
    val PosterHeight: Dp = 104.dp

    /** Compact poster used by the search rows of the "Adicionar" screen. */
    val PosterSmallWidth: Dp = 52.dp
    val PosterSmallHeight: Dp = 76.dp

    // --- Skeleton geometry -------------------------------------------------------------
    val SkeletonLineHeight: Dp = 12.dp
    val SkeletonTitleHeight: Dp = 18.dp
}
