package com.attendify.shared.usecase

import com.attendify.shared.model.LectureAdjustment
import com.attendify.shared.model.TimetableEntry
import com.attendify.shared.repository.TimetableRepository
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

class GetTimetableForDayUseCase(
    private val timetableRepository: TimetableRepository
) {
    data class DaySchedule(
        val entries: List<TimetableEntry>,
        val adjustments: Map<String, LectureAdjustment>  // entryId -> adjustment
    )

    suspend operator fun invoke(classId: String, date: String? = null): DaySchedule {
        val today = date ?: Clock.System.todayIn(TimeZone.currentSystemDefault()).toString()
        val calendar = kotlinx.datetime.LocalDate.parse(today)
        val dayOfWeek = calendar.dayOfWeek.name

        val entries = timetableRepository.getTimetableForDay(classId, dayOfWeek)
        val adjustments = timetableRepository.getAdjustmentsForDate(classId, today)

        val adjustmentMap = adjustments.associateBy { it.originalEntryId }
        return DaySchedule(entries = entries, adjustments = adjustmentMap)
    }
}
