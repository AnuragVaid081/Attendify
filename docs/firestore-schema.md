# Firestore Schema — Attendify

## Collections Overview

```
users/
departments/
classes/
subjects/
timetables/
lectureAdjustments/
lectureSessions/
attendance/
notifications/
```

---

## `users/{userId}`

| Field | Type | Description |
|---|---|---|
| `id` | string | Firebase Auth UID |
| `name` | string | Full name |
| `email` | string | Institutional email |
| `role` | string (enum) | PRINCIPAL / HOD / CLASS_INCHARGE / TEACHER / STUDENT / CLASS_REPRESENTATIVE |
| `departmentId` | string | Reference to `departments` |
| `classId` | string | Reference to `classes` (for students, CR, incharge) |
| `subjectIds` | string[] | Subjects taught (for teachers) |
| `classRollNo` | string | Class roll number (students) |
| `universityRollNo` | string | University roll number (students) |
| `qrToken` | string | Unique secret used for QR generation & verification |
| `fcmToken` | string | Firebase Cloud Messaging push token |
| `profileImageUrl` | string | Optional profile photo URL |
| `isActive` | boolean | Account active flag |

---

## `departments/{deptId}`

| Field | Type | Description |
|---|---|---|
| `id` | string | Auto-generated Firestore ID |
| `name` | string | e.g. "Computer Science & Engineering" |
| `code` | string | e.g. "CSE" |
| `hodId` | string | userId of the HOD |

---

## `classes/{classId}`

| Field | Type | Description |
|---|---|---|
| `id` | string | Auto ID |
| `name` | string | e.g. "CSE-A 3rd Year" |
| `departmentId` | string | Parent department |
| `semester` | number | 1–8 |
| `section` | string | "A", "B", etc. |
| `inchargeId` | string | userId of class incharge |
| `representativeIds` | string[] | Max 2 CR userIds |
| `studentIds` | string[] | All student userIds |

---

## `subjects/{subjectId}`

| Field | Type | Description |
|---|---|---|
| `id` | string | Auto ID |
| `name` | string | Subject name |
| `code` | string | e.g. "CS301" |
| `departmentId` | string | Department |
| `creditHours` | number | Credit hours |

---

## `timetables/{entryId}`

Fixed weekly schedule — one document per lecture slot.

| Field | Type | Description |
|---|---|---|
| `id` | string | Auto ID |
| `classId` | string | Which class |
| `subjectId` | string | Subject |
| `subjectName` | string | Denormalized for display |
| `teacherId` | string | Assigned teacher |
| `teacherName` | string | Denormalized |
| `day` | string | MONDAY / TUESDAY / … / SATURDAY |
| `period` | number | 1-based period number |
| `timeSlot.startTime` | string | "HH:mm" |
| `timeSlot.endTime` | string | "HH:mm" |
| `venue` | string | Room/lab |
| `isActive` | boolean | Soft-delete flag |

---

## `lectureAdjustments/{adjustmentId}`

Day-specific overrides to the fixed timetable.

| Field | Type | Description |
|---|---|---|
| `id` | string | Auto ID |
| `originalEntryId` | string | Which timetable entry is affected |
| `date` | string | "YYYY-MM-DD" |
| `adjustmentType` | string | SWAP / RESCHEDULE / CANCEL |
| `newTeacherId` | string? | Replacement teacher (SWAP) |
| `newTimeSlot` | object? | New time (RESCHEDULE) |
| `newVenue` | string? | New venue |
| `reason` | string | Explanation |
| `createdBy` | string | userId |
| `createdAt` | number | Unix timestamp |
| `notificationSent` | boolean | FCM delivery status |

---

## `lectureSessions/{sessionId}`

One document per attendance marking event.

| Field | Type | Description |
|---|---|---|
| `id` | string | Auto ID |
| `timetableEntryId` | string | Which timetable slot |
| `subjectId` | string | Subject |
| `teacherId` | string | Assigned teacher |
| `classId` | string | Class |
| `date` | string | "YYYY-MM-DD" |
| `startTime` | string | "HH:mm" |
| `endTime` | string | "HH:mm" |
| `venue` | string | Location |
| `status` | string | **PENDING → ACTIVE → LOCKED / CANCELLED** |
| `verifiedBy` | string | teacherId who authenticated |
| `verificationTimestamp` | number | Unix ms |
| `lockTimestamp` | number | Unix ms |
| `initiatedBy` | string | CR or teacher userId |
| `totalStudents` | number | Class size at time of creation |
| `markedCount` | number | Running count of marked records |

---

## `attendance/{recordId}`

One document per student per session.

| Field | Type | Description |
|---|---|---|
| `id` | string | Auto ID |
| `sessionId` | string | Parent session |
| `lectureId` | string | timetableEntryId |
| `studentId` | string | The student |
| `classId` | string | Class |
| `subjectId` | string | Subject |
| `teacherId` | string | Teacher who ran session |
| `status` | string | PRESENT / ABSENT / LATE |
| `timestamp` | number | Unix ms when marked |
| `markedBy` | string | userId who scanned |
| `date` | string | "YYYY-MM-DD" |

> ⚠ **Immutable**: `update` operations on attendance records are denied by security rules.

---

## `notifications/{notifId}`

| Field | Type | Description |
|---|---|---|
| `id` | string | Auto ID |
| `title` | string | Notification heading |
| `body` | string | Message body |
| `type` | string | RESCHEDULE / CANCEL / GENERAL |
| `targetClassId` | string | Class this was sent to |
| `targetStudentIds` | string[] | Individual student IDs |
| `data` | map | Extra FCM payload keys |
| `timestamp` | number | Unix ms |
| `isRead` | boolean | Per-notification read state |
