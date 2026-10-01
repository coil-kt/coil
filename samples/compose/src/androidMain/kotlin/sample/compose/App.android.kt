package sample.compose

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId

@OptIn(ExperimentalComposeUiApi::class)
@Stable
actual fun Modifier.testTagsAsResourceId(enable: Boolean): Modifier {
    return semantics {
        testTagsAsResourceId = enable
    }
}

@Composable
actual fun BackHandler(
    enabled: Boolean,
    onBack: () -> Unit,
) {
    BackHandler(enabled, onBack)
}
