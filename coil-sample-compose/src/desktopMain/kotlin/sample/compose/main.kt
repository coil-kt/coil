package sample.compose

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import coil3.PlatformContext
import coil3.SingletonImageLoader
import sample.common.JvmResources
import sample.common.MainViewModel
import sample.common.newImageLoader

fun main() {
    SingletonImageLoader.set {
        newImageLoader(PlatformContext.INSTANCE)
    }

    application {
        val viewModel = remember { MainViewModel(JvmResources()) }
        LaunchedEffect(viewModel) {
            viewModel.start()
        }

        Window(
            title = "Coil",
            onCloseRequest = ::exitApplication,
        ) {
            App(viewModel)
        }
    }
}
