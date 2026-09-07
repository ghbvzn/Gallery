package com.example.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.MediaItem
import com.example.data.MediaType
import com.example.ui.theme.RoseFavorite
import com.example.ui.util.DateTimeUtils

private val BottomOverlayGradient = Brush.verticalGradient(
    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.72f))
)

/**
 * Ultra-fast, lightweight MediaCard composable.
 * Supports tap to view (or toggle selection), and long-press to enter multi-select mode.
 * Automatically adapts complexity based on grid column density (2, 3, 4, 5 columns).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaCard(
    item: MediaItem,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    gridColumns: Int = 3,
    showVideoDurationBadge: Boolean = true,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val cornerRadius = if (gridColumns >= 4) 6.dp else 10.dp
    val targetPx = when (gridColumns) {
        2 -> 720
        3 -> 540
        4 -> 380
        else -> 280
    }

    val imageRequest = remember(item.uriString, targetPx) {
        ImageRequest.Builder(context)
            .data(item.uriString)
            .size(targetPx, targetPx)
            .precision(coil.size.Precision.INEXACT)
            .allowHardware(true)
            .crossfade(false)
            .build()
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(cornerRadius))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .then(
                if (isSelected) {
                    Modifier.border(
                        width = 3.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(cornerRadius)
                    )
                } else Modifier
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .testTag("media_card_${item.id}")
    ) {
        AsyncImage(
            model = imageRequest,
            contentDescription = item.title,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Semi-transparent primary scrim overlay when selected
        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.28f))
            )
        }

        // Selection indicator checkmark badge (visible in selection mode)
        if (isSelectionMode) {
            val indicatorSize = if (gridColumns >= 4) 22.dp else 26.dp
            val checkIconSize = if (gridColumns >= 4) 14.dp else 16.dp
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(if (gridColumns >= 4) 4.dp else 6.dp)
                    .size(indicatorSize)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else Color.Black.copy(alpha = 0.45f)
                    )
                    .then(
                        if (!isSelected) Modifier.border(1.5.dp, Color.White, CircleShape)
                        else Modifier
                    )
                    .testTag("selection_badge_${item.id}"),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(checkIconSize)
                    )
                }
            }

            // In selection mode, if item is a video, show video icon top-end
            if (item.type == MediaType.VIDEO) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(if (gridColumns >= 4) 4.dp else 6.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.65f))
                        .padding(
                            horizontal = if (gridColumns >= 4) 4.dp else 6.dp,
                            vertical = if (gridColumns >= 4) 2.dp else 3.dp
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Video",
                        tint = Color.White,
                        modifier = Modifier.size(if (gridColumns >= 4) 11.dp else 13.dp)
                    )
                }
            }
        } else {
            // Normal badges overlay: Video indicator & Favorite icon
            val showVideoBadge = item.type == MediaType.VIDEO
            val showFavoriteButton = item.isFavorite || gridColumns <= 3

            if (showVideoBadge || showFavoriteButton) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(if (gridColumns >= 4) 4.dp else 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (showVideoBadge) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.Black.copy(alpha = 0.65f))
                                .padding(
                                    horizontal = if (gridColumns >= 4) 4.dp else 6.dp,
                                    vertical = if (gridColumns >= 4) 2.dp else 3.dp
                                )
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Video",
                                    tint = Color.White,
                                    modifier = Modifier.size(if (gridColumns >= 4) 11.dp else 13.dp)
                                )
                                if (showVideoDurationBadge && gridColumns <= 3 && item.durationSeconds > 0) {
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = DateTimeUtils.formatVideoDuration(item.durationSeconds),
                                        color = Color.White,
                                        fontSize = if (gridColumns == 2) 11.sp else 10.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }

                    if (item.isFavorite) {
                        Box(
                            modifier = Modifier
                                .size(if (gridColumns >= 4) 24.dp else 28.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.45f))
                                .clickable(onClick = onToggleFavorite)
                                .testTag("favorite_button_${item.id}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Favorite,
                                contentDescription = "Unfavorite",
                                tint = RoseFavorite,
                                modifier = Modifier.size(if (gridColumns >= 4) 14.dp else 16.dp)
                            )
                        }
                    } else if (gridColumns <= 2) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.35f))
                                .clickable(onClick = onToggleFavorite)
                                .testTag("favorite_button_${item.id}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        // Bottom overlay: Only shown on 2 columns (Large) to prevent clutter and keep fast frame rate
        if (gridColumns <= 2) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .height(60.dp)
                    .background(BottomOverlayGradient)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Text(
                    text = item.title,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (item.locationName.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 1.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.LocationOn,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = item.locationName,
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
