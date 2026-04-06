package com.attendify.shared.repository

import com.attendify.shared.model.LectureAdjustment
import com.attendify.shared.model.TimetableEntry
import kotlinx.coroutines.flow.Flow

interface TimetableRepository {
    fun observeTimetableForClass(classId: String): Flow<List<TimetableEntry>>
    fun observeTimetableForTeacher(teacherId: String): Flow<List<TimetableEntry>>
    suspend fun getTimetableForDay(classId: String, day: String): List<TimetableEntry>
    suspend fun getAdjustmentsForDate(classId: String, date: String): List<LectureAdjustment>
    suspend fun swapLecture(adjustment: LectureAdjustment): Result<Unit>
    suspend fun cancelLecture(adjustment: LectureAdjustment): Result<Unit>
    suspend fun rescheduleLecture(adjustment: LectureAdjustment): Result<Unit>
}
