import Foundation
import SwiftUI

struct LogEntry: Identifiable, Codable, Equatable, Hashable, Sendable {
    let id: String
    let timestamp: String
    let core: String
    let sub1: String
    let sub2: String
    let intensity: Int
    let note: String
    let color: String

    var primaryLabel: String {
        sub2.isEmpty ? sub1 : sub2
    }

    var secondaryLabel: String {
        sub2.isEmpty ? core : sub1
    }
}

struct MoodyUser: Codable, Equatable, Sendable {
    let name: String
    let email: String
    let avatar: String
    let uid: String
    let isDemo: Bool

    init(
        name: String,
        email: String,
        avatar: String = "",
        uid: String = "",
        isDemo: Bool = true
    ) {
        self.name = name
        self.email = email
        self.avatar = avatar
        self.uid = uid
        self.isDemo = isDemo
    }
}

struct ReminderSettings: Codable, Equatable, Sendable {
    var enabled: Bool = false
    var intervalMinutes: Int = Self.defaultIntervalMinutes
    var quietHoursEnabled: Bool = false

    static let minIntervalMinutes = 15
    static let maxIntervalMinutes = 10_080 // 7 days
    static let defaultIntervalMinutes = 240 // 4 hours
    static let presetIntervalsMinutes: [Int] = [15, 30, 60, 120, 240, 360, 720, 1440]

    func normalized() -> ReminderSettings {
        var copy = self
        copy.intervalMinutes = min(
            max(intervalMinutes, Self.minIntervalMinutes),
            Self.maxIntervalMinutes
        )
        return copy
    }

    static func formatInterval(_ minutes: Int) -> String {
        let clamped = min(max(minutes, minIntervalMinutes), maxIntervalMinutes)
        if clamped < 60 {
            return "\(clamped) min"
        } else if clamped >= 2880 && clamped % 1440 == 0 {
            return "\(clamped / 1440) days"
        } else if clamped % 60 == 0 {
            let hours = clamped / 60
            return hours == 1 ? "1 hour" : "\(hours) hours"
        } else {
            return "\(clamped / 60)h \(clamped % 60)m"
        }
    }

    static func formatShortChip(_ minutes: Int) -> String {
        if minutes < 60 {
            return "\(minutes)m"
        } else if minutes % 60 == 0 {
            return "\(minutes / 60)h"
        } else {
            return "\(minutes / 60)h \(minutes % 60)m"
        }
    }
}

enum IsoTimestamp {
    static func formatUtc(_ date: Date) -> String {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.timeZone = TimeZone(secondsFromGMT: 0)
        formatter.dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
        return formatter.string(from: date)
    }

    static func parseUtc(_ value: String) -> Date? {
        let isoWithFraction = ISO8601DateFormatter()
        isoWithFraction.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        if let parsed = isoWithFraction.date(from: value) {
            return parsed
        }
        let isoPlain = ISO8601DateFormatter()
        isoPlain.formatOptions = [.withInternetDateTime]
        return isoPlain.date(from: value)
    }
}

extension Color {
    init(hex: String, fallback: Color = Color(red: 0.373, green: 0.153, blue: 0.804)) {
        let cleaned = hex.trimmingCharacters(in: .whitespacesAndNewlines)
            .replacingOccurrences(of: "#", with: "")
        var value: UInt64 = 0
        guard Scanner(string: cleaned).scanHexInt64(&value) else {
            self = fallback
            return
        }

        let r, g, b, a: Double
        switch cleaned.count {
        case 3:
            r = Double((value >> 8) & 0xF) / 15.0
            g = Double((value >> 4) & 0xF) / 15.0
            b = Double(value & 0xF) / 15.0
            a = 1.0
        case 6:
            r = Double((value >> 16) & 0xFF) / 255.0
            g = Double((value >> 8) & 0xFF) / 255.0
            b = Double(value & 0xFF) / 255.0
            a = 1.0
        case 8:
            r = Double((value >> 24) & 0xFF) / 255.0
            g = Double((value >> 16) & 0xFF) / 255.0
            b = Double((value >> 8) & 0xFF) / 255.0
            a = Double(value & 0xFF) / 255.0
        default:
            self = fallback
            return
        }

        self = Color(.sRGB, red: r, green: g, blue: b, opacity: a)
    }
}
