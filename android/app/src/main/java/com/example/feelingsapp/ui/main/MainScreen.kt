package com.example.feelingsapp.ui.main

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.feelingsapp.data.DefaultDataRepository
import com.example.feelingsapp.ui.LoginScreen
import com.example.feelingsapp.ui.LoggingWizardFlow
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

    val currentUser = user
    if (currentUser == null) {
        LoginScreen(
            onLogin = { viewModel.login(it) },
            modifier = modifier
        )
    } else {
        var currentTab by remember { mutableStateOf("home") }

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
            Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
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
                        onLogout = { viewModel.logout() },
                        onResetData = { viewModel.resetLogs() }
                    )
                }
            }
        }
    }
}
