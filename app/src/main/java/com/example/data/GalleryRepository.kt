package com.example.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class GalleryRepository(private val mediaDao: MediaDao) {

    val allMedia: Flow<List<MediaItem>> = mediaDao.getAllMedia()
    val allLocations: Flow<List<String>> = mediaDao.getAllLocations()

    suspend fun cleanupDemoData() {
        withContext(Dispatchers.IO) {
            mediaDao.deleteDemoItems()
        }
    }

    suspend fun syncDeviceMedia(scannedItems: List<MediaItem>) {
        withContext(Dispatchers.IO) {
            val existing = mediaDao.getAllMediaList()
            val existingUriSet = existing.map { it.uriString }.toSet()
            val newItems = scannedItems.filter { it.uriString !in existingUriSet }
            if (newItems.isNotEmpty()) {
                mediaDao.insertAll(newItems)
            }
        }
    }

    suspend fun insert(item: MediaItem): Long {
        return withContext(Dispatchers.IO) {
            mediaDao.insert(item)
        }
    }

    suspend fun update(item: MediaItem) {
        withContext(Dispatchers.IO) {
            mediaDao.update(item)
        }
    }

    suspend fun setFavorite(id: Long, isFavorite: Boolean) {
        withContext(Dispatchers.IO) {
            mediaDao.setFavorite(id, isFavorite)
        }
    }

    suspend fun updateMetadata(id: Long, title: String, location: String, dateEpochMillis: Long) {
        withContext(Dispatchers.IO) {
            mediaDao.updateMetadata(id, title, location, dateEpochMillis)
        }
    }

    suspend fun updateTags(id: Long, tags: List<String>) {
        withContext(Dispatchers.IO) {
            mediaDao.updateTags(id, tags)
        }
    }

    suspend fun updateSuggestedTags(id: Long, suggestedTags: List<String>) {
        withContext(Dispatchers.IO) {
            mediaDao.updateSuggestedTags(id, suggestedTags)
        }
    }

    suspend fun updateTagsAndSuggestions(id: Long, tags: List<String>, suggestedTags: List<String>) {
        withContext(Dispatchers.IO) {
            mediaDao.updateTagsAndSuggestions(id, tags, suggestedTags)
        }
    }

    suspend fun delete(id: Long) {
        withContext(Dispatchers.IO) {
            mediaDao.deleteById(id)
        }
    }
}
