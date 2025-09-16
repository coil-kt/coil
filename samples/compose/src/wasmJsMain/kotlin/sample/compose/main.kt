package sample.compose

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    document.title = Title
    ComposeViewport("ComposeTarget") {
        App()
    }
}
