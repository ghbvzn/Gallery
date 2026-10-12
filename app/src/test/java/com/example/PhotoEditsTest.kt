package com.example

import android.graphics.Bitmap
import android.graphics.Color
import com.example.data.PhotoCrop
import com.example.data.PhotoEdits
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PhotoEditsTest {
    @Test fun cropPositionSelectsTheExpectedPixelsAndKeepsOriginal() {
        val source = Bitmap.createBitmap(100, 80, Bitmap.Config.ARGB_8888)
        source.eraseColor(Color.RED)
        source.setPixel(50, 40, Color.BLUE)
        val edited = PhotoEdits.transform(source, PhotoCrop(0.5f, 0.5f, 1f, 1f), 0)
        assertEquals(50, edited.width)
        assertEquals(40, edited.height)
        assertEquals(Color.BLUE, edited.getPixel(0, 0))
        assertEquals(100, source.width)
        assertEquals(Color.BLUE, source.getPixel(50, 40))
        assertFalse(source.isRecycled)
    }

    @Test fun quarterTurnSwapsDimensionsAfterCropping() {
        val source = Bitmap.createBitmap(100, 80, Bitmap.Config.ARGB_8888)
        val edited = PhotoEdits.transform(source, PhotoCrop(0.5f, 1f), 1)
        assertEquals(80, edited.width)
        assertEquals(50, edited.height)
        assertEquals(100, source.width)
    }

    @Test fun invalidCropInputStaysInsideBitmap() {
        val source = Bitmap.createBitmap(20, 10, Bitmap.Config.ARGB_8888)
        val edited = PhotoEdits.transform(source, PhotoCrop(0f, 3f, -1f, 5f), 4)
        assertEquals(2, edited.width)
        assertEquals(10, edited.height)
    }
}
