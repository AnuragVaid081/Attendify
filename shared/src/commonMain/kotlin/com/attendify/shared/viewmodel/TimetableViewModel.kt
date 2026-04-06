package com.attendify.shared.viewmodel

import com.attendify.shared.model.TimetableEntry
import com.attendify.shared.repository.TimetableRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TimetableState(
    val entries: List<TimetableEntry> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class TimetableViewModel(
    private val timetableRepository: TimetableRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _state = MutableStateFlow(TimetableState())
    val state: StateFlow<TimetableState> = _state.asStateFlow()

    private var observeJob: Job? = null

    fun loadTimetableForClass(classId: String) {
        observeJob?.cancel()
        _state.value = _state.value.copy(isLoading = true)
        observeJob = scope.launch {
            timetableRepository.observeTimetableForClass(classId).collect { entries ->
                _state.value = TimetableState(entries = entries, isLoading = false)
            }
        }
    }

    fun loadTimetableForTeacher(teacherId: String) {
        observeJob?.cancel()
        _state.value = _state.value.copy(isLoading = true)
        observeJob = scope.launch {
            timetableRepository.observeTimetableForTeacher(teacherId).collect { entries ->
                _state.value = TimetableState(entries = entries, isLoading = false)
            }
        }
    }

    fun onDestroy() = scope.cancel()
}
