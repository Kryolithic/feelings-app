package com.example.feelingsapp.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.feelingsapp.data.ReminderSettings
import com.example.feelingsapp.data.User
import com.example.feelingsapp.notifications.MoodReminderScheduler
import kotlinx.coroutines.launch

private fun openAppNotificationSettings(context: Context) {
    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        }
    } else {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }
    }
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
}

@Composable
fun ProfileScreen(
    user: User,
    isDarkTheme: Boolean,
    reminderSettings: ReminderSettings,
    onToggleTheme: () -> Unit,
    onUpdateReminderSettings: (ReminderSettings) -> Unit,
    onSendPasswordReset: suspend (String) -> Result<Unit> = {
        Result.failure(IllegalStateException("Firebase is not configured"))
    },
    onLogout: () -> Unit,
    onResetData: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showResetDialog by remember { mutableStateOf(false) }
    var showCustomIntervalDialog by remember { mutableStateOf(false) }
    var permissionDeniedNotice by remember { mutableStateOf(false) }
    var reminderFeedback by remember { mutableStateOf<String?>(null) }
    var pendingPermissionAction by remember { mutableStateOf<String?>(null) }
    var passwordResetFeedback by remember { mutableStateOf<String?>(null) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        val action = pendingPermissionAction
        pendingPermissionAction = null
        if (granted) {
            permissionDeniedNotice = false
            when (action) {
                "enable" -> {
                    val updated = reminderSettings.copy(enabled = true)
                    onUpdateReminderSettings(updated)
                    reminderFeedback = "Reminders scheduled every ${ReminderSettings.formatInterval(updated.intervalMinutes)}"
                }
                "test" -> {
                    val sent = MoodReminderScheduler.postReminderNotification(context, isTest = true)
                    permissionDeniedNotice = !sent
                    reminderFeedback = if (sent) {
                        "Test notification sent"
                    } else {
                        "Notifications are disabled in system settings"
                    }
                }
            }
        } else {
            permissionDeniedNotice = true
            if (action == "enable") {
                onUpdateReminderSettings(reminderSettings.copy(enabled = false))
            }
        }
    }

    fun needsNotificationPermission(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
    }

    fun handleToggleReminders(enable: Boolean) {
        reminderFeedback = null
        if (!enable) {
            permissionDeniedNotice = false
            onUpdateReminderSettings(reminderSettings.copy(enabled = false))
            return
        }
        if (needsNotificationPermission()) {
            pendingPermissionAction = "enable"
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            return
        }
        val canPost = MoodReminderScheduler.canPostNotifications(context)
        permissionDeniedNotice = !canPost
        onUpdateReminderSettings(reminderSettings.copy(enabled = true))
    }

    fun handleSendTestNotification() {
        reminderFeedback = null
        if (needsNotificationPermission()) {
            pendingPermissionAction = "test"
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            return
        }
        val sent = MoodReminderScheduler.postReminderNotification(context, isTest = true)
        permissionDeniedNotice = !sent
        reminderFeedback = if (sent) {
            "Test notification sent"
        } else {
            "Notifications are disabled in system settings"
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text("Profile", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text(
            "Manage your personal settings and app preferences.",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
        )

        // User Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(20.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                ) {
                    Text(
                        text = user.name.take(1).ifEmpty { "M" },
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = user.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = user.email,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Settings items
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 28.dp)
        ) {
            SettingItem(
                title = "Theme Mode",
                actionContent = {
                    Button(
                        onClick = onToggleTheme,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text(
                            text = if (isDarkTheme) "Silent Night" else "Calm Dawn",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            )

            RemindersSettingCard(
                settings = reminderSettings,
                permissionDeniedNotice = permissionDeniedNotice,
                feedbackMessage = reminderFeedback,
                onToggleEnabled = ::handleToggleReminders,
                onSelectInterval = { minutes ->
                    reminderFeedback = null
                    onUpdateReminderSettings(reminderSettings.copy(enabled = true, intervalMinutes = minutes))
                },
                onOpenCustomDialog = { showCustomIntervalDialog = true },
                onToggleQuietHours = { quiet ->
                    reminderFeedback = null
                    onUpdateReminderSettings(reminderSettings.copy(quietHoursEnabled = quiet))
                },
                onSendTest = ::handleSendTestNotification,
                onOpenSystemSettings = { openAppNotificationSettings(context) }
            )

            SettingItem(
                title = "Connections",
                actionContent = {
                    Text(
                        text = if (user.isDemo) "Local Demo Mode" else "Firebase Cloud Sync",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            )

            SettingItem(
                title = "Password",
                actionContent = {
                    when {
                        user.isDemo -> Text(
                            "Local Demo Account",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        user.authProvider == "apple" -> Text(
                            "Managed by Apple",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        user.authProvider == "password" -> Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = passwordResetFeedback ?: "Email & Password",
                                fontSize = 13.sp,
                                color = if (passwordResetFeedback == "Reset email sent") {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                            if (user.email.isNotBlank()) {
                                TextButton(
                                    onClick = {
                                        passwordResetFeedback = null
                                        coroutineScope.launch {
                                            val result = onSendPasswordReset(user.email)
                                            passwordResetFeedback = if (result.isSuccess) {
                                                "Reset email sent"
                                            } else {
                                                result.exceptionOrNull()?.localizedMessage ?: "Reset failed"
                                            }
                                        }
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Text("Reset", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                        else -> Text(
                            "Managed by Google",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        }

        Button(
            onClick = { showResetDialog = true },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                contentColor = Color(0xFFEB5E55)
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .border(1.dp, Color(0xFFEB5E55).copy(alpha = 0.35f), RoundedCornerShape(16.dp))
        ) {
            Text("Reset History", fontSize = 15.sp, fontWeight = FontWeight.Medium)
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onLogout,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.onBackground,
                contentColor = MaterialTheme.colorScheme.background
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text("Log Out", fontSize = 15.sp, fontWeight = FontWeight.Medium)
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset History?") },
            text = { Text("Are you sure you want to clear your feelings log history? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showResetDialog = false
                        onResetData()
                    }
                ) {
                    Text("Clear History", color = Color(0xFFEB5E55), fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showCustomIntervalDialog) {
        CustomReminderIntervalDialog(
            currentIntervalMinutes = reminderSettings.intervalMinutes,
            onDismiss = { showCustomIntervalDialog = false },
            onConfirm = { totalMinutes ->
                showCustomIntervalDialog = false
                reminderFeedback = null
                onUpdateReminderSettings(
                    reminderSettings.copy(
                        enabled = true,
                        intervalMinutes = totalMinutes
                    )
                )
            }
        )
    }
}

@Composable
private fun RemindersSettingCard(
    settings: ReminderSettings,
    permissionDeniedNotice: Boolean,
    feedbackMessage: String?,
    onToggleEnabled: (Boolean) -> Unit,
    onSelectInterval: (Long) -> Unit,
    onOpenCustomDialog: () -> Unit,
    onToggleQuietHours: (Boolean) -> Unit,
    onSendTest: () -> Unit,
    onOpenSystemSettings: () -> Unit
) {
    val isCustomInterval = settings.intervalMinutes !in ReminderSettings.PRESET_INTERVALS_MINUTES
    val subtitle = if (settings.enabled) {
        val base = "Every ${ReminderSettings.formatInterval(settings.intervalMinutes)}"
        if (settings.quietHoursEnabled) "$base · Quiet 10 PM–8 AM" else base
    } else {
        "Get gentle nudges to log your mood"
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text("Check-in Reminders", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = settings.enabled,
                    onCheckedChange = onToggleEnabled
                )
            }

            if (permissionDeniedNotice) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Allow notifications in system settings to receive check-in reminders.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                    )
                    TextButton(
                        onClick = onOpenSystemSettings,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("Open Settings", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            if (settings.enabled) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Reminder interval",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ReminderSettings.PRESET_INTERVALS_MINUTES.forEach { preset ->
                        FilterChip(
                            selected = settings.intervalMinutes == preset,
                            onClick = { onSelectInterval(preset) },
                            label = {
                                Text(
                                    text = ReminderSettings.formatShortChip(preset),
                                    fontSize = 12.sp
                                )
                            }
                        )
                    }
                    FilterChip(
                        selected = isCustomInterval,
                        onClick = onOpenCustomDialog,
                        label = {
                            Text(
                                text = if (isCustomInterval) {
                                    "Custom (${ReminderSettings.formatShortChip(settings.intervalMinutes)})"
                                } else {
                                    "Custom"
                                },
                                fontSize = 12.sp
                            )
                        }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text("Quiet hours", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text(
                            text = "Pause notifications from 10 PM to 8 AM",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = settings.quietHoursEnabled,
                        onCheckedChange = onToggleQuietHours
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(
                        onClick = onSendTest,
                        contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp)
                    ) {
                        Text("Send test notification", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }

                    if (feedbackMessage != null) {
                        Text(
                            text = feedbackMessage,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomReminderIntervalDialog(
    currentIntervalMinutes: Long,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    val initialInHours = currentIntervalMinutes >= 60L && currentIntervalMinutes % 60L == 0L
    var useHours by remember { mutableStateOf(initialInHours) }
    var valueInput by remember {
        mutableStateOf(
            if (initialInHours) (currentIntervalMinutes / 60L).toString()
            else currentIntervalMinutes.toString()
        )
    }

    val parsed = valueInput.trim().toLongOrNull()
    val totalMinutes = parsed?.let { if (useHours) it * 60L else it }
    val isValid = totalMinutes != null &&
        totalMinutes in ReminderSettings.MIN_INTERVAL_MINUTES..ReminderSettings.MAX_INTERVAL_MINUTES

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Custom Reminder Interval") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Choose how often moody should nudge you to check in.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !useHours,
                        onClick = { useHours = false },
                        label = { Text("Minutes") }
                    )
                    FilterChip(
                        selected = useHours,
                        onClick = { useHours = true },
                        label = { Text("Hours") }
                    )
                }

                OutlinedTextField(
                    value = valueInput,
                    onValueChange = { input ->
                        if (input.all { it.isDigit() } && input.length <= 5) {
                            valueInput = input
                        }
                    },
                    label = { Text(if (useHours) "Every (hours)" else "Every (minutes)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = valueInput.isNotEmpty() && !isValid,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = if (valueInput.isNotEmpty() && !isValid) {
                        "Enter between 15 minutes and 7 days (168 hours)."
                    } else {
                        "Minimum 15 minutes, maximum 7 days (168 hours)."
                    },
                    fontSize = 12.sp,
                    color = if (valueInput.isNotEmpty() && !isValid) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = isValid,
                onClick = {
                    if (totalMinutes != null) {
                        onConfirm(totalMinutes)
                    }
                }
            ) {
                Text("Save", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun SettingItem(
    title: String,
    actionContent: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            actionContent()
        }
    }
}
