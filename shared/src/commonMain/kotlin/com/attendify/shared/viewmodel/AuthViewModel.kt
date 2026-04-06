package com.attendify.shared.viewmodel

import com.attendify.shared.model.UserModel
import com.attendify.shared.repository.AuthRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthState(
    val user: UserModel? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isAuthenticated: Boolean = false
)

class AuthViewModel(
    private val authRepository: AuthRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _state = MutableStateFlow(AuthState())
    val state: StateFlow<AuthState> = _state.asStateFlow()

    init {
        scope.launch {
            authRepository.currentUser.collect { user ->
                _state.value = _state.value.copy(
                    user = user,
                    isAuthenticated = user != null,
                    isLoading = false
                )
            }
        }
    }

    fun login(email: String, password: String) {
        scope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            val result = authRepository.login(email, password)
            if (result.isFailure) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = result.exceptionOrNull()?.message ?: "Login failed"
                )
            }
            // Success handled via currentUser flow
        }
    }

    fun logout() {
        scope.launch {
            authRepository.logout()
            _state.value = AuthState()
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    fun onDestroy() = scope.cancel()
}
