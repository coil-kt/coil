package coil3.compose

import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.nativeCanvas

internal actual val Canvas.nativeCanvas get() = nativeCanvas
