import Foundation
import Combine

// Represents a user on the iOS side (mirrors KMP UserModel)
struct AttendifyUser: Identifiable {
    let id: String
    let name: String
    let email: String
    let role: UserRole
    let classId: String
    let departmentId: String
    let qrToken: String
    let classRollNo: String
    let universityRollNo: String
}

enum UserRole: String {
    case principal = "PRINCIPAL"
    case hod = "HOD"
    case classIncharge = "CLASS_INCHARGE"
    case teacher = "TEACHER"
    case student = "STUDENT"
    case classRepresentative = "CLASS_REPRESENTATIVE"
}

// AuthManager wraps KMP AuthViewModel for SwiftUI observation
@MainActor
class AuthManager: ObservableObject {
    @Published var currentUser: AttendifyUser? = nil
    @Published var isLoading: Bool = true
    @Published var error: String? = nil

    // In production, this would observe the KMP StateFlow
    // via a Flow-to-Combine bridge (e.g. using KMPNativeCoroutines)
    func login(email: String, password: String) async {
        isLoading = true
        error = nil
        do {
            // Call KMP AuthViewModel via bridge
            // let result = try await SharedKMPModule.shared.authViewModel.login(email: email, password: password)
            // currentUser = result.toSwift()
            try await Task.sleep(nanoseconds: 1_500_000_000) // Simulate network
            // Mock user for demo
            currentUser = AttendifyUser(
                id: "u1",
                name: "Demo User",
                email: email,
                role: .student,
                classId: "cls1",
                departmentId: "dept1",
                qrToken: "QR-TOKEN-UNIQUE-\(email)",
                classRollNo: "101",
                universityRollNo: "21CS0101"
            )
        } catch {
            self.error = error.localizedDescription
        }
        isLoading = false
    }

    func logout() {
        currentUser = nil
    }
}
