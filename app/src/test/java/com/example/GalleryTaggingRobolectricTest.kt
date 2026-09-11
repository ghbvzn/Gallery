package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.GalleryDatabase
import com.example.data.GalleryRepository
import com.example.data.MediaItem
import com.example.data.MediaType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GalleryTaggingRobolectricTest {

    private lateinit var context: Context
    private lateinit var repository: GalleryRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        repository = GalleryRepository(GalleryDatabase.getDatabase(context).mediaDao())
    }

    @Test
    fun testManualTagsRemainOnDevice() = runBlocking {
        val insertedId = repository.insert(
            MediaItem(
                title = "Mountain Summit Sunrise",
                uriString = "android.resource://com.example/${R.drawable.img_sample_mountain}",
                type = MediaType.PHOTO,
                dateEpochMillis = System.currentTimeMillis(),
                locationName = "Rocky Mountains",
                tags = listOf("Hiking")
            )
        )
        assertTrue(insertedId > 0)

        repository.updateTags(insertedId, listOf("Hiking", "Colorado"))

        val saved = repository.allMedia.first().find { it.id == insertedId }
        assertEquals(listOf("Hiking", "Colorado"), saved?.tags)
    }

    @Test
    fun testExifMetadataHelperBackgroundSafe() = runBlocking {
        val testUri = "android.resource://com.example/${R.drawable.img_sample_beach}"
        val exifData = com.example.data.ExifMetadataHelper.readExifData(context, testUri)
        assertTrue(exifData is com.example.data.MediaExifData)
    }
}
