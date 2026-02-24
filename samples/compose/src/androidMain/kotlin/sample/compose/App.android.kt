package sample.compose

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text

@OptIn(ExperimentalComposeUiApi::class)
@Stable
actual fun Modifier.testTagsAsResourceId(enable: Boolean): Modifier {
    return semantics {
        testTagsAsResourceId = enable
    }
}

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

@Composable
actual fun DirectSvgRenderer(
    svg: String,
    modifier: Modifier,
    onClose: () -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.fillMaxSize(),
    ) {
        Text("Direct Skia SVG rendering test is available on non-Android targets.")
    }
}
