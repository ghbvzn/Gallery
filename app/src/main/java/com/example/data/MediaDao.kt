package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaDao {
    @Query("SELECT * FROM media_items ORDER BY dateEpochMillis DESC")
    fun getAllMedia(): Flow<List<MediaItem>>

    @Query("SELECT * FROM media_items WHERE id = :id")
    suspend fun getMediaById(id: Long): MediaItem?

    @Query("SELECT * FROM media_items WHERE uriString = :uriString LIMIT 1")
    suspend fun getMediaByUri(uriString: String): MediaItem?

    @Query("SELECT * FROM media_items")
    suspend fun getAllMediaList(): List<MediaItem>

    @Query("DELETE FROM media_items WHERE uriString LIKE 'android.resource://%'")
    suspend fun deleteDemoItems()

    @Query("""DELETE FROM media_items
        WHERE id NOT IN (
            SELECT MAX(id) FROM media_items GROUP BY uriString
        )""")
    suspend fun deleteDuplicateUris()

    @Query("DELETE FROM media_items WHERE uriString = :uriString AND id != :keepId")
    suspend fun deleteOtherRowsForUri(uriString: String, keepId: Long)

    @Query("SELECT DISTINCT locationName FROM media_items WHERE locationName != '' ORDER BY locationName ASC")
    fun getAllLocations(): Flow<List<String>>

    @Query("SELECT COUNT(*) FROM media_items")
    suspend fun getMediaCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: MediaItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<MediaItem>)

    @Transaction
    suspend fun upsertByUri(item: MediaItem): Long {
        val existing = getMediaByUri(item.uriString)
        if (existing == null) return insert(item)

        update(
            item.copy(
                id = existing.id,
                isFavorite = existing.isFavorite,
                isTrashed = existing.isTrashed
            )
        )
        deleteOtherRowsForUri(item.uriString, existing.id)
        return existing.id
    }

    @Transaction
    suspend fun applyDeviceMediaSync(
        newItems: List<MediaItem>,
        staleIds: List<Long>,
        scannedItems: List<MediaItem>
    ) {
        // Recheck inside this transaction because a camera completion callback can
        // register the same MediaStore URI while a scan is in progress.
        newItems.forEach { item ->
            if (getMediaByUri(item.uriString) == null) insert(item)
        }
        if (staleIds.isNotEmpty()) deleteByIds(staleIds)
        scannedItems.forEach { item ->
            updateScannedMetadata(
                uriString = item.uriString,
                folderName = item.folderName,
                locationName = item.locationName,
                latitude = item.latitude,
                longitude = item.longitude
            )
        }
        deleteDuplicateUris()
    }

    @Query("""UPDATE media_items
        SET folderName = :folderName,
            locationName = CASE WHEN locationName = '' THEN :locationName ELSE locationName END,
            latitude = COALESCE(latitude, :latitude),
            longitude = COALESCE(longitude, :longitude)
        WHERE uriString = :uriString""")
    suspend fun updateScannedMetadata(
        uriString: String,
        folderName: String,
        locationName: String,
        latitude: Double?,
        longitude: Double?
    )

    @Update
    suspend fun update(item: MediaItem)

    @Query("UPDATE media_items SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun setFavorite(id: Long, isFavorite: Boolean)

    @Query("UPDATE media_items SET isTrashed = :isTrashed WHERE id IN (:ids)")
    suspend fun setTrashedBatch(ids: List<Long>, isTrashed: Boolean)

    @Query("UPDATE media_items SET title = :title, locationName = :location, dateEpochMillis = :dateEpochMillis WHERE id = :id")
    suspend fun updateMetadata(id: Long, title: String, location: String, dateEpochMillis: Long)

    @Query("UPDATE media_items SET tags = :tags WHERE id = :id")
    suspend fun updateTags(id: Long, tags: List<String>)

    @Delete
    suspend fun delete(item: MediaItem)

    @Query("DELETE FROM media_items WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM media_items WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("UPDATE media_items SET isFavorite = :isFavorite WHERE id IN (:ids)")
    suspend fun setFavoriteBatch(ids: List<Long>, isFavorite: Boolean)
}
