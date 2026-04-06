package com.attendify.shared.viewmodel

import com.attendify.shared.model.AttendanceSummary
import com.attendify.shared.model.LectureAdjustment
import com.attendify.shared.model.LectureSession
import com.attendify.shared.model.TimetableEntry
import com.attendify.shared.model.UserModel
import com.attendify.shared.repository.AttendanceRepository
import com.attendify.shared.repository.UserRepository
import com.attendify.shared.usecase.GetStudentAttendanceSummaryUseCase
import com.attendify.shared.usecase.GetTimetableForDayUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DashboardState(
    val user: UserModel? = null,
    val todaySchedule: List<TimetableEntry> = emptyList(),
    val adjustments: Map<String, LectureAdjustment> = emptyMap(),
    val attendanceSummaries: List<AttendanceSummary> = emptyList(),
    val overallAttendance: Float = 0f,
    val lowAttendanceSubjects: List<AttendanceSummary> = emptyList(),
    val recentSessions: List<LectureSession> = emptyList(),
    val classRoster: List<UserModel> = emptyList(),
    val totalStudents: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null
)

class DashboardViewModel(
    private val attendanceRepository: AttendanceRepository,
    private val userRepository: UserRepository,
    private val getStudentAttendanceSummaryUseCase: GetStudentAttendanceSummaryUseCase,
    private val getTimetableForDayUseCase: GetTimetableForDayUseCase
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    fun loadStudentDashboard(userId: String) {
        scope.launch {
            _state.value = _state.value.copy(isLoading = true)
            try {
                val user = userRepository.getUserById(userId)
                _state.value = _state.value.copy(user = user)

                if (user != null) {
                    // Load today's schedule
                    val daySchedule = getTimetableForDayUseCase(user.classId)
                    // Load attendance summary
                    val overview = getStudentAttendanceSummaryUseCase(userId)

                    _state.value = _state.value.copy(
                        todaySchedule = daySchedule.entries,
                        adjustments = daySchedule.adjustments,
                        attendanceSummaries = overview.summaries,
                        overallAttendance = overview.overallPercentage,
                        lowAttendanceSubjects = overview.lowAttendanceSubjects,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    fun loadTeacherDashboard(userId: String) {
        scope.launch {
            _state.value = _state.value.copy(isLoading = true)
            try {
                val user = userRepository.getUserById(userId)
                val sessions = attendanceRepository.getTeacherSessions(userId)
                _state.value = _state.value.copy(
                    user = user,
                    recentSessions = sessions.take(10),
                    isLoading = false
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun loadAdminDashboard(departmentId: String) {
        scope.launch {
            _state.value = _state.value.copy(isLoading = true)
            try {
                val classes = userRepository.getAllClasses(departmentId)
                val teachers = userRepository.getTeachersInDepartment(departmentId)
                val totalStudents = classes.sumOf { it.studentIds.size }

                _state.value = _state.value.copy(
                    totalStudents = totalStudents,
                    classRoster = teachers,
                    isLoading = false
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun onDestroy() = scope.cancel()
}
