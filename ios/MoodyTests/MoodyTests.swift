import XCTest
@testable import Moody

@MainActor
final class MoodyTests: XCTestCase {

    func testFeelingsDataHappyWheelMappingIsAccurate() {
        let happy = FeelingsData.emotion(named: "Happy")
        XCTAssertNotNil(happy)
        XCTAssertEqual(happy?.sub2Options(for: "Playful"), ["Aroused", "Cheeky"])
        XCTAssertEqual(happy?.sub2Options(for: "Content"), ["Free", "Joyful"])
        XCTAssertEqual(happy?.sub2Options(for: "Optimistic"), ["Hopeful", "Inspired"])
        XCTAssertEqual(FeelingsData.emotions.count, 7)
    }

    func testIsoTimestampRoundTripsUtcMillis() {
        let now = Date()
        let formatted = IsoTimestamp.formatUtc(now)
        let parsed = IsoTimestamp.parseUtc(formatted)
        XCTAssertNotNil(parsed)
        if let parsed {
            XCTAssertEqual(now.timeIntervalSince1970, parsed.timeIntervalSince1970, accuracy: 0.002)
        }
    }

    func testSaveLogAndResetLogsUpdatesState() async {
        let repo = FakeMoodyRepository()
        let store = MoodyStore(repository: repo)

        let entry = LogEntry(
            id: "log-1",
            timestamp: IsoTimestamp.formatUtc(Date()),
            core: "Happy",
            sub1: "Content",
            sub2: "Joyful",
            intensity: 8,
            note: "Feeling calm",
            color: "#f2a380"
        )

        await store.saveLog(entry)
        XCTAssertEqual(store.logs.count, 1)
        XCTAssertEqual(store.logs.first?.primaryLabel, "Joyful")
        XCTAssertEqual(store.logs.first?.secondaryLabel, "Content")

        await store.resetLogs()
        XCTAssertTrue(store.logs.isEmpty)
    }

    func testSaveLogWithSkippedSub2FallsBackCleanly() async {
        let repo = FakeMoodyRepository()
        let store = MoodyStore(repository: repo)

        let entry = LogEntry(
            id: "log-skip-sub2",
            timestamp: IsoTimestamp.formatUtc(Date()),
            core: "Happy",
            sub1: "Content",
            sub2: "",
            intensity: 6,
            note: "Skipped refinement",
            color: "#f2a380"
        )

        await store.saveLog(entry)
        XCTAssertEqual(store.logs.count, 1)
        XCTAssertEqual(store.logs.first?.sub2, "")
        XCTAssertEqual(store.logs.first?.primaryLabel, "Content")
        XCTAssertEqual(store.logs.first?.secondaryLabel, "Happy")
    }

    func testSetDarkThemeAndDemoLoginLogoutUpdatesState() async {
        let repo = FakeMoodyRepository()
        let store = MoodyStore(repository: repo)

        XCTAssertNil(store.user)
        XCTAssertFalse(store.isDarkTheme)

        let demoUser = MoodyUser(name: "Gary", email: "gary@gmail.com", uid: "demo-gary", isDemo: true)
        await store.loginDemo(demoUser)
        XCTAssertEqual(store.user, demoUser)

        await store.setDarkTheme(true)
        XCTAssertTrue(store.isDarkTheme)

        await store.logout()
        XCTAssertNil(store.user)
        XCTAssertTrue(store.logs.isEmpty)
    }

    func testReminderSettingsClampingAndFormatting() async {
        let repo = FakeMoodyRepository()
        let store = MoodyStore(repository: repo)

        await store.updateReminderSettings(
            ReminderSettings(enabled: true, intervalMinutes: 5, quietHoursEnabled: true)
        )
        XCTAssertTrue(store.reminderSettings.enabled)
        XCTAssertEqual(store.reminderSettings.intervalMinutes, ReminderSettings.minIntervalMinutes)
        XCTAssertTrue(store.reminderSettings.quietHoursEnabled)

        let clampedHigh = ReminderSettings(enabled: true, intervalMinutes: 20_000).normalized()
        XCTAssertEqual(clampedHigh.intervalMinutes, ReminderSettings.maxIntervalMinutes)

        XCTAssertEqual(ReminderSettings.formatInterval(15), "15 min")
        XCTAssertEqual(ReminderSettings.formatInterval(60), "1 hour")
        XCTAssertEqual(ReminderSettings.formatInterval(90), "1h 30m")
        XCTAssertEqual(ReminderSettings.formatInterval(1440), "24 hours")
        XCTAssertEqual(ReminderSettings.formatInterval(2880), "2 days")
        XCTAssertEqual(ReminderSettings.formatShortChip(240), "4h")
    }

    func testQuietHoursAndUpcomingReminderDates() {
        XCTAssertTrue(MoodReminderScheduler.isWithinQuietHours(hour: 22))
        XCTAssertTrue(MoodReminderScheduler.isWithinQuietHours(hour: 23))
        XCTAssertTrue(MoodReminderScheduler.isWithinQuietHours(hour: 0))
        XCTAssertTrue(MoodReminderScheduler.isWithinQuietHours(hour: 7))
        XCTAssertFalse(MoodReminderScheduler.isWithinQuietHours(hour: 8))
        XCTAssertFalse(MoodReminderScheduler.isWithinQuietHours(hour: 14))
        XCTAssertFalse(MoodReminderScheduler.isWithinQuietHours(hour: 21))

        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = TimeZone(secondsFromGMT: 0) ?? .current
        let start = calendar.date(from: DateComponents(year: 2026, month: 11, day: 21, hour: 21, minute: 30))!

        let scheduled = MoodReminderScheduler.upcomingReminderDates(
            from: start,
            intervalMinutes: 60,
            quietHoursEnabled: true,
            maxCount: 3,
            calendar: calendar
        )

        XCTAssertEqual(scheduled.count, 3)
        for date in scheduled {
            let hour = calendar.component(.hour, from: date)
            XCTAssertFalse(MoodReminderScheduler.isWithinQuietHours(hour: hour))
        }
        XCTAssertEqual(calendar.component(.hour, from: scheduled[0]), 8)
    }
}

@MainActor
private final class FakeMoodyRepository: MoodyDataRepository {
    private(set) var currentUser: MoodyUser?
    private(set) var currentLogs: [LogEntry] = []
    private(set) var isDarkTheme: Bool = false
    private(set) var reminderSettings: ReminderSettings = ReminderSettings()
    let isFirebaseConfigured: Bool = false
    private(set) var syncError: String?

    var onStateChange: (() -> Void)?

    func saveLog(_ log: LogEntry) async {
        currentLogs.insert(log, at: 0)
        onStateChange?()
    }

    func resetLogs() async {
        currentLogs.removeAll()
        onStateChange?()
    }

    func loginDemo(_ user: MoodyUser) async {
        currentUser = user
        onStateChange?()
    }

    func signInWithGoogle() async -> Result<MoodyUser, Error> {
        let user = MoodyUser(name: "Cloud User", email: "cloud@feelings.app", uid: "uid-test", isDemo: false)
        currentUser = user
        onStateChange?()
        return .success(user)
    }

    func logout() async {
        currentUser = nil
        currentLogs = []
        onStateChange?()
    }

    func setDarkTheme(_ enabled: Bool) async {
        isDarkTheme = enabled
        onStateChange?()
    }

    func updateReminderSettings(_ settings: ReminderSettings) async {
        reminderSettings = settings.normalized()
        onStateChange?()
    }

    func startListeningToLogs(for uid: String) {}
    func stopListeningToLogs() {}
    func refreshReminderScheduleIfNeeded() async {}
}
