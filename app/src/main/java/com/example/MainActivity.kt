package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.example.data.repository.AuthRepository
import com.example.ui.navigation.AppNavigation
import com.example.ui.theme.DocTranslateTheme

class MainActivity : ComponentActivity() {
    private lateinit var authRepository: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Initialize AuthRepository
        authRepository = AuthRepository(applicationContext)

        setContent {
            DocTranslateTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AppNavigation(
                        authRepository = authRepository,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}
