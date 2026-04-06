import * as admin from "firebase-admin";

admin.initializeApp();

// ─── Attendance (all security-critical) ──────────────────────────────────────
export { validateAttendanceSession } from "./attendance/validateSession";
export { markStudentAttendance } from "./attendance/markAttendance";
export { lockAttendanceSession, autoLockExpiredSessions } from "./attendance/lockSession";

// ─── Timetable ────────────────────────────────────────────────────────────────
export { updateTimetable, onAdjustmentCreated } from "./timetable/updateTimetable";

// ─── Notifications ────────────────────────────────────────────────────────────
export { sendBatchNotification } from "./notifications/sendNotification";
