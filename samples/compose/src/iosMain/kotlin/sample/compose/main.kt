package sample.compose

import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController
import sample.common.AppleResources

fun MainViewController(): UIViewController {
    return ComposeUIViewController {
        App(remember { AppleResources() })
    }
}
