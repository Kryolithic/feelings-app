import AuthenticationServices
import SwiftUI

private let demoAccounts: [MoodyUser] = [
    MoodyUser(name: "Gary", email: "gary@gmail.com", uid: "demo-gary", isDemo: true, authProvider: "demo"),
    MoodyUser(name: "Developer User", email: "developer@feelings.app", uid: "demo-dev", isDemo: true, authProvider: "demo")
]

struct LoginView: View {
    @Environment(MoodyStore.self) private var store

    @State private var showChooser = false
    @State private var isSigningIn = false
    @State private var authError: String?
    @State private var authNotice: String?

    @State private var isSignUpMode = false
    @State private var displayName = ""
    @State private var email = ""
    @State private var password = ""
    @State private var showPassword = false
    @State private var currentAppleNonce: String?

    var body: some View {
        ScrollView {
            VStack(spacing: 0) {
                VStack(spacing: 14) {
                    Image(store.isDarkTheme ? "MoodyLogoDark" : "MoodyLogoLight")
                        .resizable()
                        .scaledToFit()
                        .frame(width: 180)
                        .accessibilityLabel("moody")

                    Text("Welcome to your space of calm. Take a moment to check in with yourself.")
                        .font(.system(size: 14))
                        .foregroundStyle(.secondary)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, 16)
                }
                .padding(.top, 20)
                .padding(.bottom, 26)

                Picker("Authentication Mode", selection: $isSignUpMode) {
                    Text("Log In").tag(false)
                    Text("Create Account").tag(true)
                }
                .pickerStyle(.segmented)
                .disabled(isSigningIn)
                .onChange(of: isSignUpMode) { _, _ in
                    authError = nil
                    authNotice = nil
                }
                .padding(.bottom, 16)

                VStack(spacing: 12) {
                    if isSignUpMode {
                        TextField("Name (optional)", text: $displayName)
                            .textContentType(.name)
                            .padding(14)
                            .background(Color(.secondarySystemBackground))
                            .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
                    }

                    TextField("Email address", text: $email)
                        .textContentType(.emailAddress)
                        .keyboardType(.emailAddress)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                        .padding(14)
                        .background(Color(.secondarySystemBackground))
                        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))

                    HStack {
                        Group {
                            if showPassword {
                                TextField("Password (6+ characters)", text: $password)
                            } else {
                                SecureField("Password (6+ characters)", text: $password)
                            }
                        }
                        .textContentType(isSignUpMode ? .newPassword : .password)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()

                        Button {
                            showPassword.toggle()
                        } label: {
                            Image(systemName: showPassword ? "eye.slash" : "eye")
                                .foregroundStyle(.secondary)
                        }
                        .accessibilityLabel(showPassword ? "Hide password" : "Show password")
                    }
                    .padding(14)
                    .background(Color(.secondarySystemBackground))
                    .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))

                    if !isSignUpMode && store.isFirebaseConfigured {
                        HStack {
                            Spacer()
                            Button("Forgot password?") {
                                handlePasswordReset()
                            }
                            .font(.system(size: 12, weight: .medium))
                            .disabled(isSigningIn)
                        }
                    }

                    Button {
                        handleEmailSubmit()
                    } label: {
                        HStack(spacing: 10) {
                            if isSigningIn {
                                ProgressView()
                                    .tint(Color(.systemBackground))
                            }
                            Text(
                                isSigningIn ? "Please wait..." :
                                isSignUpMode ? "Create Account" : "Sign In"
                            )
                            .font(.system(size: 16, weight: .semibold))
                        }
                        .frame(maxWidth: .infinity)
                        .frame(height: 52)
                        .foregroundStyle(Color(.systemBackground))
                        .background(Color.primary)
                        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                    }
                    .disabled(isSigningIn)
                    .padding(.top, 4)
                }

                HStack(spacing: 12) {
                    Rectangle()
                        .fill(Color.secondary.opacity(0.25))
                        .frame(height: 1)
                    Text("or")
                        .font(.system(size: 12))
                        .foregroundStyle(.secondary)
                    Rectangle()
                        .fill(Color.secondary.opacity(0.25))
                        .frame(height: 1)
                }
                .padding(.vertical, 18)

                VStack(spacing: 12) {
                    SignInWithAppleButton(.signIn) { request in
                        authError = nil
                        authNotice = nil
                        let nonce = AppleSignInNonce.random()
                        currentAppleNonce = nonce
                        request.requestedScopes = [.fullName, .email]
                        request.nonce = AppleSignInNonce.sha256(nonce)
                    } onCompletion: { result in
                        handleAppleCompletion(result)
                    }
                    .signInWithAppleButtonStyle(store.isDarkTheme ? .white : .black)
                    .frame(height: 52)
                    .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                    .disabled(isSigningIn)

                    Button {
                        authError = nil
                        authNotice = nil
                        if store.isFirebaseConfigured {
                            isSigningIn = true
                            Task {
                                let result = await store.signInWithGoogle()
                                await MainActor.run {
                                    isSigningIn = false
                                    if case .failure(let error) = result {
                                        if case MoodySyncError.cancelled = error { return }
                                        authError = error.localizedDescription
                                    }
                                }
                            }
                        } else {
                            showChooser = true
                        }
                    } label: {
                        Text(store.isFirebaseConfigured ? "Continue with Google" : "Choose Demo Account")
                            .font(.system(size: 15, weight: .medium))
                            .frame(maxWidth: .infinity)
                            .frame(height: 52)
                            .foregroundStyle(.primary)
                            .overlay(
                                RoundedRectangle(cornerRadius: 16, style: .continuous)
                                    .stroke(Color.secondary.opacity(0.35), lineWidth: 1)
                            )
                    }
                    .disabled(isSigningIn)

                    if let authNotice, !authNotice.isEmpty {
                        Text(authNotice)
                            .font(.system(size: 13))
                            .foregroundStyle(Color.accentColor)
                            .multilineTextAlignment(.center)
                    }

                    if let authError, !authError.isEmpty {
                        Text(authError)
                            .font(.system(size: 13))
                            .foregroundStyle(.red)
                            .multilineTextAlignment(.center)
                    }

                    if !store.isFirebaseConfigured {
                        Text("Demo Mode — add Firebase & GoogleSignIn SPM packages in Xcode to enable cloud auth & Firestore sync")
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
            }
            .padding(24)
        }
        .scrollDismissesKeyboard(.interactively)
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

    private func handleEmailSubmit() {
        authError = nil
        authNotice = nil

        let trimmedEmail = email.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmedEmail.isEmpty, trimmedEmail.contains("@") else {
            authError = "Please enter a valid email address."
            return
        }
        guard password.count >= 6 else {
            authError = "Password must be at least 6 characters."
            return
        }

        if !store.isFirebaseConfigured {
            let trimmedName = displayName.trimmingCharacters(in: .whitespacesAndNewlines)
            let derivedName = trimmedName.isEmpty
                ? String(trimmedEmail.split(separator: "@").first ?? "moody User")
                : trimmedName
            Task {
                await store.loginDemo(
                    MoodyUser(
                        name: derivedName,
                        email: trimmedEmail,
                        uid: "demo-\(trimmedEmail.lowercased())",
                        isDemo: true,
                        authProvider: "demo"
                    )
                )
            }
            return
        }

        isSigningIn = true
        let trimmedName = displayName.trimmingCharacters(in: .whitespacesAndNewlines)
        Task {
            let result = isSignUpMode
                ? await store.signUpWithEmail(email: trimmedEmail, password: password, displayName: trimmedName)
                : await store.signInWithEmail(email: trimmedEmail, password: password)

            await MainActor.run {
                isSigningIn = false
                if case .failure(let error) = result {
                    authError = error.localizedDescription
                }
            }
        }
    }

    private func handlePasswordReset() {
        authError = nil
        authNotice = nil
        let trimmedEmail = email.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmedEmail.isEmpty, trimmedEmail.contains("@") else {
            authError = "Enter your email address first to reset your password."
            return
        }

        isSigningIn = true
        Task {
            let result = await store.sendPasswordReset(email: trimmedEmail)
            await MainActor.run {
                isSigningIn = false
                switch result {
                case .success:
                    authNotice = "Password reset email sent to \(trimmedEmail)."
                case .failure(let error):
                    authError = error.localizedDescription
                }
            }
        }
    }

    private func handleAppleCompletion(_ result: Result<ASAuthorization, Error>) {
        switch result {
        case .failure(let error):
            if let authErr = error as? ASAuthorizationError, authErr.code == .canceled {
                return
            }
            authError = error.localizedDescription

        case .success(let authorization):
            guard let appleCredential = authorization.credential as? ASAuthorizationAppleIDCredential else {
                authError = "Unsupported Apple credential returned."
                return
            }

            if !store.isFirebaseConfigured {
                let appleEmail = appleCredential.email ?? "apple-demo@privaterelay.appleid.com"
                let fullName = appleCredential.fullName.map {
                    PersonNameComponentsFormatter.localizedString(from: $0, style: .default)
                }?.trimmingCharacters(in: .whitespacesAndNewlines)
                let fallbackName = (fullName?.isEmpty == false ? fullName : nil) ?? "Apple Demo User"
                Task {
                    await store.loginDemo(
                        MoodyUser(
                            name: fallbackName,
                            email: appleEmail,
                            uid: "demo-apple-\(appleCredential.user)",
                            isDemo: true,
                            authProvider: "demo"
                        )
                    )
                }
                return
            }

            guard let nonce = currentAppleNonce else {
                authError = "Invalid sign-in state: missing Apple authorization nonce."
                return
            }
            guard let idTokenData = appleCredential.identityToken,
                  let idToken = String(data: idTokenData, encoding: .utf8) else {
                authError = "Unable to fetch Apple identity token."
                return
            }

            isSigningIn = true
            Task {
                let res = await store.signInWithApple(
                    idToken: idToken,
                    rawNonce: nonce,
                    fullName: appleCredential.fullName
                )
                await MainActor.run {
                    isSigningIn = false
                    currentAppleNonce = nil
                    if case .failure(let error) = res {
                        authError = error.localizedDescription
                    }
                }
            }
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
                                isDemo: true,
                                authProvider: "demo"
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
