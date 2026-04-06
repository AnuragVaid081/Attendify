package com.attendify.shared.model

import kotlinx.serialization.Serializable

enum class SessionStatus {
    PENDING,    // Created, awaiting teacher QR verification
    ACTIVE,     // Teacher verified — marking is open
    LOCKED,     // Session closed, no more marking
    CANCELLED
}

enum class AttendanceStatus {
    PRESENT,
    ABSENT,
    LATE
}

@Serializable
data class LectureSession(
    val id: String = "",
    val timetableEntryId: String = "",
    val subjectId: String = "",
    val teacherId: String = "",
    val classId: String = "",
    val date: String = "",              // ISO date "YYYY-MM-DD"
    val startTime: String = "",         // "HH:mm"
    val endTime: String = "",
    val venue: String = "",
    val status: String = SessionStatus.PENDING.name,
    val verifiedBy: String = "",        // Teacher who verified
    val verificationTimestamp: Long = 0L,
    val lockTimestamp: Long = 0L,
    val initiatedBy: String = "",       // CR who initiated
    val totalStudents: Int = 0,
    val markedCount: Int = 0
) {
    val sessionStatus: SessionStatus get() = SessionStatus.valueOf(status)
}

@Serializable
data class AttendanceRecord(
    val id: String = "",
    val sessionId: String = "",
    val lectureId: String = "",        // timetableEntryId
    val studentId: String = "",
    val classId: String = "",
    val subjectId: String = "",
    val teacherId: String = "",
    val status: String = AttendanceStatus.PRESENT.name,
    val timestamp: Long = 0L,
    val markedBy: String = "",         // Scanner userId
    val date: String = ""              // "YYYY-MM-DD"
) {
    val attendanceStatus: AttendanceStatus get() = AttendanceStatus.valueOf(status)
}

@Serializable
data class AttendanceSummary(
    val subjectId: String = "",
    val subjectName: String = "",
    val totalLectures: Int = 0,
    val attendedLectures: Int = 0,
    val percentage: Float = 0f
)
