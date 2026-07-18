package com.example.ui.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.with
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.repository.AuthRepository
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.AuthViewModel
import com.example.ui.home.HomeScreen
import com.example.ui.profile.ProfileScreen
import kotlinx.coroutines.launch

enum class MainTab {
    HOME,
    PROFILE
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AppNavigation(
    authRepository: AuthRepository,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val authViewModel: AuthViewModel = viewModel { AuthViewModel(authRepository) }
    
    val currentProfile by authViewModel.currentProfile.collectAsState()
    val isCheckingSession by authViewModel.isCheckingSession.collectAsState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when {
            isCheckingSession -> {
                // Beautiful minimalist Loading State during session hydration
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 3.dp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Initializing DocTranslate...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            currentProfile == null -> {
                // Signed out -> Auth group
                AuthScreen(viewModel = authViewModel)
            }
            else -> {
                // Signed in -> Tabs Shell
                var selectedTab by remember { mutableStateOf(MainTab.HOME) }
                val profile = currentProfile!!

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        NavigationBar(
                            modifier = Modifier.testTag("navigation_bar"),
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 2.dp
                        ) {
                            NavigationBarItem(
                                selected = selectedTab == MainTab.HOME,
                                onClick = { selectedTab = MainTab.HOME },
                                label = { Text("Home", fontWeight = FontWeight.Bold) },
                                icon = {
                                    Icon(
                                        imageVector = if (selectedTab == MainTab.HOME) Icons.Filled.Home else Icons.Outlined.Home,
                                        contentDescription = "Home"
                                    )
                                },
                                modifier = Modifier.testTag("home_tab_button")
                            )

                            NavigationBarItem(
                                selected = selectedTab == MainTab.PROFILE,
                                onClick = { selectedTab = MainTab.PROFILE },
                                label = { Text("Profile", fontWeight = FontWeight.Bold) },
                                icon = {
                                    Icon(
                                        imageVector = if (selectedTab == MainTab.PROFILE) Icons.Filled.Person else Icons.Outlined.Person,
                                        contentDescription = "Profile"
                                    )
                                },
                                modifier = Modifier.testTag("profile_tab_button")
                            )
                        }
                    }
                ) { innerPadding ->
                    // Animated transition between Tabs
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        AnimatedContent(
                            targetState = selectedTab,
                            transitionSpec = {
                                fadeIn() with fadeOut()
                            }
                        ) { targetTab ->
                            when (targetTab) {
                                MainTab.HOME -> {
                                    HomeScreen(
                                        userName = profile.name ?: "User"
                                    )
                                }
                                MainTab.PROFILE -> {
                                    ProfileScreen(
                                        profile = profile,
                                        viewModel = authViewModel,
                                        onUpdateLanguage = { sourceLang, targetLang ->
                                            coroutineScope.launch {
                                                authRepository.updateProfile(
                                                    name = profile.name,
                                                    sourceLang = sourceLang,
                                                    targetLang = targetLang
                                                )
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
