# Security Architecture — Attendance Validation

## Threat Model & What Changed

### Before (Insecure)
```
Client → writes directly to Firestore:
  • client resolves teacher from qrToken (users collection query)
  • client sets lectureSessions/{id}.status = "ACTIVE"
  • client resolves student from qrToken
  • client creates attendance/{id} record
  • client increments lectureSessions/{id}.markedCount
```

Any attacker who:
- Decompiles the APK/IPA
- Replays network traffic
- Uses a rooted device with Frida/Xposed

...could forge any of these Firestore writes and mark arbitrary attendance without scanning a QR.

### After (Secure)

```
Client → sends only (sessionId, qrToken) to Cloud Function → TLS → Firebase Auth token
Cloud Function (Admin SDK) → validates everything → writes to Firestore
```

The client is **denied** all writes to `attendance` and all `update` operations on `lectureSessions` by Firestore security rules. The Admin SDK used by Cloud Functions bypasses these rules — it is the **only** permitted writer.

---

## Attack Vector Coverage

| Attack | Before | After |
|---|---|---|
| Client forges `status: "ACTIVE"` on session | ✅ Possible | ✗ Blocked by `allow update: if false` |
| Client writes arbitrary attendance record | ✅ Possible | ✗ Blocked by `allow create: if false` |
| Client sets arbitrary `studentId` in attendance | ✅ Possible | ✗ No client path to write |
| Client spoofs `markedCount` | ✅ Possible | ✗ Only `FieldValue.increment()` in Cloud Function |
| Client resolves student QR → studentId locally | ✅ Possible | ✗ QR resolution is server-only |
| Attacker replays QR token for duplicate mark | ✅ Possible | ✗ Atomic transaction in Cloud Function |
| Session activated without teacher QR | ✅ Possible (write directly) | ✗ Teacher QR must match session.teacherId server-side |
| Mark attendance after session locked | ✅ Possible | ✗ Cloud Function checks `status == "ACTIVE"` |
| Mark attendance past session end time | ✅ Possible | ✗ Timing guard in Cloud Function (+10 min grace) |
| Wrong teacher verifies session | ✅ Possible | ✗ Cloud Function checks `session.teacherId == teacherDoc.id` |
| CR bypasses teacher verification | ✅ Possible | ✗ QR of the *specific assigned teacher* is required |
| Forged `_verifiedByFunction` in session create | N/A | ✗ Security rule blocks it on create |

---

## Data Flow (Post-Refactor)

```
┌─────────────────────────────────────────────────────────────────────┐
│  TEACHER VERIFICATION FLOW                                          │
│                                                                     │
│  CR/Teacher scans Teacher QR                                        │
│       │                                                             │
│       ▼                                                             │
│  AttendanceViewModel.verifyTeacherQr(sessionId, qrToken)            │
│       │                                                             │
│       ▼                                                             │
│  FirebaseAttendanceRepository.verifySession()                       │
│    → functions.httpsCallable("validateAttendanceSession")           │
│    → payload: { sessionId, teacherQrToken }   (nothing else)        │
│       │                                                             │
│       │  HTTPS + Firebase Auth JWT                                  │
│       ▼                                                             │
│  Cloud Function: validateAttendanceSession                          │
│    1. Auth check (context.auth)                                     │
│    2. Load session → verify PENDING                                 │
│    3. Resolve teacher from qrToken (Admin SDK, server-only)         │
│    4. Verify caller is authorized for this session/class            │
│    5. Verify session.teacherId == resolved teacher doc ID           │
│    6. Verify session.date == today                                  │
│    7. Atomic transaction: re-read + set ACTIVE + provenance mark    │
│    8. Return { success, sessionId, activatedAt }                    │
│       │                                                             │
│       ▼                                                             │
│  Client reads updated session via observeSession() (Firestore Flow) │
└─────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────┐
│  STUDENT QR SCAN FLOW                                               │
│                                                                     │
│  Teacher/CR scans Student QR                                        │
│       │                                                             │
│       ▼                                                             │
│  AttendanceViewModel.markStudentAttendance(sessionId, qrToken, uid) │
│       │                                                             │
│       ▼                                                             │
│  FirebaseAttendanceRepository.markAttendance()                      │
│    → functions.httpsCallable("markStudentAttendance")               │
│    → payload: { sessionId, studentQrToken }   (nothing else)        │
│       │                                                             │
│       │  HTTPS + Firebase Auth JWT                                  │
│       ▼                                                             │
│  Cloud Function: markStudentAttendance                              │
│    1. Auth check (context.auth)                                     │
│    2. Verify caller role (TEACHER / CLASS_INCHARGE / CR)            │
│    3. Load session → verify ACTIVE                                  │
│    4. Verify caller is authorized for this class/session            │
│    5. Timing guard (session end + 10 min grace)                     │
│    6. Resolve student from qrToken (Admin SDK, server-only)         │
│    7. Verify student.classId == session.classId                     │
│    8. Atomic transaction:                                           │
│         a. Re-read session (TOCTOU guard) → must still be ACTIVE    │
│         b. Check for existing attendance record (duplicate guard)   │
│         c. Create attendance record with provenance fields          │
│         d. FieldValue.increment(1) on session.markedCount           │
│    9. Return { success, alreadyMarked, studentName }                │
│       │                                                             │
│       ▼                                                             │
│  Client sees new record via observeSessionRecords() (Firestore Flow)│
└─────────────────────────────────────────────────────────────────────┘
```

---

## Provenance Verification

Every attendance record created by a Cloud Function contains:

```json
{
  "_createdByFunction": true,
  "_functionVersion": "v2",
  "_callerRole": "TEACHER"
}
```

A Principal or HOD can audit the integrity of attendance data by querying:

```javascript
// Firebase Console / Admin SDK
db.collection("attendance")
  .where("_createdByFunction", "==", true)
  .get()
```

Any record **without** `_createdByFunction: true` was created before this security hardening was deployed and should be reviewed.

---

## Firestore Write Permission Matrix (Post-Refactor)

| Collection | Client Create | Client Update | Client Delete | Cloud Function |
|---|---|---|---|---|
| `users` | Principal only | FCM token self-update | Principal | Full access |
| `departments` | ✗ | ✗ | ✗ | Full access |
| `classes` | Admin/Incharge | Admin/Incharge | Principal | Full access |
| `subjects` | Admin | Admin | Admin | Full access |
| `timetables` | Admin/Incharge | Admin/Incharge | Admin | Full access |
| `lectureAdjustments` | Faculty | Creator/Admin | Admin | Full access |
| `lectureSessions` | **PENDING only** | **✗ Blocked** | Principal | Full access |
| `attendance` | **✗ Blocked** | **✗ Blocked** | Principal | Full access |
| `notifications` | ✗ | isRead only | Principal | Full access |

---

## Deployment Steps

```bash
# 1. Deploy hardened security rules (blocks client writes immediately)
firebase deploy --only firestore:rules

# 2. Deploy updated Cloud Functions
cd functions
npm install
npm run build
cd ..
firebase deploy --only functions

# 3. Verify rules in Firebase Console → Firestore → Rules
# TEST: Attempt direct client write to attendance → should be DENIED
# TEST: Call Cloud Function as authenticated user → should succeed
```

## Testing Security Rules

Use the **Firebase Rules Playground** in the Firebase Console:

```
Collection: attendance
Operation: CREATE
Auth: { uid: "teacher123", token: { role: "TEACHER" } }
Expected result: DENIED ✓
```

```
Collection: lectureSessions / {sessionId}
Operation: UPDATE (setting status: "ACTIVE")
Auth: { uid: "teacher123" }
Expected result: DENIED ✓
```
