package com.attendify.shared.repository.impl

import com.attendify.shared.model.AttendanceRecord
import com.attendify.shared.model.AttendanceSummary
import com.attendify.shared.model.LectureSession
import com.attendify.shared.model.SessionStatus
import com.attendify.shared.repository.AttendanceRepository
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.Direction
import dev.gitlive.firebase.firestore.firestore
import dev.gitlive.firebase.functions.functions
import dev.gitlive.firebase.functions.httpsCallable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * SECURE implementation of AttendanceRepository.
 *
 * Security architecture:
 *  ┌─────────────────────────────────────────────────────────────┐
 *  │ Client (KMP)                                                │
 *  │  - Reads session/attendance via Firestore (real-time)       │
 *  │  - NEVER writes directly to 'attendance' or modifies        │
 *  │    session status fields                                    │
 *  │  - ALL mutations go through Cloud Functions (HTTPS callable)│
 *  └────────────────────┬────────────────────────────────────────┘
 *                       │  HTTPS (TLS) + Firebase Auth token
 *  ┌────────────────────▼────────────────────────────────────────┐
 *  │ Cloud Functions (Admin SDK — bypasses Firestore rules)      │
 *  │  - validateAttendanceSession: verifies teacher QR           │
 *  │  - markStudentAttendance: creates attendance record         │
 *  │  - lockAttendanceSession: locks the session                 │
 *  └─────────────────────────────────────────────────────────────┘
 *
 * Firestore security rules block ALL client writes to the
 * 'attendance' collection and ALL status updates to 'lectureSessions'.
 */
class FirebaseAttendanceRepository : AttendanceRepository {

    private val db       = Firebase.firestore
    private val fnClient = Firebase.functions

    private val sessions   = db.collection("lectureSessions")
    private val attendance = db.collection("attendance")

    // ── Session creation (only PENDING — CR/teacher allowed by security rules) ──

    override suspend fun createSession(
        timetableEntryId: String,
        classId: String,
        subjectId: String,
        teacherId: String,
        date: String,
        initiatedBy: String
    ): Result<LectureSession> = try {
        // Creating a PENDING session is a low-risk client write:
        // Firestore security rules enforce status == "PENDING" on create.
        // The session cannot become ACTIVE without server-side teacher QR validation.
        val docRef = sessions.document
        val session = LectureSession(
            id = docRef.id,
            timetableEntryId = timetableEntryId,
            classId = classId,
            subjectId = subjectId,
            teacherId = teacherId,
            date = date,
            status = SessionStatus.PENDING.name,
            initiatedBy = initiatedBy
        )
        docRef.set(session)
        Result.success(session)
    } catch (e: Exception) {
        Result.failure(e)
    }

    // ── Teacher QR verification → Cloud Function only ──────────────────────────

    /**
     * Sends only [sessionId] + [teacherQrToken] to the Cloud Function.
     * The function resolves the teacher, validates the QR, checks the session,
     * and transitions PENDING → ACTIVE automatically.
     *
     * The client never touches the 'users' collection for QR lookups,
     * and never writes a status field directly.
     */
    override suspend fun verifySession(
        sessionId: String,
        teacherQrToken: String
    ): Result<LectureSession> = try {
        val callable = fnClient.httpsCallable("validateAttendanceSession")

        // Send minimal payload. GitLive HttpsCallable.invoke() returns
        // HttpsCallableResult — we don't need the result here since we
        // re-fetch the session state directly from Firestore after the call.
        callable.invoke(
            mapOf(
                "sessionId"      to sessionId,
                "teacherQrToken" to teacherQrToken
            )
        )

        // After the function call, observe the updated session via Firestore
        // (the function returns minimal data; we fetch truth from Firestore)
        val sessionSnap = sessions.document(sessionId).get()
        val updatedSession = sessionSnap.data<LectureSession>()
        Result.success(updatedSession)

    } catch (e: Exception) {
        Result.failure(mapCloudFunctionError(e, "Session verification failed"))
    }

    // ── Student QR scan → Cloud Function only ─────────────────────────────────

    /**
     * Sends only [sessionId] + [studentQrToken] to the Cloud Function.
     * The function resolves the student server-side, validates class membership,
     * runs duplicate-check + record-creation atomically.
     *
     * The client NEVER:
     *  - resolves a student from a QR token
     *  - writes to the 'attendance' collection
     *  - updates markedCount on the session
     */
    override suspend fun markAttendance(
        sessionId: String,
        studentQrToken: String,
        markedBy: String  // Still passed for audit trail (caller's UID)
    ): Result<AttendanceRecord> = try {
        val callable = fnClient.httpsCallable("markStudentAttendance")

        // GitLive HttpsCallable.invoke() returns HttpsCallableResult.
        // Access .data to get the underlying Any? payload from the function,
        // then safe-cast to Map<*, *> — do NOT use bracket access on a
        // generic type parameter (causes "No 'get' operator" compile error).
        val result = callable.invoke(
            mapOf(
                "sessionId"      to sessionId,
                "studentQrToken" to studentQrToken
            )
        )

        // result.data() gets the payload — safe-cast to Map<*, *> before key access
        val responseMap = result.data<Map<String, Boolean>>()
        val alreadyMarked = responseMap["alreadyMarked"] ?: false

        if (alreadyMarked) {
            Result.failure(AlreadyMarkedException("Attendance already marked for this student"))
        } else {
            Result.success(
                AttendanceRecord(
                    sessionId  = sessionId,
                    markedBy   = markedBy,
                    status     = "PRESENT",
                    timestamp  = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
                )
            )
        }

    } catch (e: AlreadyMarkedException) {
        Result.failure(e)
    } catch (e: Exception) {
        Result.failure(mapCloudFunctionError(e, "Attendance marking failed"))
    }

    // ── Session lock → Cloud Function only ────────────────────────────────────

    /**
     * Asks the Cloud Function to lock the session.
     * The client cannot write 'status: LOCKED' directly (blocked by security rules).
     */
    override suspend fun lockSession(sessionId: String): Result<Unit> = try {
        val callable = fnClient.httpsCallable("lockAttendanceSession")
        callable.invoke(mapOf("sessionId" to sessionId))
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(mapCloudFunctionError(e, "Failed to lock session"))
    }

    // ── Real-time observation (read-only Firestore) ────────────────────────────

    override fun observeSession(sessionId: String): Flow<LectureSession?> =
        sessions.document(sessionId).snapshots.map { snapshot ->
            if (snapshot.exists) snapshot.data<LectureSession>() else null
        }

    override fun observeSessionRecords(sessionId: String): Flow<List<AttendanceRecord>> =
        attendance.where { "sessionId" equalTo sessionId }
            .snapshots.map { snapshot ->
                snapshot.documents.map { it.data<AttendanceRecord>() }
            }

    // ── Read-only queries ──────────────────────────────────────────────────────

    override suspend fun getStudentAttendanceSummary(studentId: String): List<AttendanceSummary> {
        val records = attendance
            .where { "studentId" equalTo studentId }
            .get()
            .documents.map { it.data<AttendanceRecord>() }

        return records.groupBy { it.subjectId }.map { (subjectId, subjectRecords) ->
            val attended = subjectRecords.count { it.attendanceStatus.name == "PRESENT" }
            AttendanceSummary(
                subjectId       = subjectId,
                totalLectures   = subjectRecords.size,
                attendedLectures  = attended,
                percentage      = if (subjectRecords.isEmpty()) 0f
                                  else (attended.toFloat() / subjectRecords.size) * 100f
            )
        }
    }

    override suspend fun getClassAttendance(
        classId: String,
        subjectId: String
    ): List<AttendanceRecord> =
        attendance
            .where { "classId" equalTo classId }
            .where { "subjectId" equalTo subjectId }
            .orderBy("timestamp", Direction.DESCENDING)
            .get()
            .documents.map { it.data<AttendanceRecord>() }

    override suspend fun getTeacherSessions(teacherId: String): List<LectureSession> =
        sessions
            .where { "teacherId" equalTo teacherId }
            .orderBy("verificationTimestamp", Direction.DESCENDING)
            .get()
            .documents.map { it.data<LectureSession>() }

    // ── Error mapping ──────────────────────────────────────────────────────────

    /**
     * Translates Firebase Functions HttpsCallableException into domain errors.
     * Strips internal Cloud Function details from messages exposed to the UI.
     */
    private fun mapCloudFunctionError(e: Exception, fallbackMessage: String): Exception {
        val message = e.message ?: fallbackMessage
        return when {
            message.contains("unauthenticated", ignoreCase = true) ->
                SecurityException("Authentication required. Please log in again.")
            message.contains("permission-denied", ignoreCase = true) ->
                SecurityException("You are not authorised to perform this action.")
            message.contains("not-found", ignoreCase = true) ->
                Exception("The requested resource was not found.")
            message.contains("failed-precondition", ignoreCase = true) ->
                Exception("Session state is invalid. Please refresh and try again.")
            message.contains("deadline-exceeded", ignoreCase = true) ->
                Exception("Session has expired. Attendance can no longer be marked.")
            else -> Exception(fallbackMessage)
        }
    }
}

/** Thrown when a student's attendance has already been marked in a session. */
class AlreadyMarkedException(message: String) : Exception(message)
