import SwiftUI

// Entry point for the iOS Attendify app
@main
struct AttendifyApp: App {

    @StateObject private var authManager = AuthManager()

    init() {
        // Firebase is initialized via GoogleService-Info.plist automatically
        // KMP Koin initialization
        SharedKMPModule.shared.initialize()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(authManager)
                .preferredColorScheme(.dark)
        }
    }
}
