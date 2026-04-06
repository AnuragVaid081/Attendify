package com.attendify.shared.model

import kotlinx.serialization.Serializable

enum class DayOfWeek {
    MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY
}

@Serializable
data class TimeSlot(
    val startTime: String = "",   // "HH:mm"
    val endTime: String = ""
)

@Serializable
data class TimetableEntry(
    val id: String = "",
    val classId: String = "",
    val subjectId: String = "",
    val subjectName: String = "",
    val teacherId: String = "",
    val teacherName: String = "",
    val day: String = "",               // DayOfWeek.name
    val period: Int = 1,                // 1-based period number
    val timeSlot: TimeSlot = TimeSlot(),
    val venue: String = "",
    val isActive: Boolean = true
) {
    val dayOfWeek: DayOfWeek get() = DayOfWeek.valueOf(day)
}

@Serializable
data class LectureAdjustment(
    val id: String = "",
    val originalEntryId: String = "",
    val date: String = "",              // Specific date this applies to
    val adjustmentType: String = "",    // SWAP, RESCHEDULE, CANCEL
    val newTeacherId: String = "",      // For swaps
    val newTimeSlot: TimeSlot? = null,  // For reschedules
    val newVenue: String = "",
    val reason: String = "",
    val createdBy: String = "",
    val createdAt: Long = 0L,
    val notificationSent: Boolean = false
)

@Serializable
data class NotificationModel(
    val id: String = "",
    val title: String = "",
    val body: String = "",
    val type: String = "",              // RESCHEDULE, CANCEL, GENERAL
    val targetClassId: String = "",
    val targetStudentIds: List<String> = emptyList(),
    val data: Map<String, String> = emptyMap(),
    val timestamp: Long = 0L,
    val isRead: Boolean = false
)
