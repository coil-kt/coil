package sample.compose

import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.CanvasBasedWindow
import org.jetbrains.skiko.wasm.onWasmReady
import sample.common.JsResources

@OptIn(ExperimentalComposeUiApi::class)
fun main() = onWasmReady {
    CanvasBasedWindow(Title) {
        App(remember { JsResources() })
    }
}
