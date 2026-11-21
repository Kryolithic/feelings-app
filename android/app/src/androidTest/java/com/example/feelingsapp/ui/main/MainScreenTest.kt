package com.example.feelingsapp.ui.main

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.example.feelingsapp.data.DataRepository
import com.example.feelingsapp.data.LogEntry
import com.example.feelingsapp.data.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** UI tests for [com.example.feelingsapp.ui.main.MainScreen]. */
class MainScreenTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @Before
  fun setup() {
    val viewModel = MainScreenViewModel(FakeTestRepository())
    composeTestRule.setContent { MainScreen(viewModel = viewModel) }
  }

  @Test
  fun loggedOut_showsBrandingAndGoogleSignIn() {
    composeTestRule.onNodeWithText("myMood").assertExists()
    composeTestRule.onNodeWithText("Sign In with Google").assertExists()
  }
}

private class FakeTestRepository : DataRepository {
  private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
  override val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

  private val _user = MutableStateFlow<User?>(null)
  override val user: StateFlow<User?> = _user.asStateFlow()

  private val _isDarkTheme = MutableStateFlow(false)
  override val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

  override suspend fun saveLog(log: LogEntry) {
    _logs.value = listOf(log) + _logs.value
  }

  override suspend fun resetLogs() {
    _logs.value = emptyList()
  }

  override suspend fun login(newUser: User) {
    _user.value = newUser
  }

  override suspend fun logout() {
    _user.value = null
  }

  override suspend fun setDarkTheme(enabled: Boolean) {
    _isDarkTheme.value = enabled
  }
}
