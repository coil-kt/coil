package sample.compose

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import coil.PlatformContext
import coil.SingletonImageLoader
import sample.common.JvmResources
import sample.common.MainViewModel
import sample.common.newImageLoader

fun main() = application {
    val viewModel = MainViewModel(JvmResources())
    LaunchedEffect(Unit) {
        SingletonImageLoader.set {
            newImageLoader(PlatformContext.INSTANCE, false)
        }
        viewModel.start()
    }
    Window(
        title = "Coil",
        onCloseRequest = ::exitApplication,
    ) {
        App(viewModel)
    }
}
