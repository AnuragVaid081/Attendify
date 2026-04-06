import * as functions from "firebase-functions";
import * as admin from "firebase-admin";

const db = admin.firestore();

/**
 * Manually lock a session (called by teacher or class incharge).
 */
export const lockAttendanceSession = functions.https.onCall(
  async (data: { sessionId: string }, context) => {
    if (!context.auth) {
      throw new functions.https.HttpsError("unauthenticated", "Must be authenticated.");
    }

    const { sessionId } = data;
    const sessionRef = db.collection("lectureSessions").doc(sessionId);
    const sessionSnap = await sessionRef.get();

    if (!sessionSnap.exists) {
      throw new functions.https.HttpsError("not-found", "Session not found.");
    }

    const session = sessionSnap.data()!;

    // Only the assigned teacher or class incharge can lock
    if (session.teacherId !== context.auth.uid) {
      // Check if user is class incharge
      const userSnap = await db.collection("users").doc(context.auth.uid).get();
      const user = userSnap.data();
      if (!user || (user.role !== "CLASS_INCHARGE" && user.role !== "PRINCIPAL" && user.role !== "HOD")) {
        throw new functions.https.HttpsError("permission-denied", "Not authorized to lock this session.");
      }
    }

    if (session.status === "LOCKED") {
      return { success: true, message: "Session already locked." };
    }

    await sessionRef.update({
      status: "LOCKED",
      lockTimestamp: Date.now(),
    });

    return { success: true, message: "Session locked successfully." };
  }
);

/**
 * Scheduled function: auto-lock all ACTIVE sessions older than 90 minutes.
 * Runs every 15 minutes.
 */
export const autoLockExpiredSessions = functions.pubsub
  .schedule("every 15 minutes")
  .onRun(async () => {
    const cutoffTime = Date.now() - 90 * 60 * 1000; // 90 minutes ago

    const expiredSessions = await db
      .collection("lectureSessions")
      .where("status", "==", "ACTIVE")
      .where("verificationTimestamp", "<", cutoffTime)
      .get();

    const batch = db.batch();
    const now = Date.now();

    expiredSessions.docs.forEach((doc) => {
      batch.update(doc.ref, {
        status: "LOCKED",
        lockTimestamp: now,
      });
    });

    await batch.commit();
    functions.logger.info(`Auto-locked ${expiredSessions.size} expired sessions.`);
    return null;
  });
