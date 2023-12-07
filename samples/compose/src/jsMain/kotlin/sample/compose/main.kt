package sample.compose

import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.CanvasBasedWindow
import kotlinx.browser.window
import sample.common.JsResources

@OptIn(ExperimentalComposeUiApi::class)
fun main() = onLoad {
    initializeSingletonImageLoader()

    CanvasBasedWindow(Title) {
        App(remember { JsResources() })
    }
}

private fun onLoad(callback: () -> Unit) {
    window.addEventListener(
        type = "load",
        callback = { callback() },
    )
}
