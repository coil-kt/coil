package sample.compose

import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.CanvasBasedWindow
import sample.common.WasmJsResources

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    initializeSingletonImageLoader()

    CanvasBasedWindow(Title) {
        App(remember { WasmJsResources() })
    }
}
