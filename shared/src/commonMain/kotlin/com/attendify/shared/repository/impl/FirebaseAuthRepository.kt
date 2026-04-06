package com.attendify.shared.repository.impl

import com.attendify.shared.model.UserModel
import com.attendify.shared.repository.AuthRepository
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

class FirebaseAuthRepository : AuthRepository {

    private val auth = Firebase.auth
    private val firestore = Firebase.firestore

    override val currentUser: Flow<UserModel?> = flow {
        auth.authStateChanged.collect { firebaseUser ->
            if (firebaseUser != null) {
                val profile = getUserProfile(firebaseUser.uid)
                emit(profile)
            } else {
                emit(null)
            }
        }
    }

    override suspend fun login(email: String, password: String): Result<UserModel> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password)
            val uid = result.user?.uid ?: throw Exception("UID is null after login")
            val profile = getUserProfile(uid) ?: throw Exception("User profile not found")
            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun logout() {
        auth.signOut()
    }

    override suspend fun getCurrentUserProfile(): UserModel? {
        val uid = auth.currentUser?.uid ?: return null
        return getUserProfile(uid)
    }

    override suspend fun updateFcmToken(userId: String, token: String) {
        firestore.collection("users").document(userId)
            .update("fcmToken" to token)
    }

    private suspend fun getUserProfile(uid: String): UserModel? {
        return try {
            val snapshot = firestore.collection("users").document(uid).get()
            if (snapshot.exists) snapshot.data<UserModel>() else null
        } catch (e: Exception) {
            null
        }
    }
}
