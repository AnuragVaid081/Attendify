// swift-tools-version:5.9
import PackageDescription

let package = Package(
    name: "AttendifyiOS",
    platforms: [.iOS(.v16)],
    products: [
        .library(name: "AttendifyiOS", targets: ["AttendifyiOS"])
    ],
    targets: [
        .target(
            name: "AttendifyiOS",
            path: "Attendify",
            exclude: ["Info.plist"]
        )
    ]
)
