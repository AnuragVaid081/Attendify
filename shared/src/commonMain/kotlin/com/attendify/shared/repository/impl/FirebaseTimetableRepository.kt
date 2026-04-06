package com.attendify.shared.repository.impl

import com.attendify.shared.model.LectureAdjustment
import com.attendify.shared.model.TimetableEntry
import com.attendify.shared.repository.TimetableRepository
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FirebaseTimetableRepository : TimetableRepository {

    private val db = Firebase.firestore
    private val timetables = db.collection("timetables")
    private val adjustments = db.collection("lectureAdjustments")

    override fun observeTimetableForClass(classId: String): Flow<List<TimetableEntry>> =
        timetables.where { "classId" equalTo classId }
            .where { "isActive" equalTo true }
            .snapshots.map { snapshot ->
                snapshot.documents.map { it.data<TimetableEntry>() }
                    .sortedWith(compareBy({ it.dayOfWeek.ordinal }, { it.period }))
            }

    override fun observeTimetableForTeacher(teacherId: String): Flow<List<TimetableEntry>> =
        timetables.where { "teacherId" equalTo teacherId }
            .where { "isActive" equalTo true }
            .snapshots.map { snapshot ->
                snapshot.documents.map { it.data<TimetableEntry>() }
                    .sortedWith(compareBy({ it.dayOfWeek.ordinal }, { it.period }))
            }

    override suspend fun getTimetableForDay(
        classId: String,
        day: String
    ): List<TimetableEntry> =
        timetables.where { "classId" equalTo classId }
            .where { "day" equalTo day }
            .where { "isActive" equalTo true }
            .get()
            .documents.map { it.data<TimetableEntry>() }
            .sortedBy { it.period }

    override suspend fun getAdjustmentsForDate(
        classId: String,
        date: String
    ): List<LectureAdjustment> =
        adjustments.where { "originalEntryId" inArray
            timetables.where { "classId" equalTo classId }.get()
                .documents.map { it.id }
        }
        .where { "date" equalTo date }
        .get()
        .documents.map { it.data<LectureAdjustment>() }

    override suspend fun swapLecture(adjustment: LectureAdjustment): Result<Unit> =
        saveAdjustment(adjustment.copy(adjustmentType = "SWAP"))

    override suspend fun cancelLecture(adjustment: LectureAdjustment): Result<Unit> =
        saveAdjustment(adjustment.copy(adjustmentType = "CANCEL"))

    override suspend fun rescheduleLecture(adjustment: LectureAdjustment): Result<Unit> =
        saveAdjustment(adjustment.copy(adjustmentType = "RESCHEDULE"))

    private suspend fun saveAdjustment(adjustment: LectureAdjustment): Result<Unit> = try {
        val docRef = if (adjustment.id.isEmpty()) adjustments.document
        else adjustments.document(adjustment.id)
        docRef.set(adjustment.copy(id = docRef.id))
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
