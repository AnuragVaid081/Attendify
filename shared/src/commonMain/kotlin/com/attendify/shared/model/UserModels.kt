package com.attendify.shared.model

import kotlinx.serialization.Serializable

enum class UserRole {
    PRINCIPAL,
    HOD,
    CLASS_INCHARGE,
    TEACHER,
    STUDENT,
    CLASS_REPRESENTATIVE
}

@Serializable
data class UserModel(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val role: String = UserRole.STUDENT.name,
    val departmentId: String = "",
    val classId: String = "",
    val subjectIds: List<String> = emptyList(),  // For teachers
    val classRollNo: String = "",
    val universityRollNo: String = "",
    val qrToken: String = "",                     // Unique QR secret
    val fcmToken: String = "",
    val profileImageUrl: String = "",
    val isActive: Boolean = true
) {
    val userRole: UserRole get() = UserRole.valueOf(role)
}

@Serializable
data class Department(
    val id: String = "",
    val name: String = "",
    val hodId: String = "",
    val code: String = ""
)

@Serializable
data class ClassModel(
    val id: String = "",
    val name: String = "",              // e.g. "CSE-A 3rd Year"
    val departmentId: String = "",
    val semester: Int = 1,
    val section: String = "",
    val inchargeId: String = "",
    val representativeIds: List<String> = emptyList(),  // Max 2
    val studentIds: List<String> = emptyList()
)

@Serializable
data class SubjectModel(
    val id: String = "",
    val name: String = "",
    val code: String = "",
    val departmentId: String = "",
    val creditHours: Int = 3
)
