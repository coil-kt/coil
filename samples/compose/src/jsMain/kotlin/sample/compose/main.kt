package sample.compose

import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.CanvasBasedWindow
import kotlinx.browser.window
import org.jetbrains.skiko.wasm.onWasmReady
import sample.common.JsResources

@OptIn(ExperimentalComposeUiApi::class)
fun main() = onReady {
    initializeSingletonImageLoader()

    CanvasBasedWindow(Title) {
        App(remember { JsResources() })
    }
}

private fun onReady(callback: () -> Unit) {
    window.addEventListener(
        type = "load",
        callback = {
            onWasmReady(callback)
        },
    )
}
