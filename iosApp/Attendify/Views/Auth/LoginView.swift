import SwiftUI

struct LoginView: View {
    @EnvironmentObject var authManager: AuthManager

    @State private var email = ""
    @State private var password = ""
    @State private var passwordVisible = false

    var body: some View {
        ZStack {
            // Gradient background
            LinearGradient(
                colors: [Color(hex: "0A0E1A"), Color(hex: "0D1B35"), Color(hex: "111827")],
                startPoint: .top, endPoint: .bottom
            )
            .ignoresSafeArea()

            // Decorative circle
            Circle()
                .fill(Color(hex: "1A73E8").opacity(0.12))
                .frame(width: 320, height: 320)
                .offset(x: -100, y: -200)
                .blur(radius: 40)

            ScrollView {
                VStack(spacing: 32) {
                    Spacer().frame(height: 60)

                    // Brand logo
                    VStack(spacing: 12) {
                        Text("A")
                            .font(.system(size: 48, weight: .bold))
                            .foregroundColor(Color(hex: "1A73E8"))
                            .frame(width: 80, height: 80)
                            .background(Color(hex: "1A73E8").opacity(0.15))
                            .clipShape(RoundedRectangle(cornerRadius: 20))

                        Text("Attendify")
                            .font(.system(size: 28, weight: .bold))
                            .foregroundColor(.white)

                        Text("Smart Attendance for Modern Colleges")
                            .font(.system(size: 14))
                            .foregroundColor(Color(hex: "CBD5E1"))
                            .multilineTextAlignment(.center)
                    }

                    // Login card
                    VStack(alignment: .leading, spacing: 16) {
                        Text("Sign In")
                            .font(.system(size: 20, weight: .semibold))
                            .foregroundColor(.white)

                        Text("Use your institutional credentials")
                            .font(.system(size: 13))
                            .foregroundColor(Color(hex: "94A3B8"))

                        // Email field
                        AttendifyTextField(
                            placeholder: "Email",
                            text: $email,
                            icon: "envelope.fill",
                            keyboardType: .emailAddress
                        )

                        // Password field
                        AttendifySecureField(
                            placeholder: "Password",
                            text: $password,
                            isVisible: $passwordVisible
                        )

                        if let error = authManager.error {
                            Text(error)
                                .font(.system(size: 13))
                                .foregroundColor(Color(hex: "EF4444"))
                        }

                        Button(action: {
                            Task { await authManager.login(email: email, password: password) }
                        }) {
                            ZStack {
                                RoundedRectangle(cornerRadius: 14)
                                    .fill(Color(hex: "1A73E8"))
                                    .frame(height: 52)
                                if authManager.isLoading {
                                    ProgressView()
                                        .progressViewStyle(CircularProgressViewStyle(tint: .white))
                                } else {
                                    Text("Sign In")
                                        .font(.system(size: 16, weight: .semibold))
                                        .foregroundColor(.white)
                                }
                            }
                        }
                        .disabled(authManager.isLoading || email.isEmpty || password.isEmpty)
                    }
                    .padding(24)
                    .background(Color(hex: "162032"))
                    .clipShape(RoundedRectangle(cornerRadius: 24))
                    .padding(.horizontal, 24)

                    Text("Contact your administrator for access")
                        .font(.system(size: 13))
                        .foregroundColor(Color(hex: "6B7280"))

                    Spacer().frame(height: 32)
                }
            }
        }
    }
}

// ─── Reusable SwiftUI form components ────────────────────────────────────────

struct AttendifyTextField: View {
    let placeholder: String
    @Binding var text: String
    let icon: String
    var keyboardType: UIKeyboardType = .default

    var body: some View {
        HStack(spacing: 10) {
            Image(systemName: icon)
                .foregroundColor(Color(hex: "1A73E8"))
                .frame(width: 20)
            TextField(placeholder, text: $text)
                .keyboardType(keyboardType)
                .autocapitalization(.none)
                .foregroundColor(.white)
        }
        .padding(14)
        .background(Color(hex: "1E2A3D"))
        .clipShape(RoundedRectangle(cornerRadius: 12))
        .overlay(
            RoundedRectangle(cornerRadius: 12)
                .stroke(Color(hex: "1A73E8").opacity(0.4), lineWidth: 1)
        )
    }
}

struct AttendifySecureField: View {
    let placeholder: String
    @Binding var text: String
    @Binding var isVisible: Bool

    var body: some View {
        HStack(spacing: 10) {
            Image(systemName: "lock.fill")
                .foregroundColor(Color(hex: "1A73E8"))
                .frame(width: 20)
            if isVisible {
                TextField(placeholder, text: $text)
                    .foregroundColor(.white)
            } else {
                SecureField(placeholder, text: $text)
                    .foregroundColor(.white)
            }
            Button(action: { isVisible.toggle() }) {
                Image(systemName: isVisible ? "eye.slash" : "eye")
                    .foregroundColor(Color(hex: "9CA3AF"))
            }
        }
        .padding(14)
        .background(Color(hex: "1E2A3D"))
        .clipShape(RoundedRectangle(cornerRadius: 12))
        .overlay(
            RoundedRectangle(cornerRadius: 12)
                .stroke(Color(hex: "1A73E8").opacity(0.4), lineWidth: 1)
        )
    }
}
