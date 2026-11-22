import Foundation
import UserNotifications

enum MoodReminderScheduler {
    static let requestIdentifierPrefix = "moody_checkin_"
    static let repeatingIdentifier = "moody_checkin_repeating"
    static let testIdentifier = "moody_checkin_test"

    static let calmPrompts: [String] = [
        "How are you feeling right now?",
        "Take a breath and check in with moody.",
        "Pause for a moment to log your mood.",
        "Check in with yourself — how's your mood?"
    ]

    static func isWithinQuietHours(hour: Int) -> Bool {
        hour >= 22 || hour < 8
    }

    static func upcomingReminderDates(
        from start: Date,
        intervalMinutes: Int,
        quietHoursEnabled: Bool,
        maxCount: Int = 48,
        calendar: Calendar = .current
    ) -> [Date] {
        guard maxCount > 0 else { return [] }
        let stepSeconds = TimeInterval(max(intervalMinutes, ReminderSettings.minIntervalMinutes) * 60)
        var results: [Date] = []
        var candidate = start.addingTimeInterval(stepSeconds)
        var iterations = 0

        while results.count < maxCount && iterations < 500 {
            iterations += 1
            if !quietHoursEnabled {
                results.append(candidate)
                candidate = candidate.addingTimeInterval(stepSeconds)
                continue
            }

            let hour = calendar.component(.hour, from: candidate)
            if !isWithinQuietHours(hour: hour) {
                results.append(candidate)
                candidate = candidate.addingTimeInterval(stepSeconds)
            } else if let nextMorning = nextQuietHoursEnd(after: candidate, calendar: calendar) {
                candidate = nextMorning
            } else {
                candidate = candidate.addingTimeInterval(stepSeconds)
            }
        }

        return results
    }

    private static func nextQuietHoursEnd(after date: Date, calendar: Calendar) -> Date? {
        let hour = calendar.component(.hour, from: date)
        let baseDay: Date
        if hour >= 22 {
            guard let tomorrow = calendar.date(byAdding: .day, value: 1, to: date) else {
                return nil
            }
            baseDay = tomorrow
        } else {
            baseDay = date
        }

        var comps = calendar.dateComponents([.year, .month, .day], from: baseDay)
        comps.hour = 8
        comps.minute = 0
        comps.second = 0
        return calendar.date(from: comps)
    }

    static func authorizationStatus() async -> UNAuthorizationStatus {
        let settings = await UNUserNotificationCenter.current().notificationSettings()
        return settings.authorizationStatus
    }

    static func requestAuthorization() async -> Bool {
        do {
            return try await UNUserNotificationCenter.current()
                .requestAuthorization(options: [.alert, .sound, .badge])
        } catch {
            return false
        }
    }

    static func cancelReminders() async {
        let center = UNUserNotificationCenter.current()
        let pending = await center.pendingNotificationRequests()
        let ids = pending
            .map(\.identifier)
            .filter { $0.hasPrefix(requestIdentifierPrefix) }
        if !ids.isEmpty {
            center.removePendingNotificationRequests(withIdentifiers: ids)
        }
    }

    static func syncSchedule(settings: ReminderSettings) async {
        await cancelReminders()
        let normalized = settings.normalized()
        guard normalized.enabled else { return }

        let status = await authorizationStatus()
        guard status == .authorized || status == .provisional || status == .ephemeral else {
            return
        }

        let center = UNUserNotificationCenter.current()
        if !normalized.quietHoursEnabled {
            let content = makeNotificationContent(promptIndex: 0, isTest: false)
            let trigger = UNTimeIntervalNotificationTrigger(
                timeInterval: TimeInterval(normalized.intervalMinutes * 60),
                repeats: true
            )
            let request = UNNotificationRequest(
                identifier: repeatingIdentifier,
                content: content,
                trigger: trigger
            )
            try? await center.add(request)
        } else {
            let dates = upcomingReminderDates(
                from: Date(),
                intervalMinutes: normalized.intervalMinutes,
                quietHoursEnabled: true,
                maxCount: 48
            )
            let calendar = Calendar.current
            for (index, date) in dates.enumerated() {
                let comps = calendar.dateComponents([.year, .month, .day, .hour, .minute, .second], from: date)
                let trigger = UNCalendarNotificationTrigger(dateMatching: comps, repeats: false)
                let content = makeNotificationContent(promptIndex: index, isTest: false)
                let request = UNNotificationRequest(
                    identifier: "\(requestIdentifierPrefix)\(index)",
                    content: content,
                    trigger: trigger
                )
                try? await center.add(request)
            }
        }
    }

    static func sendTestNotification() async -> Bool {
        let status = await authorizationStatus()
        guard status == .authorized || status == .provisional || status == .ephemeral else {
            return false
        }
        let content = makeNotificationContent(promptIndex: 0, isTest: true)
        let trigger = UNTimeIntervalNotificationTrigger(timeInterval: 1.0, repeats: false)
        let request = UNNotificationRequest(
            identifier: testIdentifier,
            content: content,
            trigger: trigger
        )
        do {
            try await UNUserNotificationCenter.current().add(request)
            return true
        } catch {
            return false
        }
    }

    private static func makeNotificationContent(promptIndex: Int, isTest: Bool) -> UNMutableNotificationContent {
        let content = UNMutableNotificationContent()
        content.title = "moody"
        content.body = isTest
            ? "Reminders are working — tap to check in with moody."
            : calmPrompts[abs(promptIndex) % calmPrompts.count]
        content.sound = .default
        return content
    }
}

final class MoodNotificationDelegate: NSObject, UNUserNotificationCenterDelegate, @unchecked Sendable {
    static let shared = MoodNotificationDelegate()

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification
    ) async -> UNNotificationPresentationOptions {
        [.banner, .sound]
    }
}
