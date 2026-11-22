import Foundation

#if canImport(FirebaseCore)
import FirebaseCore
#endif

#if canImport(GoogleSignIn)
import GoogleSignIn
#endif

enum FirebaseBootstrap {
    private(set) static var isConfigured: Bool = false

    static func configureIfAvailable() {
        #if canImport(FirebaseCore)
        guard !isConfigured else { return }
        if FirebaseApp.app() != nil {
            isConfigured = true
            return
        }
        guard Bundle.main.url(forResource: "GoogleService-Info", withExtension: "plist") != nil else {
            isConfigured = false
            return
        }
        FirebaseApp.configure()
        isConfigured = true
        #else
        isConfigured = false
        #endif
    }

    @discardableResult
    static func handleOpenURL(_ url: URL) -> Bool {
        #if canImport(GoogleSignIn)
        return GIDSignIn.sharedInstance.handle(url)
        #else
        return false
        #endif
    }
}
