package com.attendify.shared.usecase

import com.attendify.shared.model.AttendanceSummary
import com.attendify.shared.repository.AttendanceRepository
import com.attendify.shared.repository.UserRepository

class GetStudentAttendanceSummaryUseCase(
    private val attendanceRepository: AttendanceRepository,
    private val userRepository: UserRepository
) {
    data class StudentAttendanceOverview(
        val summaries: List<AttendanceSummary>,
        val overallPercentage: Float,
        val lowAttendanceSubjects: List<AttendanceSummary>   // < 75%
    )

    suspend operator fun invoke(studentId: String): StudentAttendanceOverview {
        val summaries = attendanceRepository.getStudentAttendanceSummary(studentId)

        // Enrich subject names
        val enriched = summaries.map { summary ->
            val subject = userRepository.getUserById(summary.subjectId)
            summary.copy(subjectName = subject?.name ?: summary.subjectId)
        }

        val overall = if (enriched.isEmpty()) 0f else {
            val totalClasses = enriched.sumOf { it.totalLectures }
            val totalAttended = enriched.sumOf { it.attendedLectures }
            if (totalClasses == 0) 0f else (totalAttended.toFloat() / totalClasses) * 100f
        }

        val lowAttendance = enriched.filter { it.percentage < 75f }

        return StudentAttendanceOverview(
            summaries = enriched.sortedBy { it.subjectName },
            overallPercentage = overall,
            lowAttendanceSubjects = lowAttendance
        )
    }
}
