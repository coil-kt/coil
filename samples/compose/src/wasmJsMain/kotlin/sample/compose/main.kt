package sample.compose

import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.CanvasBasedWindow
import kotlinx.browser.window
import sample.common.WasmJsResources

@OptIn(ExperimentalComposeUiApi::class)
fun main() = onReady {
    initializeSingletonImageLoader()

    CanvasBasedWindow(Title) {
        App(remember { WasmJsResources() })
    }
}

private fun onReady(callback: () -> Unit) {
    window.addEventListener(
        type = "load",
        callback = {
            callback()
        },
    )
}
