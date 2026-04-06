import SwiftUI
import AVFoundation

struct StudentDashboardView: View {
    let user: AttendifyUser
    @State private var showQR = false

    var body: some View {
        NavigationStack {
            ZStack {
                Color(hex: "0A0E1A").ignoresSafeArea()

                ScrollView {
                    VStack(spacing: 20) {
                        // Attendance banner
                        AttendanceBannerCard(percentage: 82)

                        // Stats row
                        HStack(spacing: 12) {
                            StatCard(label: "Subjects", value: "6", icon: "book.fill", color: Color(hex: "1A73E8"))
                            StatCard(label: "Today's Classes", value: "4", icon: "calendar", color: Color(hex: "00BFA5"))
                        }

                        // Subject breakdown
                        VStack(alignment: .leading, spacing: 12) {
                            Text("Attendance Breakdown")
                                .font(.system(size: 17, weight: .semibold))
                                .foregroundColor(.white)

                            ForEach(sampleSubjects, id: \.subject) { item in
                                SubjectAttendanceRow(subject: item.subject, pct: item.pct, attended: item.attended, total: item.total)
                            }
                        }
                    }
                    .padding(16)
                }
            }
            .navigationTitle("Dashboard")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(action: { showQR = true }) {
                        Image(systemName: "qrcode")
                            .foregroundColor(Color(hex: "1A73E8"))
                    }
                }
            }
            .sheet(isPresented: $showQR) {
                StudentQRView(user: user)
            }
        }
    }
}

// ─── Sub-components ───────────────────────────────────────────────────────────

struct AttendanceBannerCard: View {
    let percentage: Int

    var body: some View {
        RoundedRectangle(cornerRadius: 20)
            .fill(LinearGradient(
                colors: [Color(hex: "0D47A1"), Color(hex: "1A73E8")],
                startPoint: .leading, endPoint: .trailing
            ))
            .frame(height: 130)
            .overlay(
                HStack {
                    VStack(alignment: .leading, spacing: 4) {
                        Text("Overall Attendance")
                            .font(.system(size: 13))
                            .foregroundColor(.white.opacity(0.8))
                        Text("\(percentage)%")
                            .font(.system(size: 48, weight: .bold))
                            .foregroundColor(.white)
                    }
                    Spacer()
                    ZStack {
                        Circle()
                            .stroke(Color.white.opacity(0.2), lineWidth: 7)
                            .frame(width: 80, height: 80)
                        Circle()
                            .trim(from: 0, to: CGFloat(percentage) / 100)
                            .stroke(Color(hex: "00BFA5"), style: StrokeStyle(lineWidth: 7, lineCap: .round))
                            .frame(width: 80, height: 80)
                            .rotationEffect(.degrees(-90))
                        Text("\(percentage)%")
                            .font(.system(size: 12, weight: .semibold))
                            .foregroundColor(.white)
                    }
                }
                .padding(20)
            )
    }
}

struct StatCard: View {
    let label: String
    let value: String
    let icon: String
    let color: Color

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Image(systemName: icon).foregroundColor(color).font(.system(size: 18))
            Text(value)
                .font(.system(size: 28, weight: .bold))
                .foregroundColor(.white)
            Text(label)
                .font(.system(size: 12))
                .foregroundColor(Color(hex: "94A3B8"))
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(color.opacity(0.12))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }
}

struct SubjectAttendanceRow: View {
    let subject: String
    let pct: Double
    let attended: Int
    let total: Int

    var barColor: Color {
        pct >= 75 ? Color(hex: "10B981") : pct >= 60 ? Color(hex: "F59E0B") : Color(hex: "EF4444")
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack {
                Text(subject)
                    .font(.system(size: 15, weight: .medium))
                    .foregroundColor(.white)
                Spacer()
                Text("\(Int(pct))%")
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundColor(barColor)
            }
            GeometryReader { geo in
                ZStack(alignment: .leading) {
                    RoundedRectangle(cornerRadius: 3).fill(barColor.opacity(0.15)).frame(height: 6)
                    RoundedRectangle(cornerRadius: 3).fill(barColor)
                        .frame(width: geo.size.width * pct / 100, height: 6)
                }
            }
            .frame(height: 6)
            Text("\(attended)/\(total) lectures attended")
                .font(.system(size: 11))
                .foregroundColor(Color(hex: "94A3B8"))
        }
        .padding(14)
        .background(Color(hex: "162032"))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }
}

// ─── Sample data ──────────────────────────────────────────────────────────────

struct SubjectItem { let subject: String; let pct: Double; let attended: Int; let total: Int }
let sampleSubjects = [
    SubjectItem(subject: "Data Structures", pct: 91, attended: 29, total: 32),
    SubjectItem(subject: "Operating Systems", pct: 78, attended: 25, total: 32),
    SubjectItem(subject: "Computer Networks", pct: 65, attended: 21, total: 32),
    SubjectItem(subject: "DBMS", pct: 88, attended: 28, total: 32),
    SubjectItem(subject: "Software Engineering", pct: 72, attended: 23, total: 32),
]
