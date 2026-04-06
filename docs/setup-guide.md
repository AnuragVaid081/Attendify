# Attendify — Setup Guide

## Prerequisites

| Tool | Version | Purpose |
|---|---|---|
| Android Studio | Hedgehog+ | Android development |
| Xcode | 15+ | iOS development (macOS only) |
| JDK | 17 | Kotlin compilation |
| Node.js | 20 | Cloud Functions |
| Firebase CLI | Latest | Deploy functions & rules |
| Kotlin Multiplatform Mobile plugin | Latest | KMP support in AS |

---

## Step 1: Firebase Project Setup

1. Go to [Firebase Console](https://console.firebase.google.com)
2. Click **Add project** → Name it "Attendify"
3. Enable **Google Analytics** (optional)

### Enable Services:
- **Authentication** → Sign-in method → Email/Password → Enable
- **Firestore Database** → Create in production mode
- **Cloud Messaging** → Already enabled when project is created
- **Cloud Functions** → Requires Blaze (pay-as-you-go) plan for scheduled functions

### Download Config Files:
- **Android**: Project Settings → Your apps → Download `google-services.json`
  - Place at: `AndroidApp/google-services.json`
- **iOS**: Project Settings → Your apps → Download `GoogleService-Info.plist`
  - Place at: `iosApp/Attendify/GoogleService-Info.plist`

---

## Step 2: Deploy Firestore Rules & Indexes

```bash
# Install Firebase CLI if not installed
npm install -g firebase-tools

# Login
firebase login

# Set project
firebase use --add
# Select your Attendify project

# Deploy rules
firebase deploy --only firestore:rules

# Deploy indexes (required before first query)
firebase deploy --only firestore:indexes
```

---

## Step 3: Deploy Cloud Functions

```bash
cd functions

# Install dependencies
npm install

# Build TypeScript
npm run build

# Deploy to Firebase
npm run deploy
```

> **Note**: Scheduled functions (`autoLockExpiredSessions`) require the **Blaze plan**.
> You can test all functions locally with the emulator:
> ```bash
> firebase emulators:start
> ```

---

## Step 4: Seed Initial Data

Use the Firebase Console or a seed script to create initial documents.

### Create a Principal user
In **Firestore Console** → `users` collection → New document:
```json
{
  "id": "YOUR_AUTH_UID",
  "name": "Principal Name",
  "email": "principal@college.edu",
  "role": "PRINCIPAL",
  "departmentId": "",
  "classId": "",
  "qrToken": "PRINCIPAL-QR-TOKEN",
  "fcmToken": "",
  "isActive": true
}
```

Then create the same user in **Authentication** → Users → Add user.

### 2. Create departments
```json
{
  "id": "dept_cse",
  "name": "Computer Science & Engineering",
  "code": "CSE",
  "hodId": ""
}
```

### 3. Create classes, subjects, timetable entries
Follow the schema in `docs/firestore-schema.md`.

> **Tip**: Generate unique `qrToken` values using UUID v4 for every user.

---

## Step 5: Build Android App

```bash
# From repo root
./gradlew :AndroidApp:assembleDebug

# Install on connected device/emulator
./gradlew :AndroidApp:installDebug
```

Or open the root project in **Android Studio** and run `AndroidApp`.

---

## Step 6: Build KMP Shared Module (Verify)

```bash
./gradlew :shared:build
```

---

## Step 7: Build iOS App

> **Requires macOS + Xcode**

```bash
# Generate XCFramework from KMP shared module
./gradlew :shared:assembleXCFramework

# Open iOS project in Xcode
open iosApp/Attendify.xcodeproj
```

1. Add the generated XCFramework (`shared/build/XCFrameworks/release/Shared.xcframework`) to the Xcode project target
2. Add `GoogleService-Info.plist` to the target
3. Add Camera permission in `Info.plist`:
   ```
   Privacy - Camera Usage Description → "Used to scan student QR codes for attendance"
   ```
4. Run on simulator or device

---

## Step 8: Generate QR Tokens for Users

QR tokens must be unique, non-guessable strings. Use this pattern:

```kotlin
import java.util.UUID
fun generateQrToken(userId: String): String = "${userId}_${UUID.randomUUID()}"
```

Store this token in the user's Firestore document as `qrToken`.

---

## Testing with Firebase Emulator Suite

```bash
# Start all emulators
firebase emulators:start

# Android: add to AndroidApp/src/debug/java/.../DebugSetup.kt
Firebase.firestore.useEmulator("10.0.2.2", 8080)
Firebase.auth.useEmulator("10.0.2.2", 9099)
```

---

## Security Checklist

- [ ] `google-services.json` and `GoogleService-Info.plist` are in `.gitignore`
- [ ] Firestore rules deployed and tested with Firebase rules simulator
- [ ] All user QR tokens are UUID-based and stored only server-side
- [ ] Attendance records have no `update` permission (immutable)
- [ ] Cloud Functions validate role on every operation
- [ ] Session auto-lock configured (90-minute scheduled function)

---

## Architecture Quick Reference

```
┌─────────────────────┐    ┌─────────────────────┐
│   Android (Compose) │    │    iOS (SwiftUI)     │
│  ┌───────────────┐  │    │  ┌───────────────┐   │
│  │  UI Screens   │  │    │  │  SwiftUI Views│   │
│  └──────┬────────┘  │    │  └──────┬────────┘   │
└─────────│───────────┘    └─────────│─────────────┘
          │  Koin DI                 │  KMPBridge
          ▼                          ▼
┌──────────────────────────────────────────────────┐
│             KMP Shared Module                    │
│  ViewModels → Use Cases → Repositories           │
│  (GitLive Firebase SDK — works on both platforms)│
└──────────────────────┬───────────────────────────┘
                       │
                       ▼
┌──────────────────────────────────────────────────┐
│               Firebase Backend                    │
│  Firestore  |  Auth  |  FCM  |  Cloud Functions  │
└──────────────────────────────────────────────────┘
```
