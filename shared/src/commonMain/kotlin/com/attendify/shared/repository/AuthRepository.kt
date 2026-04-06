package com.attendify.shared.repository

import com.attendify.shared.model.UserModel
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val currentUser: Flow<UserModel?>
    suspend fun login(email: String, password: String): Result<UserModel>
    suspend fun logout()
    suspend fun getCurrentUserProfile(): UserModel?
    suspend fun updateFcmToken(userId: String, token: String)
}
