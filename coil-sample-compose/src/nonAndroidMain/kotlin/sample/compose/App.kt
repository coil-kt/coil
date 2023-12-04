package sample.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.IntSize

@Stable
actual fun Modifier.testTagsAsResourceId(enable: Boolean): Modifier {
    // Only supported on Android.
    return this
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
actual fun containerSize(): IntSize {
    return LocalWindowInfo.current.containerSize
}

@Composable
actual fun BackHandler(
    enabled: Boolean,
    onBack: () -> Unit,
) { /* Do nothing. */ }
