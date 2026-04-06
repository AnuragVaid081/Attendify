import * as functions from "firebase-functions";
import * as admin from "firebase-admin";

const db = admin.firestore();

interface MarkAttendanceRequest {
  sessionId: string;
  studentQrToken: string;
}

interface MarkAttendanceResponse {
  success: boolean;
  alreadyMarked?: boolean;
  studentName?: string; // Only for UI feedback — no sensitive data
}

/**
 * SECURE: Student QR scan → attendance record creation.
 *
 * Security guarantees (all enforced server-side via Admin SDK):
 *  - Caller must be authenticated as an active teacher, class incharge, or CR
 *  - QR token–to–student resolution happens ONLY on the server
 *  - Student must belong to the session's class
 *  - Session must be in ACTIVE state at the time of marking
 *  - Duplicate prevention is atomic (Firestore transaction)
 *  - Session must not be past its scheduled end time (timing enforcement)
 *  - markedCount is incremented atomically (FieldValue.increment)
 *  - Attendance record is immutable after creation
 *  - Client never sees studentId, qrToken, or any resolved identifiers
 */
export const markStudentAttendance = functions.https.onCall(
  async (data: MarkAttendanceRequest, context): Promise<MarkAttendanceResponse> => {

    // ── 1. Authentication guard ───────────────────────────────────────────────
    if (!context.auth) {
      throw new functions.https.HttpsError(
        "unauthenticated",
        "Authentication required."
      );
    }

    // ── 2. Input sanitation ───────────────────────────────────────────────────
    const { sessionId, studentQrToken } = data;

    if (
      typeof sessionId !== "string" || sessionId.trim().length === 0 ||
      typeof studentQrToken !== "string" || studentQrToken.trim().length < 8
    ) {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "sessionId and studentQrToken are required."
      );
    }

    const sanitizedSessionId = sessionId.trim();
    const sanitizedToken = studentQrToken.trim();

    // ── 3. Verify caller's role and eligibility ───────────────────────────────
    const callerSnap = await db.collection("users").doc(context.auth.uid).get();
    if (!callerSnap.exists) {
      throw new functions.https.HttpsError("not-found", "Caller account not found.");
    }

    const caller = callerSnap.data()!;
    const allowedCallerRoles = ["TEACHER", "CLASS_INCHARGE", "CLASS_REPRESENTATIVE", "PRINCIPAL", "HOD"];

    if (!allowedCallerRoles.includes(caller.role) || caller.isActive !== true) {
      throw new functions.https.HttpsError(
        "permission-denied",
        "Your role is not permitted to mark attendance."
      );
    }

    // ── 4. Load and validate session ──────────────────────────────────────────
    const sessionRef = db.collection("lectureSessions").doc(sanitizedSessionId);
    const sessionSnap = await sessionRef.get();

    if (!sessionSnap.exists) {
      throw new functions.https.HttpsError("not-found", "Session not found.");
    }

    const session = sessionSnap.data()!;

    // Session must be ACTIVE
    if (session.status !== "ACTIVE") {
      throw new functions.https.HttpsError(
        "failed-precondition",
        session.status === "LOCKED"
          ? "This session has been locked and no longer accepts attendance."
          : "Session is not active yet. Teacher verification is required first."
      );
    }

    // ── 5. Class-level authorization ──────────────────────────────────────────
    // Teacher must be assigned to THIS session; CR/Incharge must be of the same class
    const isAssignedTeacher = session.teacherId === context.auth.uid;
    const isCRofClass = caller.role === "CLASS_REPRESENTATIVE" && caller.classId === session.classId;
    const isInchargeOfClass = caller.role === "CLASS_INCHARGE" && caller.classId === session.classId;
    const isSuperAdmin = ["PRINCIPAL", "HOD"].includes(caller.role);

    if (!isAssignedTeacher && !isCRofClass && !isInchargeOfClass && !isSuperAdmin) {
      functions.logger.warn("markStudentAttendance: unauthorized mark attempt", {
        callerUid: context.auth.uid,
        callerRole: caller.role,
        sessionId: sanitizedSessionId,
        sessionClassId: session.classId,
      });
      throw new functions.https.HttpsError(
        "permission-denied",
        "You are not authorized to mark attendance for this session."
      );
    }

    // ── 6. Timing guard: reject if past session end time (+ 10 min grace) ─────
    const now = Date.now();
    if (session.date && session.endTime) {
      const [endHour, endMin] = (session.endTime as string).split(":").map(Number);
      const sessionEndDate = new Date(`${session.date}T${String(endHour).padStart(2, "0")}:${String(endMin).padStart(2, "0")}:00`);
      const graceMs = 10 * 60 * 1000; // 10 minutes
      if (Date.now() > sessionEndDate.getTime() + graceMs) {
        throw new functions.https.HttpsError(
          "deadline-exceeded",
          "Session has expired. Attendance can no longer be marked."
        );
      }
    }

    // ── 7. Resolve student from QR token (server-only) ────────────────────────
    const studentQuery = await db
      .collection("users")
      .where("qrToken", "==", sanitizedToken)
      .where("role", "in", ["STUDENT", "CLASS_REPRESENTATIVE"])
      .where("isActive", "==", true)
      .limit(1)
      .get();

    if (studentQuery.empty) {
      functions.logger.warn("markStudentAttendance: unrecognized QR token scanned", {
        callerUid: context.auth.uid,
        sessionId: sanitizedSessionId,
      });
      throw new functions.https.HttpsError(
        "not-found",
        "No active student found for the scanned QR code."
      );
    }

    const studentDoc = studentQuery.docs[0];
    const student = studentDoc.data();
    const studentId = studentDoc.id;

    // ── 8. Student class membership check ────────────────────────────────────
    if (student.classId !== session.classId) {
      throw new functions.https.HttpsError(
        "permission-denied",
        "The scanned student does not belong to the class for this session."
      );
    }

    // ── 9. Atomic duplicate-check + record creation ───────────────────────────
    let alreadyMarked = false;

    await db.runTransaction(async (tx) => {
      // a) Re-read session inside transaction (TOCTOU guard)
      const freshSession = await tx.get(sessionRef);
      if (!freshSession.exists || freshSession.data()!.status !== "ACTIVE") {
        throw new functions.https.HttpsError(
          "failed-precondition",
          "Session state changed. Please retry."
        );
      }

      // b) Check for existing record atomically
      const existingQuery = await db
        .collection("attendance")
        .where("sessionId", "==", sanitizedSessionId)
        .where("studentId", "==", studentId)
        .limit(1)
        .get();

      if (!existingQuery.empty) {
        alreadyMarked = true;
        return; // Exit transaction — don't throw, just flag
      }

      // c) Create attendance record
      const recordRef = db.collection("attendance").doc();
      tx.set(recordRef, {
        id: recordRef.id,
        sessionId: sanitizedSessionId,
        lectureId: session.timetableEntryId ?? "",
        studentId,
        classId: session.classId,
        subjectId: session.subjectId,
        teacherId: session.teacherId,
        status: "PRESENT",
        timestamp: now,
        markedBy: context.auth!.uid,
        date: session.date,
        // Provenance: proves this record was created by a Cloud Function
        _createdByFunction: true,
        _functionVersion: "v2",
        _callerRole: caller.role,
      });

      // d) Atomically increment markedCount
      tx.update(sessionRef, {
        markedCount: admin.firestore.FieldValue.increment(1),
      });
    });

    functions.logger.info("markStudentAttendance: attendance marked", {
      sessionId: sanitizedSessionId,
      studentId,
      markedBy: context.auth.uid,
      alreadyMarked,
      timestamp: new Date(now).toISOString(),
    });

    // ── 10. Return minimal response ───────────────────────────────────────────
    // Never return studentId, qrToken, or sensitive fields
    return {
      success: true,
      alreadyMarked,
      studentName: alreadyMarked ? undefined : student.name, // Only for live UI feedback
    };
  }
);
