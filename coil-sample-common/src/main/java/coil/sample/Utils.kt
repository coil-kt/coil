@file:JvmName("-CommonUtils")
@file:Suppress("NOTHING_TO_INLINE")

package coil.sample

import android.content.Context
import android.graphics.Color
import android.util.Size
import androidx.annotation.ColorInt
import androidx.lifecycle.AndroidViewModel
import kotlin.math.ceil
import kotlin.math.roundToInt
import kotlin.random.Random

inline val AndroidViewModel.context: Context
    get() = getApplication()

fun Context.getDisplaySize(): Size {
    return resources.displayMetrics.run { Size(widthPixels, heightPixels) }
}

@ColorInt
fun randomColor(): Int {
    return Color.argb(128, Random.nextInt(256), Random.nextInt(256), Random.nextInt(256))
}

fun nextAssetType(current: AssetType): AssetType {
    val values = AssetType.values()
    return values[(values.indexOf(current) + 1) % values.size]
}

fun numberOfColumns(context: Context): Int {
    val displayWidth = context.getDisplaySize().width
    val maxColumnWidth = 320 * context.resources.displayMetrics.density
    return ceil(displayWidth / maxColumnWidth).toInt().coerceAtLeast(4)
}

fun scaledImageSize(context: Context, numColumns: Int, width: Int, height: Int): Size {
    val displayWidth = context.getDisplaySize().width
    val columnWidth = (displayWidth / numColumns.toDouble()).roundToInt()
    val scale = columnWidth / width.toDouble()
    return Size(columnWidth, (scale * height).roundToInt())
}
