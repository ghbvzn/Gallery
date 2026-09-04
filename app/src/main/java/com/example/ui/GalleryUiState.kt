package com.example.ui

import com.example.data.MediaItem

enum class GalleryViewMode {
    TIMELINE,
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

data class LocationGroup(
    val locationName: String,
    val items: List<MediaItem>,
    val coverItem: MediaItem?,
    val photoCount: Int,
    val videoCount: Int
)

data class DateGroup(
    val groupTitle: String,
    val items: List<MediaItem>,
    val photoCount: Int,
    val videoCount: Int
)

data class GalleryUiState(
    val viewMode: GalleryViewMode = GalleryViewMode.TIMELINE,
    val typeFilter: MediaTypeFilter = MediaTypeFilter.ALL,
    val sortOrder: SortOrder = SortOrder.NEWEST_FIRST,
    val searchQuery: String = "",
    val selectedLocationFilter: String? = null,
    val selectedTagFilter: String? = null,
    val allMedia: List<MediaItem> = emptyList(),
    val filteredMedia: List<MediaItem> = emptyList(),
    val dateGroups: List<DateGroup> = emptyList(),
    val locationGroups: List<LocationGroup> = emptyList(),
    val availableLocations: List<String> = emptyList(),
    val availableTags: List<String> = emptyList(),
    val activeItem: MediaItem? = null,
    val showAddDialog: Boolean = false,
    val editingItem: MediaItem? = null,
    val isSearching: Boolean = false,
    val isAnalyzingTags: Boolean = false,
    val aiTaggingNotice: String? = null,
    val hasMediaPermission: Boolean = false,
    val isLoadingMedia: Boolean = false,
    val permissionRequested: Boolean = false
)
