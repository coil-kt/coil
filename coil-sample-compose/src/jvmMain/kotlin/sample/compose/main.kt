package sample.compose

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import sample.common.JvmResources
import sample.common.MainViewModel

fun main() = application {
    val viewModel = MainViewModel(JvmResources())
    Window(
        title = "Coil",
        onCloseRequest = ::exitApplication,
    ) {
        App(viewModel)
    }
}
