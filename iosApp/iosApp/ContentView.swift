import UIKit
import SwiftUI
import Shared

/// Hosts the shared Compose Multiplatform UI inside SwiftUI.
struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        // `MainViewController.kt` -> `MainViewControllerKt.MainViewController()`
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    var body: some View {
        ComposeView()
            .ignoresSafeArea()
    }
}
