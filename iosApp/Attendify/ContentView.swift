import SwiftUI

struct ContentView: View {
    @EnvironmentObject var authManager: AuthManager

    var body: some View {
        Group {
            if authManager.isLoading {
                SplashView()
            } else if authManager.currentUser == nil {
                LoginView()
            } else {
                RoleBasedDashboard(user: authManager.currentUser!)
            }
        }
        .animation(.easeInOut(duration: 0.3), value: authManager.currentUser?.id)
    }
}

struct RoleBasedDashboard: View {
    let user: AttendifyUser

    var body: some View {
        switch user.role {
        case .student, .classRepresentative:
            StudentDashboardView(user: user)
        case .teacher, .classIncharge:
            TeacherDashboardView(user: user)
        case .hod, .principal:
            AdminDashboardView(user: user)
        }
    }
}

struct SplashView: View {
    var body: some View {
        ZStack {
            Color(hex: "0A0E1A").ignoresSafeArea()
            VStack(spacing: 16) {
                Text("A")
                    .font(.system(size: 64, weight: .bold))
                    .foregroundColor(Color(hex: "1A73E8"))
                    .frame(width: 96, height: 96)
                    .background(Color(hex: "1A73E8").opacity(0.15))
                    .clipShape(RoundedRectangle(cornerRadius: 24))
                Text("Attendify")
                    .font(.system(size: 32, weight: .bold))
                    .foregroundColor(.white)
                ProgressView()
                    .progressViewStyle(CircularProgressViewStyle(tint: Color(hex: "1A73E8")))
                    .scaleEffect(1.2)
            }
        }
    }
}
