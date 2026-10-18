package app.moviematch.presentation.theme

import androidx.compose.ui.graphics.Color

/**
 * "Serene Bento Dark" brand palette.
 *
 * These tokens are the single source of truth for colour in the shared UI and mirror the
 * values used by the existing MovieMatch product so both apps stay visually identical.
 */

// --- Canvas & containers ---------------------------------------------------------------
val Background = Color(0xFF14171A)
val Surface = Color(0xFF222222)
val SurfaceRaised = Color(0xFF222222)
val SurfaceDeep = Color(0xFF0B0E11)
val SurfaceCard = Color(0xFF1C2127)
val SurfaceCardElevated = Color(0xFF252B33)

/** Hairline outline used by dark-on-dark bento containers: `rgba(255, 255, 255, 0.07)`. */
val Border = Color(0x12FFFFFF)
val Divider = Border

/** Slightly stronger hairline used by the details hero (`rgba(255, 255, 255, 0.1)`). */
val BorderStrong = Color(0x1AFFFFFF)

/** Muted structural outline (`#414943`) — the unchecked circle of a `MovieCard`. */
val Outline = Color(0xFF414943)

/** Hero canvas of the details screen (`#11161a`). */
val SurfaceHero = Color(0xFF11161A)

/** Translucent dark chrome floating on top of imagery (`rgba(28, 33, 39, 0.9)`). */
val ChromeSurface = Color(0xE61C2127)

/** Lavender-tinted chrome of the poster category badge (`rgba(38, 28, 48, 0.85)`). */
val CategoryBadgeSurface = Color(0xD9261C30)
val CategoryBadgeBorder = Color(0x33C4B5FD)

/** Neutral translucent chip used by the media-type badge of the "Adicionar" rows. */
val ChipSurface = Color(0x14FFFFFF)
val ChipBorder = Color(0x1FFFFFFF)

/** Scrim stops of the details hero gradient (`rgba(11, 14, 17, 0.36)` -> `0.92`). */
val HeroScrimMid = Color(0x5C0B0E11)
val HeroScrimBottom = Color(0xEB0B0E11)

// --- Typography ------------------------------------------------------------------------
val TextPrimary = Color(0xFFF8FAFC)
val TextSecondary = Color(0xFF94A3B8)
val TextMuted = Color(0xFF8A938C)
val TextSoft = Color(0xFFE1E2E6)

/** Deep slate used on top of the pastel (accent) bento blocks. */
val TextOnAccent = Color(0xFF0F172A)

// --- Accents ---------------------------------------------------------------------------
val Accent = Color(0xFF9DD2B3)
val AccentDark = Color(0xFF003823)
val AccentDeep = Color(0xFF2A5C43)
val Lavender = Color(0xFFC4B5FD)
val LavenderSurface = Color(0x1FC4B5FD)
val LavenderBorder = Color(0x59C4B5FD)
val Amber = Color(0xFFFDE047)
val White = Color(0xFFFFFFFF)

// --- Feedback --------------------------------------------------------------------------
val Danger = Color(0xFFFFB4AB)
val DangerStrong = Color(0xFFDC2626)
val DangerSurface = Color(0x1FFFB4AB)
val DangerBorder = Color(0x73FFB4AB)

// --- Avatars ---------------------------------------------------------------------------
val AvatarMaria = Color(0xFF4A3D7C)
val AvatarLuiz = Color(0xFF2A5C43)

/** Background tint used by the average-rating badge when the score is excellent (>= 8). */
val ScoreGreatSurface = Color(0xFF14261D)
val ScoreGreatBorder = Color(0x599DD2B3)
val ScoreGoodSurface = Color(0xFF252114)
val ScoreGoodBorder = Color(0x59FDE047)
val ScoreBadSurface = Color(0xFF251517)
val ScoreBadBorder = Color(0x59FDA4AF)
val ScoreBad = Color(0xFFFDA4AF)

/**
 * Avatar background for a profile. Falls back to the "Luiz" lane for unknown names, which is
 * what the reference implementation does.
 */
fun avatarColor(name: String?): Color = if (name == "Maria") AvatarMaria else AvatarLuiz

/** First letter of the display name, uppercased — used when there is no avatar image. */
fun avatarInitial(name: String?): String = (name?.take(1) ?: "?").uppercase()
