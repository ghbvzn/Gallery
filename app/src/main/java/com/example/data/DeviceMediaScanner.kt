package com.example.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DeviceMediaScanner(private val context: Context) {

    suspend fun scanDeviceMedia(): List<MediaItem> = withContext(Dispatchers.IO) {
        val mediaList = mutableListOf<MediaItem>()
        try {
            mediaList.addAll(queryImages())
            mediaList.addAll(queryVideos())
        } catch (e: Exception) {
            Log.e("DeviceMediaScanner", "Error querying device media", e)
        }
        mediaList.sortedByDescending { it.dateEpochMillis }
    }

    private fun queryImages(): List<MediaItem> {
        val items = mutableListOf<MediaItem>()
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.DATE_MODIFIED,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME
        )

        val sortOrder = "${MediaStore.Images.Media.DATE_MODIFIED} DESC"

        val cursor = context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            sortOrder
        )

        cursor?.use {
            val idColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameColumn = it.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
            val dateAddedColumn = it.getColumnIndex(MediaStore.Images.Media.DATE_ADDED)
            val dateModifiedColumn = it.getColumnIndex(MediaStore.Images.Media.DATE_MODIFIED)
            val widthColumn = it.getColumnIndex(MediaStore.Images.Media.WIDTH)
            val heightColumn = it.getColumnIndex(MediaStore.Images.Media.HEIGHT)
            val bucketColumn = it.getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)

            while (it.moveToNext()) {
                val id = it.getLong(idColumn)
                val contentUri: Uri = ContentUris.withAppendedId(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    id
                )

                val displayName = if (nameColumn != -1) it.getString(nameColumn) ?: "Photo" else "Photo"
                val dateModified = if (dateModifiedColumn != -1) it.getLong(dateModifiedColumn) else 0L
                val dateAdded = if (dateAddedColumn != -1) it.getLong(dateAddedColumn) else 0L
                val timeSec = if (dateModified > 0) dateModified else dateAdded
                val timeMillis = if (timeSec > 0) timeSec * 1000L else System.currentTimeMillis()

                val width = if (widthColumn != -1) it.getInt(widthColumn) else 0
                val height = if (heightColumn != -1) it.getInt(heightColumn) else 0
                val resolution = if (width > 0 && height > 0) "${width}x$height" else "Standard Photo"

                val bucket = if (bucketColumn != -1) it.getString(bucketColumn) ?: "Device Photos" else "Device Photos"

                val cleanTitle = displayName.substringBeforeLast(".")
                    .replace('_', ' ')
                    .replace('-', ' ')
                    .trim()
                    .ifBlank { "Photo" }

                items.add(
                    MediaItem(
                        id = 0L,
                        uriString = contentUri.toString(),
                        title = cleanTitle,
                        type = MediaType.PHOTO,
                        dateEpochMillis = timeMillis,
                        locationName = bucket,
                        resolution = resolution,
                        notes = "Device image: $displayName"
                    )
                )
            }
        }
        return items
    }

    private fun queryVideos(): List<MediaItem> {
        val items = mutableListOf<MediaItem>()
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.DATE_MODIFIED,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME
        )

        val sortOrder = "${MediaStore.Video.Media.DATE_MODIFIED} DESC"

        val cursor = context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            sortOrder
        )

        cursor?.use {
            val idColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameColumn = it.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME)
            val dateAddedColumn = it.getColumnIndex(MediaStore.Video.Media.DATE_ADDED)
            val dateModifiedColumn = it.getColumnIndex(MediaStore.Video.Media.DATE_MODIFIED)
            val widthColumn = it.getColumnIndex(MediaStore.Video.Media.WIDTH)
            val heightColumn = it.getColumnIndex(MediaStore.Video.Media.HEIGHT)
            val durationColumn = it.getColumnIndex(MediaStore.Video.Media.DURATION)
            val bucketColumn = it.getColumnIndex(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)

            while (it.moveToNext()) {
                val id = it.getLong(idColumn)
                val contentUri: Uri = ContentUris.withAppendedId(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    id
                )

                val displayName = if (nameColumn != -1) it.getString(nameColumn) ?: "Video" else "Video"
                val dateModified = if (dateModifiedColumn != -1) it.getLong(dateModifiedColumn) else 0L
                val dateAdded = if (dateAddedColumn != -1) it.getLong(dateAddedColumn) else 0L
                val timeSec = if (dateModified > 0) dateModified else dateAdded
                val timeMillis = if (timeSec > 0) timeSec * 1000L else System.currentTimeMillis()

                val width = if (widthColumn != -1) it.getInt(widthColumn) else 0
                val height = if (heightColumn != -1) it.getInt(heightColumn) else 0
                val durationMs = if (durationColumn != -1) it.getLong(durationColumn) else 0L
                val durationSeconds = (durationMs / 1000L).toInt()

                val resolution = if (width > 0 && height > 0) "${width}x$height Video" else "Standard Video"
                val bucket = if (bucketColumn != -1) it.getString(bucketColumn) ?: "Device Videos" else "Device Videos"

                val cleanTitle = displayName.substringBeforeLast(".")
                    .replace('_', ' ')
                    .replace('-', ' ')
                    .trim()
                    .ifBlank { "Video" }

                items.add(
                    MediaItem(
                        id = 0L,
                        uriString = contentUri.toString(),
                        title = cleanTitle,
                        type = MediaType.VIDEO,
                        dateEpochMillis = timeMillis,
                        locationName = bucket,
                        durationSeconds = durationSeconds,
                        resolution = resolution,
                        notes = "Device video: $displayName"
                    )
                )
            }
        }
        return items
    }
}
