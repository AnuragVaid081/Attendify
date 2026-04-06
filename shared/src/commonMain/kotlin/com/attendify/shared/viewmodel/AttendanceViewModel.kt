package com.attendify.shared.viewmodel

import com.attendify.shared.model.AttendanceRecord
import com.attendify.shared.model.LectureSession
import com.attendify.shared.model.SessionStatus
import com.attendify.shared.repository.AttendanceRepository
import com.attendify.shared.usecase.MarkAttendanceUseCase
import com.attendify.shared.usecase.VerifyTeacherSessionUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AttendanceUiState(
    val activeSession: LectureSession? = null,
    val records: List<AttendanceRecord> = emptyList(),
    val isLoading: Boolean = false,
    val isScanning: Boolean = false,
    val successMessage: String? = null,
    val error: String? = null,
    val lastScannedStudent: String? = null
)

class AttendanceViewModel(
    private val attendanceRepository: AttendanceRepository,
    private val markAttendanceUseCase: MarkAttendanceUseCase,
    private val verifyTeacherSessionUseCase: VerifyTeacherSessionUseCase
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _state = MutableStateFlow(AttendanceUiState())
    val state: StateFlow<AttendanceUiState> = _state.asStateFlow()

    private var sessionObserveJob: Job? = null
    private var recordsObserveJob: Job? = null

    fun loadSession(sessionId: String) {
        sessionObserveJob?.cancel()
        recordsObserveJob?.cancel()

        sessionObserveJob = scope.launch {
            attendanceRepository.observeSession(sessionId).collect { session ->
                _state.value = _state.value.copy(activeSession = session)
            }
        }
        recordsObserveJob = scope.launch {
            attendanceRepository.observeSessionRecords(sessionId).collect { records ->
                _state.value = _state.value.copy(records = records)
            }
        }
    }

    fun verifyTeacherQr(sessionId: String, qrToken: String) {
        scope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            val result = verifyTeacherSessionUseCase(sessionId, qrToken)
            when (result) {
                is VerifyTeacherSessionUseCase.Result.Success -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        activeSession = result.session,
                        successMessage = "Session verified! Attendance is now open."
                    )
                }
                is VerifyTeacherSessionUseCase.Result.Error -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = result.message
                    )
                }
            }
        }
    }

    fun markStudentAttendance(sessionId: String, studentQrToken: String, markedBy: String) {
        scope.launch {
            _state.value = _state.value.copy(isScanning = true, error = null, successMessage = null)
            val result = markAttendanceUseCase(sessionId, studentQrToken, markedBy)
            when (result) {
                is MarkAttendanceUseCase.Result.Success -> {
                    _state.value = _state.value.copy(
                        isScanning = false,
                        successMessage = "✓ Attendance marked!",
                        lastScannedStudent = result.record.studentId
                    )
                }
                is MarkAttendanceUseCase.Result.AlreadyMarked -> {
                    _state.value = _state.value.copy(
                        isScanning = false,
                        error = "Already marked for this session"
                    )
                }
                is MarkAttendanceUseCase.Result.NotAuthorized -> {
                    _state.value = _state.value.copy(
                        isScanning = false,
                        error = result.reason
                    )
                }
                is MarkAttendanceUseCase.Result.SessionNotActive -> {
                    _state.value = _state.value.copy(
                        isScanning = false,
                        error = result.reason
                    )
                }
                is MarkAttendanceUseCase.Result.Expired -> {
                    _state.value = _state.value.copy(
                        isScanning = false,
                        error = "Session has expired. Please lock and start a new session."
                    )
                }
                is MarkAttendanceUseCase.Result.Error -> {
                    _state.value = _state.value.copy(
                        isScanning = false,
                        error = result.message
                    )
                }
            }
        }
    }

    fun lockSession(sessionId: String) {
        scope.launch {
            attendanceRepository.lockSession(sessionId)
        }
    }

    fun createSession(
        timetableEntryId: String,
        classId: String,
        subjectId: String,
        teacherId: String,
        date: String,
        initiatedBy: String
    ) {
        scope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            val result = attendanceRepository.createSession(
                timetableEntryId, classId, subjectId, teacherId, date, initiatedBy
            )
            if (result.isSuccess) {
                val session = result.getOrThrow()
                _state.value = _state.value.copy(
                    isLoading = false,
                    activeSession = session,
                    successMessage = "Session created. Awaiting teacher verification."
                )
                loadSession(session.id)
            } else {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = result.exceptionOrNull()?.message ?: "Failed to create session"
                )
            }
        }
    }

    fun clearMessages() {
        _state.value = _state.value.copy(successMessage = null, error = null)
    }

    fun onDestroy() = scope.cancel()
}
