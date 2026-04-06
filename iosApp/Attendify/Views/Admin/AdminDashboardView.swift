import SwiftUI

struct AdminDashboardView: View {
    let user: AttendifyUser

    var body: some View {
        NavigationStack {
            ZStack {
                Color(hex: "0A0E1A").ignoresSafeArea()

                ScrollView {
                    VStack(spacing: 20) {
                        // Role badge
                        HStack {
                            Text(user.role.rawValue.replacingOccurrences(of: "_", with: " "))
                                .font(.system(size: 12, weight: .semibold))
                                .foregroundColor(Color(hex: "1A73E8"))
                                .padding(.horizontal, 10)
                                .padding(.vertical, 4)
                                .background(Color(hex: "1A73E8").opacity(0.15))
                                .clipShape(Capsule())
                            Spacer()
                        }

                        // Stats
                        HStack(spacing: 12) {
                            StatCard(label: "Total Students", value: "480", icon: "person.3.fill", color: Color(hex: "1A73E8"))
                            StatCard(label: "Teachers", value: "24", icon: "person.fill", color: Color(hex: "00BFA5"))
                        }

                        HStack(spacing: 12) {
                            StatCard(label: "Classes", value: "8", icon: "building.2.fill", color: Color(hex: "FF7043"))
                            StatCard(label: "Subjects", value: "32", icon: "book.fill", color: Color(hex: "6366F1"))
                        }

                        // Quick actions
                        VStack(alignment: .leading, spacing: 12) {
                            Text("Quick Actions")
                                .font(.system(size: 17, weight: .semibold))
                                .foregroundColor(.white)

                            HStack(spacing: 12) {
                                AdminActionCard(title: "Timetable", icon: "table", color: Color(hex: "1A73E8"))
                                AdminActionCard(title: "Reports", icon: "chart.bar.fill", color: Color(hex: "00BFA5"))
                            }
                            HStack(spacing: 12) {
                                AdminActionCard(title: "Students", icon: "person.crop.circle.fill", color: Color(hex: "FF7043"))
                                AdminActionCard(title: "Notify All", icon: "bell.fill", color: Color(hex: "6366F1"))
                            }
                        }
                    }
                    .padding(16)
                }
            }
            .navigationTitle("Admin Dashboard")
            .navigationBarTitleDisplayMode(.inline)
        }
    }
}

struct AdminActionCard: View {
    let title: String
    let icon: String
    let color: Color

    var body: some View {
        Button(action: {}) {
            HStack(spacing: 10) {
                Image(systemName: icon).foregroundColor(color).font(.system(size: 18))
                Text(title).font(.system(size: 15, weight: .medium)).foregroundColor(.white)
                Spacer()
            }
            .padding(14)
            .background(color.opacity(0.1))
            .clipShape(RoundedRectangle(cornerRadius: 14))
            .overlay(
                RoundedRectangle(cornerRadius: 14)
                    .stroke(color.opacity(0.2), lineWidth: 1)
            )
        }
    }
}
