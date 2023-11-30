package sample.compose

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

@Composable
actual fun containerSize(): IntSize {
    return with(LocalConfiguration.current) {
        with(LocalDensity.current) {
            IntSize(
                width = screenWidthDp.dp.toPx().toInt(),
                height = screenHeightDp.dp.toPx().toInt(),
            )
        }
    }
}

@Composable
actual fun BackHandler(
    enabled: Boolean,
    onBack: () -> Unit,
) {
    BackHandler(enabled, onBack)
}
