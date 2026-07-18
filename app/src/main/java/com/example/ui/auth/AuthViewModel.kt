package com.example.ui.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel(private val authRepository: AuthRepository) : ViewModel() {

    var email by mutableStateOf("")
        private set

    var password by mutableStateOf("")
        private set

    var name by mutableStateOf("")
        private set

    var isSignUp by mutableStateOf(false)
        private set

    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    var successMessage by mutableStateOf<String?>(null)
        private set

    val currentProfile = authRepository.currentProfile
    val isCheckingSession = authRepository.isCheckingSession

    fun onEmailChange(newValue: String) {
        email = newValue
        errorMessage = null
    }

    fun onPasswordChange(newValue: String) {
        password = newValue
        errorMessage = null
    }

    fun onNameChange(newValue: String) {
        name = newValue
        errorMessage = null
    }

    fun toggleMode() {
        isSignUp = !isSignUp
        errorMessage = null
        successMessage = null
    }

    fun clearMessages() {
        errorMessage = null
        successMessage = null
    }

    fun authenticate() {
        if (email.isBlank() || password.isBlank()) {
            errorMessage = "Email and password cannot be blank."
            return
        }

        if (password.length < 6) {
            errorMessage = "Password must be at least 6 characters long."
            return
        }

        if (isSignUp && name.isBlank()) {
            errorMessage = "Please enter your name."
            return
        }

        isLoading = true
        errorMessage = null
        successMessage = null

        viewModelScope.launch {
            if (isSignUp) {
                val result = authRepository.signUp(email, password, name)
                isLoading = false
                result.fold(
                    onSuccess = { profile ->
                        successMessage = "Account created successfully!"
                    },
                    onFailure = { error ->
                        errorMessage = error.localizedMessage ?: "Failed to sign up. Please try again."
                    }
                )
            } else {
                val result = authRepository.signIn(email, password)
                isLoading = false
                result.fold(
                    onSuccess = { profile ->
                        // Success handled by state observer
                    },
                    onFailure = { error ->
                        errorMessage = error.localizedMessage ?: "Failed to sign in. Please verify your credentials."
                    }
                )
            }
        }
    }

    fun signOut() {
        authRepository.signOut()
    }
}
