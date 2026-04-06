import * as functions from "firebase-functions";
import * as admin from "firebase-admin";

const db = admin.firestore();
const messaging = admin.messaging();

interface NotificationPayload {
  title: string;
  body: string;
  classId?: string;
  studentIds?: string[];
  data?: Record<string, string>;
}

/**
 * HTTPS callable: Send a custom notification to a class or specific students.
 * Restricted to PRINCIPAL, HOD, CLASS_INCHARGE.
 */
export const sendBatchNotification = functions.https.onCall(
  async (payload: NotificationPayload, context) => {
    if (!context.auth) {
      throw new functions.https.HttpsError("unauthenticated", "Must be authenticated.");
    }

    const userSnap = await db.collection("users").doc(context.auth.uid).get();
    const user = userSnap.data();
    const adminRoles = ["PRINCIPAL", "HOD", "CLASS_INCHARGE"];

    if (!user || !adminRoles.includes(user.role)) {
      throw new functions.https.HttpsError("permission-denied", "Not authorized to send notifications.");
    }

    let targetStudentIds: string[] = payload.studentIds ?? [];

    // If classId given, fetch all student FCM tokens from class roster
    if (payload.classId && targetStudentIds.length === 0) {
      const studentsSnap = await db
        .collection("users")
        .where("classId", "==", payload.classId)
        .where("role", "==", "STUDENT")
        .get();
      targetStudentIds = studentsSnap.docs.map((d) => d.id);

      // Collect FCM tokens
      const fcmTokens = studentsSnap.docs
        .map((d) => d.data().fcmToken as string)
        .filter(Boolean);

      if (fcmTokens.length > 0) {
        await sendFCM(fcmTokens, payload.title, payload.body, payload.data ?? {});
      }
    } else if (targetStudentIds.length > 0) {
      // Fetch tokens for specific students
      const chunks = chunkArray(targetStudentIds, 10);
      for (const chunk of chunks) {
        const snaps = await db.collection("users").where(admin.firestore.FieldPath.documentId(), "in", chunk).get();
        const tokens = snaps.docs.map((d) => d.data().fcmToken as string).filter(Boolean);
        if (tokens.length > 0) {
          await sendFCM(tokens, payload.title, payload.body, payload.data ?? {});
        }
      }
    }

    // Save notification record to Firestore
    await db.collection("notifications").add({
      title: payload.title,
      body: payload.body,
      type: payload.data?.type ?? "GENERAL",
      targetClassId: payload.classId ?? "",
      targetStudentIds,
      data: payload.data ?? {},
      timestamp: Date.now(),
      isRead: false,
    });

    return { success: true, sentTo: targetStudentIds.length };
  }
);

/**
 * Internal helper: Used by timetable trigger to notify a class.
 */
export async function sendNotificationToClass(
  classId: string,
  title: string,
  body: string,
  data: Record<string, string>
): Promise<void> {
  const studentsSnap = await db
    .collection("users")
    .where("classId", "==", classId)
    .where("role", "in", ["STUDENT", "CLASS_REPRESENTATIVE"])
    .get();

  const studentIds = studentsSnap.docs.map((d) => d.id);
  const fcmTokens = studentsSnap.docs
    .map((d) => d.data().fcmToken as string)
    .filter(Boolean);

  if (fcmTokens.length > 0) {
    await sendFCM(fcmTokens, title, body, data);
  }

  // Persist notification
  await db.collection("notifications").add({
    title,
    body,
    type: data.type ?? "GENERAL",
    targetClassId: classId,
    targetStudentIds: studentIds,
    data,
    timestamp: Date.now(),
    isRead: false,
  });
}

async function sendFCM(
  tokens: string[],
  title: string,
  body: string,
  data: Record<string, string>
): Promise<void> {
  // FCM allows max 500 tokens per multicast
  const chunks = chunkArray(tokens, 500);
  for (const chunk of chunks) {
    const message: admin.messaging.MulticastMessage = {
      notification: { title, body },
      data,
      tokens: chunk,
      android: {
        notification: {
          channelId: "attendify_notifications",
          priority: "high",
        },
      },
      apns: {
        payload: {
          aps: {
            sound: "default",
            badge: 1,
          },
        },
      },
    };
    const response = await messaging.sendEachForMulticast(message);
    functions.logger.info(`FCM: ${response.successCount} sent, ${response.failureCount} failed`);
  }
}

function chunkArray<T>(arr: T[], size: number): T[][] {
  const chunks: T[][] = [];
  for (let i = 0; i < arr.length; i += size) {
    chunks.push(arr.slice(i, i + size));
  }
  return chunks;
}
