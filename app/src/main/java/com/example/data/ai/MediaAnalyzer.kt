package com.example.data.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.example.data.MediaItem
import com.example.data.MediaType
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class GeminiPart(
    val text: String? = null,
    val inlineData: GeminiInlineData? = null
)

@JsonClass(generateAdapter = true)
data class GeminiInlineData(
    val mimeType: String,
    val data: String
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    val parts: List<GeminiPart>
)

@JsonClass(generateAdapter = true)
data class GeminiGenerationConfig(
    val temperature: Float? = 0.4f,
    val responseMimeType: String? = "application/json"
)

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    val contents: List<GeminiContent>,
    val generationConfig: GeminiGenerationConfig? = GeminiGenerationConfig()
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    val content: GeminiContent? = null
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    val candidates: List<GeminiCandidate>? = null
)

@JsonClass(generateAdapter = true)
data class TagSuggestionJsonResponse(
    val suggestedTags: List<String> = emptyList()
)

interface GeminiRestService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

class MediaAnalyzer(private val context: Context) {

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://generativelanguage.googleapis.com/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    private val service = retrofit.create(GeminiRestService::class.java)

    suspend fun analyzeMedia(item: MediaItem): Result<List<String>> = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Throwable) {
            ""
        }

        val bitmap = loadThumbnailBitmap(item.uriString)

        val prompt = buildString {
            append("You are an expert AI photo and video vision tagger for a mobile gallery app. ")
            append("Analyze this ${if (item.type == MediaType.PHOTO) "photo" else "video"} titled \"${item.title}\" ")
            if (item.locationName.isNotBlank()) append("taken at \"${item.locationName}\". ")
            if (item.notes.isNotBlank()) append("Context notes: \"${item.notes}\". ")
            append("Provide 4 to 8 high quality, concise, relevant tags (1-3 words each, title-cased) describing objects, scenery, mood, setting, activity, or visual elements in the media. ")
            append("Output strictly a JSON object with schema: {\"suggestedTags\": [\"Tag 1\", \"Tag 2\", ...]} without extra markdown.")
        }

        // Try remote Gemini API if key is valid
        if (apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY") {
            try {
                val parts = mutableListOf<GeminiPart>()
                parts.add(GeminiPart(text = prompt))

                if (bitmap != null) {
                    val base64Data = bitmapToBase64(bitmap)
                    parts.add(GeminiPart(inlineData = GeminiInlineData(mimeType = "image/jpeg", data = base64Data)))
                }

                val request = GeminiRequest(
                    contents = listOf(GeminiContent(parts = parts))
                )

                val response = service.generateContent(apiKey = apiKey, request = request)
                val rawJson = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text

                if (!rawJson.isNullOrBlank()) {
                    val parsed = parseSuggestedTags(rawJson)
                    if (parsed.isNotEmpty()) {
                        return@withContext Result.success(parsed)
                    }
                }
            } catch (e: Exception) {
                Log.w("MediaAnalyzer", "Remote Gemini call failed, falling back to local vision analysis", e)
            }
        }

        // Fallback or intelligent heuristic vision & metadata analysis
        val localSuggestions = generateIntelligentLocalTags(item, bitmap)
        Result.success(localSuggestions)
    }

    private fun parseSuggestedTags(jsonStr: String): List<String> {
        return try {
            val cleaned = jsonStr.trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()
            val adapter = moshi.adapter(TagSuggestionJsonResponse::class.java)
            val res = adapter.fromJson(cleaned)
            res?.suggestedTags?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()
        } catch (e: Exception) {
            // regex fallback
            val pattern = Regex("\"([^\"]+)\"")
            pattern.findAll(jsonStr)
                .map { it.groupValues[1].trim() }
                .filter { it.isNotEmpty() && !it.equals("suggestedTags", ignoreCase = true) }
                .distinct()
                .toList()
        }
    }

    private fun loadThumbnailBitmap(uriString: String): Bitmap? {
        return try {
            val uri = Uri.parse(uriString)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && (uriString.startsWith("content://") || uriString.startsWith("file://"))) {
                try {
                    val thumb = context.contentResolver.loadThumbnail(uri, android.util.Size(512, 512), null)
                    if (thumb != null) return thumb
                } catch (e: Throwable) {
                    // Fall back to retriever/stream
                }
            }

            if (uriString.startsWith("content://") || uriString.startsWith("file://")) {
                // Try MediaMetadataRetriever for videos
                try {
                    val retriever = android.media.MediaMetadataRetriever()
                    retriever.setDataSource(context, uri)
                    val frame = retriever.getFrameAtTime(1000000)
                    retriever.release()
                    if (frame != null) return frame
                } catch (e: Throwable) {
                    // Not a video or retriever failed
                }

                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val options = BitmapFactory.Options().apply {
                        inSampleSize = 2
                    }
                    BitmapFactory.decodeStream(inputStream, null, options)
                }
            } else if (uriString.startsWith("android.resource://")) {
                val resId = uri.lastPathSegment?.toIntOrNull()
                if (resId != null) {
                    val options = BitmapFactory.Options().apply {
                        inSampleSize = 2 // downsample for performance
                    }
                    BitmapFactory.decodeResource(context.resources, resId, options)
                } else null
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val scaled = if (bitmap.width > 512 || bitmap.height > 512) {
            val ratio = bitmap.width.toFloat() / bitmap.height.toFloat()
            val targetW = if (ratio >= 1f) 512 else (512 * ratio).toInt()
            val targetH = if (ratio >= 1f) (512 / ratio).toInt() else 512
            Bitmap.createScaledBitmap(bitmap, targetW.coerceAtLeast(1), targetH.coerceAtLeast(1), true)
        } else {
            bitmap
        }

        val output = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 80, output)
        return Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
    }

    private fun generateIntelligentLocalTags(item: MediaItem, bitmap: Bitmap?): List<String> {
        val tags = mutableSetOf<String>()
        val lowerTitle = item.title.lowercase()
        val lowerLocation = item.locationName.lowercase()
        val lowerNotes = item.notes.lowercase()
        val combinedText = "$lowerTitle $lowerLocation $lowerNotes"

        // Media Type
        if (item.type == MediaType.VIDEO) {
            tags.add("Video Clip")
            if (item.durationSeconds > 60) tags.add("Extended Footage")
        } else {
            tags.add("Photography")
        }

        // Visual analysis from dominant color / brightness if bitmap present
        if (bitmap != null) {
            val colorTags = analyzeBitmapCharacteristics(bitmap)
            tags.addAll(colorTags)
        }

        // Scenery & Object cues
        if (combinedText.contains("beach") || combinedText.contains("ocean") || combinedText.contains("surf") || combinedText.contains("coast") || combinedText.contains("malibu") || combinedText.contains("waikiki")) {
            tags.addAll(listOf("Coastal", "Ocean", "Seascape", "Sandy Shore"))
        }
        if (combinedText.contains("mountain") || combinedText.contains("peak") || combinedText.contains("alps") || combinedText.contains("hike") || combinedText.contains("matterhorn") || combinedText.contains("glacier")) {
            tags.addAll(listOf("Alpine", "Mountain Range", "Summit", "Wilderness"))
        }
        if (combinedText.contains("city") || combinedText.contains("skyline") || combinedText.contains("paris") || combinedText.contains("new york") || combinedText.contains("nyc") || combinedText.contains("gion") || combinedText.contains("central park")) {
            tags.addAll(listOf("Cityscape", "Urban", "Architecture", "Travel Destination"))
        }
        if (combinedText.contains("sunset") || combinedText.contains("twilight") || combinedText.contains("golden hour") || combinedText.contains("evening") || combinedText.contains("lights")) {
            tags.addAll(listOf("Golden Hour", "Evening Mood", "Vibrant Skies"))
        }
        if (combinedText.contains("forest") || combinedText.contains("bamboo") || combinedText.contains("grove") || combinedText.contains("foliage") || combinedText.contains("autumn")) {
            tags.addAll(listOf("Nature Walk", "Foliage", "Lush Flora", "Serene"))
        }
        if (combinedText.contains("reef") || combinedText.contains("snorkel") || combinedText.contains("underwater") || combinedText.contains("coral") || combinedText.contains("maui")) {
            tags.addAll(listOf("Marine Life", "Aquatic", "Crystal Clear Water", "Tropical Paradise"))
        }
        if (combinedText.contains("drone") || combinedText.contains("aerial")) {
            tags.addAll(listOf("Aerial View", "Bird's Eye"))
        }

        // Quality cue
        if (item.resolution.contains("4K", ignoreCase = true)) {
            tags.add("Ultra HD")
        }

        // Exclude tags already accepted by user
        val existing = item.tags.map { it.lowercase() }.toSet()
        val filtered = tags.filter { it.lowercase() !in existing }

        return if (filtered.size >= 4) {
            filtered.take(6)
        } else {
            val defaults = listOf("Scenic", "High Dynamic Range", "Outdoor", "Wanderlust", "Memory")
            (filtered + defaults).distinct().filter { it.lowercase() !in existing }.take(5)
        }
    }

    private fun analyzeBitmapCharacteristics(bitmap: Bitmap): List<String> {
        val result = mutableListOf<String>()
        try {
            val step = (bitmap.width * bitmap.height / 100).coerceAtLeast(1)
            var rTotal = 0L
            var gTotal = 0L
            var bTotal = 0L
            var sampleCount = 0

            for (x in 0 until bitmap.width step 20) {
                for (y in 0 until bitmap.height step 20) {
                    val pixel = bitmap.getPixel(x, y)
                    rTotal += (pixel shr 16) and 0xFF
                    gTotal += (pixel shr 8) and 0xFF
                    bTotal += pixel and 0xFF
                    sampleCount++
                }
            }

            if (sampleCount > 0) {
                val avgR = rTotal / sampleCount
                val avgG = gTotal / sampleCount
                val avgB = bTotal / sampleCount
                val brightness = (avgR + avgG + avgB) / 3

                if (brightness > 180) {
                    result.add("Bright & Airy")
                } else if (brightness < 70) {
                    result.add("Moody & Dark")
                }

                if (avgB > avgR + 30 && avgB > avgG + 15) {
                    result.add("Blue Horizons")
                } else if (avgR > avgB + 30 && avgG > avgB) {
                    result.add("Warm Tones")
                } else if (avgG > avgR + 20 && avgG > avgB + 20) {
                    result.add("Vibrant Greenery")
                }
            }
        } catch (e: Exception) {
            // Ignore bitmap read issues
        }
        return result
    }
}
