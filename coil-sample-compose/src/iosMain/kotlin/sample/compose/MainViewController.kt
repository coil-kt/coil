package sample.compose

import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import androidx.compose.ui.window.Window
import coil3.PlatformContext
import coil3.SingletonImageLoader
import platform.UIKit.UIViewController
import sample.common.AppleResources
import sample.common.newImageLoader

fun MainViewController(): UIViewController {
    SingletonImageLoader.set {
        newImageLoader(PlatformContext.INSTANCE)
    }

    return ComposeUIViewController {
        Window(Title) {
            App(remember { AppleResources() })
        }
    }
}
