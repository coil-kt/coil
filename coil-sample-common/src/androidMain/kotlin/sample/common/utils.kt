package sample.common

import android.content.Context
import android.util.Size
import coil.Extras
import coil.PlatformContext
import coil.request.videoFrameMicros
import kotlin.math.ceil
import kotlin.math.roundToInt
import okio.Source

fun numberOfColumns(context: Context): Int {
    val displayWidth = context.resources.displayMetrics.widthPixels
    val maxColumnWidth = 320 * context.resources.displayMetrics.density
    return ceil(displayWidth / maxColumnWidth).toInt().coerceAtLeast(4)
}

fun Image.calculateScaledSize(context: Context, numColumns: Int): Size {
    val displayWidth = context.resources.displayMetrics.widthPixels
    val columnWidth = (displayWidth / numColumns.toDouble()).roundToInt()
    val scale = columnWidth / width.toDouble()
    return Size(columnWidth, (scale * height).roundToInt())
}

actual val Extras.Key.Companion.videoFrameMicros: Extras.Key<Long>
    get() = videoFrameMicros

actual fun PlatformContext.openResource(name: String): Source {
    TODO()
}
