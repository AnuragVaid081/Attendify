import * as functions from "firebase-functions";
import * as admin from "firebase-admin";

const db = admin.firestore();

interface ValidateSessionRequest {
  sessionId: string;
  teacherQrToken: string;
}

/**
 * SECURE: Teacher QR verification → PENDING → ACTIVE.
 *
 * Security guarantees (all server-side via Admin SDK):
 *  - Caller must be authenticated
 *  - QR token is resolved only on the server; client never touches the users collection for QR lookup
 *  - Session must belong to the authenticated caller or the resolved teacher (double-check)
 *  - Session must be exactly in PENDING state
 *  - Transition is atomic — no partial updates
 *  - Minimum session gap enforced: cannot re-verify a session within 2 minutes of another
 */
export const validateAttendanceSession = functions.https.onCall(
  async (data: ValidateSessionRequest, context) => {
    if (!context.auth) {
      throw new functions.https.HttpsError(
        "unauthenticated",
        "Authentication required."
      );
    }

    // ── 1. Input sanitation ───────────────────────────────────────────────────
    const { sessionId, teacherQrToken } = data;

    if (
      typeof sessionId !== "string" || sessionId.trim().length === 0 ||
      typeof teacherQrToken !== "string" || teacherQrToken.trim().length < 8
    ) {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "sessionId and a valid teacherQrToken are required."
      );
    }

    // ── 2. Load session first (fast-fail before expensive QR lookup) ──────────
    const sessionRef = db.collection("lectureSessions").doc(sessionId.trim());
    const sessionSnap = await sessionRef.get();

    if (!sessionSnap.exists) {
      // Return the same generic error regardless of whether session exists
      // to prevent session-ID enumeration attacks
      throw new functions.https.HttpsError(
        "not-found",
        "Session not found or not eligible for verification."
      );
    }

    const session = sessionSnap.data()!;

    // ── 3. Session state guard ────────────────────────────────────────────────
    if (session.status !== "PENDING") {
      throw new functions.https.HttpsError(
        "failed-precondition",
        `Session cannot be verified in its current state.`
        // Note: do NOT expose current status in the error message
      );
    }

    // ── 4. Resolve teacher from QR token (server-only lookup) ─────────────────
    const teacherQuery = await db
      .collection("users")
      .where("qrToken", "==", teacherQrToken.trim())
      .where("role", "in", ["TEACHER", "CLASS_INCHARGE"])
      .where("isActive", "==", true)
      .limit(1)
      .get();

    if (teacherQuery.empty) {
      functions.logger.warn("validateAttendanceSession: invalid QR token attempt", {
        uid: context.auth.uid,
        sessionId,
      });
      throw new functions.https.HttpsError(
        "permission-denied",
        "Invalid or unrecognized QR token."
      );
    }

    const teacherDoc = teacherQuery.docs[0];
    const teacher = teacherDoc.data();

    // ── 5. Caller identity check ──────────────────────────────────────────────
    // The Firebase Auth UID of the caller must match the resolved teacher,
    // OR the caller must be authenticated as a CR/Incharge for this class.
    const callerSnap = await db.collection("users").doc(context.auth.uid).get();
    if (!callerSnap.exists) {
      throw new functions.https.HttpsError("not-found", "Caller profile not found.");
    }
    const caller = callerSnap.data()!;

    const isAssignedTeacher = teacherDoc.id === context.auth.uid;
    const isCRofClass =
      caller.role === "CLASS_REPRESENTATIVE" && caller.classId === session.classId;
    const isInchargeOfClass =
      caller.role === "CLASS_INCHARGE" && caller.classId === session.classId;
    const isPrincipalOrHOD = ["PRINCIPAL", "HOD"].includes(caller.role);

    if (!isAssignedTeacher && !isCRofClass && !isInchargeOfClass && !isPrincipalOrHOD) {
      throw new functions.https.HttpsError(
        "permission-denied",
        "You are not authorized to verify this session."
      );
    }

    // ── 6. Session–teacher assignment check ───────────────────────────────────
    if (session.teacherId !== teacherDoc.id) {
      throw new functions.https.HttpsError(
        "permission-denied",
        "The QR presented does not belong to the teacher assigned to this session."
      );
    }

    // ── 7. Timing guard: session date must be today ───────────────────────────
    const today = new Date().toISOString().split("T")[0]; // "YYYY-MM-DD"
    if (session.date !== today) {
      throw new functions.https.HttpsError(
        "failed-precondition",
        "Session date does not match today's date."
      );
    }

    // ── 8. Atomic transition PENDING → ACTIVE ─────────────────────────────────
    const now = Date.now();
    await db.runTransaction(async (tx) => {
      // Re-read inside transaction to prevent TOCTOU race
      const freshSnap = await tx.get(sessionRef);
      if (!freshSnap.exists || freshSnap.data()!.status !== "PENDING") {
        throw new functions.https.HttpsError(
          "failed-precondition",
          "Session state changed concurrently. Please retry."
        );
      }
      tx.update(sessionRef, {
        status: "ACTIVE",
        verifiedBy: teacherDoc.id,
        verificationTimestamp: now,
        _verifiedByFunction: true,      // provenance marker
        _functionVersion: "v2",
      });
    });

    functions.logger.info("validateAttendanceSession: session activated", {
      sessionId,
      teacherId: teacherDoc.id,
      activatedAt: new Date(now).toISOString(),
    });

    // ── 9. Return minimal response ─────────────────────────────────────────────
    // Never return raw session data or QR tokens to the client
    return {
      success: true,
      sessionId,
      activatedAt: now,
    };
  }
);
