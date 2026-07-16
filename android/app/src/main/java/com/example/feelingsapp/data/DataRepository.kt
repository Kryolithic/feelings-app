package com.example.feelingsapp.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

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

interface DataRepository {
    val logs: Flow<List<LogEntry>>
    val user: Flow<User?>
    val isDarkTheme: Flow<Boolean>
    suspend fun saveLog(log: LogEntry)
    suspend fun resetLogs()
    suspend fun login(newUser: User)
    suspend fun logout()
    suspend fun setDarkTheme(enabled: Boolean)
}

class DefaultDataRepository(context: Context) : DataRepository {
    private val prefs = context.getSharedPreferences("serene_prefs", Context.MODE_PRIVATE)

    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    override val logs: Flow<List<LogEntry>> = _logs.asStateFlow()

    private val _user = MutableStateFlow<User?>(null)
    override val user: Flow<User?> = _user.asStateFlow()

    private val _isDarkTheme = MutableStateFlow(false)
    override val isDarkTheme: Flow<Boolean> = _isDarkTheme.asStateFlow()

    init {
        // Load theme
        _isDarkTheme.value = prefs.getBoolean("theme_dark", false)

        // Load user
        val userJson = prefs.getString("user", null)
        if (userJson != null) {
            try {
                _user.value = Json.decodeFromString<User>(userJson)
            } catch (e: Exception) {
                _user.value = null
            }
        }

        // Load logs, or initialize mock logs if empty
        val logsJson = prefs.getString("logs", null)
        if (logsJson != null) {
            try {
                _logs.value = Json.decodeFromString<List<LogEntry>>(logsJson)
            } catch (e: Exception) {
                _logs.value = getInitialMockLogs()
                saveLogsToPrefs(_logs.value)
            }
        } else {
            _logs.value = getInitialMockLogs()
            saveLogsToPrefs(_logs.value)
        }
    }

    override suspend fun saveLog(log: LogEntry) {
        val updated = listOf(log) + _logs.value
        _logs.value = updated
        saveLogsToPrefs(updated)
    }

    override suspend fun resetLogs() {
        _logs.value = emptyList()
        prefs.edit().remove("logs").apply()
    }

    override suspend fun login(newUser: User) {
        _user.value = newUser
        prefs.edit().putString("user", Json.encodeToString(newUser)).apply()
    }

    override suspend fun logout() {
        _user.value = null
        prefs.edit().remove("user").apply()
    }

    override suspend fun setDarkTheme(enabled: Boolean) {
        _isDarkTheme.value = enabled
        prefs.edit().putBoolean("theme_dark", enabled).apply()
    }

    private fun saveLogsToPrefs(list: List<LogEntry>) {
        prefs.edit().putString("logs", Json.encodeToString(list)).apply()
    }

    private fun getInitialMockLogs(): List<LogEntry> {
        val current = Calendar.getInstance()
        val mockEntries = mutableListOf<LogEntry>()

        val mockOptions = listOf(
            Triple("Happy", Pair("Content", "Joyful"), "#f2a380"),
            Triple("Sad", Pair("Lonely", "Isolated"), "#7da5d9"),
            Triple("Bad", Pair("Tired", "Sleepy"), "#70c292"),
            Triple("Surprised", Pair("Amazed", "Awe"), "#e6af73"),
            Triple("Happy", Pair("Proud", "Confident"), "#f2a380"),
            Triple("Angry", Pair("Frustrated", "Annoyed"), "#d96259")
        )

        val notes = listOf(
            "Had a wonderful lunch with friends today.",
            "Struggled to connect with coworkers today.",
            "Did not sleep well last night, feeling slow.",
            "Beautiful sunset on my evening walk.",
            "Finished a major milestone at work!",
            "Stuck in heavy traffic for an hour."
        )

        val intensities = listOf(7, 4, 6, 8, 9, 5)

        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)

        for (i in mockOptions.indices) {
            val logDate = Calendar.getInstance()
            // Distribute across days of current week
            logDate.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY + i)
            logDate.set(Calendar.HOUR_OF_DAY, 10 + i)
            logDate.set(Calendar.MINUTE, 15 * i)

            val option = mockOptions[i]
            mockEntries.add(
                LogEntry(
                    id = "mock-$i",
                    timestamp = dateFormat.format(logDate.time),
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
