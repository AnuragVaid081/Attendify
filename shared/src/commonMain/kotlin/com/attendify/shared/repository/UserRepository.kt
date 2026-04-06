package com.attendify.shared.repository

import com.attendify.shared.model.ClassModel
import com.attendify.shared.model.Department
import com.attendify.shared.model.UserModel
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    suspend fun getUserById(userId: String): UserModel?
    suspend fun getUserByQrToken(qrToken: String): UserModel?
    fun observeUser(userId: String): Flow<UserModel?>
    suspend fun getClassRoster(classId: String): List<UserModel>
    suspend fun getTeachersInDepartment(departmentId: String): List<UserModel>
    suspend fun getDepartment(departmentId: String): Department?
    suspend fun getClass(classId: String): ClassModel?
    suspend fun getAllClasses(departmentId: String): List<ClassModel>
    suspend fun updateUser(user: UserModel): Result<Unit>
}
