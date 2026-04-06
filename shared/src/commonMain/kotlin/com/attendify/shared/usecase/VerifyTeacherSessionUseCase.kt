package com.attendify.shared.usecase

import com.attendify.shared.model.LectureSession
import com.attendify.shared.model.SessionStatus
import com.attendify.shared.repository.AttendanceRepository

class VerifyTeacherSessionUseCase(
    private val attendanceRepository: AttendanceRepository
) {
    sealed class Result {
        data class Success(val session: LectureSession) : Result()
        data class Error(val message: String) : Result()
    }

    suspend operator fun invoke(sessionId: String, teacherQrToken: String): Result {
        if (teacherQrToken.isBlank()) return Result.Error("QR token is empty")
        return when (val result = attendanceRepository.verifySession(sessionId, teacherQrToken)) {
            is kotlin.Result<*> -> {
                val session = result.getOrNull() as? LectureSession
                if (session != null && session.sessionStatus == SessionStatus.ACTIVE) {
                    Result.Success(session)
                } else {
                    Result.Error(result.exceptionOrNull()?.message ?: "Verification failed")
                }
            }
        }
    }
}
