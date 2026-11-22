package com.example.feelingsapp.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import java.util.Calendar

class MoodReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val prefs = applicationContext.getSharedPreferences(
            MoodReminderScheduler.PREFS_NAME,
            Context.MODE_PRIVATE
        )
        val enabled = prefs.getBoolean(MoodReminderScheduler.KEY_ENABLED, false)
        if (!enabled) {
            return Result.success()
        }

        val quietHours = prefs.getBoolean(MoodReminderScheduler.KEY_QUIET_HOURS, false)
        if (quietHours) {
            val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            if (MoodReminderScheduler.isWithinQuietHours(currentHour)) {
                return Result.success()
            }
        }

        MoodReminderScheduler.postReminderNotification(applicationContext, isTest = false)
        return Result.success()
    }
}
