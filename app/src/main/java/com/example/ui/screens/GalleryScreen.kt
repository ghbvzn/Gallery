package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Deselect
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PermMedia
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Today
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Photo
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.MediaType
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.outlined.Settings
import com.example.ui.Album
import com.example.ui.GalleryUiState
import com.example.ui.GalleryViewMode
import com.example.ui.GalleryViewModel
import com.example.ui.MediaTypeFilter
import com.example.ui.SortOrder
import com.example.ui.components.AddMediaDialog
import com.example.ui.components.AlbumCard
import com.example.ui.components.EditMetadataDialog
import com.example.ui.components.FastGridScrollbar
import com.example.ui.components.FastListScrollbar
import com.example.ui.components.LocationCard
import com.example.ui.components.MediaCard
import com.example.ui.components.MediaDetailViewer
import com.example.ui.components.SettingsDialog
import com.example.ui.components.TimelineHeader
import com.example.ui.theme.RoseFavorite
import com.example.ui.util.DateTimeUtils

/**
 * Natural two-finger pinch-to-zoom modifier.
 * Allows user to pinch inward to zoom out (more columns) or pinch outward to zoom in (fewer columns).
 * Never intercepts single-finger scrolling!
 */
fun Modifier.pinchToZoomGrid(
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit
): Modifier = this.pointerInput(Unit) {
    awaitEachGesture {
        var totalZoom = 1f
        do {
            val event = awaitPointerEvent()
            if (event.changes.size >= 2) {
                val zoom = event.calculateZoom()
                totalZoom *= zoom
                if (totalZoom > 1.35f) {
                    onZoomIn()
                    totalZoom = 1f
                } else if (totalZoom < 0.74f) {
                    onZoomOut()
                    totalZoom = 1f
                }
            }
        } while (event.changes.any { it.pressed })
    }
}

private fun shareMediaItems(context: Context, items: List<com.example.data.MediaItem>) {
    if (items.isEmpty()) return
    val uris = ArrayList<Uri>()
    items.forEach { item ->
        try {
            uris.add(Uri.parse(item.uriString))
        } catch (_: Exception) {}
    }
    if (uris.isEmpty()) return

    val intent = if (uris.size == 1) {
        Intent(Intent.ACTION_SEND).apply {
            val item = items.first()
            type = if (item.type == MediaType.VIDEO) "video/*" else "image/*"
            putExtra(Intent.EXTRA_STREAM, uris.first())
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    } else {
        Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "*/*"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
    try {
        context.startActivity(Intent.createChooser(intent, "Share ${items.size} item${if (items.size > 1) "s" else ""}"))
    } catch (_: Exception) {}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    viewModel: GalleryViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val mediaPermissions = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO
            )
        } else {
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
        }
    }

    fun checkPermissionsGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val hasImages = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
            val hasVideos = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED
            val hasPartial = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) == PackageManager.PERMISSION_GRANTED
            hasImages || hasVideos || hasPartial
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasImages = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
            val hasVideos = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED
            hasImages || hasVideos
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsMap ->
        val granted = permissionsMap.values.any { it }
        viewModel.onPermissionResult(granted)
    }

    // Ask for media permissions early on
    LaunchedEffect(Unit) {
        if (checkPermissionsGranted()) {
            viewModel.onPermissionResult(true)
        } else {
            viewModel.setPermissionRequested()
            permissionLauncher.launch(mediaPermissions)
        }
    }

    // Back Handler: return to album list if an album is selected
    BackHandler(enabled = uiState.selectedAlbum != null && !uiState.isSelectionMode) {
        viewModel.selectAlbum(null)
    }

    // Back Handler: exit selection mode if active
    BackHandler(enabled = uiState.isSelectionMode) {
        viewModel.clearSelection()
    }

    var showBatchDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
            ) {
                if (uiState.isSelectionMode) {
                    TopAppBar(
                        navigationIcon = {
                            IconButton(
                                onClick = { viewModel.clearSelection() },
                                modifier = Modifier.testTag("exit_selection_mode_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Exit selection mode"
                                )
                            }
                        },
                        title = {
                            Text(
                                text = "${uiState.selectedItemIds.size} selected",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.testTag("selection_count_label")
                            )
                        },
                        actions = {
                            val allSelected = uiState.filteredMedia.isNotEmpty() &&
                                    uiState.filteredMedia.all { it.id in uiState.selectedItemIds }

                            // Select All / Deselect All
                            IconButton(
                                onClick = {
                                    if (allSelected) {
                                        viewModel.clearSelection()
                                    } else {
                                        viewModel.selectAll()
                                    }
                                },
                                modifier = Modifier.testTag("select_all_toggle_button")
                            ) {
                                Icon(
                                    imageVector = if (allSelected) Icons.Default.Deselect else Icons.Default.SelectAll,
                                    contentDescription = if (allSelected) "Deselect all" else "Select all"
                                )
                            }

                            // Batch Favorite
                            val selectedItems = remember(uiState.selectedItemIds, uiState.allMedia) {
                                uiState.allMedia.filter { it.id in uiState.selectedItemIds }
                            }
                            val anyNotFav = selectedItems.any { !it.isFavorite }
                            IconButton(
                                onClick = { viewModel.toggleFavoriteSelected() },
                                modifier = Modifier.testTag("batch_favorite_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Favorite,
                                    contentDescription = if (anyNotFav) "Favorite selected" else "Unfavorite selected",
                                    tint = if (anyNotFav) MaterialTheme.colorScheme.onSurface else RoseFavorite
                                )
                            }

                            // Batch Share
                            IconButton(
                                onClick = { shareMediaItems(context, selectedItems) },
                                modifier = Modifier.testTag("batch_share_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Share selected"
                                )
                            }

                            // Batch Delete
                            IconButton(
                                onClick = { showBatchDeleteDialog = true },
                                modifier = Modifier.testTag("batch_delete_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete selected",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                } else {
                    TopAppBar(
                        title = {
                            Column {
                                Text(
                                    text = "Gallery",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                val totalItems = uiState.allMedia.size
                                val albumsCount = uiState.albums.size
                                val locationsCount = uiState.availableLocations.size
                                Text(
                                    text = "$totalItems memories • $albumsCount albums • $locationsCount places",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        actions = {
                            // Refresh Device Media Button
                            IconButton(
                                onClick = {
                                    if (checkPermissionsGranted()) {
                                        viewModel.refreshDeviceMedia()
                                    } else {
                                        permissionLauncher.launch(mediaPermissions)
                                    }
                                },
                                modifier = Modifier.testTag("refresh_media_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Refresh Device Media"
                                )
                            }

                            // Search Button
                            IconButton(
                                onClick = { viewModel.toggleSearch() },
                                modifier = Modifier.testTag("toggle_search_button")
                            ) {
                                Icon(
                                    imageVector = if (uiState.isSearching) Icons.Default.Close else Icons.Default.Search,
                                    contentDescription = "Search"
                                )
                            }

                            // Sort Order Toggle Button
                            IconButton(
                                onClick = { viewModel.toggleSortOrder() },
                                modifier = Modifier.testTag("toggle_sort_button")
                            ) {
                                Icon(
                                    imageVector = if (uiState.sortOrder == SortOrder.NEWEST_FIRST)
                                        Icons.Default.ArrowDownward
                                    else
                                        Icons.Default.ArrowUpward,
                                    contentDescription = "Sort Order: ${uiState.sortOrder.name}"
                                )
                            }

                            // Settings Menu Button
                            IconButton(
                                onClick = { viewModel.setSettingsDialogVisible(true) },
                                modifier = Modifier.testTag("settings_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Settings,
                                    contentDescription = "Settings"
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                }

                // Scanning Progress Indicator
                if (uiState.isLoadingMedia) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("media_loading_indicator")
                    )
                }

                if (!uiState.isSelectionMode) {
                    // Search Bar Input (when active)
                    AnimatedVisibility(
                        visible = uiState.isSearching,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            OutlinedTextField(
                                value = uiState.searchQuery,
                                onValueChange = { viewModel.setSearchQuery(it) },
                                placeholder = { Text("Search by title, place, or notes...") },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                trailingIcon = {
                                    if (uiState.searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                                        }
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("search_text_input")
                            )
                        }
                    }

                    // Filter Chips Row (Media type & Active filters)
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // If a location filter is active, show clearable chip first
                        if (uiState.selectedLocationFilter != null) {
                            item {
                                InputChip(
                                    selected = true,
                                    onClick = { viewModel.setSelectedLocationFilter(null) },
                                    label = { Text("Place: ${uiState.selectedLocationFilter}") },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.LocationOn,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    trailingIcon = {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Clear location filter",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                        }

                        // Media Type Filters
                        item {
                            FilterChip(
                                selected = uiState.typeFilter == MediaTypeFilter.ALL,
                                onClick = { viewModel.setMediaTypeFilter(MediaTypeFilter.ALL) },
                                label = { Text("All") },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("filter_all")
                            )
                        }

                        item {
                            FilterChip(
                                selected = uiState.typeFilter == MediaTypeFilter.PHOTOS,
                                onClick = { viewModel.setMediaTypeFilter(MediaTypeFilter.PHOTOS) },
                                label = { Text("Photos") },
                                leadingIcon = {
                                    Icon(
                                        Icons.Outlined.Photo,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("filter_photos")
                            )
                        }

                        item {
                            FilterChip(
                                selected = uiState.typeFilter == MediaTypeFilter.VIDEOS,
                                onClick = { viewModel.setMediaTypeFilter(MediaTypeFilter.VIDEOS) },
                                label = { Text("Videos") },
                                leadingIcon = {
                                    Icon(
                                        Icons.Outlined.Videocam,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("filter_videos")
                            )
                        }

                        item {
                            FilterChip(
                                selected = uiState.typeFilter == MediaTypeFilter.FAVORITES,
                                onClick = { viewModel.setMediaTypeFilter(MediaTypeFilter.FAVORITES) },
                                label = { Text("Favorites") },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Favorite,
                                        contentDescription = null,
                                        tint = RoseFavorite,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("filter_favorites")
                            )
                        }

                        // Active tag filter clearable chip
                        if (uiState.selectedTagFilter != null) {
                            item {
                                InputChip(
                                    selected = true,
                                    onClick = { viewModel.setSelectedTagFilter(null) },
                                    label = { Text("#${uiState.selectedTagFilter}") },
                                    trailingIcon = {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Clear tag filter",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                        }

                        // Popular tags quick filter
                        uiState.availableTags.take(8).forEach { tag ->
                            if (tag != uiState.selectedTagFilter) {
                                item {
                                    FilterChip(
                                        selected = false,
                                        onClick = { viewModel.setSelectedTagFilter(tag) },
                                        label = { Text("#$tag") },
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            if (!uiState.isSelectionMode) {
                // Material 3 Navigation Bar for View Modes: By Date (Timeline), By Location (Places), All (Grid)
                NavigationBar(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("bottom_navigation_bar")
                ) {
                    NavigationBarItem(
                        selected = uiState.viewMode == GalleryViewMode.TIMELINE,
                        onClick = { viewModel.selectViewMode(GalleryViewMode.TIMELINE) },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Today,
                                contentDescription = "Timeline by Date"
                            )
                        },
                        label = { Text("Photos") },
                        modifier = Modifier.testTag("nav_item_timeline")
                    )

                    NavigationBarItem(
                        selected = uiState.viewMode == GalleryViewMode.ALBUMS,
                        onClick = { viewModel.selectViewMode(GalleryViewMode.ALBUMS) },
                        icon = {
                            BadgedBox(badge = {
                                if (uiState.albums.isNotEmpty()) {
                                    Badge { Text("${uiState.albums.size}") }
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.Default.CollectionsBookmark,
                                    contentDescription = "Albums"
                                )
                            }
                        },
                        label = { Text("Albums") },
                        modifier = Modifier.testTag("nav_item_albums")
                    )

                    NavigationBarItem(
                        selected = uiState.viewMode == GalleryViewMode.PLACES,
                        onClick = { viewModel.selectViewMode(GalleryViewMode.PLACES) },
                        icon = {
                            BadgedBox(badge = {
                                if (uiState.availableLocations.isNotEmpty()) {
                                    Badge { Text("${uiState.availableLocations.size}") }
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = "Places by Location"
                                )
                            }
                        },
                        label = { Text("By Location") },
                        modifier = Modifier.testTag("nav_item_places")
                    )

                    NavigationBarItem(
                        selected = uiState.viewMode == GalleryViewMode.GRID,
                        onClick = { viewModel.selectViewMode(GalleryViewMode.GRID) },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.GridView,
                                contentDescription = "All Media Grid"
                            )
                        },
                        label = { Text("Grid") },
                        modifier = Modifier.testTag("nav_item_grid")
                    )
                }
            }
        },
        floatingActionButton = {
            if (!uiState.isSelectionMode) {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.showAddDialog(true) },
                    icon = { Icon(Icons.Default.Add, contentDescription = "Add Media") },
                    text = { Text("Add Media") },
                    modifier = Modifier
                        .navigationBarsPadding()
                        .testTag("add_media_fab")
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Permission Banner (when permission is not granted)
            if (!uiState.hasMediaPermission) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .testTag("media_permission_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PermMedia,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Media Access Required",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Grant permission to load photos & videos from your device.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { permissionLauncher.launch(mediaPermissions) },
                            modifier = Modifier.testTag("grant_permission_banner_button")
                        ) {
                            Text("Grant", fontSize = 12.sp)
                        }
                    }
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                when {
                    // Albums View: Overview or Album Detail
                    uiState.viewMode == GalleryViewMode.ALBUMS -> {
                        val currentAlbum = uiState.selectedAlbum
                        if (currentAlbum != null) {
                            AlbumDetailContent(
                                album = currentAlbum,
                                uiState = uiState,
                                onBack = { viewModel.selectAlbum(null) },
                                onMediaClick = { viewModel.openDetailViewer(it) },
                                onMediaLongClick = { viewModel.toggleSelection(it.id) },
                                onToggleFavorite = { viewModel.toggleFavorite(it) },
                                onZoomIn = { viewModel.zoomInGrid() },
                                onZoomOut = { viewModel.zoomOutGrid() }
                            )
                        } else {
                            AlbumsContent(
                                albums = uiState.albums,
                                hasMediaPermission = uiState.hasMediaPermission,
                                onGrantPermission = { permissionLauncher.launch(mediaPermissions) },
                                onRefreshMedia = { viewModel.refreshDeviceMedia() },
                                onSelectAlbum = { viewModel.selectAlbum(it) }
                            )
                        }
                    }

                    // Places View: Grouped by Location
                    uiState.viewMode == GalleryViewMode.PLACES && uiState.selectedLocationFilter == null -> {
                        PlacesContent(
                            locationGroups = uiState.locationGroups,
                            hasMediaPermission = uiState.hasMediaPermission,
                            onGrantPermission = { permissionLauncher.launch(mediaPermissions) },
                            onRefreshMedia = { viewModel.refreshDeviceMedia() },
                            onSelectLocation = { location ->
                                viewModel.setSelectedLocationFilter(location)
                                viewModel.selectViewMode(GalleryViewMode.TIMELINE)
                            }
                        )
                    }

                    // Timeline View: Grouped by Date
                    uiState.viewMode == GalleryViewMode.TIMELINE -> {
                        TimelineContent(
                            uiState = uiState,
                            onGrantPermission = { permissionLauncher.launch(mediaPermissions) },
                            onRefreshMedia = { viewModel.refreshDeviceMedia() },
                            onMediaClick = { viewModel.openDetailViewer(it) },
                            onMediaLongClick = { viewModel.toggleSelection(it.id) },
                            onToggleFavorite = { viewModel.toggleFavorite(it) },
                            onLocationFilter = { viewModel.setSelectedLocationFilter(it) },
                            onZoomIn = { viewModel.zoomInGrid() },
                            onZoomOut = { viewModel.zoomOutGrid() }
                        )
                    }

                    // Grid View: All Media
                    else -> {
                        GridContent(
                            uiState = uiState,
                            hasMediaPermission = uiState.hasMediaPermission,
                            onGrantPermission = { permissionLauncher.launch(mediaPermissions) },
                            onRefreshMedia = { viewModel.refreshDeviceMedia() },
                            onMediaClick = { viewModel.openDetailViewer(it) },
                            onMediaLongClick = { viewModel.toggleSelection(it.id) },
                            onToggleFavorite = { viewModel.toggleFavorite(it) },
                            onZoomIn = { viewModel.zoomInGrid() },
                            onZoomOut = { viewModel.zoomOutGrid() }
                        )
                    }
                }
            }
        }
    }

    // Detail Fullscreen Viewer
    uiState.activeItem?.let { active ->
        MediaDetailViewer(
            item = active,
            isAnalyzingTags = uiState.isAnalyzingTags,
            aiTaggingNotice = uiState.aiTaggingNotice,
            onClose = { viewModel.closeDetailViewer() },
            onToggleFavorite = { viewModel.toggleFavorite(it) },
            onEditMetadata = { viewModel.startEditing(it) },
            onDeleteItem = { viewModel.deleteItem(it) },
            onPrevious = { viewModel.previousItem() },
            onNext = { viewModel.nextItem() },
            onAnalyzeMedia = { viewModel.analyzeMediaForTags(it) },
            onAcceptTag = { itemId, tag -> viewModel.acceptTag(itemId, tag) },
            onRejectTag = { itemId, tag -> viewModel.rejectTag(itemId, tag) },
            onAddCustomTag = { itemId, tag -> viewModel.addCustomTag(itemId, tag) },
            onRemoveTag = { itemId, tag -> viewModel.removeTag(itemId, tag) }
        )
    }

    // Add Media Dialog
    if (uiState.showAddDialog) {
        AddMediaDialog(
            availableLocations = uiState.availableLocations,
            availableTags = uiState.availableTags,
            onDismiss = { viewModel.showAddDialog(false) },
            onAddMedia = { title, uriString, type, location, dateMillis, duration, res, notes, tags ->
                viewModel.addMedia(title, uriString, type, location, dateMillis, duration, res, notes, tags)
            }
        )
    }

    // Edit Metadata Dialog
    uiState.editingItem?.let { editing ->
        EditMetadataDialog(
            item = editing,
            availableLocations = uiState.availableLocations,
            onDismiss = { viewModel.stopEditing() },
            onSave = { id, title, location, dateMillis ->
                viewModel.updateMetadata(id, title, location, dateMillis)
            }
        )
    }

    // Settings Dialog
    if (uiState.showSettingsDialog) {
        SettingsDialog(
            uiState = uiState,
            onDismiss = { viewModel.setSettingsDialogVisible(false) },
            onSetGridColumns = { viewModel.setGridColumns(it) },
            onToggleVideoBadges = { viewModel.setShowVideoDurationBadge(it) },
            onSetThemeMode = { viewModel.setThemeMode(it) },
            onRefreshMedia = { viewModel.refreshDeviceMedia() },
            onRequestPermission = { permissionLauncher.launch(mediaPermissions) }
        )
    }

    // Batch Delete Confirmation Dialog
    if (showBatchDeleteDialog) {
        val count = uiState.selectedItemIds.size
        AlertDialog(
            onDismissRequest = { showBatchDeleteDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = {
                Text("Delete $count ${if (count == 1) "item" else "items"}?")
            },
            text = {
                Text("Are you sure you want to delete the selected ${if (count == 1) "memory" else "memories"}? This will permanently remove them from the gallery.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteSelectedItems()
                        showBatchDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_batch_delete_button")
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showBatchDeleteDialog = false },
                    modifier = Modifier.testTag("cancel_batch_delete_button")
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun TimelineContent(
    uiState: GalleryUiState,
    onGrantPermission: () -> Unit,
    onRefreshMedia: () -> Unit,
    onMediaClick: (com.example.data.MediaItem) -> Unit,
    onMediaLongClick: (com.example.data.MediaItem) -> Unit = {},
    onToggleFavorite: (com.example.data.MediaItem) -> Unit,
    onLocationFilter: (String) -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit
) {
    if (uiState.filteredMedia.isEmpty()) {
        when {
            !uiState.hasMediaPermission -> {
                EmptyGalleryState(
                    title = "Media Access Required",
                    message = "Allow media permission so the gallery can load and display your photos and videos.",
                    actionText = "Grant Permission",
                    onAction = onGrantPermission
                )
            }
            uiState.allMedia.isEmpty() -> {
                EmptyGalleryState(
                    title = "No Photos or Videos Found",
                    message = "No photos or videos detected on your device yet. Take a new picture or tap refresh.",
                    actionText = "Refresh Device Media",
                    onAction = onRefreshMedia
                )
            }
            else -> {
                EmptyGalleryState(
                    title = "No Matching Memories",
                    message = if (uiState.searchQuery.isNotBlank() || uiState.selectedLocationFilter != null || uiState.selectedTagFilter != null)
                        "No photos or videos matched your search or filters."
                    else
                        "No media found in your gallery. Tap '+ Add Media' to add memories!"
                )
            }
        }
        return
    }

    val gridState = rememberLazyGridState()
    val totalItems = remember(uiState.dateGroups) {
        uiState.dateGroups.sumOf { 1 + it.items.size }
    }

    val indexToGroupTitle = remember(uiState.dateGroups) {
        val titles = ArrayList<String>()
        for (group in uiState.dateGroups) {
            titles.add(group.groupTitle)
            for (item in group.items) {
                titles.add(group.groupTitle)
            }
        }
        titles
    }

    val cellSpacing = when {
        uiState.gridColumns >= 5 -> 3.dp
        uiState.gridColumns == 4 -> 4.dp
        uiState.gridColumns == 3 -> 6.dp
        else -> 8.dp
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Fixed(uiState.gridColumns),
            modifier = Modifier
                .fillMaxSize()
                .pinchToZoomGrid(onZoomIn = onZoomIn, onZoomOut = onZoomOut),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 96.dp),
            horizontalArrangement = Arrangement.spacedBy(cellSpacing),
            verticalArrangement = Arrangement.spacedBy(cellSpacing)
        ) {
            uiState.dateGroups.forEach { dateGroup ->
                item(
                    key = "header_${dateGroup.groupTitle}",
                    span = { GridItemSpan(maxLineSpan) }
                ) {
                    TimelineHeader(
                        dateGroup = dateGroup,
                        onLocationClick = onLocationFilter
                    )
                }

                items(
                    items = dateGroup.items,
                    key = { it.id }
                ) { item ->
                    MediaCard(
                        item = item,
                        gridColumns = uiState.gridColumns,
                        showVideoDurationBadge = uiState.showVideoDurationBadge,
                        isSelected = item.id in uiState.selectedItemIds,
                        isSelectionMode = uiState.isSelectionMode,
                        onClick = {
                            if (uiState.isSelectionMode) {
                                onMediaLongClick(item)
                            } else {
                                onMediaClick(item)
                            }
                        },
                        onLongClick = { onMediaLongClick(item) },
                        onToggleFavorite = { onToggleFavorite(item) }
                    )
                }
            }
        }

        FastGridScrollbar(
            gridState = gridState,
            totalItems = totalItems,
            labelProvider = { index ->
                indexToGroupTitle.getOrElse(index) { "" }
            },
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 6.dp, top = 12.dp, bottom = 84.dp)
        )
    }
}

@Composable
private fun PlacesContent(
    locationGroups: List<com.example.ui.LocationGroup>,
    hasMediaPermission: Boolean,
    onGrantPermission: () -> Unit,
    onRefreshMedia: () -> Unit,
    onSelectLocation: (String) -> Unit
) {
    if (locationGroups.isEmpty()) {
        if (!hasMediaPermission) {
            EmptyGalleryState(
                title = "Media Access Required",
                message = "Allow media permission so photos and videos can be organized by location.",
                actionText = "Grant Permission",
                onAction = onGrantPermission
            )
        } else {
            EmptyGalleryState(
                title = "No Places Organized",
                message = "No organized locations yet. Photos with location details will be grouped here.",
                actionText = "Refresh Device Media",
                onAction = onRefreshMedia
            )
        }
        return
    }

    val listState = rememberLazyListState()
    val totalItems = 1 + locationGroups.size

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Explore,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Places & Locations",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Browse your photo and video collection grouped by travel destinations and places.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            items(locationGroups, key = { it.locationName }) { group ->
                LocationCard(
                    group = group,
                    onClick = { onSelectLocation(group.locationName) }
                )
            }
        }

        FastListScrollbar(
            listState = listState,
            totalItems = totalItems,
            labelProvider = { index ->
                if (index == 0) "Top"
                else locationGroups.getOrNull(index - 1)?.locationName ?: ""
            },
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 6.dp, top = 12.dp, bottom = 84.dp)
        )
    }
}

@Composable
private fun AlbumsContent(
    albums: List<Album>,
    hasMediaPermission: Boolean,
    onGrantPermission: () -> Unit,
    onRefreshMedia: () -> Unit,
    onSelectAlbum: (Album) -> Unit
) {
    if (albums.isEmpty()) {
        if (!hasMediaPermission) {
            EmptyGalleryState(
                title = "Media Access Required",
                message = "Allow media access so your albums and collections can be displayed.",
                actionText = "Grant Permission",
                onAction = onGrantPermission
            )
        } else {
            EmptyGalleryState(
                title = "No Albums Found",
                message = "Photos and videos on your device will automatically appear in albums here.",
                actionText = "Refresh Albums",
                onAction = onRefreshMedia
            )
        }
        return
    }

    val gridState = rememberLazyGridState()
    val smartAlbums = remember(albums) { albums.filter { it.isSmartAlbum } }
    val regularAlbums = remember(albums) { albums.filter { !it.isSmartAlbum } }

    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Adaptive(minSize = 160.dp),
        modifier = Modifier
            .fillMaxSize()
            .testTag("albums_overview_grid"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Hero Header Card
        item(span = { GridItemSpan(maxLineSpan) }) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CollectionsBookmark,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "Albums & Collections",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${albums.size} albums • Favorites, videos, and on-device folders",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Section: Smart Collections
        if (smartAlbums.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = "Smart Collections",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                )
            }

            items(smartAlbums, key = { it.id }) { album ->
                AlbumCard(
                    album = album,
                    onClick = { onSelectAlbum(album) }
                )
            }
        }

        // Section: Device Folders & Albums
        if (regularAlbums.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = "Device Folders (${regularAlbums.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 10.dp, bottom = 2.dp)
                )
            }

            items(regularAlbums, key = { it.id }) { album ->
                AlbumCard(
                    album = album,
                    onClick = { onSelectAlbum(album) }
                )
            }
        }
    }
}

@Composable
private fun AlbumDetailContent(
    album: Album,
    uiState: GalleryUiState,
    onBack: () -> Unit,
    onMediaClick: (com.example.data.MediaItem) -> Unit,
    onMediaLongClick: (com.example.data.MediaItem) -> Unit = {},
    onToggleFavorite: (com.example.data.MediaItem) -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit
) {
    val gridState = rememberLazyGridState()
    val cellSpacing = when {
        uiState.gridColumns >= 5 -> 3.dp
        uiState.gridColumns == 4 -> 4.dp
        uiState.gridColumns == 3 -> 6.dp
        else -> 8.dp
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Album Top Header Bar with Back Button & Details
        Surface(
            tonalElevation = 2.dp,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("album_detail_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to Albums"
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = album.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${album.items.size} items • ${album.photoCount} photos, ${album.videoCount} videos",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (album.items.isEmpty()) {
            EmptyGalleryState(
                title = "Album is Empty",
                message = "There are no photos or videos in this album yet.",
                actionText = "Back to Albums",
                onAction = onBack
            )
        } else {
            val totalItems = album.items.size
            val dateLabels = remember(album.items) {
                album.items.map { DateTimeUtils.formatShortDate(it.dateEpochMillis) }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Fixed(uiState.gridColumns),
                    modifier = Modifier
                        .fillMaxSize()
                        .pinchToZoomGrid(onZoomIn = onZoomIn, onZoomOut = onZoomOut)
                        .testTag("album_detail_grid"),
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 96.dp),
                    horizontalArrangement = Arrangement.spacedBy(cellSpacing),
                    verticalArrangement = Arrangement.spacedBy(cellSpacing)
                ) {
                    items(album.items, key = { it.id }) { item ->
                        MediaCard(
                            item = item,
                            gridColumns = uiState.gridColumns,
                            showVideoDurationBadge = uiState.showVideoDurationBadge,
                            isSelected = item.id in uiState.selectedItemIds,
                            isSelectionMode = uiState.isSelectionMode,
                            onClick = {
                                if (uiState.isSelectionMode) {
                                    onMediaLongClick(item)
                                } else {
                                    onMediaClick(item)
                                }
                            },
                            onLongClick = { onMediaLongClick(item) },
                            onToggleFavorite = { onToggleFavorite(item) }
                        )
                    }
                }

                FastGridScrollbar(
                    gridState = gridState,
                    totalItems = totalItems,
                    labelProvider = { index ->
                        if (index in dateLabels.indices) {
                            "${dateLabels[index]} (${index + 1}/$totalItems)"
                        } else {
                            "${index + 1} / $totalItems"
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 6.dp, top = 12.dp, bottom = 84.dp)
                )
            }
        }
    }
}

@Composable
private fun GridContent(
    uiState: GalleryUiState,
    hasMediaPermission: Boolean,
    onGrantPermission: () -> Unit,
    onRefreshMedia: () -> Unit,
    onMediaClick: (com.example.data.MediaItem) -> Unit,
    onMediaLongClick: (com.example.data.MediaItem) -> Unit = {},
    onToggleFavorite: (com.example.data.MediaItem) -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit
) {
    if (uiState.filteredMedia.isEmpty()) {
        when {
            !hasMediaPermission -> {
                EmptyGalleryState(
                    title = "Media Access Required",
                    message = "Allow media permission so the gallery can load and display your photos and videos.",
                    actionText = "Grant Permission",
                    onAction = onGrantPermission
                )
            }
            uiState.allMedia.isEmpty() -> {
                EmptyGalleryState(
                    title = "No Photos or Videos Found",
                    message = "No photos or videos detected on your device yet.",
                    actionText = "Refresh Device Media",
                    onAction = onRefreshMedia
                )
            }
            else -> {
                EmptyGalleryState(
                    title = "No Matching Memories",
                    message = "No media matched your current search or filters."
                )
            }
        }
        return
    }

    val gridState = rememberLazyGridState()
    val totalItems = uiState.filteredMedia.size

    val indexToDateLabel = remember(uiState.filteredMedia) {
        uiState.filteredMedia.map { com.example.ui.util.DateTimeUtils.formatShortDate(it.dateEpochMillis) }
    }

    val cellSpacing = when {
        uiState.gridColumns >= 5 -> 3.dp
        uiState.gridColumns == 4 -> 4.dp
        uiState.gridColumns == 3 -> 6.dp
        else -> 8.dp
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Fixed(uiState.gridColumns),
            modifier = Modifier
                .fillMaxSize()
                .pinchToZoomGrid(onZoomIn = onZoomIn, onZoomOut = onZoomOut),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 96.dp),
            horizontalArrangement = Arrangement.spacedBy(cellSpacing),
            verticalArrangement = Arrangement.spacedBy(cellSpacing)
        ) {
            items(uiState.filteredMedia, key = { it.id }) { item ->
                MediaCard(
                    item = item,
                    gridColumns = uiState.gridColumns,
                    showVideoDurationBadge = uiState.showVideoDurationBadge,
                    isSelected = item.id in uiState.selectedItemIds,
                    isSelectionMode = uiState.isSelectionMode,
                    onClick = {
                        if (uiState.isSelectionMode) {
                            onMediaLongClick(item)
                        } else {
                            onMediaClick(item)
                        }
                    },
                    onLongClick = { onMediaLongClick(item) },
                    onToggleFavorite = { onToggleFavorite(item) }
                )
            }
        }

        FastGridScrollbar(
            gridState = gridState,
            totalItems = totalItems,
            labelProvider = { index ->
                if (index in indexToDateLabel.indices) {
                    "${indexToDateLabel[index]} (${index + 1}/$totalItems)"
                } else {
                    "${index + 1} / $totalItems"
                }
            },
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 6.dp, top = 12.dp, bottom = 84.dp)
        )
    }
}

@Composable
private fun EmptyGalleryState(
    title: String = "No Memories Found",
    message: String,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.widthIn(max = 360.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(72.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PhotoLibrary,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (actionText != null && onAction != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onAction,
                    modifier = Modifier.testTag("empty_state_action_button")
                ) {
                    Text(actionText)
                }
            }
        }
    }
}
