package com.example.data

import android.content.Context
import android.media.ExifInterface
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Immutable data holder for EXIF metadata extracted from media files.
 */
@Immutable
data class MediaExifData(
    val cameraMake: String? = null,
    val cameraModel: String? = null,
    val aperture: String? = null,
    val iso: String? = null,
    val exposureTime: String? = null,
    val focalLength: String? = null,
    val dateTime: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val imageWidth: Int = 0,
    val imageHeight: Int = 0,
    val orientation: Int = ExifInterface.ORIENTATION_NORMAL
) {
    val hasCameraDetails: Boolean
        get() = !cameraModel.isNullOrBlank() || !cameraMake.isNullOrBlank() ||
                !aperture.isNullOrBlank() || !iso.isNullOrBlank() || !exposureTime.isNullOrBlank()

    val formattedCamera: String?
        get() {
            return when {
                !cameraMake.isNullOrBlank() && !cameraModel.isNullOrBlank() -> {
                    if (cameraModel.startsWith(cameraMake, ignoreCase = true)) cameraModel
                    else "$cameraMake $cameraModel"
                }
                !cameraModel.isNullOrBlank() -> cameraModel
                !cameraMake.isNullOrBlank() -> cameraMake
                else -> null
            }
        }

    val formattedShootingSpecs: String?
        get() {
            val specs = mutableListOf<String>()
            focalLength?.let { specs.add(it) }
            aperture?.let { specs.add("f/$it") }
            exposureTime?.let { specs.add("${it}s") }
            iso?.let { specs.add("ISO $it") }
            return if (specs.isNotEmpty()) specs.joinToString(" • ") else null
        }
}

/**
 * Helper to safely extract EXIF metadata on background threads (Dispatchers.IO).
 * Ensures heavy disk I/O and header parsing NEVER run on the main UI thread.
 */
object ExifMetadataHelper {

    private const val TAG = "ExifMetadataHelper"

    /**
     * Reads EXIF data for a given content or file URI strictly on Dispatchers.IO.
     */
    suspend fun readExifData(context: Context, uriString: String): MediaExifData = withContext(Dispatchers.IO) {
        if (uriString.isBlank() || uriString.startsWith("android.resource://")) {
            return@withContext MediaExifData()
        }

        val uri = try {
            Uri.parse(uriString)
        } catch (e: Exception) {
            return@withContext MediaExifData()
        }

        try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val exifInterface = ExifInterface(stream)
                val make = exifInterface.getAttribute(ExifInterface.TAG_MAKE)?.trim()
                val model = exifInterface.getAttribute(ExifInterface.TAG_MODEL)?.trim()
                val aperture = exifInterface.getAttribute(ExifInterface.TAG_F_NUMBER)
                val iso = exifInterface.getAttribute(ExifInterface.TAG_ISO_SPEED_RATINGS)
                    ?: exifInterface.getAttribute("PhotographicSensitivity")
                val exposure = exifInterface.getAttribute(ExifInterface.TAG_EXPOSURE_TIME)
                val focal = exifInterface.getAttribute(ExifInterface.TAG_FOCAL_LENGTH)
                val dateTime = exifInterface.getAttribute(ExifInterface.TAG_DATETIME)
                val width = exifInterface.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, 0)
                val height = exifInterface.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, 0)
                val orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)

                val latLong = FloatArray(2)
                val hasLocation = exifInterface.getLatLong(latLong)

                MediaExifData(
                    cameraMake = make?.takeIf { it.isNotBlank() },
                    cameraModel = model?.takeIf { it.isNotBlank() },
                    aperture = aperture?.takeIf { it.isNotBlank() },
                    iso = iso?.takeIf { it.isNotBlank() },
                    exposureTime = formatExposureTime(exposure),
                    focalLength = formatFocalLength(focal),
                    dateTime = dateTime?.takeIf { it.isNotBlank() },
                    latitude = if (hasLocation) latLong[0].toDouble() else null,
                    longitude = if (hasLocation) latLong[1].toDouble() else null,
                    imageWidth = width,
                    imageHeight = height,
                    orientation = orientation
                )
            } ?: MediaExifData()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to read EXIF for $uriString: ${e.message}")
            MediaExifData()
        }
    }

    private fun formatExposureTime(exposure: String?): String? {
        if (exposure.isNullOrBlank()) return null
        return try {
            val value = exposure.toDouble()
            if (value <= 0) return null
            if (value < 1.0) {
                val denominator = (1.0 / value).toInt()
                "1/$denominator"
            } else {
                String.format(java.util.Locale.US, "%.1f", value)
            }
        } catch (e: Exception) {
            exposure
        }
    }

    private fun formatFocalLength(focal: String?): String? {
        if (focal.isNullOrBlank()) return null
        return try {
            val value = focal.toDouble()
            "${value.toInt()} mm"
        } catch (e: Exception) {
            focal
        }
    }
}
