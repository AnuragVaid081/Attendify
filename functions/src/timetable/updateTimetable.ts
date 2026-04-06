import * as functions from "firebase-functions";
import * as admin from "firebase-admin";
import { sendNotificationToClass } from "../notifications/sendNotification";

const db = admin.firestore();

interface TimetableAdjustmentData {
  id: string;
  originalEntryId: string;
  date: string;
  adjustmentType: "SWAP" | "RESCHEDULE" | "CANCEL";
  newTeacherId?: string;
  newTimeSlot?: { startTime: string; endTime: string };
  newVenue?: string;
  reason: string;
  createdBy: string;
}

/**
 * HTTPS Callable: Teacher or class incharge creates a timetable adjustment.
 * Validates permissions, saves adjustment, and sends notifications.
 */
export const updateTimetable = functions.https.onCall(
  async (data: TimetableAdjustmentData, context) => {
    if (!context.auth) {
      throw new functions.https.HttpsError("unauthenticated", "Must be authenticated.");
    }

    // Only TEACHER, CLASS_INCHARGE, HOD, PRINCIPAL can update timetable
    const userSnap = await db.collection("users").doc(context.auth.uid).get();
    const user = userSnap.data();
    const allowedRoles = ["TEACHER", "CLASS_INCHARGE", "HOD", "PRINCIPAL"];

    if (!user || !allowedRoles.includes(user.role)) {
      throw new functions.https.HttpsError(
        "permission-denied",
        "You do not have permission to modify the timetable."
      );
    }

    // Validate original timetable entry exists
    const entrySnap = await db.collection("timetables").doc(data.originalEntryId).get();
    if (!entrySnap.exists) {
      throw new functions.https.HttpsError("not-found", "Timetable entry not found.");
    }

    const entry = entrySnap.data()!;

    // Teachers can only modify their own entries
    if (user.role === "TEACHER" && entry.teacherId !== context.auth.uid) {
      throw new functions.https.HttpsError(
        "permission-denied",
        "Teachers can only modify their own timetable entries."
      );
    }

    // Save adjustment
    const docRef = db.collection("lectureAdjustments").doc();
    await docRef.set({
      ...data,
      id: docRef.id,
      createdBy: context.auth.uid,
      createdAt: Date.now(),
      notificationSent: false,
    });

    return { success: true, adjustmentId: docRef.id };
  }
);

/**
 * Firestore trigger: When an adjustment is created, send FCM notifications to affected students.
 */
export const onAdjustmentCreated = functions.firestore
  .document("lectureAdjustments/{adjustmentId}")
  .onCreate(async (snap, context) => {
    const adjustment = snap.data() as TimetableAdjustmentData;

    // Get the timetable entry to find classId
    const entrySnap = await db.collection("timetables").doc(adjustment.originalEntryId).get();
    if (!entrySnap.exists) return;

    const entry = entrySnap.data()!;
    const classId = entry.classId as string;
    const subjectName = entry.subjectName as string;

    // Build notification content based on adjustment type
    let title = "";
    let body = "";

    switch (adjustment.adjustmentType) {
      case "CANCEL":
        title = `Class Cancelled: ${subjectName}`;
        body = `Your ${subjectName} class on ${adjustment.date} has been cancelled. Reason: ${adjustment.reason}`;
        break;
      case "RESCHEDULE":
        title = `Class Rescheduled: ${subjectName}`;
        body = `Your ${subjectName} class on ${adjustment.date} has been rescheduled. ${adjustment.reason}`;
        break;
      case "SWAP":
        title = `Class Swap: ${subjectName}`;
        body = `Your ${subjectName} class on ${adjustment.date} will be taken by a different teacher. ${adjustment.reason}`;
        break;
    }

    // Send to all students in the class
    await sendNotificationToClass(classId, title, body, {
      type: adjustment.adjustmentType,
      adjustmentId: snap.id,
      date: adjustment.date,
    });

    // Mark notification as sent
    await snap.ref.update({ notificationSent: true });
  });
