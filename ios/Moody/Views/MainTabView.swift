import SwiftUI

struct MainTabView: View {
    @Environment(MoodyStore.self) private var store
    @Environment(\.scenePhase) private var scenePhase

    @State private var selectedTab: String = "home"

    var body: some View {
        Group {
            if store.user == nil {
                LoginView()
            } else {
                VStack(spacing: 0) {
                    if let syncError = store.syncError, !syncError.isEmpty {
                        Text("Sync warning: \(syncError)")
                            .font(.system(size: 12))
                            .foregroundStyle(.red)
                            .multilineTextAlignment(.center)
                            .frame(maxWidth: .infinity)
                            .padding(.horizontal, 16)
                            .padding(.vertical, 8)
                            .background(Color.red.opacity(0.12))
                    }

                    TabView(selection: $selectedTab) {
                        LoggingWizardView()
                            .tabItem {
                                Label("Home", systemImage: "house.fill")
                            }
                            .tag("home")

                        TrendsView()
                            .tabItem {
                                Label("Trends", systemImage: "star.fill")
                            }
                            .tag("trends")

                        ProfileView()
                            .tabItem {
                                Label("Profile", systemImage: "person.fill")
                            }
                            .tag("profile")
                    }
                }
                .task(id: store.user?.uid) {
                    store.syncLogsListener(for: store.user?.uid)
                }
                .onChange(of: store.user?.uid) { _, newValue in
                    if newValue == nil {
                        selectedTab = "home"
                    }
                }
                .onChange(of: scenePhase) { _, phase in
                    if phase == .active {
                        Task {
                            await store.refreshReminderScheduleIfNeeded()
                        }
                    }
                }
            }
        }
    }
}
