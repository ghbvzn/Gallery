package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * High-performance fast scrollbar for LazyListState.
 * Zero recomposition during scrolling by separating state reads to layout phase lambda.
 */
@Composable
fun FastListScrollbar(
    listState: LazyListState,
    totalItems: Int,
    modifier: Modifier = Modifier,
    labelProvider: ((Int) -> String)? = null
) {
    if (totalItems <= 2) return

    val coroutineScope = rememberCoroutineScope()
    var scrollJob by remember { mutableStateOf<Job?>(null) }

    FastScrollbarUI(
        progressProvider = {
            if (totalItems <= 1) 0f
            else (listState.firstVisibleItemIndex.toFloat() / (totalItems - 1)).coerceIn(0f, 1f)
        },
        totalItems = totalItems,
        onScrollToIndex = { targetIndex ->
            scrollJob?.cancel()
            scrollJob = coroutineScope.launch {
                listState.scrollToItem(targetIndex)
            }
        },
        onScrollToTop = {
            scrollJob?.cancel()
            scrollJob = coroutineScope.launch {
                if (listState.firstVisibleItemIndex > 15) {
                    listState.scrollToItem(15)
                }
                listState.animateScrollToItem(0)
            }
        },
        onScrollToBottom = {
            scrollJob?.cancel()
            scrollJob = coroutineScope.launch {
                val last = (totalItems - 1).coerceAtLeast(0)
                if (last - listState.firstVisibleItemIndex > 15) {
                    listState.scrollToItem((last - 15).coerceAtLeast(0))
                }
                listState.animateScrollToItem(last)
            }
        },
        labelProvider = labelProvider,
        modifier = modifier
    )
}

/**
 * High-performance fast scrollbar for LazyGridState.
 * Zero recomposition during scrolling by separating state reads to layout phase lambda.
 */
@Composable
fun FastGridScrollbar(
    gridState: LazyGridState,
    totalItems: Int,
    modifier: Modifier = Modifier,
    labelProvider: ((Int) -> String)? = null
) {
    if (totalItems <= 4) return

    val coroutineScope = rememberCoroutineScope()
    var scrollJob by remember { mutableStateOf<Job?>(null) }

    FastScrollbarUI(
        progressProvider = {
            if (totalItems <= 1) 0f
            else (gridState.firstVisibleItemIndex.toFloat() / (totalItems - 1)).coerceIn(0f, 1f)
        },
        totalItems = totalItems,
        onScrollToIndex = { targetIndex ->
            scrollJob?.cancel()
            scrollJob = coroutineScope.launch {
                gridState.scrollToItem(targetIndex)
            }
        },
        onScrollToTop = {
            scrollJob?.cancel()
            scrollJob = coroutineScope.launch {
                if (gridState.firstVisibleItemIndex > 15) {
                    gridState.scrollToItem(15)
                }
                gridState.animateScrollToItem(0)
            }
        },
        onScrollToBottom = {
            scrollJob?.cancel()
            scrollJob = coroutineScope.launch {
                val last = (totalItems - 1).coerceAtLeast(0)
                if (last - gridState.firstVisibleItemIndex > 15) {
                    gridState.scrollToItem((last - 15).coerceAtLeast(0))
                }
                gridState.animateScrollToItem(last)
            }
        },
        labelProvider = labelProvider,
        modifier = modifier
    )
}

@Composable
private fun FastScrollbarUI(
    progressProvider: () -> Float,
    totalItems: Int,
    onScrollToIndex: (Int) -> Unit,
    onScrollToTop: () -> Unit,
    onScrollToBottom: () -> Unit,
    labelProvider: ((Int) -> String)?,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    var isDragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }
    var dragItemIndex by remember { mutableIntStateOf(0) }
    var lastScrolledIndex by remember { mutableIntStateOf(-1) }
    var trackHeightPx by remember { mutableFloatStateOf(0f) }

    val thumbHeightDp = 44.dp
    val thumbHeightPx = with(density) { thumbHeightDp.toPx() }

    Box(
        modifier = modifier.testTag("fast_scrollbar_container")
    ) {
        // Floating live section tooltip badge when actively dragging
        AnimatedVisibility(
            visible = isDragging,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            val badgeText = labelProvider?.invoke(dragItemIndex) ?: "Item ${dragItemIndex + 1} of $totalItems"

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                tonalElevation = 6.dp,
                shadowElevation = 4.dp,
                modifier = Modifier
                    .padding(end = 48.dp)
                    .testTag("scrollbar_drag_tooltip")
            ) {
                Text(
                    text = badgeText,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                )
            }
        }

        // Main Scrollbar Rail Container
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f),
            tonalElevation = 3.dp,
            shadowElevation = 3.dp,
            modifier = Modifier
                .width(32.dp)
                .fillMaxHeight(0.64f)
                .align(Alignment.CenterEnd)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 2.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Quick "Go to Top" button
                IconButton(
                    onClick = onScrollToTop,
                    modifier = Modifier
                        .size(28.dp)
                        .testTag("scrollbar_scroll_to_top")
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowUpward,
                        contentDescription = "Scroll to top",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Vertical Track Box with Draggable Thumb
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .width(28.dp)
                        .onSizeChanged { size ->
                            trackHeightPx = size.height.toFloat()
                        }
                        .pointerInput(totalItems) {
                            detectTapGestures { tapOffset ->
                                if (trackHeightPx > 0) {
                                    val fraction = (tapOffset.y / trackHeightPx).coerceIn(0f, 1f)
                                    val targetIndex = (fraction * (totalItems - 1)).roundToInt().coerceIn(0, totalItems - 1)
                                    onScrollToIndex(targetIndex)
                                }
                            }
                        }
                        .pointerInput(totalItems) {
                            detectVerticalDragGestures(
                                onDragStart = { offset ->
                                    isDragging = true
                                    if (trackHeightPx > 0) {
                                        val fraction = (offset.y / trackHeightPx).coerceIn(0f, 1f)
                                        dragFraction = fraction
                                        val targetIndex = (fraction * (totalItems - 1)).roundToInt().coerceIn(0, totalItems - 1)
                                        dragItemIndex = targetIndex
                                        lastScrolledIndex = targetIndex
                                        onScrollToIndex(targetIndex)
                                    }
                                },
                                onDragEnd = { isDragging = false },
                                onDragCancel = { isDragging = false },
                                onVerticalDrag = { change, _ ->
                                    change.consume()
                                    if (trackHeightPx > 0) {
                                        val currentY = change.position.y
                                        val fraction = (currentY / trackHeightPx).coerceIn(0f, 1f)
                                        dragFraction = fraction
                                        val targetIndex = (fraction * (totalItems - 1)).roundToInt().coerceIn(0, totalItems - 1)
                                        dragItemIndex = targetIndex
                                        if (targetIndex != lastScrolledIndex) {
                                            lastScrolledIndex = targetIndex
                                            onScrollToIndex(targetIndex)
                                        }
                                    }
                                }
                            )
                        },
                    contentAlignment = Alignment.TopCenter
                ) {
                    // Track Line
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(2.dp))
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
                    )

                    // Draggable Thumb Pill with Layout Phase Offset Lambda
                    // Reading fraction inside offset { ... } completely bypasses recomposition during scrolls!
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isDragging) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.88f),
                        tonalElevation = if (isDragging) 6.dp else 2.dp,
                        shadowElevation = if (isDragging) 4.dp else 1.dp,
                        modifier = Modifier
                            .offset {
                                val fraction = if (isDragging) dragFraction else progressProvider()
                                val maxOffsetPx = (trackHeightPx - thumbHeightPx).coerceAtLeast(0f)
                                IntOffset(x = 0, y = (fraction * maxOffsetPx).roundToInt())
                            }
                            .size(width = 22.dp, height = thumbHeightDp)
                            .testTag("scrollbar_thumb")
                    ) {
                        // Grip lines inside thumb
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(9.dp)
                                    .height(2.dp)
                                    .clip(RoundedCornerShape(1.dp))
                                    .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f))
                            )
                            Box(
                                modifier = Modifier
                                    .padding(top = 3.dp)
                                    .width(9.dp)
                                    .height(2.dp)
                                    .clip(RoundedCornerShape(1.dp))
                                    .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f))
                            )
                        }
                    }
                }

                // Quick "Go to Bottom" button
                IconButton(
                    onClick = onScrollToBottom,
                    modifier = Modifier
                        .size(28.dp)
                        .testTag("scrollbar_scroll_to_bottom")
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowDownward,
                        contentDescription = "Scroll to bottom",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
