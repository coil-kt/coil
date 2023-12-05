package sample.common

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.CanvasBasedWindow
import androidx.compose.ui.window.Window
import coil3.PlatformContext
import coil3.SingletonImageLoader
import org.jetbrains.skiko.wasm.onWasmReady
import sample.compose.App

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    SingletonImageLoader.set {
        newImageLoader(PlatformContext.INSTANCE)
    }

    onWasmReady {
        CanvasBasedWindow("Coil") {
            val viewModel = remember { MainViewModel(JsResources()) }
            LaunchedEffect(viewModel) {
                viewModel.start()
            }

            Window("Coil") {
                App(viewModel)
            }
        }
    }
}
