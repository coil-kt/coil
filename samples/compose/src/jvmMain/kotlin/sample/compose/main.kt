package sample.compose

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() {
    application {
        Window(
            title = Title,
            onCloseRequest = ::exitApplication,
        ) {
            App()
        }
    }
}
