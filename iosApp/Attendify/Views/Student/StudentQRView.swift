import SwiftUI
import CoreImage.CIFilterBuiltins

struct StudentQRView: View {
    let user: AttendifyUser
    @Environment(\.dismiss) var dismiss

    var qrImage: UIImage {
        generateQRCode(from: user.qrToken)
    }

    var body: some View {
        NavigationStack {
            ZStack {
                Color(hex: "0A0E1A").ignoresSafeArea()

                VStack(spacing: 24) {
                    Text("Show this QR to mark attendance")
                        .font(.system(size: 15))
                        .foregroundColor(Color(hex: "94A3B8"))

                    // QR code on white background
                    Image(uiImage: qrImage)
                        .interpolation(.none)
                        .resizable()
                        .scaledToFit()
                        .frame(width: 260, height: 260)
                        .padding(16)
                        .background(Color.white)
                        .clipShape(RoundedRectangle(cornerRadius: 20))

                    // Student info
                    VStack(spacing: 1) {
                        InfoRow(label: "Name", value: user.name)
                        InfoRow(label: "Class Roll No.", value: user.classRollNo)
                        InfoRow(label: "University Roll No.", value: user.universityRollNo)
                    }
                    .background(Color(hex: "162032"))
                    .clipShape(RoundedRectangle(cornerRadius: 16))
                    .padding(.horizontal)

                    Text("⚠ This QR is unique to you. Do not share it.")
                        .font(.system(size: 12))
                        .foregroundColor(Color(hex: "EF4444").opacity(0.8))
                        .multilineTextAlignment(.center)
                        .padding(.horizontal)
                }
                .padding(.top, 24)
            }
            .navigationTitle("My QR Code")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Done") { dismiss() }
                }
            }
        }
    }

    private func generateQRCode(from string: String) -> UIImage {
        let context = CIContext()
        let filter = CIFilter.qrCodeGenerator()
        filter.message = Data(string.utf8)
        filter.correctionLevel = "H"
        if let outputImage = filter.outputImage,
           let cgImage = context.createCGImage(outputImage, from: outputImage.extent) {
            return UIImage(cgImage: cgImage)
        }
        return UIImage(systemName: "xmark.circle") ?? UIImage()
    }
}

struct InfoRow: View {
    let label: String
    let value: String

    var body: some View {
        HStack {
            Text(label)
                .font(.system(size: 13))
                .foregroundColor(Color(hex: "94A3B8"))
            Spacer()
            Text(value)
                .font(.system(size: 14, weight: .medium))
                .foregroundColor(.white)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 10)
        .overlay(Divider().opacity(0.3), alignment: .bottom)
    }
}
