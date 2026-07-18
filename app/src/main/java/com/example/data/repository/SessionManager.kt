package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "doctranslate_prefs"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_PROFILE_NAME = "profile_name"
        private const val KEY_DEFAULT_SOURCE_LANG = "default_source_lang"
        private const val KEY_DEFAULT_TARGET_LANG = "default_target_lang"
    }

    fun saveSession(accessToken: String, refreshToken: String?, userId: String, email: String?) {
        prefs.edit().apply {
            putString(KEY_ACCESS_TOKEN, accessToken)
            putString(KEY_REFRESH_TOKEN, refreshToken)
            putString(KEY_USER_ID, userId)
            putString(KEY_USER_EMAIL, email)
            apply()
        }
    }

    fun saveProfile(name: String?, defaultSourceLanguage: String, defaultTargetLanguage: String) {
        prefs.edit().apply {
            putString(KEY_PROFILE_NAME, name)
            putString(KEY_DEFAULT_SOURCE_LANG, defaultSourceLanguage)
            putString(KEY_DEFAULT_TARGET_LANG, defaultTargetLanguage)
            apply()
        }
    }

    fun getAccessToken(): String? = prefs.getString(KEY_ACCESS_TOKEN, null)
    
    fun getRefreshToken(): String? = prefs.getString(KEY_REFRESH_TOKEN, null)
    
    fun getUserId(): String? = prefs.getString(KEY_USER_ID, null)
    
    fun getUserEmail(): String? = prefs.getString(KEY_USER_EMAIL, null)
    
    fun getProfileName(): String? = prefs.getString(KEY_PROFILE_NAME, null)
    
    fun getDefaultSourceLanguage(): String = prefs.getString(KEY_DEFAULT_SOURCE_LANG, "auto") ?: "auto"
    
    fun getDefaultTargetLanguage(): String = prefs.getString(KEY_DEFAULT_TARGET_LANG, "en") ?: "en"

    fun clearSession() {
        prefs.edit().clear().apply()
    }

    fun isLoggedIn(): Boolean = getAccessToken() != null
}
