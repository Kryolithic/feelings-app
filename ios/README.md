# moody — Native iOS App (SwiftUI)

Native iOS port of the `moody` Android app (`com.example.feelingsapp`), sharing the same Plutchik/Feelings Wheel taxonomy, Google Sign-In + Cloud Firestore schema (`/users/{uid}` and `/users/{uid}/logs/{logId}`), light/dark themes, and local check-in reminder notifications.

> **Why isn't an `.ipa` prebuilt here?**
> Apple's iOS SDK and `xcodebuild` only run on macOS with Xcode installed, so the iOS app cannot be compiled on Windows. All Swift source files, assets, Firebase config, and unit tests are ready to build on a Mac.

---

## Building & Running on macOS

### Option A: Using XcodeGen (Fastest, Recommended)

1. Install [XcodeGen](https://github.com/yonaskolb/XcodeGen) if you don't already have it:
   ```bash
   brew install xcodegen
   ```
2. Generate `Moody.xcodeproj` and open it in Xcode:
   ```bash
   cd ios
   xcodegen generate
   open Moody.xcodeproj
   ```
3. In Xcode:
   - Wait for Swift Package Manager to finish resolving `firebase-ios-sdk` (`>= 13.2.0`) and `GoogleSignIn-iOS` (`>= 10.1.0`).
   - Select your Apple Developer Team under **Moody target → Signing & Capabilities** if running on a physical iPhone (or run directly on an iOS 17+ Simulator).
   - Press **⌘R** to build and run, or **⌘U** to run `MoodyTests`.

### Option B: Creating the Xcode Project Manually

If you prefer not to install XcodeGen:

1. In Xcode, choose **File → New → Project… → iOS → App**:
   - **Product Name:** `Moody`
   - **Organization Identifier:** `com.example`
   - **Bundle Identifier:** `com.example.feelingsapp`
   - **Interface:** SwiftUI · **Language:** Swift · **Storage:** None
2. Move or copy the existing contents of `ios/Moody/` (all folders, `Assets.xcassets`, `Info.plist`, and `GoogleService-Info.plist`) and `ios/MoodyTests/` into your project folder (replacing the template placeholder files). Because modern Xcode projects use folder synchronization, source files and assets appear automatically.
3. Add Swift Package Dependencies via **Project → Package Dependencies**:
   - `https://github.com/firebase/firebase-ios-sdk` (`13.2.0+`): link `FirebaseCore`, `FirebaseAuth`, and `FirebaseFirestore` to the `Moody` target.
   - `https://github.com/google/GoogleSignIn-iOS` (`10.1.0+`): link `GoogleSignIn` to the `Moody` target.
4. Confirm in **Moody target → Build Settings**:
   - **Other Linker Flags (`OTHER_LDFLAGS`)** includes `-ObjC` (required for Firebase Auth / Firestore Objective-C categories).
   - **Info.plist File (`INFOPLIST_FILE`)** points to `Moody/Info.plist` (which already contains `CFBundleDisplayName = moody`, `GIDClientID`, and the `REVERSED_CLIENT_ID` URL scheme for Google Sign-In).
   - **Minimum Deployments:** `iOS 17.0`.

---

## Firebase & Auth Setup

* `ios/Moody/GoogleService-Info.plist` is registered for **Firebase App ID** `1:803615896975:ios:b6952e7704faab263307fa` (`com.example.feelingsapp`) in project `feelings-app-19030`.
* In [Firebase Authentication Console → Sign-in method](https://console.firebase.google.com/project/feelings-app-19030/authentication/providers), make sure **Email/Password** and **Apple** (alongside **Google**) are enabled.
* When building in Xcode for a physical iPhone, ensure the **Sign in with Apple** capability is enabled under **Signing & Capabilities** (matching `ios/Moody/Moody.entitlements`).
* Without Firebase SPM packages linked or without `GoogleService-Info.plist`, the app automatically runs in **Local Demo Mode** (`UserDefaults` storage + seeded sample logs).
* Check-in reminders use `UNUserNotificationCenter` (`UserNotifications`). Quiet hours (`10 PM – 8 AM`) pre-schedules up to 48 calendar-triggered reminders (within iOS's 64-pending-notification limit) and tops up the schedule whenever the app becomes active.
