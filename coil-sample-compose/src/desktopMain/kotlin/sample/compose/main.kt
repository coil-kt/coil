package sample.compose

import androidx.compose.runtime.remember
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import coil3.PlatformContext
import coil3.SingletonImageLoader
import sample.common.JvmResources
import sample.common.newImageLoader

fun main() {
    SingletonImageLoader.set {
        newImageLoader(PlatformContext.INSTANCE)
    }

    application {
        Window(
            title = Title,
            onCloseRequest = ::exitApplication,
        ) {
            App(remember { JvmResources() })
        }
    }
}
