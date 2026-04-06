import SwiftUI
import AVFoundation

struct QRScannerView: View {
    let onQRScanned: (String) -> Void
    @Environment(\.dismiss) var dismiss
    @State private var lastScanned: String? = nil
    @State private var feedback: ScanFeedback? = nil

    enum ScanFeedback { case success(String); case error(String) }

    var body: some View {
        NavigationStack {
            ZStack {
                CameraPreviewRepresentable(onQRDetected: { value in
                    guard value != lastScanned else { return }
                    lastScanned = value
                    feedback = .success("Attendance marked!")
                    onQRScanned(value)
                    // Reset after 2 seconds
                    DispatchQueue.main.asyncAfter(deadline: .now() + 2) {
                        lastScanned = nil
                        feedback = nil
                    }
                })
                .ignoresSafeArea()

                // Scanning frame overlay
                VStack {
                    Spacer()
                    RoundedRectangle(cornerRadius: 16)
                        .stroke(Color(hex: "1A73E8"), lineWidth: 2)
                        .frame(width: 240, height: 240)
                    Text("Point at student QR code")
                        .font(.system(size: 15))
                        .foregroundColor(.white)
                        .padding(.top, 16)
                    Spacer()
                }

                // Feedback feedback
                if let fb = feedback {
                    VStack {
                        Spacer()
                        HStack(spacing: 8) {
                            Image(systemName: {
                                if case .success = fb { return "checkmark.circle.fill" }
                                return "exclamationmark.circle.fill"
                            }())
                            .foregroundColor(.white)
                            Text({
                                if case .success(let msg) = fb { return msg }
                                if case .error(let msg) = fb { return msg }
                                return ""
                            }())
                            .foregroundColor(.white)
                            .font(.system(size: 15, weight: .medium))
                        }
                        .padding(14)
                        .background(
                            (feedback.map { fb in
                                if case .success = fb { return Color(hex: "10B981") }
                                return Color(hex: "EF4444")
                            } ?? Color.gray).opacity(0.9)
                        )
                        .clipShape(RoundedRectangle(cornerRadius: 12))
                        .padding()
                        .transition(.move(edge: .bottom).combined(with: .opacity))
                    }
                    .animation(.spring(), value: feedback != nil)
                }
            }
            .navigationTitle("Scan Attendance")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Close") { dismiss() }
                }
            }
        }
    }
}

// ─── AVFoundation Camera Preview ─────────────────────────────────────────────

struct CameraPreviewRepresentable: UIViewControllerRepresentable {
    let onQRDetected: (String) -> Void

    func makeUIViewController(context: Context) -> QRScannerViewController {
        let vc = QRScannerViewController()
        vc.onQRDetected = onQRDetected
        return vc
    }

    func updateUIViewController(_ uiViewController: QRScannerViewController, context: Context) {}
}

class QRScannerViewController: UIViewController, AVCaptureMetadataOutputObjectsDelegate {
    var captureSession: AVCaptureSession!
    var onQRDetected: ((String) -> Void)?

    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = .black
        captureSession = AVCaptureSession()
        guard let device = AVCaptureDevice.default(for: .video),
              let input = try? AVCaptureDeviceInput(device: device) else { return }
        captureSession.addInput(input)

        let output = AVCaptureMetadataOutput()
        captureSession.addOutput(output)
        output.setMetadataObjectsDelegate(self, queue: .main)
        output.metadataObjectTypes = [.qr]

        let preview = AVCaptureVideoPreviewLayer(session: captureSession)
        preview.frame = view.layer.bounds
        preview.videoGravity = .resizeAspectFill
        view.layer.addSublayer(preview)

        DispatchQueue.global(qos: .background).async {
            self.captureSession.startRunning()
        }
    }

    func metadataOutput(_ output: AVCaptureMetadataOutput, didOutput metadataObjects: [AVMetadataObject], from connection: AVCaptureConnection) {
        guard let obj = metadataObjects.first as? AVMetadataMachineReadableCodeObject,
              let value = obj.stringValue else { return }
        onQRDetected?(value)
    }

    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        if captureSession?.isRunning == true { captureSession.stopRunning() }
    }
}
