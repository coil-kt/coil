package sample.compose

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import coil3.PlatformContext
import coil3.SingletonImageLoader
import sample.common.AppleResources
import sample.common.MainViewModel
import sample.common.newImageLoader

fun MainViewController() = ComposeUIViewController {
    LaunchedEffect(Unit) {
        SingletonImageLoader.set {
            newImageLoader(PlatformContext.INSTANCE)
        }
    }

    val viewModel = remember { MainViewModel(AppleResources()) }
    LaunchedEffect(viewModel) {
        viewModel.start()
    }

    App(viewModel)
}
