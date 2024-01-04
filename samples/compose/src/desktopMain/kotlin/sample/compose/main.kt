package sample.compose

import androidx.compose.runtime.remember
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import sample.common.JvmResources

fun main() {
    application {
        Window(
            title = Title,
            onCloseRequest = ::exitApplication,
        ) {
            App(remember { JvmResources() })
        }
    }
}
