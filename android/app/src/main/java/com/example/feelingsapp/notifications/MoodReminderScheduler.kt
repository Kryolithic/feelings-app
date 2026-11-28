package com.example.feelingsapp.notifications

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.feelingsapp.MainActivity
import com.example.feelingsapp.R
import com.example.feelingsapp.data.ReminderSettings
import java.util.concurrent.TimeUnit
import kotlin.math.abs

object MoodReminderScheduler {
    const val PREFS_NAME = "serene_prefs"
    const val KEY_ENABLED = "reminders_enabled"
    const val KEY_INTERVAL_MINUTES = "reminders_interval_minutes"
    const val KEY_QUIET_HOURS = "reminders_quiet_hours"

    const val CHANNEL_ID = "moody_reminders"
    const val UNIQUE_WORK_NAME = "moody_checkin_reminder"
    const val NOTIFICATION_ID = 1001
    const val EXTRA_OPEN_CHECKIN = "extra_open_checkin"

    private val calmPrompts = listOf(
        "How are you feeling right now?",
        "Take a breath and check in with moody.",
        "Pause for a moment to log your mood.",
        "Check in with yourself — how's your mood?"
    )

    fun isWithinQuietHours(hourOfDay: Int): Boolean = hourOfDay >= 22 || hourOfDay < 8

    fun ensureNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Mood Check-in Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Periodic reminders to pause and log how you're feeling in moody."
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    fun scheduleReminders(
        context: Context,
        settings: ReminderSettings,
        replaceExisting: Boolean
    ) {
        ensureNotificationChannel(context)
        val intervalMinutes = settings.normalized().intervalMinutes
        val request = PeriodicWorkRequestBuilder<MoodReminderWorker>(
            intervalMinutes,
            TimeUnit.MINUTES
        )
            .setInitialDelay(intervalMinutes, TimeUnit.MINUTES)
            .build()

        val policy = if (replaceExisting) {
            ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE
        } else {
            ExistingPeriodicWorkPolicy.KEEP
        }

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_WORK_NAME,
            policy,
            request
        )
    }

    fun cancelReminders(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
    }

    fun canPostNotifications(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return false
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    @SuppressLint("MissingPermission")
    fun postReminderNotification(context: Context, isTest: Boolean = false): Boolean {
        ensureNotificationChannel(context)
        if (!canPostNotifications(context)) return false

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_CHECKIN, true)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val bodyText = if (isTest) {
            "Reminders are working — tap to check in with moody."
        } else {
            val index = abs((System.currentTimeMillis() / 60_000L).toInt()) % calmPrompts.size
            calmPrompts[index]
        }

        val publicNotification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_mood)
            .setContentTitle("moody")
            .setContentText("Check-in reminder")
            .build()

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_mood)
            .setContentTitle("moody")
            .setContentText(bodyText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bodyText))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(publicNotification)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        return true
    }
}
