package coil.sample

import androidx.annotation.ColorInt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter

@Composable
fun rememberColorPainter(@ColorInt color: Int): ColorPainter {
    return remember(color) { ColorPainter(Color(color)) }
}
