package com.example.data

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.media.MediaScannerConnection
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

data class PhotoCrop(val width: Float = 1f, val height: Float = 1f,
    val horizontal: Float = 0.5f, val vertical: Float = 0.5f)

object PhotoEdits {
    fun load(context: Context, uri: Uri, maxDimension: Int): Bitmap {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Cannot read this photo" }
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > maxDimension) sample *= 2
        val decoded = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: error("Cannot open this photo")
        val orientation = try {
            resolver.openInputStream(uri)?.use {
                ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            } ?: ExifInterface.ORIENTATION_NORMAL
        } catch (_: java.io.IOException) { ExifInterface.ORIENTATION_NORMAL }
        val matrix = Matrix().apply {
            when (orientation) {
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> setScale(-1f, 1f)
                ExifInterface.ORIENTATION_ROTATE_180 -> setRotate(180f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> setScale(1f, -1f)
                ExifInterface.ORIENTATION_TRANSPOSE -> { setRotate(90f); postScale(-1f, 1f) }
                ExifInterface.ORIENTATION_ROTATE_90 -> setRotate(90f)
                ExifInterface.ORIENTATION_TRANSVERSE -> { setRotate(270f); postScale(-1f, 1f) }
                ExifInterface.ORIENTATION_ROTATE_270 -> setRotate(270f)
            }
        }
        val oriented = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
        if (oriented !== decoded) decoded.recycle()
        return oriented
    }

    fun transform(source: Bitmap, crop: PhotoCrop, turns: Int): Bitmap {
        val width = (source.width * crop.width.coerceIn(0.1f, 1f)).toInt().coerceIn(1, source.width)
        val height = (source.height * crop.height.coerceIn(0.1f, 1f)).toInt().coerceIn(1, source.height)
        val left = ((source.width - width) * crop.horizontal.coerceIn(0f, 1f)).toInt()
        val top = ((source.height - height) * crop.vertical.coerceIn(0f, 1f)).toInt()
        return Bitmap.createBitmap(source, left, top, width, height,
            Matrix().apply { postRotate(((turns % 4 + 4) % 4) * 90f) }, true)
    }

    suspend fun saveCopy(context: Context, sourceUri: Uri, crop: PhotoCrop, turns: Int): Pair<Uri, String> {
        val source = load(context, sourceUri, 4096)
        var output: Bitmap? = null
        try {
            val edited = transform(source, crop, turns)
            output = edited
            val name = "Gallery_edit_${UUID.randomUUID()}.jpg"
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, name)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.WIDTH, edited.width)
                put(MediaStore.Images.Media.HEIGHT, edited.height)
                if (Build.VERSION.SDK_INT >= 29) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Gallery")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }
            val uri = if (Build.VERSION.SDK_INT >= 29) {
                val inserted = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    ?: error("Cannot create edited copy")
                try {
                    resolver.openOutputStream(inserted)?.use {
                        check(edited.compress(Bitmap.CompressFormat.JPEG, 95, it)) { "Cannot encode copy" }
                    } ?: error("Cannot write copy")
                    check(resolver.update(inserted, ContentValues().apply {
                        put(MediaStore.Images.Media.IS_PENDING, 0)
                    }, null, null) > 0) { "Cannot publish copy" }
                } catch (e: Exception) {
                    resolver.delete(inserted, null, null)
                    throw e
                }
                inserted
            } else {
                val directory = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Gallery")
                check(directory.exists() || directory.mkdirs()) { "Cannot create Pictures/Gallery" }
                val file = File(directory, name)
                try {
                    file.outputStream().use { check(edited.compress(Bitmap.CompressFormat.JPEG, 95, it)) }
                    suspendCancellableCoroutine<Uri> { continuation ->
                        MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), arrayOf("image/jpeg")) { _, scanned ->
                            if (continuation.isActive) {
                                if (scanned != null) continuation.resume(scanned)
                                else continuation.resumeWithException(IllegalStateException("Cannot index copy"))
                            }
                        }
                    }
                } catch (e: Exception) { file.delete(); throw e }
            }
            return uri to "${edited.width}x${edited.height}"
        } finally {
            if (output !== source) output?.recycle()
            source.recycle()
        }
    }
}
