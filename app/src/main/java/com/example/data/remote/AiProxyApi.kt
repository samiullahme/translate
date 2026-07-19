package com.example.data.remote

import com.example.data.model.OcrResponse
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface AiProxyApi {
    @Multipart
    @POST("api/ocr")
    suspend fun extractText(
        @Header("Authorization") token: String,
        @Part file: MultipartBody.Part
    ): Response<OcrResponse>
}
