package com.example.feelingsapp.ui.main

import com.example.feelingsapp.data.DataRepository
import com.example.feelingsapp.data.FeelingsData
import com.example.feelingsapp.data.IsoTimestamp
import com.example.feelingsapp.data.LogEntry
import com.example.feelingsapp.data.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Date

@OptIn(ExperimentalCoroutinesApi::class)
class MainScreenViewModelTest {
  private val testDispatcher = StandardTestDispatcher()

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun login_and_logout_updatesUserState() = runTest(testDispatcher) {
    val repo = FakeFeelingsRepository()
    val viewModel = MainScreenViewModel(repo)

    assertNull(viewModel.user.value)

    val testUser = User("Gary", "gary@gmail.com", "")
    viewModel.login(testUser)
    advanceUntilIdle()
    assertEquals(testUser, viewModel.user.value)

    viewModel.logout()
    advanceUntilIdle()
    assertNull(viewModel.user.value)
  }

  @Test
  fun saveLog_and_resetLogs_updatesLogsState() = runTest(testDispatcher) {
    val repo = FakeFeelingsRepository()
    val viewModel = MainScreenViewModel(repo)

    val entry = LogEntry(
      id = "log-1",
      timestamp = IsoTimestamp.formatUtc(Date()),
      core = "Happy",
      sub1 = "Content",
      sub2 = "Joyful",
      intensity = 8,
      note = "Feeling calm",
      color = "#f2a380"
    )

    viewModel.saveLog(entry)
    advanceUntilIdle()
    assertEquals(1, viewModel.logs.value.size)
    assertEquals("Joyful", viewModel.logs.value.first().sub2)

    viewModel.resetLogs()
    advanceUntilIdle()
    assertTrue(viewModel.logs.value.isEmpty())
  }

  @Test
  fun setDarkTheme_updatesThemeState() = runTest(testDispatcher) {
    val repo = FakeFeelingsRepository()
    val viewModel = MainScreenViewModel(repo)

    assertEquals(false, viewModel.isDarkTheme.value)
    viewModel.setDarkTheme(true)
    advanceUntilIdle()
    assertEquals(true, viewModel.isDarkTheme.value)
  }

  @Test
  fun feelingsData_happyWheelMappingIsAccurate() {
    val happySub1 = FeelingsData.emotions["Happy"]?.sub1
    assertNotNull(happySub1)
    assertEquals(listOf("Aroused", "Cheeky"), happySub1?.get("Playful"))
    assertEquals(listOf("Free", "Joyful"), happySub1?.get("Content"))
    assertEquals(listOf("Hopeful", "Inspired"), happySub1?.get("Optimistic"))
  }

  @Test
  fun isoTimestamp_roundTripsUtcMillis() {
    val now = Date()
    val formatted = IsoTimestamp.formatUtc(now)
    val parsed = IsoTimestamp.parseUtc(formatted)
    assertNotNull(parsed)
    assertEquals(now.time, parsed?.time)
  }
}

private class FakeFeelingsRepository : DataRepository {
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
    _logs.value = emptyList()
  }

  override suspend fun setDarkTheme(enabled: Boolean) {
    _isDarkTheme.value = enabled
  }
}
