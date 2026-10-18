package app.moviematch.presentation.components.skeletons

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.moviematch.presentation.components.BentoCard
import app.moviematch.presentation.components.Skeleton
import app.moviematch.presentation.components.SkeletonCircle
import app.moviematch.presentation.theme.Dimens

/**
 * Per-screen loading shapes.
 *
 * They must reuse the same card/tile/pill vocabulary as the loaded screens (same radii, same
 * paddings) so the transition from skeleton to content does not shift the layout.
 */

/** Header used by nearly every bento card skeleton: title + subtitle + action disc. */
@Composable
private fun CardHeaderSkeleton(titleWidth: Dp, subtitleWidth: Dp) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Dimens.Spacing8),
        ) {
            Skeleton(width = titleWidth, height = Dimens.SkeletonTitleHeight, borderRadius = 6.dp)
            Skeleton(width = subtitleWidth, height = Dimens.SkeletonLineHeight, borderRadius = 6.dp)
        }
        SkeletonCircle(size = Dimens.IconBadgeLarge)
    }
}

/** Home bento grid: pipoca stat tiles, harmony donut, duel, consensus, genres and the random card. */
@Composable
fun HomeSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Dimens.Spacing16),
    ) {
        // 1. Termômetro de Pipoca
        BentoCard {
            CardHeaderSkeleton(titleWidth = 160.dp, subtitleWidth = 240.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Dimens.Spacing16),
                horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing8),
            ) {
                repeat(3) {
                    Skeleton(
                        modifier = Modifier.weight(1f),
                        height = 74.dp,
                        borderRadius = Dimens.RadiusTile,
                    )
                }
            }
        }

        // 2. Índice de Sintonia
        BentoCard {
            Skeleton(width = 140.dp, height = Dimens.SkeletonLineHeight, borderRadius = 6.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Dimens.Spacing16),
                horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing16),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SkeletonCircle(size = 90.dp)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(Dimens.Spacing8),
                ) {
                    Skeleton(width = 170.dp, height = Dimens.SkeletonTitleHeight, borderRadius = 6.dp)
                    Skeleton(height = Dimens.SkeletonLineHeight, borderRadius = 6.dp)
                    Skeleton(width = 180.dp, height = Dimens.SkeletonLineHeight, borderRadius = 6.dp)
                }
            }
        }

        // 3. Duelo de Críticos
        BentoCard {
            CardHeaderSkeleton(titleWidth = 150.dp, subtitleWidth = 120.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Dimens.Spacing16),
                horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing12),
            ) {
                repeat(2) {
                    Skeleton(
                        modifier = Modifier.weight(1f),
                        height = 96.dp,
                        borderRadius = Dimens.RadiusTile,
                    )
                }
            }
            Skeleton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Dimens.Spacing12),
                height = 58.dp,
                borderRadius = Dimens.RadiusTile,
            )
        }

        // 4. Consenso do Grupo
        BentoCard {
            CardHeaderSkeleton(titleWidth = 130.dp, subtitleWidth = 90.dp)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Dimens.Spacing16),
                verticalArrangement = Arrangement.spacedBy(Dimens.Spacing8),
            ) {
                repeat(3) {
                    Skeleton(height = 52.dp, borderRadius = Dimens.RadiusTile)
                }
            }
        }

        // 5. Gêneros Favoritos
        BentoCard {
            CardHeaderSkeleton(titleWidth = 150.dp, subtitleWidth = 70.dp)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Dimens.Spacing16),
                verticalArrangement = Arrangement.spacedBy(Dimens.Spacing12),
            ) {
                repeat(3) {
                    Skeleton(height = 40.dp, borderRadius = Dimens.RadiusSmall)
                }
            }
        }

        // 6. Sugestão para filme
        BentoCard {
            Skeleton(width = 160.dp, height = Dimens.SkeletonTitleHeight, borderRadius = 6.dp)
            Skeleton(
                modifier = Modifier.padding(top = Dimens.Spacing8),
                width = 150.dp,
                height = Dimens.SkeletonLineHeight,
                borderRadius = 6.dp,
            )
            Skeleton(
                modifier = Modifier.padding(top = Dimens.Spacing16),
                width = 150.dp,
                height = 44.dp,
                borderRadius = Dimens.RadiusPill,
            )
        }
    }
}

/** One list row: poster, title/meta/genre chips and the ratings bar. */
@Composable
fun MovieCardSkeleton(modifier: Modifier = Modifier) {
    BentoCard(modifier = modifier, padding = Dimens.Spacing16) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing12),
        ) {
            Skeleton(
                width = Dimens.PosterWidth,
                height = Dimens.PosterHeight,
                borderRadius = Dimens.RadiusTile,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Dimens.Spacing8),
            ) {
                Skeleton(width = 150.dp, height = Dimens.SkeletonTitleHeight, borderRadius = 6.dp)
                Skeleton(width = 90.dp, height = 13.dp, borderRadius = 6.dp)
                Row(horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing8)) {
                    Skeleton(width = 64.dp, height = 24.dp, borderRadius = Dimens.RadiusPill)
                    Skeleton(width = 48.dp, height = 24.dp, borderRadius = Dimens.RadiusPill)
                }
            }
            SkeletonCircle(size = Dimens.IconBadgeMedium)
        }

        Skeleton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Dimens.Spacing12),
            height = 30.dp,
            borderRadius = 20.dp,
        )
    }
}

/** Poster hero + verdict + synopsis + action row. */
@Composable
fun MovieDetailsSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Dimens.Spacing16),
    ) {
        BentoCard(padding = Dimens.Spacing8) {
            Skeleton(
                modifier = Modifier.fillMaxWidth(),
                height = 380.dp,
                borderRadius = Dimens.RadiusCard,
            )
            Row(
                modifier = Modifier.padding(Dimens.Spacing16),
                horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing8),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SkeletonCircle(size = 16.dp)
                Skeleton(width = 170.dp, height = 14.dp, borderRadius = 6.dp)
            }
        }

        BentoCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing12),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SkeletonCircle(size = Dimens.IconBadgeMedium)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(Dimens.Spacing4),
                ) {
                    Skeleton(width = 140.dp, height = Dimens.SkeletonTitleHeight, borderRadius = 6.dp)
                    Skeleton(width = 90.dp, height = 11.dp, borderRadius = 6.dp)
                }
                Skeleton(width = 78.dp, height = 34.dp, borderRadius = 16.dp)
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Dimens.Spacing16),
                verticalArrangement = Arrangement.spacedBy(Dimens.Spacing12),
            ) {
                repeat(2) { RatingBoxSkeleton() }
            }
        }

        BentoCard {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing8),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SkeletonCircle(size = 18.dp)
                Skeleton(width = 90.dp, height = Dimens.SkeletonTitleHeight, borderRadius = 6.dp)
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Dimens.Spacing12),
                verticalArrangement = Arrangement.spacedBy(Dimens.Spacing8),
            ) {
                Skeleton(height = Dimens.SkeletonLineHeight, borderRadius = 6.dp)
                Skeleton(width = 260.dp, height = Dimens.SkeletonLineHeight, borderRadius = 6.dp)
                Skeleton(width = 180.dp, height = Dimens.SkeletonLineHeight, borderRadius = 6.dp)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing12)) {
            Skeleton(
                modifier = Modifier.weight(1f),
                height = 48.dp,
                borderRadius = Dimens.RadiusPill,
            )
            Skeleton(
                modifier = Modifier.weight(1f),
                height = 48.dp,
                borderRadius = Dimens.RadiusPill,
            )
            SkeletonCircle(size = 48.dp)
        }
    }
}

@Composable
private fun RatingBoxSkeleton() {
    Box(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.Spacing8)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing8),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SkeletonCircle(size = Dimens.AvatarMedium)
                Skeleton(width = 64.dp, height = 14.dp, borderRadius = 6.dp)
                Skeleton(width = 40.dp, height = 16.dp, borderRadius = 6.dp)
            }
            Skeleton(width = 200.dp, height = Dimens.SkeletonLineHeight, borderRadius = 6.dp)
        }
    }
}

/** Search result rows on the "Adicionar" screen. */
@Composable
fun SearchResultsSkeleton(
    modifier: Modifier = Modifier,
    count: Int = 4,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Dimens.Spacing12),
    ) {
        repeat(count) {
            BentoCard(padding = Dimens.Spacing12) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing12),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Skeleton(width = 52.dp, height = 76.dp, borderRadius = Dimens.RadiusTile)
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(Dimens.Spacing8),
                    ) {
                        Skeleton(width = 150.dp, height = 15.dp, borderRadius = 6.dp)
                        Skeleton(width = 90.dp, height = Dimens.SkeletonLineHeight, borderRadius = 6.dp)
                        Skeleton(height = Dimens.SkeletonLineHeight, borderRadius = 6.dp)
                    }
                    SkeletonCircle(size = Dimens.IconButtonSize)
                }
            }
        }
    }
}

/** Profile card + avatar form on the settings screen. */
@Composable
fun SettingsSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Dimens.Spacing16),
    ) {
        BentoCard {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing16),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SkeletonCircle(size = Dimens.AvatarLarge)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(Dimens.Spacing8),
                ) {
                    Skeleton(width = 120.dp, height = Dimens.SkeletonTitleHeight, borderRadius = 6.dp)
                    Skeleton(width = 170.dp, height = 13.dp, borderRadius = 6.dp)
                }
            }
        }

        BentoCard {
            Skeleton(width = 110.dp, height = Dimens.SkeletonLineHeight, borderRadius = 6.dp)
            Skeleton(
                modifier = Modifier.padding(top = Dimens.Spacing8),
                height = Dimens.SkeletonLineHeight,
                borderRadius = 6.dp,
            )
            Skeleton(
                modifier = Modifier.padding(top = Dimens.Spacing16),
                height = Dimens.SearchInputHeight,
                borderRadius = Dimens.RadiusPill,
            )
            Skeleton(
                modifier = Modifier.padding(top = Dimens.Spacing12),
                height = Dimens.ButtonHeight,
                borderRadius = Dimens.RadiusPill,
            )
        }
    }
}

/** Shell shown while the app boots (before the first frame of real UI). */
@Composable
fun AppBootSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(Dimens.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(Dimens.Spacing16),
    ) {
        Skeleton(width = 120.dp, height = 24.dp, borderRadius = 6.dp)

        BentoCard {
            Skeleton(width = 160.dp, height = Dimens.SkeletonTitleHeight, borderRadius = 6.dp)
            Skeleton(
                modifier = Modifier.padding(top = Dimens.Spacing8),
                height = Dimens.SkeletonLineHeight,
                borderRadius = 6.dp,
            )
            Row(
                modifier = Modifier.padding(top = Dimens.Spacing16),
                horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing8),
            ) {
                repeat(3) {
                    Skeleton(
                        modifier = Modifier.weight(1f),
                        height = 74.dp,
                        borderRadius = Dimens.RadiusTile,
                    )
                }
            }
        }

        BentoCard {
            Skeleton(width = 130.dp, height = Dimens.SkeletonTitleHeight, borderRadius = 6.dp)
            Skeleton(
                modifier = Modifier.padding(top = Dimens.Spacing8),
                width = 200.dp,
                height = Dimens.SkeletonLineHeight,
                borderRadius = 6.dp,
            )
            Skeleton(
                modifier = Modifier.padding(top = Dimens.Spacing12),
                height = 96.dp,
                borderRadius = Dimens.RadiusTile,
            )
        }
    }
}
