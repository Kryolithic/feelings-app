import Foundation

#if canImport(FirebaseCore)
import FirebaseCore
#endif

#if canImport(FirebaseAuth)
import FirebaseAuth
#endif

#if canImport(FirebaseFirestore)
import FirebaseFirestore
#endif

#if canImport(GoogleSignIn)
import GoogleSignIn
import UIKit
#endif

enum MoodySyncError: LocalizedError {
    case cancelled
    case message(String)

    var errorDescription: String? {
        switch self {
        case .cancelled:
            return nil
        case .message(let text):
            return text
        }
    }
}

@MainActor
protocol MoodyDataRepository: AnyObject {
    var currentUser: MoodyUser? { get }
    var currentLogs: [LogEntry] { get }
    var isDarkTheme: Bool { get }
    var reminderSettings: ReminderSettings { get }
    var isFirebaseConfigured: Bool { get }
    var syncError: String? { get }

    var onStateChange: (() -> Void)? { get set }

    func saveLog(_ log: LogEntry) async
    func resetLogs() async
    func loginDemo(_ user: MoodyUser) async
    func signInWithGoogle() async -> Result<MoodyUser, Error>
    func signInWithEmail(email: String, password: String) async -> Result<MoodyUser, Error>
    func signUpWithEmail(email: String, password: String, displayName: String) async -> Result<MoodyUser, Error>
    func sendPasswordReset(email: String) async -> Result<Void, Error>
    func signInWithApple(idToken: String, rawNonce: String, fullName: PersonNameComponents?) async -> Result<MoodyUser, Error>
    func logout() async
    func setDarkTheme(_ enabled: Bool) async
    func updateReminderSettings(_ settings: ReminderSettings) async
    func startListeningToLogs(for uid: String)
    func stopListeningToLogs()
    func refreshReminderScheduleIfNeeded() async
}

@MainActor
final class DefaultMoodyRepository: MoodyDataRepository {
    private enum PrefKey {
        static let themeDark = "theme_dark"
        static let demoUser = "user"
        static let remindersEnabled = "reminders_enabled"
        static let remindersIntervalMinutes = "reminders_interval_minutes"
        static let remindersQuietHours = "reminders_quiet_hours"
    }

    private let defaults: UserDefaults
    private let encoder = JSONEncoder()
    private let decoder = JSONDecoder()

    private(set) var currentUser: MoodyUser?
    private(set) var currentLogs: [LogEntry] = []
    private(set) var isDarkTheme: Bool = false
    private(set) var reminderSettings: ReminderSettings = ReminderSettings()
    let isFirebaseConfigured: Bool
    private(set) var syncError: String?

    var onStateChange: (() -> Void)?

    #if canImport(FirebaseFirestore)
    private nonisolated(unsafe) var logsListener: ListenerRegistration?
    #endif

    #if canImport(FirebaseAuth)
    private nonisolated(unsafe) var authListenerHandle: AuthStateDidChangeListenerHandle?
    #endif

    init(
        defaults: UserDefaults = .standard,
        isFirebaseConfigured: Bool = FirebaseBootstrap.isConfigured
    ) {
        self.defaults = defaults
        self.isFirebaseConfigured = isFirebaseConfigured

        self.isDarkTheme = defaults.bool(forKey: PrefKey.themeDark)

        let storedInterval = defaults.object(forKey: PrefKey.remindersIntervalMinutes) as? Int
            ?? ReminderSettings.defaultIntervalMinutes
        self.reminderSettings = ReminderSettings(
            enabled: defaults.bool(forKey: PrefKey.remindersEnabled),
            intervalMinutes: storedInterval,
            quietHoursEnabled: defaults.bool(forKey: PrefKey.remindersQuietHours)
        ).normalized()

        if isFirebaseConfigured {
            attachFirebaseAuthListener()
        } else {
            loadLocalDemoSession()
        }
    }

    deinit {
        #if canImport(FirebaseFirestore)
        logsListener?.remove()
        logsListener = nil
        #endif

        #if canImport(FirebaseAuth) && canImport(FirebaseCore)
        if FirebaseApp.app() != nil, let handle = authListenerHandle {
            Auth.auth().removeStateDidChangeListener(handle)
        }
        authListenerHandle = nil
        #endif
    }

    func refreshReminderScheduleIfNeeded() async {
        guard reminderSettings.enabled else { return }
        await MoodReminderScheduler.syncSchedule(settings: reminderSettings)
    }

    // MARK: - Firebase Auth & Firestore Listeners

    private func attachFirebaseAuthListener() {
        #if canImport(FirebaseAuth) && canImport(FirebaseCore)
        guard FirebaseApp.app() != nil else { return }
        authListenerHandle = Auth.auth().addStateDidChangeListener { [weak self] _, fbUser in
            guard let self else { return }
            Task { @MainActor [weak self] in
                guard let self else { return }
                if let fbUser {
                    self.currentUser = self.mapFirebaseUser(fbUser)
                    self.startListeningToLogs(for: fbUser.uid)
                    await self.loadRemoteThemePreference(uid: fbUser.uid)
                } else {
                    self.stopListeningToLogs()
                    self.currentUser = nil
                    self.currentLogs = []
                    self.syncError = nil
                }
                self.onStateChange?()
            }
        }
        #endif
    }

    #if canImport(FirebaseAuth)
    private func resolveAuthProvider(_ fbUser: FirebaseAuth.User) -> String {
        let providerIds = fbUser.providerData.map(\.providerID)
        if providerIds.contains("apple.com") { return "apple" }
        if providerIds.contains("google.com") { return "google" }
        if providerIds.contains("password") { return "password" }
        return "password"
    }

    private func deriveDisplayName(
        displayName: String?,
        email: String?,
        overrideName: String? = nil
    ) -> String {
        if let overrideTrimmed = overrideName?.trimmingCharacters(in: .whitespacesAndNewlines),
           !overrideTrimmed.isEmpty {
            return String(overrideTrimmed.prefix(200))
        }
        if let displayTrimmed = displayName?.trimmingCharacters(in: .whitespacesAndNewlines),
           !displayTrimmed.isEmpty {
            return String(displayTrimmed.prefix(200))
        }
        if let prefix = email?.split(separator: "@").first?.trimmingCharacters(in: .whitespacesAndNewlines),
           !prefix.isEmpty {
            return String(prefix.prefix(200))
        }
        return "moody User"
    }

    private func mapFirebaseUser(_ fbUser: FirebaseAuth.User, overrideName: String? = nil) -> MoodyUser {
        return MoodyUser(
            name: deriveDisplayName(
                displayName: fbUser.displayName,
                email: fbUser.email,
                overrideName: overrideName
            ),
            email: fbUser.email?.trimmingCharacters(in: .whitespacesAndNewlines) ?? "",
            avatar: fbUser.photoURL?.absoluteString ?? "",
            uid: fbUser.uid,
            isDemo: false,
            authProvider: resolveAuthProvider(fbUser)
        )
    }

    private func friendlyAuthError(_ error: Error) -> Error {
        let nsError = error as NSError
        guard nsError.domain == AuthErrorDomain,
              let code = AuthErrorCode(rawValue: nsError.code) else {
            return error
        }
        switch code {
        case .wrongPassword, .invalidCredential, .userNotFound:
            return MoodySyncError.message("Incorrect email or password.")
        case .emailAlreadyInUse:
            return MoodySyncError.message("An account with this email already exists.")
        case .invalidEmail:
            return MoodySyncError.message("Please enter a valid email address.")
        case .weakPassword:
            return MoodySyncError.message("Password must be at least 6 characters.")
        case .operationNotAllowed:
            return MoodySyncError.message("This sign-in method is not enabled yet in Firebase Authentication Console.")
        case .tooManyRequests:
            return MoodySyncError.message("Too many attempts. Please wait a moment and try again.")
        default:
            return error
        }
    }
    #endif

    private func upsertUserProfile(_ mapped: MoodyUser) async {
        #if canImport(FirebaseFirestore) && canImport(FirebaseCore)
        guard FirebaseApp.app() != nil else { return }
        let profilePayload: [String: Any] = [
            "uid": mapped.uid,
            "name": String(mapped.name.prefix(200)),
            "email": String(mapped.email.prefix(320)),
            "avatar": String(mapped.avatar.prefix(2048)),
            "updatedAt": FieldValue.serverTimestamp()
        ]
        try? await Firestore.firestore()
            .collection("users")
            .document(mapped.uid)
            .setData(profilePayload, merge: true)
        #endif
    }

    func startListeningToLogs(for uid: String) {
        guard isFirebaseConfigured, !uid.isEmpty else { return }
        #if canImport(FirebaseFirestore) && canImport(FirebaseCore)
        guard FirebaseApp.app() != nil else { return }
        stopListeningToLogs()
        let db = Firestore.firestore()
        logsListener = db.collection("users")
            .document(uid)
            .collection("logs")
            .order(by: "timestamp", descending: true)
            .addSnapshotListener { [weak self] snapshot, error in
                guard let self else { return }
                Task { @MainActor [weak self] in
                    guard let self else { return }
                    if let error {
                        self.syncError = error.localizedDescription
                        self.onStateChange?()
                        return
                    }
                    guard let snapshot else { return }
                    self.syncError = nil
                    self.currentLogs = snapshot.documents.map { doc in
                        let data = doc.data()
                        let rawIntensity = (data["intensity"] as? NSNumber)?.intValue ?? 5
                        return LogEntry(
                            id: (data["id"] as? String) ?? doc.documentID,
                            timestamp: (data["timestamp"] as? String) ?? IsoTimestamp.formatUtc(Date()),
                            core: (data["core"] as? String) ?? "Happy",
                            sub1: (data["sub1"] as? String) ?? "",
                            sub2: (data["sub2"] as? String) ?? "",
                            intensity: min(max(rawIntensity, 1), 10),
                            note: (data["note"] as? String) ?? "",
                            color: (data["color"] as? String) ?? "#5f27cd"
                        )
                    }
                    self.onStateChange?()
                }
            }
        #endif
    }

    func stopListeningToLogs() {
        #if canImport(FirebaseFirestore)
        logsListener?.remove()
        logsListener = nil
        #endif
    }

    private func loadRemoteThemePreference(uid: String) async {
        #if canImport(FirebaseFirestore) && canImport(FirebaseCore)
        guard FirebaseApp.app() != nil else { return }
        do {
            let doc = try await Firestore.firestore().collection("users").document(uid).getDocument()
            if let remoteDark = doc.data()?["darkTheme"] as? Bool {
                isDarkTheme = remoteDark
                defaults.set(remoteDark, forKey: PrefKey.themeDark)
                onStateChange?()
            }
        } catch {
            // Best-effort preference fetch
        }
        #endif
    }

    // MARK: - Actions

    func signInWithGoogle() async -> Result<MoodyUser, Error> {
        #if canImport(FirebaseAuth) && canImport(FirebaseCore) && canImport(GoogleSignIn)
        guard isFirebaseConfigured, FirebaseApp.app() != nil else {
            return .failure(MoodySyncError.message("Firebase is not configured."))
        }
        guard let clientID = FirebaseApp.app()?.options.clientID else {
            return .failure(MoodySyncError.message("Missing Firebase client ID in GoogleService-Info.plist."))
        }
        guard let presentingVC = topViewController() else {
            return .failure(MoodySyncError.message("Unable to find window to present Google Sign-In."))
        }

        do {
            GIDSignIn.sharedInstance.configuration = GIDConfiguration(clientID: clientID)
            let gidResult = try await GIDSignIn.sharedInstance.signIn(withPresenting: presentingVC)
            guard let idToken = gidResult.user.idToken?.tokenString else {
                return .failure(MoodySyncError.message("Google Sign-In did not return an ID token."))
            }
            let accessToken = gidResult.user.accessToken.tokenString
            let credential = GoogleAuthProvider.credential(withIDToken: idToken, accessToken: accessToken)
            let authResult = try await Auth.auth().signIn(with: credential)
            let mapped = mapFirebaseUser(authResult.user)
            currentUser = mapped
            syncError = nil
            onStateChange?()
            await upsertUserProfile(mapped)
            return .success(mapped)
        } catch let nsError as NSError {
            if nsError.domain == kGIDSignInErrorDomain, nsError.code == GIDSignInError.canceled.rawValue {
                return .failure(MoodySyncError.cancelled)
            }
            return .failure(friendlyAuthError(nsError))
        } catch {
            return .failure(friendlyAuthError(error))
        }
        #else
        return .failure(MoodySyncError.message("Add FirebaseAuth, FirebaseFirestore, and GoogleSignIn packages in Xcode to enable Google Sign-In."))
        #endif
    }

    func signInWithEmail(email: String, password: String) async -> Result<MoodyUser, Error> {
        #if canImport(FirebaseAuth) && canImport(FirebaseCore)
        guard isFirebaseConfigured, FirebaseApp.app() != nil else {
            return .failure(MoodySyncError.message("Firebase is not configured."))
        }
        do {
            let trimmedEmail = email.trimmingCharacters(in: .whitespacesAndNewlines)
            let authResult = try await Auth.auth().signIn(withEmail: trimmedEmail, password: password)
            let mapped = mapFirebaseUser(authResult.user)
            currentUser = mapped
            syncError = nil
            onStateChange?()
            await upsertUserProfile(mapped)
            return .success(mapped)
        } catch {
            return .failure(friendlyAuthError(error))
        }
        #else
        return .failure(MoodySyncError.message("Add FirebaseAuth and FirebaseFirestore packages in Xcode to enable Email/Password sign-in."))
        #endif
    }

    func signUpWithEmail(
        email: String,
        password: String,
        displayName: String
    ) async -> Result<MoodyUser, Error> {
        #if canImport(FirebaseAuth) && canImport(FirebaseCore)
        guard isFirebaseConfigured, FirebaseApp.app() != nil else {
            return .failure(MoodySyncError.message("Firebase is not configured."))
        }
        do {
            let trimmedEmail = email.trimmingCharacters(in: .whitespacesAndNewlines)
            let trimmedName = displayName.trimmingCharacters(in: .whitespacesAndNewlines)
            let authResult = try await Auth.auth().createUser(withEmail: trimmedEmail, password: password)
            if !trimmedName.isEmpty {
                let changeRequest = authResult.user.createProfileChangeRequest()
                changeRequest.displayName = trimmedName
                try? await changeRequest.commitChanges()
            }
            let mapped = mapFirebaseUser(
                authResult.user,
                overrideName: trimmedName.isEmpty ? nil : trimmedName
            )
            currentUser = mapped
            syncError = nil
            onStateChange?()
            await upsertUserProfile(mapped)
            return .success(mapped)
        } catch {
            return .failure(friendlyAuthError(error))
        }
        #else
        return .failure(MoodySyncError.message("Add FirebaseAuth and FirebaseFirestore packages in Xcode to enable Email/Password sign-up."))
        #endif
    }

    func sendPasswordReset(email: String) async -> Result<Void, Error> {
        #if canImport(FirebaseAuth) && canImport(FirebaseCore)
        guard isFirebaseConfigured, FirebaseApp.app() != nil else {
            return .failure(MoodySyncError.message("Firebase is not configured."))
        }
        do {
            let trimmedEmail = email.trimmingCharacters(in: .whitespacesAndNewlines)
            try await Auth.auth().sendPasswordReset(withEmail: trimmedEmail)
            return .success(())
        } catch {
            return .failure(friendlyAuthError(error))
        }
        #else
        return .failure(MoodySyncError.message("Add FirebaseAuth package in Xcode to enable password reset."))
        #endif
    }

    func signInWithApple(
        idToken: String,
        rawNonce: String,
        fullName: PersonNameComponents?
    ) async -> Result<MoodyUser, Error> {
        #if canImport(FirebaseAuth) && canImport(FirebaseCore)
        guard isFirebaseConfigured, FirebaseApp.app() != nil else {
            return .failure(MoodySyncError.message("Firebase is not configured."))
        }
        do {
            let credential = OAuthProvider.appleCredential(
                withIDToken: idToken,
                rawNonce: rawNonce,
                fullName: fullName
            )
            let authResult = try await Auth.auth().signIn(with: credential)
            let formattedFullName = fullName.map {
                PersonNameComponentsFormatter.localizedString(from: $0, style: .default)
            }
            let mapped = mapFirebaseUser(authResult.user, overrideName: formattedFullName)
            currentUser = mapped
            syncError = nil
            onStateChange?()
            await upsertUserProfile(mapped)
            return .success(mapped)
        } catch {
            return .failure(friendlyAuthError(error))
        }
        #else
        return .failure(MoodySyncError.message("Add FirebaseAuth and FirebaseFirestore packages in Xcode to enable Sign in with Apple."))
        #endif
    }

    func saveLog(_ log: LogEntry) async {
        if let activeUser = currentUser,
           !activeUser.isDemo,
           !activeUser.uid.isEmpty,
           isFirebaseConfigured {
            #if canImport(FirebaseFirestore) && canImport(FirebaseCore)
            if FirebaseApp.app() != nil {
                do {
                    let payload: [String: Any] = [
                        "id": String(log.id.prefix(128)),
                        "timestamp": log.timestamp,
                        "core": log.core,
                        "sub1": String(log.sub1.prefix(50)),
                        "sub2": String(log.sub2.prefix(50)),
                        "intensity": min(max(log.intensity, 1), 10),
                        "note": String(log.note.prefix(2000)),
                        "color": log.color,
                        "createdAt": FieldValue.serverTimestamp()
                    ]
                    try await Firestore.firestore()
                        .collection("users")
                        .document(activeUser.uid)
                        .collection("logs")
                        .document(log.id)
                        .setData(payload)
                    syncError = nil
                    onStateChange?()
                } catch {
                    syncError = error.localizedDescription
                    onStateChange?()
                }
                return
            }
            #endif
        }

        let updated = ([log] + currentLogs).sorted { $0.timestamp > $1.timestamp }
        currentLogs = updated
        saveLogsToDefaults(updated, email: currentUser?.email)
        onStateChange?()
    }

    func resetLogs() async {
        if let activeUser = currentUser,
           !activeUser.isDemo,
           !activeUser.uid.isEmpty,
           isFirebaseConfigured {
            #if canImport(FirebaseFirestore) && canImport(FirebaseCore)
            if FirebaseApp.app() != nil {
                do {
                    let db = Firestore.firestore()
                    let snapshot = try await db.collection("users")
                        .document(activeUser.uid)
                        .collection("logs")
                        .getDocuments()
                    let docs = snapshot.documents
                    for start in stride(from: 0, to: docs.count, by: 450) {
                        let end = min(start + 450, docs.count)
                        let batch = db.batch()
                        for doc in docs[start..<end] {
                            batch.deleteDocument(doc.reference)
                        }
                        try await batch.commit()
                    }
                    currentLogs = []
                    syncError = nil
                    onStateChange?()
                } catch {
                    syncError = error.localizedDescription
                    onStateChange?()
                }
                return
            }
            #endif
        }

        currentLogs = []
        saveLogsToDefaults([], email: currentUser?.email)
        onStateChange?()
    }

    func loginDemo(_ user: MoodyUser) async {
        let demoUser = MoodyUser(
            name: user.name,
            email: user.email,
            avatar: user.avatar,
            uid: user.uid,
            isDemo: true
        )
        currentUser = demoUser
        if let encoded = try? encoder.encode(demoUser) {
            defaults.set(encoded, forKey: PrefKey.demoUser)
        }
        currentLogs = loadLogsForUser(email: demoUser.email)
        syncError = nil
        onStateChange?()
    }

    func logout() async {
        stopListeningToLogs()
        #if canImport(FirebaseAuth) && canImport(FirebaseCore)
        if FirebaseApp.app() != nil {
            try? Auth.auth().signOut()
        }
        #endif
        #if canImport(GoogleSignIn)
        GIDSignIn.sharedInstance.signOut()
        #endif
        currentUser = nil
        currentLogs = []
        syncError = nil
        defaults.removeObject(forKey: PrefKey.demoUser)
        onStateChange?()
    }

    func setDarkTheme(_ enabled: Bool) async {
        isDarkTheme = enabled
        defaults.set(enabled, forKey: PrefKey.themeDark)
        onStateChange?()

        if let activeUser = currentUser,
           !activeUser.isDemo,
           !activeUser.uid.isEmpty,
           isFirebaseConfigured {
            #if canImport(FirebaseFirestore) && canImport(FirebaseCore)
            if FirebaseApp.app() != nil {
                try? await Firestore.firestore()
                    .collection("users")
                    .document(activeUser.uid)
                    .setData([
                        "darkTheme": enabled,
                        "updatedAt": FieldValue.serverTimestamp()
                    ], merge: true)
            }
            #endif
        }
    }

    func updateReminderSettings(_ settings: ReminderSettings) async {
        let normalized = settings.normalized()
        reminderSettings = normalized
        defaults.set(normalized.enabled, forKey: PrefKey.remindersEnabled)
        defaults.set(normalized.intervalMinutes, forKey: PrefKey.remindersIntervalMinutes)
        defaults.set(normalized.quietHoursEnabled, forKey: PrefKey.remindersQuietHours)
        onStateChange?()

        await MoodReminderScheduler.syncSchedule(settings: normalized)
    }

    // MARK: - Local Demo Storage

    private func loadLocalDemoSession() {
        guard let data = defaults.data(forKey: PrefKey.demoUser),
              let saved = try? decoder.decode(MoodyUser.self, from: data) else {
            return
        }
        let demoUser = MoodyUser(
            name: saved.name,
            email: saved.email,
            avatar: saved.avatar,
            uid: saved.uid,
            isDemo: true
        )
        currentUser = demoUser
        currentLogs = loadLogsForUser(email: demoUser.email)
    }

    private func logsKey(for email: String?) -> String {
        let normalized = email?.trimmingCharacters(in: .whitespacesAndNewlines).lowercased() ?? ""
        return normalized.isEmpty ? "logs_default" : "logs_\(normalized)"
    }

    private func loadLogsForUser(email: String) -> [LogEntry] {
        let key = logsKey(for: email)
        if let data = defaults.data(forKey: key),
           let decoded = try? decoder.decode([LogEntry].self, from: data) {
            return decoded.sorted { $0.timestamp > $1.timestamp }
        }
        let seeded = makeInitialMockLogs()
        saveLogsToDefaults(seeded, email: email)
        return seeded
    }

    private func saveLogsToDefaults(_ entries: [LogEntry], email: String?) {
        if let data = try? encoder.encode(entries) {
            defaults.set(data, forKey: logsKey(for: email))
        }
    }

    private func makeInitialMockLogs() -> [LogEntry] {
        let now = Date()
        let calendar = Calendar.current
        let specs: [(core: String, sub1: String, sub2: String, color: String, intensity: Int, hour: Int, minute: Int, note: String)] = [
            ("Happy", "Content", "Joyful", "#f2a380", 7, 10, 15, "Had a wonderful lunch with friends today."),
            ("Surprised", "Amazed", "Awe", "#e6af73", 8, 14, 30, "Beautiful sunset on my evening walk."),
            ("Bad", "Tired", "Sleepy", "#70c292", 6, 11, 0, "Did not sleep well last night, feeling slow."),
            ("Happy", "Proud", "Confident", "#f2a380", 9, 16, 45, "Finished a major milestone at work!"),
            ("Sad", "Lonely", "Isolated", "#7da5d9", 4, 9, 20, "Quiet evening indoors reflecting on the week.")
        ]

        var entries: [LogEntry] = []
        for (i, spec) in specs.enumerated() {
            var date = calendar.date(byAdding: .day, value: -i, to: now) ?? now
            var comps = calendar.dateComponents([.year, .month, .day], from: date)
            comps.hour = spec.hour
            comps.minute = spec.minute
            comps.second = 0
            if let composed = calendar.date(from: comps) {
                date = composed > now ? now.addingTimeInterval(TimeInterval(-i * 3600)) : composed
            }

            entries.append(
                LogEntry(
                    id: "mock-\(i)",
                    timestamp: IsoTimestamp.formatUtc(date),
                    core: spec.core,
                    sub1: spec.sub1,
                    sub2: spec.sub2,
                    intensity: spec.intensity,
                    note: spec.note,
                    color: spec.color
                )
            )
        }
        return entries.sorted { $0.timestamp > $1.timestamp }
    }

    #if canImport(GoogleSignIn)
    private func topViewController() -> UIViewController? {
        let root = UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .flatMap(\.windows)
            .first(where: \.isKeyWindow)?
            .rootViewController
        var current = root
        while let presented = current?.presentedViewController {
            current = presented
        }
        return current
    }
    #endif
}
