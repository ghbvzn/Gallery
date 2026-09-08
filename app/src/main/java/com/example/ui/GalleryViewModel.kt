package com.example.ui

import android.app.Application
import android.net.Uri
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.DeviceMediaScanner
import com.example.data.ExifMetadataHelper
import com.example.data.GalleryDatabase
import com.example.data.GalleryRepository
import com.example.data.MediaItem
import com.example.data.MediaType
import com.example.data.ai.MediaAnalyzer
import com.example.ui.util.DateTimeUtils
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Immutable
data class ViewSettings(
    val viewMode: GalleryViewMode = GalleryViewMode.TIMELINE,
    val typeFilter: MediaTypeFilter = MediaTypeFilter.ALL,
    val sortOrder: SortOrder = SortOrder.NEWEST_FIRST,
    val gridColumns: Int = 3,
    val searchQuery: String = "",
    val selectedLocationFilter: String? = null,
    val selectedTagFilter: String? = null,
    val selectedAlbumId: String? = null,
    val showSettingsDialog: Boolean = false,
    val showCreateAlbumDialog: Boolean = false,
    val showVideoDurationBadge: Boolean = true,
    val themeMode: String = "system",
    val activeItem: MediaItem? = null,
    val showAddDialog: Boolean = false,
    val editingItem: MediaItem? = null,
    val isSearching: Boolean = false,
    val isAnalyzingTags: Boolean = false,
    val aiTaggingNotice: String? = null,
    val hasMediaPermission: Boolean = false,
    val isLoadingMedia: Boolean = false,
    val permissionRequested: Boolean = false,
    val selectedItemIds: Set<Long> = emptySet(),
    val showCameraScreen: Boolean = false
)

private data class ContentSettings(
    val typeFilter: MediaTypeFilter,
    val sortOrder: SortOrder,
    val searchQuery: String,
    val selectedLocationFilter: String?,
    val selectedTagFilter: String?,
    val selectedAlbumId: String?
)

private data class LibraryIndex(
    val allMedia: List<MediaItem>,
    val mediaById: Map<Long, MediaItem>,
    val locationGroups: List<LocationGroup>,
    val albums: List<Album>,
    val availableLocations: List<String>,
    val availableTags: List<String>
)

private data class DerivedContent(
    val library: LibraryIndex,
    val filteredMedia: List<MediaItem>,
    val dateGroups: List<DateGroup>,
    val selectedAlbum: Album?
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

    private val filterSettings = _settings
        .map { settings ->
            ContentSettings(
                typeFilter = settings.typeFilter,
                sortOrder = settings.sortOrder,
                searchQuery = "",
                selectedLocationFilter = settings.selectedLocationFilter,
                selectedTagFilter = settings.selectedTagFilter,
                selectedAlbumId = settings.selectedAlbumId
            )
        }
        .distinctUntilChanged()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private val debouncedSearchQuery = _settings
        .map { it.searchQuery }
        .distinctUntilChanged()
        .mapLatest { query ->
            if (query.isNotBlank()) delay(150)
            query
        }

    private val contentSettings = combine(
        filterSettings,
        debouncedSearchQuery
    ) { settings, searchQuery ->
        settings.copy(searchQuery = searchQuery)
    }

    /*
     * Build the expensive library index only when Room emits changed media. UI-only
     * changes such as selection, dialogs, or the active viewer no longer regroup the
     * entire library. Keep this work off the main thread so large libraries cannot
     * stall Compose frames.
     */
    private val libraryIndex = repository.allMedia
        .map(::buildLibraryIndex)
        .flowOn(Dispatchers.Default)

    private val derivedContent = combine(libraryIndex, contentSettings) { library, settings ->
        val sortedList = if (settings.sortOrder == SortOrder.NEWEST_FIRST) {
            // Room already emits newest-first, so avoid an O(n log n) sort.
            library.allMedia
        } else {
            library.allMedia.asReversed()
        }
        val query = settings.searchQuery.trim()
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

            val matchesQuery = if (query.isEmpty()) {
                true
            } else {
                item.title.contains(query, ignoreCase = true) ||
                        item.locationName.contains(query, ignoreCase = true) ||
                        item.notes.contains(query, ignoreCase = true) ||
                        item.tags.any { it.contains(query, ignoreCase = true) }
            }

            matchesType && matchesLocation && matchesTag && matchesQuery
        }

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

        DerivedContent(
            library = library,
            filteredMedia = filtered,
            dateGroups = dateGroups,
            selectedAlbum = settings.selectedAlbumId?.let { id ->
                library.albums.find { it.id == id }
            }
        )
    }.flowOn(Dispatchers.Default)

    val uiState: StateFlow<GalleryUiState> = combine(
        derivedContent,
        _settings
    ) { content, settings ->
        val library = content.library
        GalleryUiState(
            viewMode = settings.viewMode,
            typeFilter = settings.typeFilter,
            sortOrder = settings.sortOrder,
            gridColumns = settings.gridColumns,
            searchQuery = settings.searchQuery,
            selectedLocationFilter = settings.selectedLocationFilter,
            selectedTagFilter = settings.selectedTagFilter,
            allMedia = library.allMedia,
            filteredMedia = content.filteredMedia,
            dateGroups = content.dateGroups,
            locationGroups = library.locationGroups,
            albums = library.albums,
            selectedAlbum = content.selectedAlbum,
            availableLocations = library.availableLocations,
            availableTags = library.availableTags,
            activeItem = settings.activeItem?.let { library.mediaById[it.id] ?: it },
            showAddDialog = settings.showAddDialog,
            showSettingsDialog = settings.showSettingsDialog,
            showCreateAlbumDialog = settings.showCreateAlbumDialog,
            showVideoDurationBadge = settings.showVideoDurationBadge,
            themeMode = settings.themeMode,
            editingItem = settings.editingItem,
            isSearching = settings.isSearching,
            isAnalyzingTags = settings.isAnalyzingTags,
            aiTaggingNotice = settings.aiTaggingNotice,
            hasMediaPermission = settings.hasMediaPermission,
            isLoadingMedia = settings.isLoadingMedia,
            permissionRequested = settings.permissionRequested,
            selectedItemIds = settings.selectedItemIds,
            showCameraScreen = settings.showCameraScreen
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = GalleryUiState()
    )

    private fun buildLibraryIndex(allMedia: List<MediaItem>): LibraryIndex {
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

        // Smart Albums (Favorites, Videos)
        val smartAlbums = mutableListOf<Album>()
        val favorites = allMedia.filter { it.isFavorite }
        smartAlbums.add(
            Album(
                id = "smart_favorites",
                name = "Favorites",
                items = favorites,
                coverItem = favorites.firstOrNull(),
                photoCount = favorites.count { it.type == MediaType.PHOTO },
                videoCount = favorites.count { it.type == MediaType.VIDEO },
                isSmartAlbum = true,
                iconType = "favorite"
            )
        )
        val videos = allMedia.filter { it.type == MediaType.VIDEO }
        smartAlbums.add(
            Album(
                id = "smart_videos",
                name = "Videos",
                items = videos,
                coverItem = videos.firstOrNull(),
                photoCount = 0,
                videoCount = videos.size,
                isSmartAlbum = true,
                iconType = "video"
            )
        )

        // Device & Folder Albums
        val folderAlbums = allMedia
            .groupBy { it.locationName.ifBlank { "Photos" } }
            .map { (folderName, items) ->
                val iconType = when {
                    folderName.contains("camera", ignoreCase = true) -> "camera"
                    folderName.contains("screenshot", ignoreCase = true) -> "screenshot"
                    folderName.contains("download", ignoreCase = true) -> "download"
                    else -> "folder"
                }
                Album(
                    id = "folder_$folderName",
                    name = folderName,
                    items = items,
                    coverItem = items.firstOrNull(),
                    photoCount = items.count { it.type == MediaType.PHOTO },
                    videoCount = items.count { it.type == MediaType.VIDEO },
                    isSmartAlbum = false,
                    iconType = iconType
                )
            }
            .sortedByDescending { it.items.size }

        val allAlbums = smartAlbums + folderAlbums

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

        return LibraryIndex(
            allMedia = allMedia,
            mediaById = allMedia.associateBy { it.id },
            locationGroups = locationGroups,
            albums = allAlbums,
            availableLocations = uniqueLocations,
            availableTags = uniqueTags
        )
    }

    fun setGridColumns(columns: Int) {
        val clamped = columns.coerceIn(2, 5)
        _settings.update { it.copy(gridColumns = clamped) }
    }

    fun zoomInGrid() {
        _settings.update { it.copy(gridColumns = (it.gridColumns - 1).coerceAtLeast(2)) }
    }

    fun zoomOutGrid() {
        _settings.update { it.copy(gridColumns = (it.gridColumns + 1).coerceAtMost(5)) }
    }

    fun selectViewMode(mode: GalleryViewMode) {
        _settings.update {
            if (mode == GalleryViewMode.ALBUMS && it.viewMode == GalleryViewMode.ALBUMS && it.selectedAlbumId != null) {
                // Tapping Albums while in an album pops back to root Albums
                it.copy(selectedAlbumId = null)
            } else {
                it.copy(viewMode = mode)
            }
        }
    }

    fun selectAlbum(album: Album?) {
        _settings.update { it.copy(selectedAlbumId = album?.id) }
    }

    fun setSettingsDialogVisible(visible: Boolean) {
        _settings.update { it.copy(showSettingsDialog = visible) }
    }

    fun setCreateAlbumDialogVisible(visible: Boolean) {
        _settings.update { it.copy(showCreateAlbumDialog = visible) }
    }

    fun setShowVideoDurationBadge(show: Boolean) {
        _settings.update { it.copy(showVideoDurationBadge = show) }
    }

    fun setThemeMode(mode: String) {
        _settings.update { it.copy(themeMode = mode) }
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

    fun openCamera() {
        _settings.update { it.copy(showCameraScreen = true) }
    }

    fun closeCamera() {
        _settings.update { it.copy(showCameraScreen = false) }
    }

    fun onMediaCaptured(
        fileUri: Uri,
        type: MediaType,
        durationSeconds: Int = 0,
        resolution: String = "High Definition"
    ) {
        val timestamp = System.currentTimeMillis()
        val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date(timestamp))
        val defaultTitle = if (type == MediaType.PHOTO) "IMG_$dateStr" else "VID_$dateStr"
        addMedia(
            title = defaultTitle,
            uriString = fileUri.toString(),
            type = type,
            location = "Camera",
            dateEpochMillis = timestamp,
            durationSeconds = durationSeconds,
            resolution = resolution,
            notes = if (type == MediaType.PHOTO) "Photo captured with in-app camera" else "Video captured with in-app camera",
            tags = listOf(if (type == MediaType.PHOTO) "photo" else "video", "camera")
        )
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
            // Read EXIF and parse dimensions on background thread
            val (exifLat, exifLon, effectiveRes) = withContext(Dispatchers.IO) {
                if (type == MediaType.PHOTO) {
                    val exif = ExifMetadataHelper.readExifData(getApplication(), uriString)
                    val res = if (resolution == "High Definition" && exif.imageWidth > 0 && exif.imageHeight > 0) {
                        "${exif.imageWidth}x${exif.imageHeight}"
                    } else resolution
                    Triple(exif.latitude, exif.longitude, res)
                } else {
                    Triple(null, null, resolution)
                }
            }

            val newItem = MediaItem(
                title = title.ifBlank { if (type == MediaType.PHOTO) "New Photo" else "New Video" },
                uriString = uriString,
                type = type,
                locationName = location.ifBlank { "Home" },
                latitude = exifLat,
                longitude = exifLon,
                dateEpochMillis = dateEpochMillis,
                durationSeconds = durationSeconds,
                resolution = effectiveRes,
                notes = notes,
                tags = tags
            )
            val newId = withContext(Dispatchers.IO) {
                repository.insert(newItem)
            }
            _settings.update { it.copy(showAddDialog = false) }

            // Auto-trigger AI analysis on newly uploaded media on background thread
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

    fun toggleSelection(itemId: Long) {
        _settings.update { current ->
            val updated = current.selectedItemIds.toMutableSet()
            if (updated.contains(itemId)) {
                updated.remove(itemId)
            } else {
                updated.add(itemId)
            }
            current.copy(selectedItemIds = updated)
        }
    }

    fun selectAll() {
        val allIds = uiState.value.filteredMedia.map { it.id }.toSet()
        _settings.update { it.copy(selectedItemIds = allIds) }
    }

    fun clearSelection() {
        _settings.update { it.copy(selectedItemIds = emptySet()) }
    }

    fun deleteSelectedItems() {
        val idsToDelete = _settings.value.selectedItemIds.toList()
        if (idsToDelete.isEmpty()) return
        viewModelScope.launch {
            repository.deleteBatch(idsToDelete)
            _settings.update { current ->
                val newActive = if (current.activeItem?.id in idsToDelete) null else current.activeItem
                current.copy(
                    selectedItemIds = emptySet(),
                    activeItem = newActive
                )
            }
        }
    }

    fun toggleFavoriteSelected() {
        val ids = _settings.value.selectedItemIds.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            val selectedMedia = uiState.value.allMedia.filter { it.id in ids }
            val anyNotFavorite = selectedMedia.any { !it.isFavorite }
            repository.setFavoriteBatch(ids, anyNotFavorite)
        }
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
                withContext(Dispatchers.IO) {
                    repository.cleanupDemoData()
                    val scanned = deviceMediaScanner.scanDeviceMedia()
                    repository.syncDeviceMedia(scanned)
                }
            } catch (e: Exception) {
                Log.w("GalleryViewModel", "Failed to refresh device media: ${e.message}")
            } finally {
                _settings.update { it.copy(isLoadingMedia = false) }
            }
        }
    }
}
