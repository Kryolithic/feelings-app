package com.example.feelingsapp.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Serializable
data class LogEntry(
    val id: String,
    val timestamp: String,
    val core: String,
    val sub1: String,
    val sub2: String,
    val intensity: Int,
    val note: String,
    val color: String
)

@Serializable
data class User(
    val name: String,
    val email: String,
    val avatar: String
)

object IsoTimestamp {
    private val utcTimeZone: TimeZone = TimeZone.getTimeZone("UTC")

    fun formatUtc(date: Date): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = utcTimeZone
        }
        return formatter.format(date)
    }

    fun parseUtc(value: String): Date? {
        val patterns = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'"
        )
        for (pattern in patterns) {
            try {
                val parser = SimpleDateFormat(pattern, Locale.US).apply {
                    timeZone = utcTimeZone
                    isLenient = false
                }
                val parsed = parser.parse(value)
                if (parsed != null) return parsed
            } catch (_: Exception) {
                // Try next ISO pattern
            }
        }
        return null
    }
}

interface DataRepository {
    val logs: StateFlow<List<LogEntry>>
    val user: StateFlow<User?>
    val isDarkTheme: StateFlow<Boolean>
    suspend fun saveLog(log: LogEntry)
    suspend fun resetLogs()
    suspend fun login(newUser: User)
    suspend fun logout()
    suspend fun setDarkTheme(enabled: Boolean)
}

class DefaultDataRepository(context: Context) : DataRepository {
    private val prefs = context.getSharedPreferences("serene_prefs", Context.MODE_PRIVATE)

    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    override val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    private val _user = MutableStateFlow<User?>(null)
    override val user: StateFlow<User?> = _user.asStateFlow()

    private val _isDarkTheme = MutableStateFlow(false)
    override val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    init {
        _isDarkTheme.value = prefs.getBoolean("theme_dark", false)

        val userJson = prefs.getString("user", null)
        if (userJson != null) {
            try {
                val savedUser = Json.decodeFromString<User>(userJson)
                _user.value = savedUser
                _logs.value = loadLogsForUser(savedUser.email)
            } catch (_: Exception) {
                _user.value = null
                _logs.value = emptyList()
            }
        }
    }

    override suspend fun saveLog(log: LogEntry) {
        val updated = (listOf(log) + _logs.value).sortedByDescending { it.timestamp }
        _logs.value = updated
        saveLogsToPrefs(updated, _user.value?.email)
    }

    override suspend fun resetLogs() {
        _logs.value = emptyList()
        saveLogsToPrefs(emptyList(), _user.value?.email)
    }

    override suspend fun login(newUser: User) {
        _user.value = newUser
        prefs.edit().putString("user", Json.encodeToString(newUser)).apply()
        _logs.value = loadLogsForUser(newUser.email)
    }

    override suspend fun logout() {
        _user.value = null
        _logs.value = emptyList()
        prefs.edit().remove("user").apply()
    }

    override suspend fun setDarkTheme(enabled: Boolean) {
        _isDarkTheme.value = enabled
        prefs.edit().putBoolean("theme_dark", enabled).apply()
    }

    private fun logsKey(email: String?): String {
        val normalized = email?.trim()?.lowercase(Locale.US)
        return if (normalized.isNullOrEmpty()) "logs_default" else "logs_$normalized"
    }

    private fun loadLogsForUser(email: String): List<LogEntry> {
        val key = logsKey(email)
        val logsJson = prefs.getString(key, null)
        if (logsJson != null) {
            return try {
                Json.decodeFromString<List<LogEntry>>(logsJson).sortedByDescending { it.timestamp }
            } catch (_: Exception) {
                val seeded = getInitialMockLogs()
                saveLogsToPrefs(seeded, email)
                seeded
            }
        }

        val seeded = getInitialMockLogs()
        saveLogsToPrefs(seeded, email)
        return seeded
    }

    private fun saveLogsToPrefs(list: List<LogEntry>, email: String?) {
        prefs.edit().putString(logsKey(email), Json.encodeToString(list)).apply()
    }

    private fun getInitialMockLogs(): List<LogEntry> {
        val now = Date()
        val mockEntries = mutableListOf<LogEntry>()

        val mockOptions = listOf(
            Triple("Happy", Pair("Content", "Joyful"), "#f2a380"),
            Triple("Surprised", Pair("Amazed", "Awe"), "#e6af73"),
            Triple("Bad", Pair("Tired", "Sleepy"), "#70c292"),
            Triple("Happy", Pair("Proud", "Confident"), "#f2a380"),
            Triple("Sad", Pair("Lonely", "Isolated"), "#7da5d9")
        )

        val notes = listOf(
            "Had a wonderful lunch with friends today.",
            "Beautiful sunset on my evening walk.",
            "Did not sleep well last night, feeling slow.",
            "Finished a major milestone at work!",
            "Quiet evening indoors reflecting on the week."
        )

        val intensities = listOf(7, 8, 6, 9, 4)
        val hours = listOf(10, 14, 11, 16, 9)
        val minutes = listOf(15, 30, 0, 45, 20)

        for (i in mockOptions.indices) {
            val logDate = Calendar.getInstance().apply {
                time = now
                add(Calendar.DATE, -i)
                set(Calendar.HOUR_OF_DAY, hours[i])
                set(Calendar.MINUTE, minutes[i])
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (time.after(now)) {
                    time = Date(now.time - i * 3600_000L)
                }
            }

            val option = mockOptions[i]
            mockEntries.add(
                LogEntry(
                    id = "mock-$i",
                    timestamp = IsoTimestamp.formatUtc(logDate.time),
                    core = option.first,
                    sub1 = option.second.first,
                    sub2 = option.second.second,
                    intensity = intensities[i],
                    note = notes[i],
                    color = option.third
                )
            )
        }

        return mockEntries.sortedByDescending { it.timestamp }
    }
}
