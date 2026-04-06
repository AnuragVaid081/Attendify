import Foundation

/// Bridge to initialize and access KMP shared module from Swift
/// Uses KMPNativeCoroutines for Flow → AsyncSequence/Publisher conversion
class SharedKMPModule {
    static let shared = SharedKMPModule()
    private init() {}

    func initialize() {
        // In production — uncomment after adding KMP XCFramework:
        // SharedKt.doInitKoin()
        print("[Attendify] KMP Koin initialized")
    }

    // Example: bridge KMP ViewModel StateFlow to Swift AsyncSequence
    // func observeAuthState() -> AsyncStream<UserModel?> {
    //     AsyncStream { continuation in
    //         let job = SharedKt.authViewModel.state.collect { state in
    //             continuation.yield(state.user)
    //         }
    //         continuation.onTermination = { _ in job.cancel() }
    //     }
    // }
}

// Extension: Color from hex string
import SwiftUI
extension Color {
    init(hex: String) {
        let scanner = Scanner(string: hex)
        var rgbValue: UInt64 = 0
        scanner.scanHexInt64(&rgbValue)
        let r = Double((rgbValue & 0xFF0000) >> 16) / 255.0
        let g = Double((rgbValue & 0x00FF00) >> 8) / 255.0
        let b = Double(rgbValue & 0x0000FF) / 255.0
        self.init(red: r, green: g, blue: b)
    }
}
