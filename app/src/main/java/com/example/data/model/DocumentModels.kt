package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class DocumentInsertRequest(
    @Json(name = "user_id") val userId: String,
    val status: String,
    @Json(name = "original_name") val originalName: String? = null,
    @Json(name = "storage_path") val storagePath: String? = null
)

@JsonClass(generateAdapter = true)
data class DocumentUpdateRequest(
    val status: String? = null,
    @Json(name = "ocr_text") val ocrText: String? = null,
    @Json(name = "error_message") val errorMessage: String? = null
)

@JsonClass(generateAdapter = true)
data class Document(
    val id: String,
    @Json(name = "user_id") val userId: String,
    val status: String,
    @Json(name = "original_name") val originalName: String?,
    @Json(name = "storage_path") val storagePath: String?,
    @Json(name = "ocr_text") val ocrText: String?,
    @Json(name = "error_message") val errorMessage: String?,
    @Json(name = "created_at") val createdAt: String,
    @Json(name = "updated_at") val updatedAt: String
)
