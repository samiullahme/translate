package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Block(
    val text: String
)

@JsonClass(generateAdapter = true)
data class OcrResponse(
    val text: String,
    val blocks: List<Block>
)
