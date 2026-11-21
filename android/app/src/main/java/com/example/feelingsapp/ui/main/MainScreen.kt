package com.example.feelingsapp.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.feelingsapp.ui.LoggingWizardFlow
import com.example.feelingsapp.ui.LoginScreen
import com.example.feelingsapp.ui.ProfileScreen
import com.example.feelingsapp.ui.TrendsScreen

@Composable
fun MainScreen(
    viewModel: MainScreenViewModel,
    modifier: Modifier = Modifier
) {
    val user by viewModel.user.collectAsState()
    val logs by viewModel.logs.collectAsState()
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()
    val syncError by viewModel.syncError.collectAsState()

    val currentUser = user
    if (currentUser == null) {
        LoginScreen(
            onLogin = { viewModel.login(it) },
            modifier = modifier,
            isFirebaseConfigured = viewModel.isFirebaseConfigured,
            onGoogleIdToken = { viewModel.signInWithGoogleIdToken(it) }
        )
    } else {
        var currentTab by rememberSaveable { mutableStateOf("home") }

        Scaffold(
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    NavigationBarItem(
                        selected = currentTab == "home",
                        onClick = { currentTab = "home" },
                        icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                        label = { Text("Home") }
                    )
                    NavigationBarItem(
                        selected = currentTab == "trends",
                        onClick = { currentTab = "trends" },
                        icon = { Icon(Icons.Default.Star, contentDescription = "Trends") },
                        label = { Text("Trends") }
                    )
                    NavigationBarItem(
                        selected = currentTab == "profile",
                        onClick = { currentTab = "profile" },
                        icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                        label = { Text("Profile") }
                    )
                }
            },
            modifier = modifier
        ) { innerPadding ->
            Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                if (syncError != null) {
                    Text(
                        text = "Sync warning: $syncError",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f))
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    when (currentTab) {
                        "home" -> LoggingWizardFlow(
                            onAddLog = { viewModel.saveLog(it) }
                        )
                        "trends" -> TrendsScreen(
                            logs = logs
                        )
                        "profile" -> ProfileScreen(
                            user = currentUser,
                            isDarkTheme = isDarkTheme,
                            onToggleTheme = { viewModel.setDarkTheme(!isDarkTheme) },
                            onLogout = {
                                currentTab = "home"
                                viewModel.logout()
                            },
                            onResetData = { viewModel.resetLogs() }
                        )
                    }
                }
            }
        }
    }
}
