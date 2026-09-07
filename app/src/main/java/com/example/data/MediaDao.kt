package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
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

    @Query("SELECT DISTINCT locationName FROM media_items WHERE locationName != '' ORDER BY locationName ASC")
    fun getAllLocations(): Flow<List<String>>

    @Query("SELECT COUNT(*) FROM media_items")
    suspend fun getMediaCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: MediaItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<MediaItem>)

    @Update
    suspend fun update(item: MediaItem)

    @Query("UPDATE media_items SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun setFavorite(id: Long, isFavorite: Boolean)

    @Query("UPDATE media_items SET title = :title, locationName = :location, dateEpochMillis = :dateEpochMillis WHERE id = :id")
    suspend fun updateMetadata(id: Long, title: String, location: String, dateEpochMillis: Long)

    @Query("UPDATE media_items SET tags = :tags WHERE id = :id")
    suspend fun updateTags(id: Long, tags: List<String>)

    @Query("UPDATE media_items SET suggestedTags = :suggestedTags WHERE id = :id")
    suspend fun updateSuggestedTags(id: Long, suggestedTags: List<String>)

    @Query("UPDATE media_items SET tags = :tags, suggestedTags = :suggestedTags WHERE id = :id")
    suspend fun updateTagsAndSuggestions(id: Long, tags: List<String>, suggestedTags: List<String>)

    @Delete
    suspend fun delete(item: MediaItem)

    @Query("DELETE FROM media_items WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM media_items WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("UPDATE media_items SET isFavorite = :isFavorite WHERE id IN (:ids)")
    suspend fun setFavoriteBatch(ids: List<Long>, isFavorite: Boolean)
}
