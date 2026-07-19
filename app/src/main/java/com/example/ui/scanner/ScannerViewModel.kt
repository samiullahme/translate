package com.example.ui.scanner

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.DocumentInsertRequest
import com.example.data.model.DocumentUpdateRequest
import com.example.data.remote.AiProxyApi
import com.example.data.remote.SupabaseApi
import com.example.data.repository.SessionManager
import com.example.util.ImageCompressor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

enum class PipelineStage {
    IDLE,
    COMPRESSING,
    UPLOADING,
    SCANNING,
    EXTRACT_READY,
    FAILED
}

class ScannerViewModel(
    private val supabaseApi: SupabaseApi,
    private val aiProxyApi: AiProxyApi,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _stage = MutableStateFlow(PipelineStage.IDLE)
    val stage: StateFlow<PipelineStage> = _stage.asStateFlow()

    private val _ocrText = MutableStateFlow<String?>(null)
    val ocrText: StateFlow<String?> = _ocrText.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var currentDocumentId: String? = null
    private var imageUri: Uri? = null

    fun processImage(context: Context, uri: Uri) {
        imageUri = uri
        viewModelScope.launch {
            try {
                _stage.value = PipelineStage.COMPRESSING
                _errorMessage.value = null
                
                val token = sessionManager.getAccessToken() ?: throw Exception("Not authenticated")
                val userId = sessionManager.getUserId() ?: throw Exception("User ID not found")

                // Step 1: Create Document row
                val docReq = DocumentInsertRequest(userId = userId, status = "processing")
                val createRes = supabaseApi.createDocument(
                    apiKey = com.example.BuildConfig.SUPABASE_ANON_KEY,
                    token = "Bearer $token",
                    document = docReq
                )

                if (!createRes.isSuccessful) {
                    throw Exception("Failed to create document: ${createRes.errorBody()?.string()}")
                }
                val createdDocs = createRes.body()
                if (createdDocs.isNullOrEmpty()) {
                    throw Exception("No document returned from creation")
                }
                currentDocumentId = createdDocs[0].id

                // Step 2: Compress
                val compressedFile = ImageCompressor.compressImage(context, uri)
                    ?: throw Exception("Failed to compress image")

                // Step 3: Upload
                _stage.value = PipelineStage.UPLOADING
                val requestFile = compressedFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
                val body = MultipartBody.Part.createFormData("file", compressedFile.name, requestFile)

                // Step 4: Scan (Calling AI Proxy)
                _stage.value = PipelineStage.SCANNING
                val ocrRes = aiProxyApi.extractText("Bearer $token", body)
                if (!ocrRes.isSuccessful) {
                    throw Exception("OCR Failed: ${ocrRes.errorBody()?.string()}")
                }
                
                val extractedText = ocrRes.body()?.text ?: throw Exception("Empty OCR result")
                _ocrText.value = extractedText

                // Step 5: Update Document
                supabaseApi.updateDocument(
                    apiKey = com.example.BuildConfig.SUPABASE_ANON_KEY,
                    token = "Bearer $token",
                    filter = "eq.${currentDocumentId}",
                    document = DocumentUpdateRequest(
                        status = "extract_ready",
                        ocrText = extractedText
                    )
                )

                _stage.value = PipelineStage.EXTRACT_READY
                
                // Cleanup temp file
                compressedFile.delete()
                
            } catch (e: Exception) {
                e.printStackTrace()
                _errorMessage.value = e.localizedMessage
                _stage.value = PipelineStage.FAILED
                
                // Try to update document status to failed
                currentDocumentId?.let { docId ->
                    val token = sessionManager.getAccessToken()
                    if (token != null) {
                        try {
                            supabaseApi.updateDocument(
                                apiKey = com.example.BuildConfig.SUPABASE_ANON_KEY,
                                token = "Bearer $token",
                                filter = "eq.$docId",
                                document = DocumentUpdateRequest(
                                    status = "failed",
                                    errorMessage = e.localizedMessage
                                )
                            )
                        } catch (ignored: Exception) {}
                    }
                }
            }
        }
    }
    
    fun reset() {
        _stage.value = PipelineStage.IDLE
        _ocrText.value = null
        _errorMessage.value = null
        currentDocumentId = null
        imageUri = null
    }
    
    fun retry(context: Context) {
        imageUri?.let { uri ->
            processImage(context, uri)
        }
    }
    
    fun cancel() {
        // Simple cancel implementation. Proper cancellation requires Job handling.
        reset()
    }
}
