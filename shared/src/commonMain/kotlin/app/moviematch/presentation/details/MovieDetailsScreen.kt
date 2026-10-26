package app.moviematch.presentation.details

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import app.moviematch.presentation.components.ButtonVariant
import app.moviematch.presentation.components.Chip
import app.moviematch.presentation.components.ChipSize
import app.moviematch.presentation.components.ChipTone
import app.moviematch.presentation.components.HeaderIconButton
import app.moviematch.presentation.components.IconBadge
import app.moviematch.presentation.components.ScreenTopBar
import app.moviematch.presentation.components.StateError
import app.moviematch.presentation.components.icons.IconChevronLeft
import app.moviematch.presentation.components.icons.IconDocument
import app.moviematch.presentation.components.icons.IconFilm
import app.moviematch.presentation.components.icons.IconSize
import app.moviematch.presentation.components.icons.iconSize
import app.moviematch.presentation.components.skeletons.MovieDetailsSkeleton
import app.moviematch.presentation.theme.CardMetaStyle
import app.moviematch.presentation.theme.DialogBodyStyle
import app.moviematch.presentation.theme.DialogTitleStyle
import app.moviematch.presentation.theme.Dimens
import app.moviematch.presentation.theme.HeroTitleStyle
import app.moviematch.presentation.theme.MicroLabelStyle
import coil3.compose.AsyncImage
import org.koin.compose.viewmodel.koinViewModel

/**
 * Detail screen of one title, ported from the reference `MovieDetails`.
 *
 * The hero (poster + scrim + badges + tags), the "Nosso Veredito" card, the synopsis and the action
 * row all read from a single [ContentItem]. The two destructive flows live in their own dialogs, and
 * the screen only navigates back once the deletion actually succeeded.
 *
 * Material 3: the hero is a [Card] on `surfaceContainerLow`, the transparent top app bar floats over
 * the artwork (the back button carries its own scrim via [HeaderIconButton]'s `overPoster`), and the
 * only gradient left is the neutral black scrim that keeps the text over the poster legible.
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

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        ) {
            when {
                state.isLoading -> Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .navigationBarsPadding()
                        .padding(
                            start = Dimens.ScreenPadding,
                            end = Dimens.ScreenPadding,
                            bottom = Dimens.Spacing8,
                        ),
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
                        .navigationBarsPadding()
                        .padding(
                            start = Dimens.ScreenPadding,
                            end = Dimens.ScreenPadding,
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

                    Spacer(Modifier.height(Dimens.Spacing24))
                }
            }
        }

        // Transparent app bar floating over the hero: the poster starts at the top of the canvas, so
        // an opaque (`floating = true`) bar would hide the artwork. Contrast comes from the scrim of
        // the icon button, not from a blurred chrome layer.
        ScreenTopBar(
            title = "",
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding(),
            navigationIcon = {
                HeaderIconButton(
                    onClick = onBack,
                    contentDescription = "Voltar",
                    overPoster = true,
                ) {
                    IconChevronLeft(modifier = Modifier.iconSize(18.dp), color = Color.White)
                }
            },
        )
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

/** Poster + badges + the title band, the signature block of the screen. */
@Composable
private fun HeroCard(item: ContentItem) {
    val watched = item.isWatched
    var posterFailed by remember(item.posterUrl) { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = Dimens.ElevationCard),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.82f),
            ) {
                // The placeholder is always painted first, so a missing/broken poster leaves a
                // readable glyph instead of a hole behind the badges.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                    contentAlignment = Alignment.Center,
                ) {
                    IconFilm(
                        modifier = Modifier.iconSize(IconSize.Large),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                if (item.posterUrl != null && !posterFailed) {
                    AsyncImage(
                        model = item.posterUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        placeholder = null,
                        onError = { posterFailed = true },
                    )
                }

                // Neutral scrim: darker at the top and bottom so the status area and the badge row
                // never sit on the raw artwork. No brand gradient — only legibility.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0f to Color.Black.copy(alpha = 0.40f),
                                0.30f to Color.Transparent,
                                0.62f to Color.Transparent,
                                1f to Color.Black.copy(alpha = 0.70f),
                            ),
                        ),
                )

                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(Dimens.Spacing12),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    HeroBadge(
                        label = if (watched) "Assistido" else "Pendente",
                        highlighted = watched,
                    )
                    if (watched) {
                        WatchedDateBadge(item = item)
                    }
                }
            }

            HeroBottom(item = item)
        }
    }
}

@Composable
private fun HeroBadge(label: String, highlighted: Boolean) {
    // Category chip = badge pinned over imagery (uppercase micro label); `selected` lifts the
    // "Assistido" state to the `secondaryContainer` role.
    Chip(
        label = label,
        size = ChipSize.Small,
        tone = ChipTone.Category,
        selected = highlighted,
    )
}

@Composable
private fun WatchedDateBadge(item: ContentItem) {
    Chip(
        label = formatDateShort(item.watchedAt),
        size = ChipSize.Small,
        tone = ChipTone.Category,
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
            )
        },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HeroBottom(item: ContentItem) {
    val metaLine = listOfNotNull(
        item.year?.toString(),
        if (item.type == ContentType.MOVIE) item.duration else null,
        item.seasons?.let { "$it temporada${if (it > 1) "s" else ""}" },
        item.episodes?.let { "$it episódios" },
    ).joinToString(" • ")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(Dimens.Spacing16),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (metaLine.isNotEmpty()) {
            Text(
                text = metaLine,
                style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.8.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(Dimens.Spacing16))
        }

        Text(
            text = (item.title.ifBlank {
                if (item.type == ContentType.TV) "Série sem título" else "Filme sem título"
            }),
            style = HeroTitleStyle,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )

        if (item.genres.isNotEmpty()) {
            Spacer(Modifier.height(Dimens.Spacing16))
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

        Spacer(Modifier.height(Dimens.Spacing16))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing8),
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
            Row {
                Text(
                    text = "Escolhido por ",
                    style = DialogBodyStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = item.suggestedBy?.displayName ?: "—",
                    style = DialogBodyStyle.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
private fun HeroTag(genre: String) {
    Chip(label = genre, tone = ChipTone.Neutral)
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
            IconBadge(size = Dimens.IconBadgeMedium) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
            }

            Column {
                Text(
                    text = "Nosso Veredito",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Row(
                    modifier = Modifier.padding(top = Dimens.Spacing4),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing4),
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = if (approved) {
                            MaterialTheme.colorScheme.tertiary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(13.dp),
                    )
                    Text(
                        text = approvedLabel(average, item.ratings.size),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Spacer(Modifier.height(Dimens.Spacing12))

        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) {
            Row(
                modifier = Modifier.padding(
                    horizontal = Dimens.Spacing12,
                    vertical = Dimens.Spacing8,
                ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing8),
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    text = average?.formatDecimals1() ?: "—",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "MÉDIA",
                    style = MicroLabelStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
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
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing8),
            ) {
                Avatar(name = profile.displayName, size = Dimens.AvatarMedium)
                Text(
                    text = profile.displayName,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = rating?.score?.formatDecimals1() ?: "—",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = " /10",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(Dimens.Spacing12))

            Text(
                text = comment,
                style = CardMetaStyle.copy(fontStyle = FontStyle.Italic),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(Dimens.Spacing12))

            HorizontalDivider(
                thickness = Dimens.DividerWidth,
                color = MaterialTheme.colorScheme.outlineVariant,
            )

            Spacer(Modifier.height(Dimens.Spacing8))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (rating != null) "Nota em" else "Sem nota",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = rating?.createdAt?.let(::formatDateShort) ?: "—",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary,
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
            horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing12),
        ) {
            IconDocument(
                modifier = Modifier.iconSize(18.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "Sinopse",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        Spacer(Modifier.height(Dimens.Spacing12))

        Text(
            text = overview.orEmpty(),
            style = DialogBodyStyle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Dimens.Spacing12),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing12),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppButton(
                label = "Editar Notas",
                onClick = onEditRatings,
                variant = ButtonVariant.Primary,
                modifier = Modifier.weight(1f),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                    )
                },
            )

            // Destructive affordance: the `error` role, and nothing else.
            Surface(
                onClick = onDelete,
                modifier = Modifier.size(Dimens.TouchTarget),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Apagar",
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }

        // Secondary action on its own line: two labelled buttons side by side clip the Portuguese
        // labels on a 360dp-wide screen, so the outlined action gets the full width instead.
        AppButton(
            label = "Reassistir",
            onClick = onRewatch,
            variant = ButtonVariant.Outline,
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp),
                )
            },
        )
    }
}

/** Informational dialog shown when "Reassistir" is pressed on a title that is still queued. */
@Composable
private fun QueueNoticeDialog(isTv: Boolean, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Já está na fila",
                style = DialogTitleStyle,
                textAlign = TextAlign.Center,
            )
        },
        text = {
            Text(
                text = if (isTv) {
                    "Esta série ainda não foi marcada como assistida."
                } else {
                    "Este filme ainda não foi marcado como assistido."
                },
                style = DialogBodyStyle,
                textAlign = TextAlign.Center,
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Entendi")
            }
        },
    )
}

/** The rating written by [profile], if any. Unknown authors are ignored. */
private fun ratingFor(
    ratings: List<RatingWithProfile>,
    profile: ProfileName,
): RatingWithProfile? = ratings.firstOrNull {
    ProfileName.fromDisplayName(it.profile?.displayName) == profile
}
