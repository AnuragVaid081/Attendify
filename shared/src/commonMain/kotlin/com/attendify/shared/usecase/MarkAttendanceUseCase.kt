package com.attendify.shared.usecase

import com.attendify.shared.model.AttendanceRecord
import com.attendify.shared.repository.AttendanceRepository
import com.attendify.shared.repository.impl.AlreadyMarkedException

class MarkAttendanceUseCase(
    private val attendanceRepository: AttendanceRepository
) {
    sealed class Result {
        data class Success(val record: AttendanceRecord) : Result()
        data class AlreadyMarked(val sessionId: String) : Result()
        data class NotAuthorized(val reason: String) : Result()
        data class SessionNotActive(val reason: String) : Result()
        data class Expired(val reason: String) : Result()
        data class Error(val message: String) : Result()
    }

    suspend operator fun invoke(
        sessionId: String,
        studentQrToken: String,
        scannedBy: String
    ): Result {
        if (sessionId.isBlank() || studentQrToken.isBlank()) {
            return Result.Error("Invalid parameters: sessionId and QR token are required.")
        }

        val repoResult = attendanceRepository.markAttendance(sessionId, studentQrToken, scannedBy)

        return if (repoResult.isSuccess) {
            Result.Success(repoResult.getOrThrow())
        } else {
            val exception = repoResult.exceptionOrNull()
            when {
                exception is AlreadyMarkedException ->
                    Result.AlreadyMarked(sessionId)
                exception is SecurityException ->
                    Result.NotAuthorized(exception.message ?: "Not authorized")
                exception?.message?.contains("expired", ignoreCase = true) == true ->
                    Result.Expired(exception.message ?: "Session expired")
                exception?.message?.contains("not active", ignoreCase = true) == true ||
                exception?.message?.contains("precondition", ignoreCase = true) == true ->
                    Result.SessionNotActive(exception?.message ?: "Session not active")
                else ->
                    Result.Error(exception?.message ?: "Attendance marking failed")
            }
        }
    }
}
