import SwiftUI
import UIKit

struct ProfileView: View {
    @Environment(MoodyStore.self) private var store
    @Environment(\.openURL) private var openURL

    @State private var showResetAlert = false
    @State private var showCustomIntervalSheet = false
    @State private var permissionDeniedNotice = false
    @State private var reminderFeedback: String?
    @State private var passwordResetFeedback: String?

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 24) {
                VStack(alignment: .leading, spacing: 4) {
                    Text("Profile")
                        .font(.system(size: 24, weight: .bold))
                    Text("Manage your personal settings and app preferences.")
                        .font(.system(size: 14))
                        .foregroundStyle(.secondary)
                }

                if let currentUser = store.user {
                    userHeaderCard(currentUser)
                }

                VStack(spacing: 12) {
                    themeSettingCard
                    remindersSettingCard
                    connectionsSettingCard
                    passwordSettingCard
                }

                VStack(spacing: 12) {
                    Button {
                        showResetAlert = true
                    } label: {
                        Text("Reset History")
                            .font(.system(size: 15, weight: .medium))
                            .foregroundStyle(Color(hex: "#EB5E55"))
                            .frame(maxWidth: .infinity)
                            .frame(height: 50)
                            .overlay(
                                RoundedRectangle(cornerRadius: 16, style: .continuous)
                                    .stroke(Color(hex: "#EB5E55").opacity(0.4), lineWidth: 1)
                            )
                    }

                    Button {
                        Task { await store.logout() }
                    } label: {
                        Text("Log Out")
                            .font(.system(size: 15, weight: .medium))
                            .foregroundStyle(Color(.systemBackground))
                            .frame(maxWidth: .infinity)
                            .frame(height: 50)
                            .background(Color.primary)
                            .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                    }
                }
            }
            .padding(24)
        }
        .alert("Reset History?", isPresented: $showResetAlert) {
            Button("Cancel", role: .cancel) {}
            Button("Clear History", role: .destructive) {
                Task { await store.resetLogs() }
            }
        } message: {
            Text("Are you sure you want to clear your feelings log history? This action cannot be undone.")
        }
        .sheet(isPresented: $showCustomIntervalSheet) {
            CustomIntervalSheet(currentIntervalMinutes: store.reminderSettings.intervalMinutes) { totalMinutes in
                showCustomIntervalSheet = false
                reminderFeedback = nil
                var next = store.reminderSettings
                next.enabled = true
                next.intervalMinutes = totalMinutes
                Task { await store.updateReminderSettings(next) }
            }
            .presentationDetents([.medium])
        }
    }

    private func userHeaderCard(_ currentUser: MoodyUser) -> some View {
        HStack(spacing: 16) {
            ZStack {
                Circle()
                    .fill(Color.accentColor.opacity(0.2))
                    .frame(width: 60, height: 60)
                Text(String(currentUser.name.prefix(1)).isEmpty ? "M" : String(currentUser.name.prefix(1)))
                    .font(.system(size: 24, weight: .bold))
                    .foregroundStyle(Color.accentColor)
            }

            VStack(alignment: .leading, spacing: 2) {
                Text(currentUser.name)
                    .font(.system(size: 18, weight: .bold))
                Text(currentUser.email)
                    .font(.system(size: 14))
                    .foregroundStyle(.secondary)
            }
            Spacer()
        }
        .padding(20)
        .background(Color(.secondarySystemBackground))
        .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
    }

    private var themeSettingCard: some View {
        HStack {
            Text("Theme Mode")
                .font(.system(size: 15, weight: .medium))
            Spacer()
            Button {
                let next = !store.isDarkTheme
                Task { await store.setDarkTheme(next) }
            } label: {
                Text(store.isDarkTheme ? "Silent Night" : "Calm Dawn")
                    .font(.system(size: 12, weight: .bold))
                    .foregroundStyle(Color.accentColor)
                    .padding(.horizontal, 14)
                    .padding(.vertical, 8)
                    .background(Color.accentColor.opacity(0.15))
                    .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
            }
        }
        .padding(16)
        .background(Color(.secondarySystemBackground))
        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
    }

    private var remindersSettingCard: some View {
        let settings = store.reminderSettings
        let isCustomInterval = !ReminderSettings.presetIntervalsMinutes.contains(settings.intervalMinutes)
        let subtitle: String = {
            if !settings.enabled {
                return "Get gentle nudges to log your mood"
            }
            let base = "Every \(ReminderSettings.formatInterval(settings.intervalMinutes))"
            return settings.quietHoursEnabled ? "\(base) · Quiet 10 PM–8 AM" : base
        }()

        return VStack(alignment: .leading, spacing: 12) {
            Toggle(isOn: Binding(
                get: { settings.enabled },
                set: { handleToggleReminders($0) }
            )) {
                VStack(alignment: .leading, spacing: 2) {
                    Text("Check-in Reminders")
                        .font(.system(size: 15, weight: .medium))
                    Text(subtitle)
                        .font(.system(size: 12))
                        .foregroundStyle(.secondary)
                }
            }
            .tint(Color.accentColor)

            if permissionDeniedNotice {
                HStack(spacing: 10) {
                    Text("Allow notifications in Settings to receive check-in reminders.")
                        .font(.system(size: 12))
                        .foregroundStyle(.red)
                    Spacer()
                    Button("Open Settings") {
                        if let url = URL(string: UIApplication.openNotificationSettingsURLString) {
                            openURL(url)
                        }
                    }
                    .font(.system(size: 12, weight: .semibold))
                }
                .padding(10)
                .background(Color.red.opacity(0.1))
                .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
            }

            if settings.enabled {
                Divider()

                VStack(alignment: .leading, spacing: 8) {
                    Text("Reminder interval")
                        .font(.system(size: 12, weight: .semibold))
                        .foregroundStyle(.secondary)

                    FlowLayout(spacing: 8, lineSpacing: 8) {
                        ForEach(ReminderSettings.presetIntervalsMinutes, id: \.self) { preset in
                            IntervalChipButton(
                                title: ReminderSettings.formatShortChip(preset),
                                isSelected: settings.intervalMinutes == preset
                            ) {
                                reminderFeedback = nil
                                var next = settings
                                next.enabled = true
                                next.intervalMinutes = preset
                                Task { await store.updateReminderSettings(next) }
                            }
                        }

                        IntervalChipButton(
                            title: isCustomInterval
                                ? "Custom (\(ReminderSettings.formatShortChip(settings.intervalMinutes)))"
                                : "Custom",
                            isSelected: isCustomInterval
                        ) {
                            showCustomIntervalSheet = true
                        }
                    }
                }

                Toggle(isOn: Binding(
                    get: { settings.quietHoursEnabled },
                    set: { quiet in
                        reminderFeedback = nil
                        var next = settings
                        next.quietHoursEnabled = quiet
                        Task { await store.updateReminderSettings(next) }
                    }
                )) {
                    VStack(alignment: .leading, spacing: 2) {
                        Text("Quiet hours")
                            .font(.system(size: 14, weight: .medium))
                        Text("Pause notifications from 10 PM to 8 AM")
                            .font(.system(size: 12))
                            .foregroundStyle(.secondary)
                    }
                }
                .tint(Color.accentColor)

                HStack {
                    Button("Send test notification") {
                        sendTestNotification()
                    }
                    .font(.system(size: 13, weight: .semibold))

                    Spacer()

                    if let reminderFeedback {
                        Text(reminderFeedback)
                            .font(.system(size: 12))
                            .foregroundStyle(Color.accentColor)
                    }
                }
            }
        }
        .padding(16)
        .background(Color(.secondarySystemBackground))
        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
    }

    private var connectionsSettingCard: some View {
        HStack {
            Text("Connections")
                .font(.system(size: 15, weight: .medium))
            Spacer()
            Text((store.user?.isDemo ?? true) ? "Local Demo Mode" : "Firebase Cloud Sync")
                .font(.system(size: 13))
                .foregroundStyle(.secondary)
        }
        .padding(16)
        .background(Color(.secondarySystemBackground))
        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
    }

    private var passwordSettingCard: some View {
        let user = store.user
        let isDemo = user?.isDemo ?? true
        let provider = user?.authProvider ?? "demo"

        return HStack(spacing: 10) {
            Text("Password")
                .font(.system(size: 15, weight: .medium))
            Spacer()
            if isDemo {
                Text("Local Demo Account")
                    .font(.system(size: 13))
                    .foregroundStyle(.secondary)
            } else if provider == "apple" {
                Text("Managed by Apple")
                    .font(.system(size: 13))
                    .foregroundStyle(.secondary)
            } else if provider == "password" {
                HStack(spacing: 8) {
                    Text(passwordResetFeedback ?? "Email & Password")
                        .font(.system(size: 13))
                        .foregroundStyle(
                            passwordResetFeedback == "Reset email sent"
                                ? Color.accentColor
                                : .secondary
                        )
                    if let email = user?.email, !email.isEmpty {
                        Button("Reset") {
                            passwordResetFeedback = nil
                            Task {
                                let res = await store.sendPasswordReset(email: email)
                                await MainActor.run {
                                    switch res {
                                    case .success:
                                        passwordResetFeedback = "Reset email sent"
                                    case .failure(let error):
                                        passwordResetFeedback = error.localizedDescription
                                    }
                                }
                            }
                        }
                        .font(.system(size: 12, weight: .semibold))
                        .padding(.horizontal, 10)
                        .padding(.vertical, 5)
                        .background(Color.accentColor.opacity(0.15))
                        .clipShape(RoundedRectangle(cornerRadius: 8, style: .continuous))
                    }
                }
            } else {
                Text("Managed by Google")
                    .font(.system(size: 13))
                    .foregroundStyle(.secondary)
            }
        }
        .padding(16)
        .background(Color(.secondarySystemBackground))
        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
    }

    private func handleToggleReminders(_ enable: Bool) {
        reminderFeedback = nil
        if !enable {
            permissionDeniedNotice = false
            var next = store.reminderSettings
            next.enabled = false
            Task { await store.updateReminderSettings(next) }
            return
        }

        Task {
            let status = await MoodReminderScheduler.authorizationStatus()
            let allowed: Bool
            switch status {
            case .authorized, .provisional, .ephemeral:
                allowed = true
            case .notDetermined:
                allowed = await MoodReminderScheduler.requestAuthorization()
            case .denied:
                allowed = false
            @unknown default:
                allowed = false
            }

            await MainActor.run {
                permissionDeniedNotice = !allowed
                if allowed {
                    var next = store.reminderSettings
                    next.enabled = true
                    Task { await store.updateReminderSettings(next) }
                } else {
                    var next = store.reminderSettings
                    next.enabled = false
                    Task { await store.updateReminderSettings(next) }
                }
            }
        }
    }

    private func sendTestNotification() {
        reminderFeedback = nil
        Task {
            let status = await MoodReminderScheduler.authorizationStatus()
            if status == .notDetermined {
                _ = await MoodReminderScheduler.requestAuthorization()
            }
            let sent = await MoodReminderScheduler.sendTestNotification()
            await MainActor.run {
                permissionDeniedNotice = !sent
                reminderFeedback = sent
                    ? "Test notification sent"
                    : "Notifications are disabled in Settings"
            }
        }
    }
}

private struct IntervalChipButton: View {
    let title: String
    let isSelected: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(title)
                .font(.system(size: 12, weight: isSelected ? .semibold : .medium))
                .foregroundStyle(isSelected ? Color.accentColor : .primary)
                .padding(.horizontal, 12)
                .padding(.vertical, 7)
                .background(isSelected ? Color.accentColor.opacity(0.16) : Color(.tertiarySystemFill))
                .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
                .overlay(
                    RoundedRectangle(cornerRadius: 10, style: .continuous)
                        .stroke(isSelected ? Color.accentColor : .clear, lineWidth: 1)
                )
        }
        .buttonStyle(.plain)
    }
}

private struct CustomIntervalSheet: View {
    @Environment(\.dismiss) private var dismiss

    let currentIntervalMinutes: Int
    let onConfirm: (Int) -> Void

    @State private var useHours: Bool
    @State private var valueText: String

    init(currentIntervalMinutes: Int, onConfirm: @escaping (Int) -> Void) {
        self.currentIntervalMinutes = currentIntervalMinutes
        self.onConfirm = onConfirm
        let initialInHours = currentIntervalMinutes >= 60 && currentIntervalMinutes % 60 == 0
        _useHours = State(initialValue: initialInHours)
        _valueText = State(
            initialValue: initialInHours
                ? "\(currentIntervalMinutes / 60)"
                : "\(currentIntervalMinutes)"
        )
    }

    private var computedMinutes: Int? {
        guard let parsed = Int(valueText.trimmingCharacters(in: .whitespaces)) else { return nil }
        return useHours ? parsed * 60 : parsed
    }

    private var isValid: Bool {
        guard let mins = computedMinutes else { return false }
        return mins >= ReminderSettings.minIntervalMinutes && mins <= ReminderSettings.maxIntervalMinutes
    }

    var body: some View {
        NavigationStack {
            VStack(alignment: .leading, spacing: 16) {
                Text("Choose how often moody should nudge you to check in.")
                    .font(.system(size: 14))
                    .foregroundStyle(.secondary)

                Picker("Unit", selection: $useHours) {
                    Text("Minutes").tag(false)
                    Text("Hours").tag(true)
                }
                .pickerStyle(.segmented)

                TextField(useHours ? "Every (hours)" : "Every (minutes)", text: $valueText)
                    .keyboardType(.numberPad)
                    .padding(12)
                    .background(Color(.secondarySystemBackground))
                    .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                    .onChange(of: valueText) { _, newValue in
                        let digitsOnly = String(newValue.filter(\.isNumber).prefix(5))
                        if digitsOnly != newValue {
                            valueText = digitsOnly
                        }
                    }

                Text(
                    !valueText.isEmpty && !isValid
                        ? "Enter between 15 minutes and 7 days (168 hours)."
                        : "Minimum 15 minutes, maximum 7 days (168 hours)."
                )
                .font(.system(size: 12))
                .foregroundStyle(!valueText.isEmpty && !isValid ? .red : .secondary)

                Spacer()
            }
            .padding(24)
            .navigationTitle("Custom Reminder Interval")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Save") {
                        if let mins = computedMinutes, isValid {
                            onConfirm(mins)
                        }
                    }
                    .disabled(!isValid)
                }
            }
        }
    }
}
