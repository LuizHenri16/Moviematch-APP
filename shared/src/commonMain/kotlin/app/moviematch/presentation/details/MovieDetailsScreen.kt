package app.moviematch.presentation.details

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.moviematch.domain.approvedLabel
import app.moviematch.domain.averageOf
import app.moviematch.domain.formatDateShort
import app.moviematch.domain.model.ContentItem
import app.moviematch.domain.model.ContentType
import app.moviematch.domain.model.ProfileName
import app.moviematch.domain.model.RatingWithProfile
import app.moviematch.presentation.common.dataOrNull
import app.moviematch.presentation.common.errorOrNull
import app.moviematch.presentation.common.formatDecimals1
import app.moviematch.presentation.common.isLoading
import app.moviematch.presentation.components.AppButton
import app.moviematch.presentation.components.Avatar
import app.moviematch.presentation.components.BentoCard
import app.moviematch.presentation.components.BentoTile
import app.moviematch.presentation.components.HeaderIconButton
import app.moviematch.presentation.components.IconBadge
import app.moviematch.presentation.components.StateError
import app.moviematch.presentation.components.icons.IconChevronLeft
import app.moviematch.presentation.components.icons.IconDocument
import app.moviematch.presentation.components.icons.iconSize
import app.moviematch.presentation.components.skeletons.MovieDetailsSkeleton
import app.moviematch.presentation.theme.Amber
import app.moviematch.presentation.theme.Border
import app.moviematch.presentation.theme.BorderStrong
import app.moviematch.presentation.theme.ChromeSurface
import app.moviematch.presentation.theme.DangerStrong
import app.moviematch.presentation.theme.Dimens
import app.moviematch.presentation.theme.HeroScrimBottom
import app.moviematch.presentation.theme.HeroScrimMid
import app.moviematch.presentation.theme.HeroTitleStyle
import app.moviematch.presentation.theme.Surface
import app.moviematch.presentation.theme.SurfaceCard
import app.moviematch.presentation.theme.SurfaceCardElevated
import app.moviematch.presentation.theme.SurfaceHero
import app.moviematch.presentation.theme.TextMuted
import app.moviematch.presentation.theme.TextPrimary
import app.moviematch.presentation.theme.TextSecondary
import app.moviematch.presentation.theme.TextSoft
import app.moviematch.presentation.theme.White
import coil3.compose.AsyncImage
import org.koin.compose.viewmodel.koinViewModel

/**
 * Detail screen of one title, ported from the reference `MovieDetails`.
 *
 * The hero (poster + scrim + badges + tags), the "Nosso Veredito" card, the synopsis and the action
 * row all read from a single [ContentItem]. The two destructive flows live in their own dialogs, and
 * the screen only navigates back once the deletion actually succeeded.
 */
@Composable
fun MovieDetailsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MovieDetailsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val deleteResult by viewModel.deleteResult.collectAsStateWithLifecycle()
    val ratingDialogOpen by viewModel.ratingDialogOpen.collectAsStateWithLifecycle()
    val currentProfile by viewModel.currentProfile.collectAsStateWithLifecycle()

    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showQueueNotice by remember { mutableStateOf(false) }

    LaunchedEffect(deleteResult) {
        if (deleteResult is DeleteResult.Deleted) {
            viewModel.consumeDeleteResult()
            showDeleteConfirm = false
            onBack()
        }
    }

    val item = state.dataOrNull()

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.ScreenPadding, vertical = Dimens.Spacing12),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            HeaderIconButton(onClick = onBack) {
                IconChevronLeft(modifier = Modifier.iconSize(18.dp), color = TextPrimary)
            }
            // Keeps the back arrow on the leading edge, like the reference header's empty twin.
            Spacer(Modifier.size(Dimens.IconButtonSize))
        }

        when {
            state.isLoading -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Dimens.ScreenPadding, vertical = Dimens.Spacing8),
            ) {
                MovieDetailsSkeleton()
            }

            item == null -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                StateError(
                    message = state.errorOrNull() ?: "Não foi possível carregar este título.",
                    onRetry = viewModel::reload,
                )
            }

            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(
                        start = Dimens.ScreenPadding,
                        end = Dimens.ScreenPadding,
                        top = Dimens.Spacing8,
                        bottom = Dimens.Spacing24,
                    ),
            ) {
                HeroCard(item = item)

                Spacer(Modifier.height(Dimens.Spacing16))

                VerdictCard(item = item)

                if (!item.overview.isNullOrBlank()) {
                    Spacer(Modifier.height(Dimens.Spacing16))
                    SynopsisCard(overview = item.overview)
                }

                Spacer(Modifier.height(Dimens.Spacing16))

                ActionRow(
                    onEditRatings = viewModel::openRatingDialog,
                    onRewatch = {
                        if (item.isWatched) {
                            viewModel.moveBackToQueue()
                        } else {
                            showQueueNotice = true
                        }
                    },
                    onDelete = { showDeleteConfirm = true },
                )

                Spacer(Modifier.height(30.dp))
            }
        }
    }

    if (ratingDialogOpen && item != null) {
        val mariaRating = ratingFor(item.ratings, ProfileName.MARIA)
        val luizRating = ratingFor(item.ratings, ProfileName.LUIZ)
        RatingDialog(
            currentProfile = currentProfile,
            mariaScore = mariaRating?.score,
            luizScore = luizRating?.score,
            mariaComment = mariaRating?.comment,
            luizComment = luizRating?.comment,
            initiallyWatched = item.isWatched,
            onSave = { profile, score, comment, watchedTogether ->
                viewModel.saveRating(profile, score, comment, watchedTogether)
            },
            onDismiss = viewModel::closeRatingDialog,
        )
    }

    if (showDeleteConfirm && item != null) {
        ConfirmDeleteDialog(
            movieTitle = item.title,
            deleting = deleteResult is DeleteResult.Deleting,
            onConfirm = viewModel::delete,
            onDismiss = { showDeleteConfirm = false },
        )
    }

    if (showQueueNotice && item != null) {
        QueueNoticeDialog(
            isTv = item.type == ContentType.TV,
            onDismiss = { showQueueNotice = false },
        )
    }
}

/** Poster + scrim + badges + title/tags, the signature block of the screen. */
@Composable
private fun HeroCard(item: ContentItem) {
    val shape = RoundedCornerShape(Dimens.RadiusHero)
    val watched = item.isWatched

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(SurfaceHero, shape)
            .border(1.dp, BorderStrong, shape),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.82f),
        ) {
            if (item.posterUrl != null) {
                AsyncImage(
                    model = item.posterUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    placeholder = null,
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(SurfaceCardElevated),
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .fillMaxHeight(0.62f)
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            0.48f to HeroScrimMid,
                            1f to HeroScrimBottom,
                        ),
                    ),
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    HeroBadge(
                        label = if (watched) "ASSISTIDO" else "PENDENTE",
                        fontWeight = FontWeight.ExtraBold,
                    )
                    if (watched) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(ChromeSurface, RoundedCornerShape(16.dp))
                                .border(1.dp, Border, RoundedCornerShape(16.dp))
                                .padding(horizontal = Dimens.Spacing12, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = TextSoft,
                                modifier = Modifier.size(14.dp),
                            )
                            Text(
                                text = formatDateShort(item.watchedAt),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextSoft,
                            )
                        }
                    }
                }

                HeroBottom(item = item)
            }
        }
    }
}

@Composable
private fun HeroBadge(label: String, fontWeight: FontWeight) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(ChromeSurface, RoundedCornerShape(16.dp))
            .border(1.dp, Border, RoundedCornerShape(16.dp))
            .padding(horizontal = Dimens.Spacing12, vertical = 6.dp),
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = fontWeight,
            letterSpacing = 0.5.sp,
            color = TextPrimary,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HeroBottom(item: ContentItem) {
    val metaLine = listOfNotNull(
        item.year?.toString(),
        if (item.type == ContentType.MOVIE) item.duration?.uppercase() else null,
        item.seasons?.let { "$it temporada${if (it > 1) "s" else ""}" },
        item.episodes?.let { "$it episódios" },
    ).joinToString(" • ")

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (metaLine.isNotEmpty()) {
            Text(
                text = metaLine,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.8.sp,
                color = TextSoft,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(28.dp))
        }

        Text(
            text = (item.title.ifBlank {
                if (item.type == ContentType.TV) "Série sem título" else "Filme sem título"
            }).uppercase(),
            style = HeroTitleStyle,
            color = TextPrimary,
            textAlign = TextAlign.Center,
        )

        if (item.genres.isNotEmpty()) {
            Spacer(Modifier.height(32.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(
                    space = Dimens.Spacing8,
                    alignment = Alignment.CenterHorizontally,
                ),
                verticalArrangement = Arrangement.spacedBy(Dimens.Spacing8),
            ) {
                item.genres.forEach { genre -> HeroTag(genre) }
            }
        }

        Spacer(Modifier.height(28.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing8),
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(16.dp),
            )
            Row {
                Text(
                    text = "Escolhido por ",
                    fontSize = 13.sp,
                    color = TextSecondary,
                )
                Text(
                    text = item.suggestedBy?.displayName ?: "—",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                )
            }
        }
    }
}

@Composable
private fun HeroTag(genre: String) {
    val shape = RoundedCornerShape(16.dp)
    Text(
        text = genre,
        modifier = Modifier
            .clip(shape)
            .background(SurfaceCardElevated.copy(alpha = 0.9f), shape)
            .border(1.dp, Border, shape)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        fontSize = 12.sp,
        color = TextSoft,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

/** "Nosso Veredito": average badge plus one rating box per member. */
@Composable
private fun VerdictCard(item: ContentItem) {
    val average = averageOf(item.ratings)
    val approved = item.ratings.isNotEmpty() && average != null && average >= 7

    BentoCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing12),
        ) {
            IconBadge(size = Dimens.IconBadgeMedium, containerColor = SurfaceCardElevated) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = TextPrimary,
                    modifier = Modifier.size(18.dp),
                )
            }

            Column {
                Text(
                    text = "Nosso Veredito",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                )
                Row(
                    modifier = Modifier.padding(top = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing4),
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = if (approved) Amber else TextMuted,
                        modifier = Modifier.size(13.dp),
                    )
                    Text(
                        text = approvedLabel(average, item.ratings.size),
                        fontSize = 11.sp,
                        color = TextSecondary,
                    )
                }
            }
        }

        Spacer(Modifier.height(Dimens.Spacing12))

        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceCardElevated, RoundedCornerShape(16.dp))
                .border(1.dp, Border, RoundedCornerShape(16.dp))
                .padding(horizontal = Dimens.Spacing12, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = Amber,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = average?.formatDecimals1() ?: "—",
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary,
            )
            Text(
                text = "MÉDIA",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
            )
        }

        Spacer(Modifier.height(Dimens.Spacing24))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Dimens.Spacing12),
        ) {
            RatingBox(
                profile = ProfileName.MARIA,
                rating = ratingFor(item.ratings, ProfileName.MARIA),
            )
            RatingBox(
                profile = ProfileName.LUIZ,
                rating = ratingFor(item.ratings, ProfileName.LUIZ),
            )
        }
    }
}

@Composable
private fun RatingBox(profile: ProfileName, rating: RatingWithProfile?) {
    val comment = when {
        rating == null -> "Aguardando a nota."
        rating.comment.isNullOrBlank() -> "Sem comentário."
        else -> "“${rating.comment}”"
    }

    BentoTile(
        modifier = Modifier.fillMaxWidth(),
        padding = Dimens.Spacing16,
        containerColor = Surface,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Avatar(name = profile.displayName, size = Dimens.AvatarMedium)
                Text(
                    text = profile.displayName,
                    modifier = Modifier.weight(1f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                )
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = rating?.score?.formatDecimals1() ?: "—",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary,
                    )
                    Text(
                        text = " /10",
                        fontSize = 11.sp,
                        color = TextMuted,
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = comment,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                fontStyle = FontStyle.Italic,
                color = TextSoft,
            )

            Spacer(Modifier.height(14.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Border),
            )

            Spacer(Modifier.height(Dimens.Spacing8))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (rating != null) "Nota em" else "Sem nota",
                    fontSize = 11.sp,
                    color = TextSecondary,
                )
                Text(
                    text = rating?.createdAt?.let(::formatDateShort) ?: "—",
                    fontSize = 11.sp,
                    color = Amber,
                )
            }
        }
    }
}

/** Synopsis block; only rendered when the title actually has an overview. */
@Composable
private fun SynopsisCard(overview: String?) {
    BentoCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            IconDocument(modifier = Modifier.iconSize(18.dp), color = TextSecondary)
            Text(
                text = "Sinopse",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
            )
        }

        Spacer(Modifier.height(Dimens.Spacing12))

        Text(
            text = overview.orEmpty(),
            fontSize = 13.sp,
            lineHeight = 20.sp,
            color = TextSecondary,
        )
    }
}

/** "Editar Notas" / "Reassistir" / delete. */
@Composable
private fun ActionRow(
    onEditRatings: () -> Unit,
    onRewatch: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing12),
    ) {
        ActionButton(
            label = "Editar Notas",
            icon = { tint ->
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(15.dp),
                )
            },
            onClick = onEditRatings,
            modifier = Modifier.weight(1f),
        )
        ActionButton(
            label = "Reassistir",
            icon = { tint ->
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(15.dp),
                )
            },
            onClick = onRewatch,
            modifier = Modifier.weight(1f),
        )

        val shape = RoundedCornerShape(Dimens.RadiusPill)
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(shape)
                .background(DangerStrong, shape)
                .border(1.dp, Border, shape)
                .clickable(onClick = onDelete),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Apagar",
                tint = White,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun ActionButton(
    label: String,
    icon: @Composable (Color) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(Dimens.RadiusPill)
    Row(
        modifier = modifier
            .height(48.dp)
            .clip(shape)
            .background(SurfaceCard, shape)
            .border(1.dp, Border, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing8),
    ) {
        icon(TextPrimary)
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Informational dialog shown when "Reassistir" is pressed on a title that is still queued. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QueueNoticeDialog(isTv: Boolean, onDismiss: () -> Unit) {
    BasicAlertDialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Dimens.RadiusCard))
                .background(Surface)
                .border(1.dp, Border, RoundedCornerShape(Dimens.RadiusCard))
                .padding(Dimens.Spacing24),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Já está na fila",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(Dimens.Spacing8))
            Text(
                text = if (isTv) {
                    "Esta série ainda não foi marcada como assistida."
                } else {
                    "Este filme ainda não foi marcado como assistido."
                },
                fontSize = 13.sp,
                lineHeight = 20.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(Dimens.Spacing24))
            AppButton(
                label = "Entendi",
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** The rating written by [profile], if any. Unknown authors are ignored. */
private fun ratingFor(
    ratings: List<RatingWithProfile>,
    profile: ProfileName,
): RatingWithProfile? = ratings.firstOrNull {
    ProfileName.fromDisplayName(it.profile?.displayName) == profile
}
