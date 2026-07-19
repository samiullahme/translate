package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.example.data.model.*
import com.example.data.remote.SupabaseApi
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

class AuthRepository(context: Context) {
    val sessionManager = SessionManager(context)
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    
    private val _currentProfile = MutableStateFlow<Profile?>(null)
    val currentProfile: StateFlow<Profile?> = _currentProfile

    private val _isCheckingSession = MutableStateFlow(true)
    val isCheckingSession: StateFlow<Boolean> = _isCheckingSession

    var supabaseApi: SupabaseApi? = null
    private val supabaseUrl = BuildConfig.SUPABASE_URL
    private val supabaseAnonKey = BuildConfig.SUPABASE_ANON_KEY

    init {
        initRetrofit()
        hydrateSession()
    }

    private fun initRetrofit() {
        if (supabaseUrl.isEmpty() || supabaseUrl.startsWith("https://your-supabase-project")) {
            Log.e("AuthRepository", "Supabase URL is not configured or is using default placeholder.")
            return
        }

        try {
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val okHttpClient = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .addInterceptor(loggingInterceptor)
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(if (supabaseUrl.endsWith("/")) supabaseUrl else "$supabaseUrl/")
                .client(okHttpClient)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()

            supabaseApi = retrofit.create(SupabaseApi::class.java)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Failed to initialize Retrofit for Supabase: ${e.message}", e)
        }
    }

    private fun hydrateSession() {
        if (sessionManager.isLoggedIn()) {
            val userId = sessionManager.getUserId() ?: ""
            val email = sessionManager.getUserEmail()
            val name = sessionManager.getProfileName()
            val sourceLang = sessionManager.getDefaultSourceLanguage()
            val targetLang = sessionManager.getDefaultTargetLanguage()
            
            _currentProfile.value = Profile(
                id = userId,
                email = email,
                name = name,
                avatarUrl = null,
                defaultSourceLanguage = sourceLang,
                defaultTargetLanguage = targetLang
            )
            
            // Try to sync/fetch freshest profile details in background
            _isCheckingSession.value = false
        } else {
            _isCheckingSession.value = false
        }
    }

    suspend fun signUp(email: String, password: String, name: String?): Result<Profile> {
        val api = supabaseApi ?: return Result.failure(Exception("Supabase API is not initialized. Please configure SUPABASE_URL and SUPABASE_ANON_KEY in your secrets."))
        
        try {
            val metadata = if (name != null) mapOf("name" to name) else null
            val response = api.signUp(
                apiKey = supabaseAnonKey,
                request = SignUpRequest(email = email, password = password, data = metadata)
            )

            if (response.isSuccessful) {
                val bodyString = response.body()?.string() ?: ""
                if (bodyString.isEmpty()) {
                    return Result.failure(Exception("Empty response body from Supabase"))
                }

                if (bodyString.contains("\"access_token\"")) {
                    // Scenario A: Email confirmation is DISABLED - active session returned
                    val adapter = moshi.adapter(AuthResponse::class.java)
                    val authRes = adapter.fromJson(bodyString)
                    if (authRes != null) {
                        sessionManager.saveSession(
                            accessToken = authRes.accessToken,
                            refreshToken = authRes.refreshToken,
                            userId = authRes.user.id,
                            email = authRes.user.email
                        )
                        
                        val profileName = name ?: authRes.user.userMetadata?.name ?: email.substringBefore("@")
                        sessionManager.saveProfile(profileName, "auto", "en")
                        
                        val newProfile = Profile(
                            id = authRes.user.id,
                            email = authRes.user.email,
                            name = profileName,
                            avatarUrl = null,
                            defaultSourceLanguage = "auto",
                            defaultTargetLanguage = "en"
                        )
                        _currentProfile.value = newProfile
                        return Result.success(newProfile)
                    }
                    return Result.failure(Exception("Failed to parse AuthResponse JSON"))
                } else {
                    // Scenario B: Email confirmation is ENABLED - only user info returned, no session
                    val adapter = moshi.adapter(AuthUser::class.java)
                    val authUser = adapter.fromJson(bodyString)
                    if (authUser != null) {
                        val profileName = name ?: authUser.userMetadata?.name ?: email.substringBefore("@")
                        val newProfile = Profile(
                            id = authUser.id,
                            email = authUser.email,
                            name = profileName,
                            avatarUrl = null,
                            defaultSourceLanguage = "auto",
                            defaultTargetLanguage = "en"
                        )
                        return Result.failure(Exception("Account created! Please check your email to confirm your account before logging in."))
                    }
                    return Result.failure(Exception("Failed to parse AuthUser JSON"))
                }
            } else {
                val errorMsg = parseErrorBody(response.errorBody()?.string())
                return Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    suspend fun signIn(email: String, password: String): Result<Profile> {
        val api = supabaseApi ?: return Result.failure(Exception("Supabase API is not initialized. Please configure SUPABASE_URL and SUPABASE_ANON_KEY in your secrets."))
        
        try {
            val response = api.signIn(
                apiKey = supabaseAnonKey,
                request = SignInRequest(email = email, password = password)
            )

            if (response.isSuccessful) {
                val authRes = response.body()
                if (authRes != null) {
                    sessionManager.saveSession(
                        accessToken = authRes.accessToken,
                        refreshToken = authRes.refreshToken,
                        userId = authRes.user.id,
                        email = authRes.user.email
                    )
                    
                    // Fetch latest profile from database
                    val profileResult = fetchFreshProfile(authRes.accessToken, authRes.user.id)
                    val profile = if (profileResult.isSuccess) {
                        profileResult.getOrThrow()
                    } else {
                        // Fallback in case table row wasn't ready immediately
                        val fallbackName = authRes.user.userMetadata?.name ?: email.substringBefore("@")
                        sessionManager.saveProfile(fallbackName, "auto", "en")
                        Profile(
                            id = authRes.user.id,
                            email = authRes.user.email,
                            name = fallbackName,
                            avatarUrl = null,
                            defaultSourceLanguage = "auto",
                            defaultTargetLanguage = "en"
                        )
                    }
                    
                    _currentProfile.value = profile
                    return Result.success(profile)
                }
                return Result.failure(Exception("Empty response body from Supabase"))
            } else {
                val errorMsg = parseErrorBody(response.errorBody()?.string())
                return Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    fun signOut() {
        sessionManager.clearSession()
        _currentProfile.value = null
    }

    private suspend fun fetchFreshProfile(token: String, userId: String): Result<Profile> {
        val api = supabaseApi ?: return Result.failure(Exception("Supabase API is not initialized"))
        try {
            val bearer = "Bearer $token"
            val response = api.getProfile(
                apiKey = supabaseAnonKey,
                token = bearer,
                filter = "eq.$userId"
            )

            if (response.isSuccessful) {
                val profiles = response.body()
                if (!profiles.isNullOrEmpty()) {
                    val fresh = profiles.first()
                    sessionManager.saveProfile(
                        name = fresh.name,
                        defaultSourceLanguage = fresh.defaultSourceLanguage,
                        defaultTargetLanguage = fresh.defaultTargetLanguage
                    )
                    return Result.success(fresh)
                }
                return Result.failure(Exception("Profile row not found for user $userId"))
            }
            return Result.failure(Exception("Failed to fetch profile: ${response.code()}"))
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    suspend fun syncProfile(): Result<Profile> {
        val token = sessionManager.getAccessToken() ?: return Result.failure(Exception("Not logged in"))
        val userId = sessionManager.getUserId() ?: return Result.failure(Exception("User ID missing"))
        val result = fetchFreshProfile(token, userId)
        if (result.isSuccess) {
            _currentProfile.value = result.getOrThrow()
        }
        return result
    }

    suspend fun updateProfile(name: String?, sourceLang: String, targetLang: String): Result<Unit> {
        val api = supabaseApi ?: return Result.failure(Exception("Supabase API is not initialized"))
        val token = sessionManager.getAccessToken() ?: return Result.failure(Exception("Not logged in"))
        val userId = sessionManager.getUserId() ?: return Result.failure(Exception("User ID missing"))

        try {
            val response = api.updateProfile(
                apiKey = supabaseAnonKey,
                token = "Bearer $token",
                filter = "eq.$userId",
                profileUpdate = mapOf(
                    "name" to name,
                    "default_source_language" to sourceLang,
                    "default_target_language" to targetLang
                )
            )

            if (response.isSuccessful) {
                sessionManager.saveProfile(name, sourceLang, targetLang)
                _currentProfile.value = _currentProfile.value?.copy(
                    name = name,
                    defaultSourceLanguage = sourceLang,
                    defaultTargetLanguage = targetLang
                )
                return Result.success(Unit)
            } else {
                return Result.failure(Exception("Failed to update profile: ${response.message()}"))
            }
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    private fun parseErrorBody(errorBody: String?): String {
        if (errorBody.isNullOrEmpty()) return "Unknown error occurred"
        return try {
            if (errorBody.contains("\"error_description\"")) {
                val startIndex = errorBody.indexOf("\"error_description\"") + 20
                val endIndex = errorBody.indexOf("\"", startIndex)
                errorBody.substring(startIndex, endIndex)
            } else if (errorBody.contains("\"msg\"")) {
                val startIndex = errorBody.indexOf("\"msg\"") + 6
                val endIndex = errorBody.indexOf("\"", startIndex)
                errorBody.substring(startIndex, endIndex)
            } else if (errorBody.contains("\"message\"")) {
                val startIndex = errorBody.indexOf("\"message\"") + 10
                val endIndex = errorBody.indexOf("\"", startIndex)
                errorBody.substring(startIndex, endIndex)
            } else {
                errorBody
            }
        } catch (e: Exception) {
            errorBody
        }
    }
}
