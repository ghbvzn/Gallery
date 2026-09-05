package com.example.ui.components

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Photo
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem as ExoMediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Precision
import coil.size.Size
import com.example.data.MediaItem
import com.example.data.MediaType
import com.example.ui.theme.RoseFavorite
import com.example.ui.util.DateTimeUtils
import kotlinx.coroutines.delay

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MediaDetailViewer(
    item: MediaItem,
    isAnalyzingTags: Boolean,
    aiTaggingNotice: String?,
    onClose: () -> Unit,
    onToggleFavorite: (MediaItem) -> Unit,
    onEditMetadata: (MediaItem) -> Unit,
    onDeleteItem: (Long) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onAnalyzeMedia: (MediaItem) -> Unit,
    onAcceptTag: (itemId: Long, tag: String) -> Unit,
    onRejectTag: (itemId: Long, tag: String) -> Unit,
    onAddCustomTag: (itemId: Long, customTag: String) -> Unit,
    onRemoveTag: (itemId: Long, tag: String) -> Unit
) {
    var showInfoSheet by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showAddCustomTagDialog by remember { mutableStateOf(false) }
    var customTagInput by remember { mutableStateOf("") }

    // Real Media3 ExoPlayer video playback state
    val context = LocalContext.current
    var isPlaying by remember(item.id) { mutableStateOf(false) }
    var isBuffering by remember(item.id) { mutableStateOf(false) }
    var videoProgress by remember(item.id) { mutableFloatStateOf(0f) }
    var isScrubbing by remember(item.id) { mutableStateOf(false) }
    var currentPositionSeconds by remember(item.id) { mutableStateOf(0) }
    var totalDurationSeconds by remember(item.id) { mutableStateOf(if (item.durationSeconds > 0) item.durationSeconds else 0) }
    var showControls by remember(item.id) { mutableStateOf(true) }

    // Media Zoom & Pan interactive state
    var scale by remember(item.id) { mutableFloatStateOf(1f) }
    var offsetX by remember(item.id) { mutableFloatStateOf(0f) }
    var offsetY by remember(item.id) { mutableFloatStateOf(0f) }

    val exoPlayer = remember(item.id, item.uriString) {
        if (item.type == MediaType.VIDEO) {
            ExoPlayer.Builder(context).build().apply {
                try {
                    val mediaItem = ExoMediaItem.fromUri(Uri.parse(item.uriString))
                    setMediaItem(mediaItem)
                    prepare()
                    playWhenReady = false
                } catch (e: Exception) {
                    // Fallback handled safely
                }
            }
        } else {
            null
        }
    }

    DisposableEffect(exoPlayer) {
        if (exoPlayer == null) {
            onDispose { }
        } else {
            val listener = object : Player.Listener {
                override fun onIsPlayingChanged(playing: Boolean) {
                    isPlaying = playing
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    isBuffering = (playbackState == Player.STATE_BUFFERING)
                    if (playbackState == Player.STATE_ENDED) {
                        isPlaying = false
                        videoProgress = 1f
                        if (totalDurationSeconds > 0) {
                            currentPositionSeconds = totalDurationSeconds
                        }
                    }
                    if (exoPlayer.duration > 0) {
                        totalDurationSeconds = (exoPlayer.duration / 1000).toInt()
                    }
                }
            }
            exoPlayer.addListener(listener)
            onDispose {
                exoPlayer.removeListener(listener)
                exoPlayer.release()
            }
        }
    }

    // Keep UI progress and timer updated while video is playing
    LaunchedEffect(exoPlayer, isPlaying, isScrubbing) {
        if (exoPlayer != null) {
            while (true) {
                if (!isScrubbing && exoPlayer.duration > 0) {
                    val currentMs = exoPlayer.currentPosition.coerceAtLeast(0L)
                    val durMs = exoPlayer.duration
                    videoProgress = (currentMs.toFloat() / durMs.toFloat()).coerceIn(0f, 1f)
                    currentPositionSeconds = (currentMs / 1000).toInt()
                    totalDurationSeconds = (durMs / 1000).toInt()
                }
                delay(100)
            }
        }
    }

    Dialog(
        onDismissRequest = {
            exoPlayer?.stop()
            onClose()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .testTag("media_detail_viewer")
        ) {
            // Main Media Display: Zoomable container with pinch, double-tap, and pan for photos and videos
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clipToBounds()
                    .pointerInput(item.id) {
                        detectTapGestures(
                            onDoubleTap = {
                                if (scale > 1.05f) {
                                    scale = 1f
                                    offsetX = 0f
                                    offsetY = 0f
                                } else {
                                    scale = 2.5f
                                    offsetX = 0f
                                    offsetY = 0f
                                }
                            },
                            onTap = {
                                showControls = !showControls
                            }
                        )
                    }
                    .pointerInput(item.id) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            val newScale = (scale * zoom).coerceIn(1f, 5f)
                            if (newScale <= 1.02f) {
                                scale = 1f
                                offsetX = 0f
                                offsetY = 0f
                            } else {
                                scale = newScale
                                val maxOffsetX = 800f * (newScale - 1f)
                                val maxOffsetY = 1200f * (newScale - 1f)
                                offsetX = (offsetX + pan.x).coerceIn(-maxOffsetX, maxOffsetX)
                                offsetY = (offsetY + pan.y).coerceIn(-maxOffsetY, maxOffsetY)
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = offsetX
                            translationY = offsetY
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (item.type == MediaType.VIDEO && exoPlayer != null) {
                        AndroidView(
                            factory = { ctx ->
                                PlayerView(ctx).apply {
                                    player = exoPlayer
                                    useController = false
                                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                                    setOnTouchListener { _, _ -> false }
                                }
                            },
                            update = { playerView ->
                                playerView.player = exoPlayer
                                playerView.setOnTouchListener { _, _ -> false }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        val context = LocalContext.current
                        val fullResRequest = remember(item.uriString) {
                            ImageRequest.Builder(context)
                                .data(item.uriString)
                                .size(Size.ORIGINAL)
                                .precision(Precision.EXACT)
                                .crossfade(true)
                                .allowHardware(true)
                                .build()
                        }
                        AsyncImage(
                            model = fullResRequest,
                            contentDescription = item.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }

            // Video Player Controls Overlay
            if (item.type == MediaType.VIDEO && exoPlayer != null) {
                AnimatedVisibility(
                    visible = showControls,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Prominent Center Video Controls: [-10s]  [PLAY/PAUSE]  [+10s]
                        Row(
                            modifier = Modifier.align(Alignment.Center),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(24.dp)
                        ) {
                            // Rewind 10s
                            Surface(
                                shape = CircleShape,
                                color = Color.Black.copy(alpha = 0.65f),
                                modifier = Modifier.size(54.dp)
                            ) {
                                IconButton(
                                    onClick = {
                                        val newPos = (exoPlayer.currentPosition - 10_000L).coerceAtLeast(0L)
                                        exoPlayer.seekTo(newPos)
                                    },
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Replay10,
                                        contentDescription = "Rewind 10 seconds",
                                        tint = Color.White,
                                        modifier = Modifier.size(30.dp)
                                    )
                                }
                            }

                            // Big, High-Contrast Play / Pause Button
                            Surface(
                                shape = CircleShape,
                                color = Color.Black.copy(alpha = 0.75f),
                                tonalElevation = 8.dp,
                                modifier = Modifier.size(86.dp)
                            ) {
                                IconButton(
                                    onClick = {
                                        if (exoPlayer.playbackState == Player.STATE_IDLE) {
                                            exoPlayer.prepare()
                                        }
                                        if (exoPlayer.playbackState == Player.STATE_ENDED || videoProgress >= 1f) {
                                            exoPlayer.seekTo(0)
                                            exoPlayer.playWhenReady = true
                                            isPlaying = true
                                            videoProgress = 0f
                                        } else {
                                            val nextPlay = !isPlaying
                                            isPlaying = nextPlay
                                            exoPlayer.playWhenReady = nextPlay
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .testTag("video_play_pause_button")
                                ) {
                                    if (isBuffering && isPlaying) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(36.dp),
                                            color = Color.White,
                                            strokeWidth = 3.5.dp
                                        )
                                    } else {
                                        Icon(
                                            imageVector = when {
                                                videoProgress >= 1f || exoPlayer.playbackState == Player.STATE_ENDED -> Icons.Default.Replay
                                                isPlaying -> Icons.Default.Pause
                                                else -> Icons.Default.PlayArrow
                                            },
                                            contentDescription = if (isPlaying) "Pause" else "Play",
                                            tint = Color.White,
                                            modifier = Modifier.size(48.dp)
                                        )
                                    }
                                }
                            }

                            // Forward 10s
                            Surface(
                                shape = CircleShape,
                                color = Color.Black.copy(alpha = 0.65f),
                                modifier = Modifier.size(54.dp)
                            ) {
                                IconButton(
                                    onClick = {
                                        val dur = exoPlayer.duration.takeIf { it > 0 } ?: Long.MAX_VALUE
                                        val newPos = (exoPlayer.currentPosition + 10_000L).coerceAtMost(dur)
                                        exoPlayer.seekTo(newPos)
                                    },
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Forward10,
                                        contentDescription = "Forward 10 seconds",
                                        tint = Color.White,
                                        modifier = Modifier.size(30.dp)
                                    )
                                }
                            }
                        }

                        // Video timeline progress bar at bottom of screen
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .padding(horizontal = 20.dp)
                                .padding(bottom = 76.dp)
                                .navigationBarsPadding()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = DateTimeUtils.formatVideoDuration(currentPositionSeconds),
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = DateTimeUtils.formatVideoDuration(totalDurationSeconds),
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Slider(
                                value = videoProgress,
                                onValueChange = { progress ->
                                    isScrubbing = true
                                    videoProgress = progress
                                    if (totalDurationSeconds > 0) {
                                        currentPositionSeconds = (progress * totalDurationSeconds).toInt()
                                    }
                                },
                                onValueChangeFinished = {
                                    if (exoPlayer.duration > 0) {
                                        val targetMs = (videoProgress * exoPlayer.duration).toLong()
                                        exoPlayer.seekTo(targetMs)
                                    }
                                    isScrubbing = false
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary,
                                    inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                                )
                            )
                        }
                    }
                }
            }

            // Left / Right Navigation Touch Targets
            AnimatedVisibility(
                visible = showControls,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center)
                    .padding(horizontal = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.35f),
                        modifier = Modifier.size(44.dp)
                    ) {
                        IconButton(
                            onClick = {
                                scale = 1f
                                offsetX = 0f
                                offsetY = 0f
                                onPrevious()
                            },
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChevronLeft,
                                contentDescription = "Previous photo",
                                tint = Color.White
                            )
                        }
                    }

                    Surface(
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.35f),
                        modifier = Modifier.size(44.dp)
                    ) {
                        IconButton(
                            onClick = {
                                scale = 1f
                                offsetX = 0f
                                offsetY = 0f
                                onNext()
                            },
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Next photo",
                                tint = Color.White
                            )
                        }
                    }
                }
            }

            // Floating Zoom Control HUD
            AnimatedVisibility(
                visible = showControls || scale > 1.05f,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(top = 68.dp, end = 16.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = Color.Black.copy(alpha = 0.72f),
                    tonalElevation = 6.dp
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        IconButton(
                            onClick = {
                                val newScale = (scale - 0.5f).coerceIn(1f, 5f)
                                scale = newScale
                                if (newScale <= 1.05f) {
                                    scale = 1f
                                    offsetX = 0f
                                    offsetY = 0f
                                }
                            },
                            enabled = scale > 1.05f,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ZoomOut,
                                contentDescription = "Zoom Out",
                                tint = if (scale > 1.05f) Color.White else Color.White.copy(alpha = 0.35f),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Text(
                            text = "${(scale * 100).toInt()}%",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .padding(horizontal = 6.dp)
                                .clickable {
                                    if (scale > 1.05f) {
                                        scale = 1f
                                        offsetX = 0f
                                        offsetY = 0f
                                    } else {
                                        scale = 2.5f
                                        offsetX = 0f
                                        offsetY = 0f
                                    }
                                }
                        )

                        IconButton(
                            onClick = {
                                val newScale = (scale + 0.5f).coerceIn(1f, 5f)
                                scale = newScale
                            },
                            enabled = scale < 5f,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ZoomIn,
                                contentDescription = "Zoom In",
                                tint = if (scale < 5f) Color.White else Color.White.copy(alpha = 0.35f),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        if (scale > 1.05f) {
                            IconButton(
                                onClick = {
                                    scale = 1f
                                    offsetX = 0f
                                    offsetY = 0f
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.RestartAlt,
                                    contentDescription = "Reset Zoom",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Top Toolbar Scrim & Action Bar
            AnimatedVisibility(
                visible = showControls,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Black.copy(alpha = 0.85f), Color.Transparent)
                            )
                        )
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(onClick = onClose) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Close viewer",
                                tint = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Column {
                            Text(
                                text = item.title,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = DateTimeUtils.formatShortDate(item.dateEpochMillis),
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 12.sp
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { onToggleFavorite(item) }) {
                            Icon(
                                imageVector = if (item.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (item.isFavorite) RoseFavorite else Color.White
                            )
                        }

                        IconButton(
                            onClick = { onAnalyzeMedia(item) },
                            modifier = Modifier.testTag("ai_analyze_button")
                        ) {
                            if (isAnalyzingTags) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "Analyze Media with AI",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        IconButton(onClick = { onEditMetadata(item) }) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Date and Location",
                                tint = Color.White
                            )
                        }

                        IconButton(
                            onClick = { showInfoSheet = !showInfoSheet },
                            modifier = Modifier.testTag("info_sheet_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Metadata Info & AI Tags",
                                tint = if (showInfoSheet) MaterialTheme.colorScheme.primary else Color.White
                            )
                        }

                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }
                }
            }
        }

        // Quick Tag Chips Overlay near Bottom (when controls are shown and info sheet is closed)
            AnimatedVisibility(
                visible = showControls && !showInfoSheet,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.BottomStart)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                            )
                        )
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .navigationBarsPadding()
                ) {
                    if (aiTaggingNotice != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = aiTaggingNotice,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // Active tags preview
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Label,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Tags:",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            if (item.tags.isEmpty()) {
                                Text(
                                    text = "No tags yet",
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 12.sp
                                )
                            } else {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    item.tags.take(3).forEach { tag ->
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)
                                        ) {
                                            Text(
                                                text = "#$tag",
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    if (item.tags.size > 3) {
                                        Text(
                                            text = "+${item.tags.size - 3} more",
                                            color = Color.White.copy(alpha = 0.8f),
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                    }
                                }
                            }
                        }

                        TextButton(
                            onClick = { showInfoSheet = true },
                            modifier = Modifier.testTag("manage_tags_button")
                        ) {
                            Text(
                                text = if (item.suggestedTags.isNotEmpty()) "Review AI Tags (${item.suggestedTags.size})" else "Manage Tags",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // If there are AI suggestions waiting, display a quick banner
                    if (item.suggestedTags.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Text(
                                text = "Suggested: ",
                                color = MaterialTheme.colorScheme.secondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                item.suggestedTags.take(2).forEach { sug ->
                                    AssistChip(
                                        onClick = { onAcceptTag(item.id, sug) },
                                        label = { Text(sug, fontSize = 10.sp) },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = "Add",
                                                modifier = Modifier.size(12.dp)
                                            )
                                        },
                                        colors = AssistChipDefaults.assistChipColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        modifier = Modifier.height(26.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Info Details & AI Tagging Bottom Sheet Overlay
            AnimatedVisibility(
                visible = showInfoSheet,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
            ) {
                Surface(
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Photo & Video Details",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TextButton(onClick = { onEditMetadata(item) }) {
                                    Text("Edit Info")
                                }
                                IconButton(onClick = { showInfoSheet = false }) {
                                    Icon(Icons.Default.Close, contentDescription = "Close Sheet")
                                }
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                        // AI TAGGING SECTION
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "AI Content Tagging",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Button(
                                onClick = { onAnalyzeMedia(item) },
                                enabled = !isAnalyzingTags,
                                modifier = Modifier.testTag("analyze_content_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            ) {
                                if (isAnalyzingTags) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = MaterialTheme.colorScheme.primary,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Analyzing...", fontSize = 12.sp)
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Detect Tags", fontSize = 12.sp)
                                }
                            }
                        }

                        // AI Suggested Tags Section
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "SUGGESTED TAGS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        if (item.suggestedTags.isEmpty()) {
                            Text(
                                text = "No pending suggestions. Tap \"Detect Tags\" to let AI inspect image and video context.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                        } else {
                            Text(
                                text = "Tap ✓ to accept or ✕ to reject:",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 2.dp, bottom = 6.dp)
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                item.suggestedTags.forEach { suggested ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier.testTag("suggested_tag_$suggested")
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(start = 10.dp, end = 4.dp, top = 4.dp, bottom = 4.dp)
                                        ) {
                                            Text(
                                                text = suggested,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            // Accept button
                                            IconButton(
                                                onClick = { onAcceptTag(item.id, suggested) },
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .testTag("accept_tag_$suggested")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = "Accept tag $suggested",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                            // Reject button
                                            IconButton(
                                                onClick = { onRejectTag(item.id, suggested) },
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .testTag("reject_tag_$suggested")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Reject tag $suggested",
                                                    tint = MaterialTheme.colorScheme.outline,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Current Accepted Tags Section
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ACTIVE TAGS (${item.tags.size})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            TextButton(
                                onClick = { showAddCustomTagDialog = true },
                                modifier = Modifier.testTag("add_custom_tag_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Custom Tag", fontSize = 12.sp)
                            }
                        }

                        if (item.tags.isEmpty()) {
                            Text(
                                text = "No active tags. Accept suggestions above or add your own custom tags.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                        } else {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                item.tags.forEach { tag ->
                                    InputChip(
                                        selected = true,
                                        onClick = { onRemoveTag(item.id, tag) },
                                        label = { Text("#$tag", fontSize = 12.sp, fontWeight = FontWeight.Medium) },
                                        trailingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Remove tag $tag",
                                                modifier = Modifier.size(14.dp)
                                            )
                                        },
                                        colors = InputChipDefaults.inputChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        ),
                                        modifier = Modifier.testTag("active_tag_$tag")
                                    )
                                }
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp))

                        // METADATA DETAILS (Date, Location, Format)
                        // Date & Time Row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CalendarToday,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Date & Time",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = DateTimeUtils.formatFullDateTime(item.dateEpochMillis),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        // Location Row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Location",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = item.locationName,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (item.latitude != null && item.longitude != null) {
                                    Text(
                                        text = String.format("%.4f° N, %.4f° W", item.latitude, item.longitude),
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Type and Specifications Row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = if (item.type == MediaType.PHOTO) Icons.Outlined.Photo else Icons.Outlined.Videocam,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Format & Quality",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = buildString {
                                        append(if (item.type == MediaType.PHOTO) "Photo" else "Video")
                                        if (item.type == MediaType.VIDEO && item.durationSeconds > 0) {
                                            append(" (${DateTimeUtils.formatVideoDuration(item.durationSeconds)})")
                                        }
                                        if (item.resolution.isNotBlank()) {
                                            append(" • ${item.resolution}")
                                        }
                                    },
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        if (item.notes.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Notes: ${item.notes}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    // Custom Tag Dialog
    if (showAddCustomTagDialog) {
        AlertDialog(
            onDismissRequest = {
                showAddCustomTagDialog = false
                customTagInput = ""
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Label,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Custom Tag")
                }
            },
            text = {
                Column {
                    Text(
                        text = "Enter a custom tag for this media:",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = customTagInput,
                        onValueChange = { customTagInput = it },
                        label = { Text("Tag Name") },
                        placeholder = { Text("e.g. Vacation, Family, Architecture") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_tag_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (customTagInput.isNotBlank()) {
                            onAddCustomTag(item.id, customTagInput)
                            customTagInput = ""
                            showAddCustomTagDialog = false
                        }
                    },
                    modifier = Modifier.testTag("submit_custom_tag_button")
                ) {
                    Text("Add Tag")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAddCustomTagDialog = false
                        customTagInput = ""
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Media") },
            text = { Text("Are you sure you want to remove \"${item.title}\" from your gallery?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteItem(item.id)
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
