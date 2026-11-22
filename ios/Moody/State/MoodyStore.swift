import Foundation
import Observation

@MainActor
@Observable
final class MoodyStore {
    private let repository: any MoodyDataRepository

    private(set) var user: MoodyUser?
    private(set) var logs: [LogEntry] = []
    private(set) var isDarkTheme: Bool = false
    private(set) var reminderSettings: ReminderSettings = ReminderSettings()
    private(set) var syncError: String?

    var isFirebaseConfigured: Bool {
        repository.isFirebaseConfigured
    }

    init(repository: (any MoodyDataRepository)? = nil) {
        let resolvedRepo = repository ?? DefaultMoodyRepository()
        self.repository = resolvedRepo
        syncFromRepository()

        resolvedRepo.onStateChange = { [weak self] in
            self?.syncFromRepository()
        }
    }

    private func syncFromRepository() {
        user = repository.currentUser
        logs = repository.currentLogs
        isDarkTheme = repository.isDarkTheme
        reminderSettings = repository.reminderSettings
        syncError = repository.syncError
    }

    func loginDemo(_ newUser: MoodyUser) async {
        await repository.loginDemo(newUser)
        syncFromRepository()
    }

    func signInWithGoogle() async -> Result<MoodyUser, Error> {
        let result = await repository.signInWithGoogle()
        syncFromRepository()
        return result
    }

    func signInWithEmail(email: String, password: String) async -> Result<MoodyUser, Error> {
        let result = await repository.signInWithEmail(email: email, password: password)
        syncFromRepository()
        return result
    }

    func signUpWithEmail(email: String, password: String, displayName: String) async -> Result<MoodyUser, Error> {
        let result = await repository.signUpWithEmail(email: email, password: password, displayName: displayName)
        syncFromRepository()
        return result
    }

    func sendPasswordReset(email: String) async -> Result<Void, Error> {
        await repository.sendPasswordReset(email: email)
    }

    func signInWithApple(
        idToken: String,
        rawNonce: String,
        fullName: PersonNameComponents?
    ) async -> Result<MoodyUser, Error> {
        let result = await repository.signInWithApple(idToken: idToken, rawNonce: rawNonce, fullName: fullName)
        syncFromRepository()
        return result
    }

    func logout() async {
        await repository.logout()
        syncFromRepository()
    }

    func saveLog(_ log: LogEntry) async {
        await repository.saveLog(log)
        syncFromRepository()
    }

    func resetLogs() async {
        await repository.resetLogs()
        syncFromRepository()
    }

    func setDarkTheme(_ enabled: Bool) async {
        await repository.setDarkTheme(enabled)
        syncFromRepository()
    }

    func updateReminderSettings(_ settings: ReminderSettings) async {
        await repository.updateReminderSettings(settings)
        syncFromRepository()
    }

    func syncLogsListener(for uid: String?) {
        if let uid, !uid.isEmpty, !(user?.isDemo ?? true), isFirebaseConfigured {
            repository.startListeningToLogs(for: uid)
        } else {
            repository.stopListeningToLogs()
        }
    }

    func refreshReminderScheduleIfNeeded() async {
        await repository.refreshReminderScheduleIfNeeded()
    }
}
