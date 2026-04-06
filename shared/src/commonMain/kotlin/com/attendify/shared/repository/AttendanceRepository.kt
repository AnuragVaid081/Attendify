package com.attendify.shared.repository

import com.attendify.shared.model.AttendanceRecord
import com.attendify.shared.model.AttendanceSummary
import com.attendify.shared.model.LectureSession
import kotlinx.coroutines.flow.Flow

interface AttendanceRepository {
    /**
     * CR initiates a session — PENDING state.
     */
    suspend fun createSession(
        timetableEntryId: String,
        classId: String,
        subjectId: String,
        teacherId: String,
        date: String,
        initiatedBy: String
    ): Result<LectureSession>

    /**
     * Teacher verifies via their QR — moves session to ACTIVE.
     */
    suspend fun verifySession(sessionId: String, teacherQrToken: String): Result<LectureSession>

    /**
     * Mark a student present by scanning their QR.
     */
    suspend fun markAttendance(
        sessionId: String,
        studentQrToken: String,
        markedBy: String
    ): Result<AttendanceRecord>

    /**
     * Lock session — no more marking allowed.
     */
    suspend fun lockSession(sessionId: String): Result<Unit>

    /**
     * Live updates to a session.
     */
    fun observeSession(sessionId: String): Flow<LectureSession?>

    /**
     * All records for a session.
     */
    fun observeSessionRecords(sessionId: String): Flow<List<AttendanceRecord>>

    /**
     * Student's attendance summary per subject.
     */
    suspend fun getStudentAttendanceSummary(studentId: String): List<AttendanceSummary>

    /**
     * Records for a specific class + subject.
     */
    suspend fun getClassAttendance(
        classId: String,
        subjectId: String
    ): List<AttendanceRecord>

    /**
     * Teacher's lecture history.
     */
    suspend fun getTeacherSessions(teacherId: String): List<LectureSession>
}
