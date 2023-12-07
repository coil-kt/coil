package sample.compose

import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.CanvasBasedWindow
import androidx.compose.ui.window.Window
import coil3.PlatformContext
import coil3.SingletonImageLoader
import org.jetbrains.skiko.wasm.onWasmReady
import sample.common.JsResources
import sample.common.newImageLoader

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    SingletonImageLoader.set {
        newImageLoader(PlatformContext.INSTANCE)
    }

    onWasmReady {
        CanvasBasedWindow(Title) {
            Window(Title) {
                App(remember { JsResources() })
            }
        }
    }
}
