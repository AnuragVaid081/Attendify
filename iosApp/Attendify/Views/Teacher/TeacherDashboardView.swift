import SwiftUI
import AVFoundation

struct TeacherDashboardView: View {
    let user: AttendifyUser
    @State private var showScanner = false

    var body: some View {
        NavigationStack {
            ZStack {
                Color(hex: "0A0E1A").ignoresSafeArea()

                ScrollView {
                    VStack(spacing: 20) {
                        // Quick actions
                        HStack(spacing: 12) {
                            TeacherActionCard(
                                title: "Start Session",
                                subtitle: "Initiate attendance",
                                icon: "play.fill",
                                color: Color(hex: "10B981")
                            ) { /* create session */ }

                            TeacherActionCard(
                                title: "Scan QR",
                                subtitle: "Mark attendance",
                                icon: "qrcode.viewfinder",
                                color: Color(hex: "1A73E8")
                            ) { showScanner = true }
                        }

                        // Stats
                        HStack(spacing: 12) {
                            StatCard(label: "Today's Classes", value: "3", icon: "calendar", color: Color(hex: "1A73E8"))
                            StatCard(label: "Sessions Done", value: "12", icon: "checkmark.circle", color: Color(hex: "10B981"))
                        }

                        // Recent sessions
                        VStack(alignment: .leading, spacing: 12) {
                            Text("Recent Sessions")
                                .font(.system(size: 17, weight: .semibold))
                                .foregroundColor(.white)

                            ForEach(0..<3) { i in
                                SessionRow(date: "2026-04-0\(6-i)", count: 30 - i, status: i == 0 ? "Active" : "Locked")
                            }
                        }
                    }
                    .padding(16)
                }
            }
            .navigationTitle("Teacher Panel")
            .navigationBarTitleDisplayMode(.inline)
            .sheet(isPresented: $showScanner) {
                QRScannerView(onQRScanned: { value in
                    print("Scanned: \(value)")
                    showScanner = false
                })
            }
        }
    }
}

struct TeacherActionCard: View {
    let title: String
    let subtitle: String
    let icon: String
    let color: Color
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            VStack(alignment: .leading, spacing: 8) {
                Image(systemName: icon).foregroundColor(color).font(.system(size: 22))
                Text(title).font(.system(size: 15, weight: .semibold)).foregroundColor(.white)
                Text(subtitle).font(.system(size: 12)).foregroundColor(Color(hex: "94A3B8"))
            }
            .padding(16)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(color.opacity(0.12))
            .clipShape(RoundedRectangle(cornerRadius: 16))
        }
    }
}

struct SessionRow: View {
    let date: String
    let count: Int
    let status: String

    var statusColor: Color {
        status == "Active" ? Color(hex: "10B981") : Color(hex: "6B7280")
    }

    var body: some View {
        HStack {
            VStack(alignment: .leading, spacing: 4) {
                Text(date).font(.system(size: 14, weight: .medium)).foregroundColor(.white)
                Text("\(count) students marked").font(.system(size: 12)).foregroundColor(Color(hex: "94A3B8"))
            }
            Spacer()
            Text(status)
                .font(.system(size: 12, weight: .semibold))
                .foregroundColor(statusColor)
                .padding(.horizontal, 10)
                .padding(.vertical, 4)
                .background(statusColor.opacity(0.15))
                .clipShape(Capsule())
        }
        .padding(14)
        .background(Color(hex: "162032"))
        .clipShape(RoundedRectangle(cornerRadius: 14))
    }
}
