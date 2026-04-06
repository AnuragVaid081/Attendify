# Attendify

**A production-ready, scalable attendance management system for educational institutions.**

Built with **Kotlin Multiplatform**, **Jetpack Compose**, **SwiftUI**, and **Firebase**.

---

## Features

- 🔐 **Role-Based Access** — Principal, HOD, Class Incharge, Teacher, Student, Class Representative
- 📱 **QR-Based Attendance** — Unique QR per student; teacher verification required before marking
- 📅 **Dynamic Timetable** — Fixed weekly schedule with day-specific adjustments (swap, cancel, reschedule)
- 🔔 **Real-Time Notifications** — FCM-powered alerts for schedule changes
- 📊 **Dashboards** — Role-aware dashboards with attendance analytics
- 🔒 **Strict Security** — Immutable attendance records, time-bound sessions, Firestore security rules

---

## Tech Stack

| Layer | Technology |
|---|---|
| Shared Logic | Kotlin Multiplatform (KMP) |
| Android UI | Jetpack Compose + Material 3 |
| iOS UI | SwiftUI |
| Backend | Firebase (Firestore, Auth, Cloud Functions, FCM) |
| DI | Koin |
| QR (Android) | ZXing + ML Kit |
| QR (iOS) | CoreImage + AVFoundation |

---

## Project Structure

```
Attendify/
├── shared/          # KMP: Models, Repositories, Use Cases, ViewModels
├── AndroidApp/      # Jetpack Compose Android app
├── iosApp/          # SwiftUI iOS app
├── functions/       # Firebase Cloud Functions (TypeScript)
├── docs/            # Schema, security rules docs, setup guide
├── firestore.rules  # Firestore security rules
└── firestore.indexes.json
```

---

## Quick Start

See [docs/setup-guide.md](docs/setup-guide.md) for full setup instructions.

---

## Security Architecture

- Attendance records are **immutable** (no update allowed via security rules)
- Sessions auto-lock after **90 minutes**
- Teachers can only mark attendance in **their own** sessions
- QR tokens are **server-side secrets** — never exposed in UI beyond the QR image
- All Cloud Functions enforce role validation independently of client rules

---

## License

MIT License — for educational use.
