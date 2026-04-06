package com.attendify.shared.repository.impl

import com.attendify.shared.model.ClassModel
import com.attendify.shared.model.Department
import com.attendify.shared.model.UserModel
import com.attendify.shared.repository.UserRepository
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FirebaseUserRepository : UserRepository {

    private val db = Firebase.firestore
    private val users = db.collection("users")
    private val departments = db.collection("departments")
    private val classes = db.collection("classes")

    override suspend fun getUserById(userId: String): UserModel? = try {
        val doc = users.document(userId).get()
        if (doc.exists) doc.data<UserModel>() else null
    } catch (e: Exception) { null }

    override suspend fun getUserByQrToken(qrToken: String): UserModel? = try {
        val result = users.where { "qrToken" equalTo qrToken }.get()
        result.documents.firstOrNull()?.data<UserModel>()
    } catch (e: Exception) { null }

    override fun observeUser(userId: String): Flow<UserModel?> =
        users.document(userId).snapshots.map { snapshot ->
            if (snapshot.exists) snapshot.data<UserModel>() else null
        }

    override suspend fun getClassRoster(classId: String): List<UserModel> = try {
        users.where { "classId" equalTo classId }
            .where { "role" equalTo "STUDENT" }
            .get()
            .documents.map { it.data<UserModel>() }
            .sortedBy { it.classRollNo }
    } catch (e: Exception) { emptyList() }

    override suspend fun getTeachersInDepartment(departmentId: String): List<UserModel> = try {
        users.where { "departmentId" equalTo departmentId }
            .where { "role" equalTo "TEACHER" }
            .get()
            .documents.map { it.data<UserModel>() }
    } catch (e: Exception) { emptyList() }

    override suspend fun getDepartment(departmentId: String): Department? = try {
        val doc = departments.document(departmentId).get()
        if (doc.exists) doc.data<Department>() else null
    } catch (e: Exception) { null }

    override suspend fun getClass(classId: String): ClassModel? = try {
        val doc = classes.document(classId).get()
        if (doc.exists) doc.data<ClassModel>() else null
    } catch (e: Exception) { null }

    override suspend fun getAllClasses(departmentId: String): List<ClassModel> = try {
        classes.where { "departmentId" equalTo departmentId }
            .get()
            .documents.map { it.data<ClassModel>() }
    } catch (e: Exception) { emptyList() }

    override suspend fun updateUser(user: UserModel): Result<Unit> = try {
        users.document(user.id).set(user)
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
