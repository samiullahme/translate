package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SignUpRequest(
    val email: String,
    val password: String,
    val data: Map<String, String>? = null
)

@JsonClass(generateAdapter = true)
data class SignInRequest(
    val email: String,
    val password: String
)

@JsonClass(generateAdapter = true)
data class UserMetadata(
    val name: String? = null,
    @Json(name = "avatar_url") val avatarUrl: String? = null
)

@JsonClass(generateAdapter = true)
data class AuthUser(
    val id: String,
    val email: String?,
    @Json(name = "user_metadata") val userMetadata: UserMetadata? = null
)

@JsonClass(generateAdapter = true)
data class AuthResponse(
    @Json(name = "access_token") val accessToken: String,
    @Json(name = "token_type") val tokenType: String,
    @Json(name = "expires_in") val expiresIn: Long,
    @Json(name = "refresh_token") val refreshToken: String?,
    val user: AuthUser
)

@JsonClass(generateAdapter = true)
data class Profile(
    val id: String,
    val email: String?,
    val name: String?,
    @Json(name = "avatar_url") val avatarUrl: String?,
    @Json(name = "default_source_language") val defaultSourceLanguage: String = "auto",
    @Json(name = "default_target_language") val defaultTargetLanguage: String = "en",
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "updated_at") val updatedAt: String? = null
)
