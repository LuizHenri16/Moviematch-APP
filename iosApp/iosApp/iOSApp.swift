import SwiftUI
import Shared

@main
struct MovieMatchApp: App {
    init() {
        // Starts Koin once for the whole app. Kotlin file `di/Koin.kt` -> `KoinKt` in Swift.
        KoinKt.doInitKoin()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
