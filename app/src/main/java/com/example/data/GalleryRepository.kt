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
            mediaDao.deleteDuplicateUris()
        }
    }

    suspend fun syncDeviceMedia(scanResult: DeviceMediaScanResult) {
        withContext(Dispatchers.IO) {
            val existing = mediaDao.getAllMediaList()
            val existingUriSet = existing.map { it.uriString }.toSet()
            val scannedUriSet = scanResult.items.map { it.uriString }.toSet()
            val newItems = scanResult.items.filter { it.uriString !in existingUriSet }
            val staleIds = existing.asSequence()
                .filter { it.type in scanResult.fullyScannedTypes }
                .filter { it.isDeviceMediaStoreItem() }
                .filterNot { it.isTrashed }
                .filter { it.uriString !in scannedUriSet }
                .map { it.id }
                .toList()

            // One Room transaction means one coherent UI update for additions/removals.
            mediaDao.applyDeviceMediaSync(newItems, staleIds, scanResult.items)
        }
    }

    private fun MediaItem.isDeviceMediaStoreItem(): Boolean {
        return when (type) {
            MediaType.PHOTO -> uriString.startsWith("content://media/external/images/media/")
            MediaType.VIDEO -> uriString.startsWith("content://media/external/video/media/")
        }
    }

    suspend fun insert(item: MediaItem): Long {
        return withContext(Dispatchers.IO) {
            mediaDao.upsertByUri(item)
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

    suspend fun getMediaCount(): Int = withContext(Dispatchers.IO) {
        mediaDao.getMediaCount()
    }

    suspend fun delete(id: Long) {
        withContext(Dispatchers.IO) {
            mediaDao.deleteById(id)
        }
    }

    suspend fun deleteBatch(ids: List<Long>) {
        if (ids.isEmpty()) return
        withContext(Dispatchers.IO) {
            mediaDao.deleteByIds(ids)
        }
    }

    suspend fun setFavoriteBatch(ids: List<Long>, isFavorite: Boolean) {
        if (ids.isEmpty()) return
        withContext(Dispatchers.IO) {
            mediaDao.setFavoriteBatch(ids, isFavorite)
        }
    }

    suspend fun setTrashedBatch(ids: List<Long>, isTrashed: Boolean) {
        if (ids.isEmpty()) return
        withContext(Dispatchers.IO) {
            mediaDao.setTrashedBatch(ids, isTrashed)
        }
    }
}
