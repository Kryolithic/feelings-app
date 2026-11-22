import SwiftUI

private let demoAccounts: [MoodyUser] = [
    MoodyUser(name: "Gary", email: "gary@gmail.com", uid: "demo-gary", isDemo: true),
    MoodyUser(name: "Developer User", email: "developer@feelings.app", uid: "demo-dev", isDemo: true)
]

struct LoginView: View {
    @Environment(MoodyStore.self) private var store

    @State private var showChooser = false
    @State private var isSigningIn = false
    @State private var authError: String?

    var body: some View {
        ZStack {
            VStack(spacing: 0) {
                Spacer()

                VStack(spacing: 16) {
                    Image(store.isDarkTheme ? "MoodyLogoDark" : "MoodyLogoLight")
                        .resizable()
                        .scaledToFit()
                        .frame(width: 200)
                        .accessibilityLabel("moody")

                    Text("Welcome to your space of calm. Take a moment to check in with yourself.")
                        .font(.system(size: 15))
                        .foregroundStyle(.secondary)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, 24)
                }
                .padding(.bottom, 48)

                VStack(spacing: 14) {
                    Button {
                        authError = nil
                        if store.isFirebaseConfigured {
                            isSigningIn = true
                            Task {
                                let result = await store.signInWithGoogle()
                                await MainActor.run {
                                    isSigningIn = false
                                    if case .failure(let error) = result {
                                        if case MoodySyncError.cancelled = error {
                                            return
                                        }
                                        authError = error.localizedDescription
                                    }
                                }
                            }
                        } else {
                            showChooser = true
                        }
                    } label: {
                        HStack(spacing: 10) {
                            if isSigningIn {
                                ProgressView()
                                    .tint(Color(.systemBackground))
                            }
                            Text(isSigningIn ? "Signing in..." : "Sign In")
                                .font(.system(size: 16, weight: .semibold))
                        }
                        .frame(maxWidth: .infinity)
                        .frame(height: 54)
                        .foregroundStyle(Color(.systemBackground))
                        .background(Color.primary)
                        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                    }
                    .disabled(isSigningIn)

                    if let authError, !authError.isEmpty {
                        Text(authError)
                            .font(.system(size: 13))
                            .foregroundStyle(.red)
                            .multilineTextAlignment(.center)
                    }

                    if !store.isFirebaseConfigured {
                        Text("Demo Mode — add ios/Moody/GoogleService-Info.plist and Firebase SPM packages in Xcode to enable Google Sign-In & Firestore Cloud Sync")
                            .font(.system(size: 12))
                            .foregroundStyle(.secondary)
                            .multilineTextAlignment(.center)
                            .padding(.horizontal, 12)
                            .padding(.vertical, 10)
                            .frame(maxWidth: .infinity)
                            .overlay(
                                RoundedRectangle(cornerRadius: 12, style: .continuous)
                                    .stroke(Color.secondary.opacity(0.3), lineWidth: 1)
                            )
                    }
                }

                Spacer()
            }
            .padding(24)
        }
        .sheet(isPresented: $showChooser) {
            DemoAccountChooserSheet(
                onSelect: { selected in
                    showChooser = false
                    Task {
                        await store.loginDemo(selected)
                    }
                }
            )
            .presentationDetents([.medium])
        }
    }
}

private struct DemoAccountChooserSheet: View {
    @Environment(\.dismiss) private var dismiss
    let onSelect: (MoodyUser) -> Void

    var body: some View {
        NavigationStack {
            VStack(spacing: 16) {
                VStack(spacing: 4) {
                    Text("Choose an account")
                        .font(.system(size: 20, weight: .semibold))
                    Text("to continue to moody")
                        .font(.system(size: 14))
                        .foregroundStyle(.secondary)
                }
                .padding(.top, 8)

                VStack(spacing: 12) {
                    ForEach(demoAccounts, id: \.uid) { account in
                        Button {
                            onSelect(account)
                        } label: {
                            HStack(spacing: 14) {
                                ZStack {
                                    Circle()
                                        .fill(Color.accentColor.opacity(0.18))
                                        .frame(width: 40, height: 40)
                                    Text(String(account.name.prefix(1)))
                                        .font(.system(size: 16, weight: .bold))
                                        .foregroundStyle(Color.accentColor)
                                }

                                VStack(alignment: .leading, spacing: 2) {
                                    Text(account.name)
                                        .font(.system(size: 15, weight: .medium))
                                        .foregroundStyle(.primary)
                                    Text(account.email)
                                        .font(.system(size: 12))
                                        .foregroundStyle(.secondary)
                                }
                                Spacer()
                            }
                            .padding(12)
                            .background(Color(.secondarySystemBackground))
                            .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                        }
                        .buttonStyle(.plain)
                    }

                    Button("Use guest account") {
                        onSelect(
                            MoodyUser(
                                name: "Guest User",
                                email: "guest@feelings.app",
                                uid: "demo-guest",
                                isDemo: true
                            )
                        )
                    }
                    .font(.system(size: 14, weight: .medium))
                    .padding(.top, 6)
                }

                Spacer()

                Text("Local demo mode stores entries on your device. Add GoogleService-Info.plist and Firebase SDKs in Xcode for cloud sync.")
                    .font(.system(size: 11))
                    .foregroundStyle(.secondary)
                    .multilineTextAlignment(.center)
            }
            .padding(24)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
            }
        }
    }
}
