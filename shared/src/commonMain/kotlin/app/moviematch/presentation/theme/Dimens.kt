package app.moviematch.presentation.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Spacing, shape, elevation and component-size tokens of the Material 3 app.
 *
 * The scales follow the Material 3 specs:
 * * **spacing** — the 4dp baseline grid (4 / 8 / 12 / 16 / 24);
 * * **shape** — MD3 corner scale: 4dp extra-small, 8dp small, 12dp medium, 16dp large, 28dp
 *   extra-large; pills stay fully rounded;
 * * **elevation** — expressed by `surfaceContainer*` tones; shadows are only used by the few
 *   floating components that overlap content (dialogs), never by content cards.
 *
 * Screens keep using named tokens instead of raw `dp` values.
 */
object Dimens {

    // --- Spacing scale (4 / 8 / 12 / 16 / 24) ------------------------------------------
    val Spacing4: Dp = 4.dp
    val Spacing8: Dp = 8.dp
    val Spacing12: Dp = 12.dp
    val Spacing16: Dp = 16.dp
    val Spacing24: Dp = 24.dp

    /** Outer margin / gutter used by every screen. */
    val ScreenPadding: Dp = Spacing16

    // --- Corner radius (Material 3 shape scale) ----------------------------------------
    /** MD3 extra-small shape — badges pinned to imagery, small discs. */
    val RadiusMicro: Dp = 4.dp

    /** MD3 small shape — skeletons, nested rows, chips. */
    val RadiusSmall: Dp = 8.dp

    /** MD3 medium shape — tiles, posters, fields. */
    val RadiusTile: Dp = 12.dp

    /** MD3 large shape — cards and surfaces. */
    val RadiusCard: Dp = 16.dp

    /** MD3 extra-large shape — modal dialogs. */
    val DialogRadius: Dp = 28.dp

    /** Corner radius of skeleton blocks. */
    val SkeletonRadius: Dp = 8.dp

    /** Fully rounded pill (filled buttons and the search bar). */
    val RadiusPill: Dp = 999.dp

    // --- Borders ----------------------------------------------------------------------
    /** Standard border width (outline / outlineVariant). */
    val BorderWidth: Dp = 1.dp

    /** Divider width inside a card. */
    val DividerWidth: Dp = 1.dp

    // --- Elevation --------------------------------------------------------------------
    /** Content cards do not cast shadows in MD3; depth comes from the surface tone. */
    val ElevationCard: Dp = 0.dp

    /** Compact rows keep the same rule. */
    val ElevationRow: Dp = 0.dp

    /** Dialogs and menus are the only elevated surfaces. */
    val ElevationHero: Dp = 6.dp

    // --- Component sizes (Material 3 specs) --------------------------------------------
    /** Minimum touch target recommended by the accessibility guidelines (48dp). */
    val TouchTarget: Dp = 48.dp

    /** MD3 buttons are 40dp tall. */
    val ButtonHeight: Dp = 40.dp

    /** MD3 outlined/filled text fields are 56dp tall. */
    val SearchInputHeight: Dp = 56.dp

    /** MD3 icon buttons are 40dp square inside a 48dp target. */
    val IconButtonSize: Dp = 40.dp

    /** MD3 chips are 32dp tall. */
    val ChipHeightDefault: Dp = 32.dp
    val ChipHeightSmall: Dp = 24.dp

    /** Height of the ratings bar inside a `MovieCard`. */
    val RatingBarHeight: Dp = 40.dp
    val AvatarSmall: Dp = 24.dp
    val AvatarMedium: Dp = 32.dp
    val AvatarLarge: Dp = 64.dp
    val IconBadgeSmall: Dp = 32.dp
    val IconBadgeMedium: Dp = 40.dp
    val IconBadgeLarge: Dp = 48.dp

    /** MD3 navigation bar height. */
    val BottomBarContentHeight: Dp = 80.dp

    val PosterWidth: Dp = 80.dp
    val PosterHeight: Dp = 104.dp

    /** Compact poster used by the search rows of the "Adicionar" screen. */
    val PosterSmallWidth: Dp = 52.dp
    val PosterSmallHeight: Dp = 76.dp

    // --- Skeleton geometry -------------------------------------------------------------
    val SkeletonLineHeight: Dp = 12.dp
    val SkeletonTitleHeight: Dp = 18.dp
}
