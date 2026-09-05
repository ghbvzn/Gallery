package com.example.ui

import androidx.compose.runtime.Immutable
import com.example.data.MediaItem

enum class GalleryViewMode {
    TIMELINE,
    ALBUMS,
    PLACES,
    GRID
}

enum class MediaTypeFilter {
    ALL,
    PHOTOS,
    VIDEOS,
    FAVORITES
}

enum class SortOrder {
    NEWEST_FIRST,
    OLDEST_FIRST
}

@Immutable
data class Album(
    val id: String,
    val name: String,
    val items: List<MediaItem>,
    val coverItem: MediaItem?,
    val photoCount: Int,
    val videoCount: Int,
    val isSmartAlbum: Boolean = false,
    val iconType: String = "folder" // "favorite", "video", "folder", "camera", "screenshots"
)

@Immutable
data class LocationGroup(
    val locationName: String,
    val items: List<MediaItem>,
    val coverItem: MediaItem?,
    val photoCount: Int,
    val videoCount: Int
)

@Immutable
data class DateGroup(
    val groupTitle: String,
    val items: List<MediaItem>,
    val photoCount: Int,
    val videoCount: Int
)

@Immutable
data class GalleryUiState(
    val viewMode: GalleryViewMode = GalleryViewMode.TIMELINE,
    val typeFilter: MediaTypeFilter = MediaTypeFilter.ALL,
    val sortOrder: SortOrder = SortOrder.NEWEST_FIRST,
    val gridColumns: Int = 3,
    val searchQuery: String = "",
    val selectedLocationFilter: String? = null,
    val selectedTagFilter: String? = null,
    val allMedia: List<MediaItem> = emptyList(),
    val filteredMedia: List<MediaItem> = emptyList(),
    val dateGroups: List<DateGroup> = emptyList(),
    val locationGroups: List<LocationGroup> = emptyList(),
    val albums: List<Album> = emptyList(),
    val selectedAlbum: Album? = null,
    val availableLocations: List<String> = emptyList(),
    val availableTags: List<String> = emptyList(),
    val activeItem: MediaItem? = null,
    val showAddDialog: Boolean = false,
    val showSettingsDialog: Boolean = false,
    val showCreateAlbumDialog: Boolean = false,
    val showVideoDurationBadge: Boolean = true,
    val themeMode: String = "system",
    val editingItem: MediaItem? = null,
    val isSearching: Boolean = false,
    val isAnalyzingTags: Boolean = false,
    val aiTaggingNotice: String? = null,
    val hasMediaPermission: Boolean = false,
    val isLoadingMedia: Boolean = false,
    val permissionRequested: Boolean = false
)
