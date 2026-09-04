package com.example.data

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class MediaAnalysisResult(
    val suggestedTags: List<String> = emptyList(),
    val summary: String = ""
)
