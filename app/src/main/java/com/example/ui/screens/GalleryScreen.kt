package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PermMedia
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Photo
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.MediaType
import com.example.ui.GalleryUiState
import com.example.ui.GalleryViewMode
import com.example.ui.GalleryViewModel
import com.example.ui.MediaTypeFilter
import com.example.ui.SortOrder
import com.example.ui.components.AddMediaDialog
import com.example.ui.components.EditMetadataDialog
import com.example.ui.components.LocationCard
import com.example.ui.components.MediaCard
import com.example.ui.components.MediaDetailViewer
import com.example.ui.components.TimelineHeader
import com.example.ui.theme.RoseFavorite

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
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "Gallery",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold
                            )
                            val totalItems = uiState.allMedia.size
                            val locationsCount = uiState.availableLocations.size
                            Text(
                                text = "$totalItems memories • $locationsCount places",
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
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )

                // Scanning Progress Indicator
                if (uiState.isLoadingMedia) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("media_loading_indicator")
                    )
                }

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

                // Filter Chips Row (Media type & Active location filter)
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
        },
        bottomBar = {
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
                    label = { Text("By Date") },
                    modifier = Modifier.testTag("nav_item_timeline")
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
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.showAddDialog(true) },
                icon = { Icon(Icons.Default.Add, contentDescription = "Add Media") },
                text = { Text("Add Media") },
                modifier = Modifier
                    .navigationBarsPadding()
                    .testTag("add_media_fab")
            )
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
                            onToggleFavorite = { viewModel.toggleFavorite(it) },
                            onLocationFilter = { viewModel.setSelectedLocationFilter(it) }
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
                            onToggleFavorite = { viewModel.toggleFavorite(it) }
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
}

@Composable
private fun TimelineContent(
    uiState: GalleryUiState,
    onGrantPermission: () -> Unit,
    onRefreshMedia: () -> Unit,
    onMediaClick: (com.example.data.MediaItem) -> Unit,
    onToggleFavorite: (com.example.data.MediaItem) -> Unit,
    onLocationFilter: (String) -> Unit
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

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        uiState.dateGroups.forEach { dateGroup ->
            item(key = "header_${dateGroup.groupTitle}") {
                TimelineHeader(
                    dateGroup = dateGroup,
                    onLocationClick = onLocationFilter
                )
            }

            item(key = "grid_${dateGroup.groupTitle}") {
                // Render 2 items per row in responsive grid style
                val items = dateGroup.items
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val rows = items.chunked(2)
                    rows.forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            rowItems.forEach { item ->
                                Box(modifier = Modifier.weight(1f)) {
                                    MediaCard(
                                        item = item,
                                        onClick = { onMediaClick(item) },
                                        onToggleFavorite = { onToggleFavorite(item) }
                                    )
                                }
                            }
                            if (rowItems.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
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

    LazyColumn(
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
}

@Composable
private fun GridContent(
    uiState: GalleryUiState,
    hasMediaPermission: Boolean,
    onGrantPermission: () -> Unit,
    onRefreshMedia: () -> Unit,
    onMediaClick: (com.example.data.MediaItem) -> Unit,
    onToggleFavorite: (com.example.data.MediaItem) -> Unit
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

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 150.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 96.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(uiState.filteredMedia, key = { it.id }) { item ->
            MediaCard(
                item = item,
                onClick = { onMediaClick(item) },
                onToggleFavorite = { onToggleFavorite(item) }
            )
        }
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
