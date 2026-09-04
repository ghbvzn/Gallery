package com.example.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.DeviceMediaScanner
import com.example.data.GalleryDatabase
import com.example.data.GalleryRepository
import com.example.data.MediaItem
import com.example.data.MediaType
import com.example.data.ai.MediaAnalyzer
import com.example.ui.util.DateTimeUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ViewSettings(
    val viewMode: GalleryViewMode = GalleryViewMode.TIMELINE,
    val typeFilter: MediaTypeFilter = MediaTypeFilter.ALL,
    val sortOrder: SortOrder = SortOrder.NEWEST_FIRST,
    val searchQuery: String = "",
    val selectedLocationFilter: String? = null,
    val selectedTagFilter: String? = null,
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

class GalleryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: GalleryRepository
    private val mediaAnalyzer = MediaAnalyzer(application)
    private val deviceMediaScanner = DeviceMediaScanner(application)

    init {
        val db = GalleryDatabase.getDatabase(application)
        repository = GalleryRepository(db.mediaDao())
        viewModelScope.launch {
            // Remove demo photos and videos
            repository.cleanupDemoData()
        }
    }

    private val _settings = MutableStateFlow(ViewSettings())

    val uiState: StateFlow<GalleryUiState> = combine(
        repository.allMedia,
        _settings
    ) { allMedia: List<MediaItem>, settings: ViewSettings ->

        val sortedList = if (settings.sortOrder == SortOrder.NEWEST_FIRST) {
            allMedia.sortedByDescending { it.dateEpochMillis }
        } else {
            allMedia.sortedBy { it.dateEpochMillis }
        }

        val filtered = sortedList.filter { item ->
            val matchesType = when (settings.typeFilter) {
                MediaTypeFilter.ALL -> true
                MediaTypeFilter.PHOTOS -> item.type == MediaType.PHOTO
                MediaTypeFilter.VIDEOS -> item.type == MediaType.VIDEO
                MediaTypeFilter.FAVORITES -> item.isFavorite
            }

            val matchesLocation = settings.selectedLocationFilter == null ||
                    item.locationName.equals(settings.selectedLocationFilter, ignoreCase = true)

            val matchesTag = settings.selectedTagFilter == null ||
                    item.tags.any { it.equals(settings.selectedTagFilter, ignoreCase = true) }

            val matchesQuery = if (settings.searchQuery.isBlank()) {
                true
            } else {
                val q = settings.searchQuery.trim().lowercase()
                item.title.lowercase().contains(q) ||
                        item.locationName.lowercase().contains(q) ||
                        item.notes.lowercase().contains(q) ||
                        item.tags.any { it.lowercase().contains(q) }
            }

            matchesType && matchesLocation && matchesTag && matchesQuery
        }

        // Group by Date for Timeline
        val dateGroups = filtered
            .groupBy { item -> DateTimeUtils.formatTimelineHeader(item.dateEpochMillis) }
            .map { (header, items) ->
                DateGroup(
                    groupTitle = header,
                    items = items,
                    photoCount = items.count { it.type == MediaType.PHOTO },
                    videoCount = items.count { it.type == MediaType.VIDEO }
                )
            }

        // Group by Location for Places View
        val locationGroups = allMedia
            .groupBy { it.locationName.ifBlank { "Unspecified Location" } }
            .map { (locName, items) ->
                LocationGroup(
                    locationName = locName,
                    items = items,
                    coverItem = items.firstOrNull(),
                    photoCount = items.count { it.type == MediaType.PHOTO },
                    videoCount = items.count { it.type == MediaType.VIDEO }
                )
            }
            .sortedByDescending { it.items.size }

        val uniqueLocations = allMedia
            .map { it.locationName }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()

        val uniqueTags = allMedia
            .flatMap { it.tags }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()

        // Keep activeItem synced with latest item in database
        val syncedActiveItem = settings.activeItem?.let { active ->
            allMedia.find { it.id == active.id } ?: active
        }

        GalleryUiState(
            viewMode = settings.viewMode,
            typeFilter = settings.typeFilter,
            sortOrder = settings.sortOrder,
            searchQuery = settings.searchQuery,
            selectedLocationFilter = settings.selectedLocationFilter,
            selectedTagFilter = settings.selectedTagFilter,
            allMedia = allMedia,
            filteredMedia = filtered,
            dateGroups = dateGroups,
            locationGroups = locationGroups,
            availableLocations = uniqueLocations,
            availableTags = uniqueTags,
            activeItem = syncedActiveItem,
            showAddDialog = settings.showAddDialog,
            editingItem = settings.editingItem,
            isSearching = settings.isSearching,
            isAnalyzingTags = settings.isAnalyzingTags,
            aiTaggingNotice = settings.aiTaggingNotice,
            hasMediaPermission = settings.hasMediaPermission,
            isLoadingMedia = settings.isLoadingMedia,
            permissionRequested = settings.permissionRequested
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = GalleryUiState()
    )

    fun selectViewMode(mode: GalleryViewMode) {
        _settings.update { it.copy(viewMode = mode) }
    }

    fun setMediaTypeFilter(filter: MediaTypeFilter) {
        _settings.update { it.copy(typeFilter = filter) }
    }

    fun toggleSortOrder() {
        _settings.update {
            it.copy(
                sortOrder = if (it.sortOrder == SortOrder.NEWEST_FIRST)
                    SortOrder.OLDEST_FIRST
                else
                    SortOrder.NEWEST_FIRST
            )
        }
    }

    fun setSearchQuery(query: String) {
        _settings.update { it.copy(searchQuery = query) }
    }

    fun toggleSearch() {
        _settings.update {
            val nextSearching = !it.isSearching
            it.copy(
                isSearching = nextSearching,
                searchQuery = if (!nextSearching) "" else it.searchQuery
            )
        }
    }

    fun setSelectedLocationFilter(location: String?) {
        _settings.update { it.copy(selectedLocationFilter = location) }
    }

    fun setSelectedTagFilter(tag: String?) {
        _settings.update { it.copy(selectedTagFilter = tag) }
    }

    fun openDetailViewer(item: MediaItem) {
        _settings.update { it.copy(activeItem = item) }
    }

    fun closeDetailViewer() {
        _settings.update { it.copy(activeItem = null, isAnalyzingTags = false, aiTaggingNotice = null) }
    }

    fun nextItem() {
        val current = _settings.value.activeItem ?: return
        val currentList = uiState.value.filteredMedia
        val currentIndex = currentList.indexOfFirst { it.id == current.id }
        if (currentIndex in 0 until currentList.size - 1) {
            _settings.update { it.copy(activeItem = currentList[currentIndex + 1], isAnalyzingTags = false, aiTaggingNotice = null) }
        }
    }

    fun previousItem() {
        val current = _settings.value.activeItem ?: return
        val currentList = uiState.value.filteredMedia
        val currentIndex = currentList.indexOfFirst { it.id == current.id }
        if (currentIndex > 0) {
            _settings.update { it.copy(activeItem = currentList[currentIndex - 1], isAnalyzingTags = false, aiTaggingNotice = null) }
        }
    }

    fun toggleFavorite(item: MediaItem) {
        viewModelScope.launch {
            val newFav = !item.isFavorite
            repository.setFavorite(item.id, newFav)
            if (_settings.value.activeItem?.id == item.id) {
                _settings.update { it.copy(activeItem = it.activeItem?.copy(isFavorite = newFav)) }
            }
        }
    }

    fun showAddDialog(show: Boolean) {
        _settings.update { it.copy(showAddDialog = show) }
    }

    fun startEditing(item: MediaItem) {
        _settings.update { it.copy(editingItem = item) }
    }

    fun stopEditing() {
        _settings.update { it.copy(editingItem = null) }
    }

    fun addMedia(
        title: String,
        uriString: String,
        type: MediaType,
        location: String,
        dateEpochMillis: Long,
        durationSeconds: Int = 0,
        resolution: String = "High Definition",
        notes: String = "",
        tags: List<String> = emptyList()
    ) {
        viewModelScope.launch {
            val newItem = MediaItem(
                title = title.ifBlank { if (type == MediaType.PHOTO) "New Photo" else "New Video" },
                uriString = uriString,
                type = type,
                locationName = location.ifBlank { "Home" },
                dateEpochMillis = dateEpochMillis,
                durationSeconds = durationSeconds,
                resolution = resolution,
                notes = notes,
                tags = tags
            )
            val newId = repository.insert(newItem)
            _settings.update { it.copy(showAddDialog = false) }

            // Auto-trigger AI analysis on newly uploaded media
            val created = newItem.copy(id = newId)
            analyzeMediaForTags(created)
        }
    }

    fun updateMetadata(id: Long, title: String, location: String, dateEpochMillis: Long) {
        viewModelScope.launch {
            repository.updateMetadata(id, title, location, dateEpochMillis)
            if (_settings.value.activeItem?.id == id) {
                _settings.update {
                    it.copy(
                        activeItem = it.activeItem?.copy(
                            title = title,
                            locationName = location,
                            dateEpochMillis = dateEpochMillis
                        )
                    )
                }
            }
            _settings.update { it.copy(editingItem = null) }
        }
    }

    /**
     * AI-powered analysis to detect content and suggest relevant tags.
     */
    fun analyzeMediaForTags(item: MediaItem) {
        viewModelScope.launch {
            _settings.update { it.copy(isAnalyzingTags = true, aiTaggingNotice = "AI analyzing ${if (item.type == MediaType.PHOTO) "photo" else "video"} content...") }
            try {
                val result = mediaAnalyzer.analyzeMedia(item)
                val suggestions = result.getOrNull() ?: emptyList()
                // Filter out any tags the item already has
                val newSuggestions = (item.suggestedTags + suggestions)
                    .distinct()
                    .filter { sug -> item.tags.none { it.equals(sug, ignoreCase = true) } }

                repository.updateSuggestedTags(item.id, newSuggestions)

                _settings.update {
                    val updatedItem = if (it.activeItem?.id == item.id) {
                        it.activeItem.copy(suggestedTags = newSuggestions)
                    } else it.activeItem
                    it.copy(
                        activeItem = updatedItem,
                        isAnalyzingTags = false,
                        aiTaggingNotice = if (newSuggestions.isNotEmpty()) "Found ${newSuggestions.size} AI tag suggestions!" else "No additional tags suggested"
                    )
                }
            } catch (e: Exception) {
                _settings.update { it.copy(isAnalyzingTags = false, aiTaggingNotice = "Analysis failed: ${e.localizedMessage}") }
            }
        }
    }

    /**
     * Accept a suggested tag: moves it to active tags and removes from suggestions.
     */
    fun acceptTag(itemId: Long, tag: String) {
        viewModelScope.launch {
            val item = uiState.value.allMedia.find { it.id == itemId } ?: return@launch
            val updatedTags = (item.tags + tag).distinct()
            val updatedSuggestions = item.suggestedTags.filterNot { it.equals(tag, ignoreCase = true) }
            repository.updateTagsAndSuggestions(itemId, updatedTags, updatedSuggestions)

            if (_settings.value.activeItem?.id == itemId) {
                _settings.update {
                    it.copy(activeItem = it.activeItem?.copy(tags = updatedTags, suggestedTags = updatedSuggestions))
                }
            }
        }
    }

    /**
     * Reject a suggested tag: removes it from suggestions.
     */
    fun rejectTag(itemId: Long, tag: String) {
        viewModelScope.launch {
            val item = uiState.value.allMedia.find { it.id == itemId } ?: return@launch
            val updatedSuggestions = item.suggestedTags.filterNot { it.equals(tag, ignoreCase = true) }
            repository.updateSuggestedTags(itemId, updatedSuggestions)

            if (_settings.value.activeItem?.id == itemId) {
                _settings.update {
                    it.copy(activeItem = it.activeItem?.copy(suggestedTags = updatedSuggestions))
                }
            }
        }
    }

    /**
     * Add a custom tag entered by the user.
     */
    fun addCustomTag(itemId: Long, customTag: String) {
        val trimmed = customTag.trim()
        if (trimmed.isBlank()) return

        viewModelScope.launch {
            val item = uiState.value.allMedia.find { it.id == itemId } ?: return@launch
            if (item.tags.any { it.equals(trimmed, ignoreCase = true) }) return@launch

            val updatedTags = item.tags + trimmed
            val updatedSuggestions = item.suggestedTags.filterNot { it.equals(trimmed, ignoreCase = true) }
            repository.updateTagsAndSuggestions(itemId, updatedTags, updatedSuggestions)

            if (_settings.value.activeItem?.id == itemId) {
                _settings.update {
                    it.copy(activeItem = it.activeItem?.copy(tags = updatedTags, suggestedTags = updatedSuggestions))
                }
            }
        }
    }

    /**
     * Remove an existing tag.
     */
    fun removeTag(itemId: Long, tag: String) {
        viewModelScope.launch {
            val item = uiState.value.allMedia.find { it.id == itemId } ?: return@launch
            val updatedTags = item.tags.filterNot { it.equals(tag, ignoreCase = true) }
            repository.updateTags(itemId, updatedTags)

            if (_settings.value.activeItem?.id == itemId) {
                _settings.update {
                    it.copy(activeItem = it.activeItem?.copy(tags = updatedTags))
                }
            }
        }
    }

    fun clearAiNotice() {
        _settings.update { it.copy(aiTaggingNotice = null) }
    }

    fun deleteItem(id: Long) {
        viewModelScope.launch {
            repository.delete(id)
            if (_settings.value.activeItem?.id == id) {
                _settings.update { it.copy(activeItem = null) }
            }
        }
    }

    fun onPermissionResult(granted: Boolean) {
        _settings.update {
            it.copy(
                hasMediaPermission = granted,
                permissionRequested = true
            )
        }
        if (granted) {
            refreshDeviceMedia()
        }
    }

    fun setPermissionRequested() {
        _settings.update { it.copy(permissionRequested = true) }
    }

    fun refreshDeviceMedia() {
        viewModelScope.launch {
            _settings.update { it.copy(isLoadingMedia = true) }
            try {
                repository.cleanupDemoData()
                val scanned = deviceMediaScanner.scanDeviceMedia()
                repository.syncDeviceMedia(scanned)
            } catch (e: Exception) {
                Log.e("GalleryViewModel", "Failed to refresh device media", e)
            } finally {
                _settings.update { it.copy(isLoadingMedia = false) }
            }
        }
    }
}
