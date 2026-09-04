package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.GalleryDatabase
import com.example.data.GalleryRepository
import com.example.data.MediaItem
import com.example.data.MediaType
import com.example.data.ai.MediaAnalyzer
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
    private lateinit var db: GalleryDatabase
    private lateinit var repository: GalleryRepository
    private lateinit var analyzer: MediaAnalyzer

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = GalleryDatabase.getDatabase(context)
        repository = GalleryRepository(db.mediaDao())
        analyzer = MediaAnalyzer(context)
    }

    @Test
    fun testAddAndAcceptAiSuggestedTags() = runBlocking {
        val testItem = MediaItem(
            title = "Mountain Summit Sunrise",
            uriString = "android.resource://com.example/${R.drawable.img_sample_mountain}",
            type = MediaType.PHOTO,
            dateEpochMillis = System.currentTimeMillis(),
            locationName = "Rocky Mountains",
            notes = "Hiking high elevation trail at dawn",
            tags = listOf("Hiking"),
            suggestedTags = listOf("Summit", "Sunrise", "Alpine")
        )

        val insertedId = repository.insert(testItem)
        assertTrue(insertedId > 0)

        val all = repository.allMedia.first()
        val found = all.find { it.id == insertedId }
        assertTrue(found != null)
        assertEquals(listOf("Hiking"), found?.tags)
        assertEquals(listOf("Summit", "Sunrise", "Alpine"), found?.suggestedTags)

        // Accept a suggested tag
        val updatedTags = (found!!.tags + "Summit").distinct()
        val updatedSuggestions = found.suggestedTags.filterNot { it == "Summit" }
        repository.updateTagsAndSuggestions(insertedId, updatedTags, updatedSuggestions)

        val afterAccept = repository.allMedia.first().find { it.id == insertedId }
        assertEquals(listOf("Hiking", "Summit"), afterAccept?.tags)
        assertEquals(listOf("Sunrise", "Alpine"), afterAccept?.suggestedTags)

        // Reject a suggested tag
        val afterRejectSuggestions = afterAccept!!.suggestedTags.filterNot { it == "Sunrise" }
        repository.updateSuggestedTags(insertedId, afterRejectSuggestions)

        val afterReject = repository.allMedia.first().find { it.id == insertedId }
        assertEquals(listOf("Alpine"), afterReject?.suggestedTags)

        // Add a custom user tag
        val finalTags = (afterReject!!.tags + "Colorado").distinct()
        repository.updateTags(insertedId, finalTags)

        val finalItem = repository.allMedia.first().find { it.id == insertedId }
        assertEquals(listOf("Hiking", "Summit", "Colorado"), finalItem?.tags)
    }

    @Test
    fun testLocalMediaAnalyzerFallback() = runBlocking {
        val testItem = MediaItem(
            title = "Pacific Ocean Sunset Surf",
            uriString = "android.resource://com.example/${R.drawable.img_sample_beach}",
            type = MediaType.PHOTO,
            dateEpochMillis = System.currentTimeMillis(),
            locationName = "Malibu Coast",
            notes = "Sunset waves with surfers",
            tags = emptyList()
        )

        val result = analyzer.analyzeMedia(testItem)
        assertTrue(result.isSuccess)
        val suggestions = result.getOrNull() ?: emptyList()
        assertTrue("Expected suggestions to not be empty", suggestions.isNotEmpty())
        assertTrue("Expected coastal/sunset related tags", suggestions.any {
            it.contains("Coastal", ignoreCase = true) ||
            it.contains("Ocean", ignoreCase = true) ||
            it.contains("Golden Hour", ignoreCase = true) ||
            it.contains("Photography", ignoreCase = true)
        })
    }
}
