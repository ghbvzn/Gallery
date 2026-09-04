package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "media_items")
data class MediaItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val uriString: String,
    val title: String,
    val type: MediaType,
    val dateEpochMillis: Long,
    val locationName: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val durationSeconds: Int = 0,
    val isFavorite: Boolean = false,
    val resolution: String = "",
    val notes: String = "",
    val tags: List<String> = emptyList(),
    val suggestedTags: List<String> = emptyList()
)
