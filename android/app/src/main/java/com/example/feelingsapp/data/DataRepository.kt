package com.example.feelingsapp.data

import android.content.Context
import com.example.feelingsapp.notifications.MoodReminderScheduler
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
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
    val avatar: String,
    val uid: String = "",
    val isDemo: Boolean = true,
    val authProvider: String = "demo"
)

data class ReminderSettings(
    val enabled: Boolean = false,
    val intervalMinutes: Long = DEFAULT_INTERVAL_MINUTES,
    val quietHoursEnabled: Boolean = false
) {
    fun normalized(): ReminderSettings = copy(
        intervalMinutes = intervalMinutes.coerceIn(MIN_INTERVAL_MINUTES, MAX_INTERVAL_MINUTES)
    )

    companion object {
        const val MIN_INTERVAL_MINUTES = 15L
        const val MAX_INTERVAL_MINUTES = 10_080L // 7 days
        const val DEFAULT_INTERVAL_MINUTES = 240L // 4 hours
        val PRESET_INTERVALS_MINUTES = listOf(15L, 30L, 60L, 120L, 240L, 360L, 720L, 1440L)

        fun formatInterval(minutes: Long): String {
            val clamped = minutes.coerceIn(MIN_INTERVAL_MINUTES, MAX_INTERVAL_MINUTES)
            return when {
                clamped < 60L -> "$clamped min"
                clamped >= 2880L && clamped % 1440L == 0L -> "${clamped / 1440L} days"
                clamped % 60L == 0L -> {
                    val hours = clamped / 60L
                    if (hours == 1L) "1 hour" else "$hours hours"
                }
                else -> "${clamped / 60L}h ${clamped % 60L}m"
            }
        }

        fun formatShortChip(minutes: Long): String {
            return when {
                minutes < 60L -> "${minutes}m"
                minutes % 60L == 0L -> "${minutes / 60L}h"
                else -> "${minutes / 60L}h ${minutes % 60L}m"
            }
        }
    }
}

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
    val reminderSettings: StateFlow<ReminderSettings>
    val isFirebaseConfigured: Boolean
    val syncError: StateFlow<String?>
    suspend fun saveLog(log: LogEntry)
    suspend fun resetLogs()
    suspend fun login(newUser: User)
    suspend fun signInWithGoogleIdToken(idToken: String): Result<User>
    suspend fun signInWithEmail(email: String, password: String): Result<User>
    suspend fun signUpWithEmail(email: String, password: String, displayName: String): Result<User>
    suspend fun sendPasswordResetEmail(email: String): Result<Unit>
    suspend fun logout()
    suspend fun setDarkTheme(enabled: Boolean)
    suspend fun updateReminderSettings(settings: ReminderSettings)
    fun clear()
}

class DefaultDataRepository(context: Context) : DataRepository {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(MoodReminderScheduler.PREFS_NAME, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    override val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    private val _user = MutableStateFlow<User?>(null)
    override val user: StateFlow<User?> = _user.asStateFlow()

    private val _isDarkTheme = MutableStateFlow(false)
    override val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    private val _reminderSettings = MutableStateFlow(ReminderSettings())
    override val reminderSettings: StateFlow<ReminderSettings> = _reminderSettings.asStateFlow()

    override val isFirebaseConfigured: Boolean = FirebaseConfig.isConfigured(appContext)

    private val _syncError = MutableStateFlow<String?>(null)
    override val syncError: StateFlow<String?> = _syncError.asStateFlow()

    private val firebaseAuth: FirebaseAuth? = if (isFirebaseConfigured) Firebase.auth else null
    private val firestore: FirebaseFirestore? = if (isFirebaseConfigured) Firebase.firestore else null

    private var logsListener: ListenerRegistration? = null
    private var authStateListener: FirebaseAuth.AuthStateListener? = null

    init {
        _isDarkTheme.value = prefs.getBoolean("theme_dark", false)

        val savedReminders = ReminderSettings(
            enabled = prefs.getBoolean(MoodReminderScheduler.KEY_ENABLED, false),
            intervalMinutes = prefs.getLong(
                MoodReminderScheduler.KEY_INTERVAL_MINUTES,
                ReminderSettings.DEFAULT_INTERVAL_MINUTES
            ),
            quietHoursEnabled = prefs.getBoolean(MoodReminderScheduler.KEY_QUIET_HOURS, false)
        ).normalized()
        _reminderSettings.value = savedReminders
        MoodReminderScheduler.ensureNotificationChannel(appContext)
        if (savedReminders.enabled) {
            MoodReminderScheduler.scheduleReminders(appContext, savedReminders, replaceExisting = false)
        }

        if (firebaseAuth != null) {
            val listener = FirebaseAuth.AuthStateListener { auth ->
                val fbUser = auth.currentUser
                if (fbUser != null) {
                    val mapped = mapFirebaseUser(fbUser)
                    _user.value = mapped
                    attachFirestoreLogsListener(fbUser.uid)
                    loadRemoteThemePreference(fbUser.uid)
                } else {
                    logsListener?.remove()
                    logsListener = null
                    _user.value = null
                    _logs.value = emptyList()
                    _syncError.value = null
                }
            }
            authStateListener = listener
            firebaseAuth.addAuthStateListener(listener)
        } else {
            val userJson = prefs.getString("user", null)
            if (userJson != null) {
                try {
                    val savedUser = json.decodeFromString<User>(userJson).copy(isDemo = true)
                    _user.value = savedUser
                    _logs.value = loadLogsForUser(savedUser.email)
                } catch (_: Exception) {
                    _user.value = null
                    _logs.value = emptyList()
                }
            }
        }
    }

    private fun resolveAuthProvider(fbUser: FirebaseUser): String {
        val providers = fbUser.providerData.map { it.providerId }
        return when {
            providers.contains("apple.com") -> "apple"
            providers.contains("google.com") -> "google"
            providers.contains("password") -> "password"
            else -> "password"
        }
    }

    private fun deriveDisplayName(
        displayName: String?,
        email: String?,
        overrideName: String? = null
    ): String {
        val candidate = overrideName?.trim()?.takeIf { it.isNotEmpty() }
            ?: displayName?.trim()?.takeIf { it.isNotEmpty() }
            ?: email?.substringBefore("@")?.trim()?.takeIf { it.isNotEmpty() }
            ?: "moody User"
        return candidate.take(200)
    }

    private fun mapFirebaseUser(fbUser: FirebaseUser, overrideName: String? = null): User {
        return User(
            name = deriveDisplayName(fbUser.displayName, fbUser.email, overrideName),
            email = fbUser.email?.trim() ?: "",
            avatar = fbUser.photoUrl?.toString() ?: "",
            uid = fbUser.uid,
            isDemo = false,
            authProvider = resolveAuthProvider(fbUser)
        )
    }

    private suspend fun upsertFirestoreUserProfile(mapped: User) {
        try {
            firestore?.collection("users")
                ?.document(mapped.uid)
                ?.set(
                    mapOf(
                        "uid" to mapped.uid,
                        "name" to mapped.name.take(200),
                        "email" to mapped.email.take(320),
                        "avatar" to mapped.avatar.take(2048),
                        "updatedAt" to FieldValue.serverTimestamp()
                    ),
                    SetOptions.merge()
                )
                ?.await()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Best-effort profile sync
        }
    }

    private fun friendlyAuthErrorMessage(e: Exception): String {
        val code = (e as? FirebaseAuthException)?.errorCode
        return when (code) {
            "ERROR_INVALID_CREDENTIAL",
            "ERROR_WRONG_PASSWORD",
            "ERROR_USER_NOT_FOUND" -> "Incorrect email or password."
            "ERROR_EMAIL_ALREADY_IN_USE" -> "An account with this email already exists."
            "ERROR_INVALID_EMAIL" -> "Please enter a valid email address."
            "ERROR_WEAK_PASSWORD" -> "Password must be at least 6 characters."
            "ERROR_OPERATION_NOT_ALLOWED" ->
                "Email/Password sign-in is not enabled yet in Firebase Authentication Console."
            "ERROR_TOO_MANY_REQUESTS" -> "Too many attempts. Please wait a moment and try again."
            else -> e.localizedMessage ?: "Authentication failed. Please try again."
        }
    }

    private fun attachFirestoreLogsListener(uid: String) {
        val db = firestore ?: return
        logsListener?.remove()
        logsListener = db.collection("users")
            .document(uid)
            .collection("logs")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    _syncError.value = error.localizedMessage ?: "Unable to sync logs."
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    _syncError.value = null
                    _logs.value = snapshot.documents.map { doc ->
                        LogEntry(
                            id = doc.getString("id") ?: doc.id,
                            timestamp = doc.getString("timestamp") ?: IsoTimestamp.formatUtc(Date()),
                            core = doc.getString("core") ?: "Happy",
                            sub1 = doc.getString("sub1") ?: "",
                            sub2 = doc.getString("sub2") ?: "",
                            intensity = (doc.getLong("intensity")?.toInt() ?: 5).coerceIn(1, 10),
                            note = doc.getString("note") ?: "",
                            color = doc.getString("color") ?: "#5f27cd"
                        )
                    }
                }
            }
    }

    private fun loadRemoteThemePreference(uid: String) {
        val db = firestore ?: return
        db.collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener { snapshot ->
                val remoteDark = snapshot.getBoolean("darkTheme")
                if (remoteDark != null) {
                    _isDarkTheme.value = remoteDark
                    prefs.edit().putBoolean("theme_dark", remoteDark).apply()
                }
            }
    }

    override suspend fun signInWithGoogleIdToken(idToken: String): Result<User> {
        val auth = firebaseAuth
            ?: return Result.failure(IllegalStateException("Firebase is not configured."))
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            val fbUser = result.user
                ?: return Result.failure(IllegalStateException("Google sign-in returned no user."))
            val mapped = mapFirebaseUser(fbUser)
            upsertFirestoreUserProfile(mapped)
            Result.success(mapped)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(IllegalStateException(friendlyAuthErrorMessage(e), e))
        }
    }

    override suspend fun signInWithEmail(email: String, password: String): Result<User> {
        val auth = firebaseAuth
            ?: return Result.failure(IllegalStateException("Firebase is not configured."))
        return try {
            val result = auth.signInWithEmailAndPassword(email.trim(), password).await()
            val fbUser = result.user
                ?: return Result.failure(IllegalStateException("Email sign-in returned no user."))
            val mapped = mapFirebaseUser(fbUser)
            upsertFirestoreUserProfile(mapped)
            Result.success(mapped)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(IllegalStateException(friendlyAuthErrorMessage(e), e))
        }
    }

    override suspend fun signUpWithEmail(
        email: String,
        password: String,
        displayName: String
    ): Result<User> {
        val auth = firebaseAuth
            ?: return Result.failure(IllegalStateException("Firebase is not configured."))
        return try {
            val result = auth.createUserWithEmailAndPassword(email.trim(), password).await()
            val fbUser = result.user
                ?: return Result.failure(IllegalStateException("Account creation returned no user."))
            val trimmedName = displayName.trim()
            if (trimmedName.isNotEmpty()) {
                try {
                    val profileUpdates = userProfileChangeRequest {
                        this.displayName = trimmedName
                    }
                    fbUser.updateProfile(profileUpdates).await()
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    // Display name is also derived during Firestore profile upsert
                }
            }
            val mapped = mapFirebaseUser(fbUser, overrideName = trimmedName.ifEmpty { null })
            _user.value = mapped
            upsertFirestoreUserProfile(mapped)
            Result.success(mapped)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(IllegalStateException(friendlyAuthErrorMessage(e), e))
        }
    }

    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        val auth = firebaseAuth
            ?: return Result.failure(IllegalStateException("Firebase is not configured."))
        return try {
            auth.sendPasswordResetEmail(email.trim()).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(IllegalStateException(friendlyAuthErrorMessage(e), e))
        }
    }

    override suspend fun saveLog(log: LogEntry) {
        val activeUser = _user.value
        val db = firestore
        if (db != null && activeUser != null && !activeUser.isDemo && activeUser.uid.isNotBlank()) {
            try {
                val payload = mapOf(
                    "id" to log.id.take(128),
                    "timestamp" to log.timestamp,
                    "core" to log.core,
                    "sub1" to log.sub1.take(50),
                    "sub2" to log.sub2.take(50),
                    "intensity" to log.intensity.coerceIn(1, 10),
                    "note" to log.note.take(2000),
                    "color" to log.color,
                    "createdAt" to FieldValue.serverTimestamp()
                )
                db.collection("users")
                    .document(activeUser.uid)
                    .collection("logs")
                    .document(log.id)
                    .set(payload)
                    .await()
                _syncError.value = null
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _syncError.value = e.localizedMessage ?: "Failed to save log to cloud."
            }
            return
        }

        val updated = (listOf(log) + _logs.value).sortedByDescending { it.timestamp }
        _logs.value = updated
        saveLogsToPrefs(updated, activeUser?.email)
    }

    override suspend fun resetLogs() {
        val activeUser = _user.value
        val db = firestore
        if (db != null && activeUser != null && !activeUser.isDemo && activeUser.uid.isNotBlank()) {
            try {
                val snapshot = db.collection("users")
                    .document(activeUser.uid)
                    .collection("logs")
                    .get()
                    .await()
                val docs = snapshot.documents
                for (chunk in docs.chunked(450)) {
                    val batch = db.batch()
                    for (doc in chunk) {
                        batch.delete(doc.reference)
                    }
                    batch.commit().await()
                }
                _logs.value = emptyList()
                _syncError.value = null
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _syncError.value = e.localizedMessage ?: "Failed to clear cloud history."
            }
            return
        }

        _logs.value = emptyList()
        saveLogsToPrefs(emptyList(), activeUser?.email)
    }

    override suspend fun login(newUser: User) {
        val demoUser = newUser.copy(isDemo = true)
        _user.value = demoUser
        prefs.edit().putString("user", json.encodeToString(demoUser)).apply()
        _logs.value = loadLogsForUser(demoUser.email)
    }

    override suspend fun logout() {
        logsListener?.remove()
        logsListener = null
        firebaseAuth?.signOut()
        _user.value = null
        _logs.value = emptyList()
        _syncError.value = null
        prefs.edit().remove("user").apply()
    }

    override suspend fun setDarkTheme(enabled: Boolean) {
        _isDarkTheme.value = enabled
        prefs.edit().putBoolean("theme_dark", enabled).apply()

        val activeUser = _user.value
        val db = firestore
        if (db != null && activeUser != null && !activeUser.isDemo && activeUser.uid.isNotBlank()) {
            try {
                db.collection("users")
                    .document(activeUser.uid)
                    .set(
                        mapOf(
                            "darkTheme" to enabled,
                            "updatedAt" to FieldValue.serverTimestamp()
                        ),
                        SetOptions.merge()
                    )
                    .await()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Best-effort preference sync
            }
        }
    }

    override suspend fun updateReminderSettings(settings: ReminderSettings) {
        val previous = _reminderSettings.value
        val normalized = settings.normalized()
        _reminderSettings.value = normalized

        prefs.edit()
            .putBoolean(MoodReminderScheduler.KEY_ENABLED, normalized.enabled)
            .putLong(MoodReminderScheduler.KEY_INTERVAL_MINUTES, normalized.intervalMinutes)
            .putBoolean(MoodReminderScheduler.KEY_QUIET_HOURS, normalized.quietHoursEnabled)
            .apply()

        if (normalized.enabled) {
            val replace = !previous.enabled || previous.intervalMinutes != normalized.intervalMinutes
            MoodReminderScheduler.scheduleReminders(appContext, normalized, replaceExisting = replace)
        } else {
            MoodReminderScheduler.cancelReminders(appContext)
        }
    }

    override fun clear() {
        logsListener?.remove()
        logsListener = null
        authStateListener?.let { firebaseAuth?.removeAuthStateListener(it) }
        authStateListener = null
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
                json.decodeFromString<List<LogEntry>>(logsJson).sortedByDescending { it.timestamp }
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
        prefs.edit().putString(logsKey(email), json.encodeToString(list)).apply()
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
