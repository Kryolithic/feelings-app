import SwiftUI
import UserNotifications

@main
struct MoodyApp: App {
    @State private var store: MoodyStore

    init() {
        FirebaseBootstrap.configureIfAvailable()
        UNUserNotificationCenter.current().delegate = MoodNotificationDelegate.shared
        _store = State(initialValue: MoodyStore())
    }

    var body: some Scene {
        WindowGroup {
            MainTabView()
                .environment(store)
                .preferredColorScheme(store.isDarkTheme ? .dark : .light)
                .onOpenURL { url in
                    FirebaseBootstrap.handleOpenURL(url)
                }
        }
    }
}
