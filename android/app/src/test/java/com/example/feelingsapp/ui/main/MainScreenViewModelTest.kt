package com.example.feelingsapp.ui.main

import com.example.feelingsapp.data.DataRepository
import com.example.feelingsapp.data.FeelingsData
import com.example.feelingsapp.data.IsoTimestamp
import com.example.feelingsapp.data.LogEntry
import com.example.feelingsapp.data.ReminderSettings
import com.example.feelingsapp.data.SecurityUtils
import com.example.feelingsapp.data.User
import com.example.feelingsapp.notifications.MoodReminderScheduler
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
import org.junit.Assert.assertFalse
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
  fun signInWithGoogleIdToken_updatesFirebaseUserState() = runTest(testDispatcher) {
    val repo = FakeFeelingsRepository()
    val viewModel = MainScreenViewModel(repo)

    val result = viewModel.signInWithGoogleIdToken("sample-id-token")
    assertTrue(result.isSuccess)
    val signedIn = viewModel.user.value
    assertNotNull(signedIn)
    assertFalse(signedIn!!.isDemo)
    assertTrue(signedIn.uid.isNotBlank())
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

  @Test
  fun saveLog_withSkippedSub2_savesEmptySub2() = runTest(testDispatcher) {
    val repo = FakeFeelingsRepository()
    val viewModel = MainScreenViewModel(repo)

    val entry = LogEntry(
      id = "log-skip-sub2",
      timestamp = IsoTimestamp.formatUtc(Date()),
      core = "Happy",
      sub1 = "Content",
      sub2 = "",
      intensity = 6,
      note = "Skipped refinement step",
      color = "#f2a380"
    )

    viewModel.saveLog(entry)
    advanceUntilIdle()
    assertEquals(1, viewModel.logs.value.size)
    assertEquals("Content", viewModel.logs.value.first().sub1)
    assertEquals("", viewModel.logs.value.first().sub2)
  }

  @Test
  fun updateReminderSettings_updatesAndNormalizesState() = runTest(testDispatcher) {
    val repo = FakeFeelingsRepository()
    val viewModel = MainScreenViewModel(repo)

    assertFalse(viewModel.reminderSettings.value.enabled)
    assertEquals(ReminderSettings.DEFAULT_INTERVAL_MINUTES, viewModel.reminderSettings.value.intervalMinutes)
    assertFalse(viewModel.reminderSettings.value.quietHoursEnabled)

    viewModel.updateReminderSettings(
      ReminderSettings(
        enabled = true,
        intervalMinutes = 5L,
        quietHoursEnabled = true
      )
    )
    advanceUntilIdle()

    val updated = viewModel.reminderSettings.value
    assertTrue(updated.enabled)
    assertEquals(ReminderSettings.MIN_INTERVAL_MINUTES, updated.intervalMinutes)
    assertTrue(updated.quietHoursEnabled)
  }

  @Test
  fun reminderSettings_clampsAndFormatsIntervals() {
    val tooLong = ReminderSettings(enabled = true, intervalMinutes = 20_000L).normalized()
    assertEquals(ReminderSettings.MAX_INTERVAL_MINUTES, tooLong.intervalMinutes)

    assertEquals("15 min", ReminderSettings.formatInterval(15L))
    assertEquals("1 hour", ReminderSettings.formatInterval(60L))
    assertEquals("1h 30m", ReminderSettings.formatInterval(90L))
    assertEquals("24 hours", ReminderSettings.formatInterval(1440L))
    assertEquals("2 days", ReminderSettings.formatInterval(2880L))
    assertEquals("4h", ReminderSettings.formatShortChip(240L))
  }

  @Test
  fun quietHours_detectsNightWindowCorrectly() {
    assertTrue(MoodReminderScheduler.isWithinQuietHours(22))
    assertTrue(MoodReminderScheduler.isWithinQuietHours(23))
    assertTrue(MoodReminderScheduler.isWithinQuietHours(0))
    assertTrue(MoodReminderScheduler.isWithinQuietHours(7))
    assertFalse(MoodReminderScheduler.isWithinQuietHours(8))
    assertFalse(MoodReminderScheduler.isWithinQuietHours(14))
    assertFalse(MoodReminderScheduler.isWithinQuietHours(21))
  }

  @Test
  fun signUpWithEmail_and_signInWithEmail_updatesUserWithPasswordProvider() = runTest(testDispatcher) {
    val repo = FakeFeelingsRepository()
    val viewModel = MainScreenViewModel(repo)

    val signUpRes = viewModel.signUpWithEmail("alex@example.com", "secret123", "Alex")
    assertTrue(signUpRes.isSuccess)
    assertEquals("Alex", viewModel.user.value?.name)
    assertEquals("alex@example.com", viewModel.user.value?.email)
    assertEquals("password", viewModel.user.value?.authProvider)
    assertFalse(viewModel.user.value!!.isDemo)

    viewModel.logout()
    advanceUntilIdle()
    assertNull(viewModel.user.value)

    val signInRes = viewModel.signInWithEmail("alex@example.com", "secret123")
    assertTrue(signInRes.isSuccess)
    assertEquals("alex@example.com", viewModel.user.value?.email)
    assertEquals("password", viewModel.user.value?.authProvider)
  }

  @Test
  fun sendPasswordResetEmail_succeedsForValidEmail() = runTest(testDispatcher) {
    val repo = FakeFeelingsRepository()
    val viewModel = MainScreenViewModel(repo)

    val res = viewModel.sendPasswordResetEmail("alex@example.com")
    assertTrue(res.isSuccess)
  }

  @Test
  fun securityUtils_generatesDistinctHexNonces() {
    val first = SecurityUtils.generateHashedNonce()
    val second = SecurityUtils.generateHashedNonce()

    assertEquals(64, first.length)
    assertEquals(64, second.length)
    assertTrue(first.all { it in '0'..'9' || it in 'a'..'f' })
    assertFalse(first == second)
  }

  @Test
  fun securityUtils_validatesSignUpPasswordAndSanitizesNotes() {
    assertNotNull(SecurityUtils.validateSignUpPassword("short7"))
    assertNotNull(SecurityUtils.validateSignUpPassword("        "))
    assertNull(SecurityUtils.validateSignUpPassword("StrongPass1"))

    val longNote = "  " + "a".repeat(2500) + "  "
    val sanitized = SecurityUtils.sanitizeNote(longNote)
    assertEquals(SecurityUtils.MAX_NOTE_LENGTH, sanitized.length)
  }

  @Test
  fun securityUtils_mapsAuthAndFirestoreErrorsWithoutRawLeakage() {
    val emailInUse = SecurityUtils.mapAuthErrorCode("ERROR_EMAIL_ALREADY_IN_USE")
    assertFalse(emailInUse.contains("already exists", ignoreCase = true))

    val network = SecurityUtils.mapAuthErrorCode(null, isNetworkError = true)
    assertTrue(network.contains("Network", ignoreCase = true))

    val unknownAuth = SecurityUtils.mapAuthErrorCode("UNKNOWN_INTERNAL_DETAIL")
    assertEquals("Authentication failed. Please try again.", unknownAuth)

    val permDenied = SecurityUtils.mapFirestoreErrorCode("PERMISSION_DENIED", "Fallback")
    assertTrue(permDenied.contains("permission", ignoreCase = true))

    val unknownFirestore = SecurityUtils.mapFirestoreErrorCode("INTERNAL", "Fallback")
    assertEquals("Fallback", unknownFirestore)
  }
}

private class FakeFeelingsRepository : DataRepository {
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
    val firebaseUser = User(
      name = "Cloud User",
      email = "cloud@feelings.app",
      avatar = "",
      uid = "uid-$idToken",
      isDemo = false,
      authProvider = "google"
    )
    _user.value = firebaseUser
    return Result.success(firebaseUser)
  }

  override suspend fun signInWithEmail(email: String, password: String): Result<User> {
    val firebaseUser = User(
      name = email.substringBefore("@").ifBlank { "moody User" },
      email = email,
      avatar = "",
      uid = "uid-email-$email",
      isDemo = false,
      authProvider = "password"
    )
    _user.value = firebaseUser
    return Result.success(firebaseUser)
  }

  override suspend fun signUpWithEmail(email: String, password: String, displayName: String): Result<User> {
    val firebaseUser = User(
      name = displayName.ifBlank { email.substringBefore("@").ifBlank { "moody User" } },
      email = email,
      avatar = "",
      uid = "uid-email-$email",
      isDemo = false,
      authProvider = "password"
    )
    _user.value = firebaseUser
    return Result.success(firebaseUser)
  }

  override suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
    return if (email.contains("@")) {
      Result.success(Unit)
    } else {
      Result.failure(IllegalArgumentException("Invalid email"))
    }
  }

  override suspend fun logout() {
    _user.value = null
    _logs.value = emptyList()
  }

  override suspend fun setDarkTheme(enabled: Boolean) {
    _isDarkTheme.value = enabled
  }

  override suspend fun updateReminderSettings(settings: ReminderSettings) {
    _reminderSettings.value = settings.normalized()
  }

  override fun clear() {}
}
