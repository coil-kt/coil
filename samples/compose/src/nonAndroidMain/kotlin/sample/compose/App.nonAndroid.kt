package sample.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier

@Stable
actual fun Modifier.testTagsAsResourceId(enable: Boolean): Modifier {
    // Only supported on Android.
    return this
}

@Composable
actual fun BackHandler(
    enabled: Boolean,
    onBack: () -> Unit,
) { /* Do nothing. */ }
