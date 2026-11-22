package com.example.feelingsapp.ui.main

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.example.feelingsapp.data.DataRepository
import com.example.feelingsapp.data.LogEntry
import com.example.feelingsapp.data.ReminderSettings
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
  fun loggedOut_showsBrandingAndSignIn() {
    composeTestRule.onNodeWithContentDescription("moody").assertExists()
    composeTestRule.onNodeWithText("Sign In").assertExists()
  }
}

private class FakeTestRepository : DataRepository {
  private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
  override val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

  private val _user = MutableStateFlow<User?>(null)
  override val user: StateFlow<User?> = _user.asStateFlow()

  private val _isDarkTheme = MutableStateFlow(false)
  override val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

  private val _reminderSettings = MutableStateFlow(ReminderSettings())
  override val reminderSettings: StateFlow<ReminderSettings> = _reminderSettings.asStateFlow()

  override val isFirebaseConfigured: Boolean = false

  private val _syncError = MutableStateFlow<String?>(null)
  override val syncError: StateFlow<String?> = _syncError.asStateFlow()

  override suspend fun saveLog(log: LogEntry) {
    _logs.value = listOf(log) + _logs.value
  }

  override suspend fun resetLogs() {
    _logs.value = emptyList()
  }

  override suspend fun login(newUser: User) {
    _user.value = newUser
  }

  override suspend fun signInWithGoogleIdToken(idToken: String): Result<User> {
    return Result.failure(IllegalStateException("Not configured in test"))
  }

  override suspend fun signInWithEmail(email: String, password: String): Result<User> {
    return Result.failure(IllegalStateException("Not configured in test"))
  }

  override suspend fun signUpWithEmail(email: String, password: String, displayName: String): Result<User> {
    return Result.failure(IllegalStateException("Not configured in test"))
  }

  override suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
    return Result.failure(IllegalStateException("Not configured in test"))
  }

  override suspend fun logout() {
    _user.value = null
  }

  override suspend fun setDarkTheme(enabled: Boolean) {
    _isDarkTheme.value = enabled
  }

  override suspend fun updateReminderSettings(settings: ReminderSettings) {
    _reminderSettings.value = settings.normalized()
  }

  override fun clear() {}
}
