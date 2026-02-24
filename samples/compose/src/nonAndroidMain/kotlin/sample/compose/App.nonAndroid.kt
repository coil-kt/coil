package sample.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.IntSize
import androidx.compose.material3.Text
import androidx.compose.ui.platform.LocalDensity
import org.jetbrains.compose.resources.decodeToSvgPainter

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

@Composable
actual fun DirectSvgRenderer(
    svg: String,
    modifier: Modifier,
    onClose: () -> Unit,
) {
    val density = LocalDensity.current
    val painter = remember(svg, density) {
        runCatching {
            svg.encodeToByteArray().decodeToSvgPainter(density)
        }.getOrNull()
    }

    if (painter == null) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier.fillMaxSize(),
        ) {
            Text("Failed to parse SVG data.")
        }
        return
    }

    Image(
        painter = painter,
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = modifier,
    )
}
