package sample.common

import coil3.Extras
import kotlin.math.roundToInt
import kotlin.random.Random

fun AssetType.next(): AssetType {
    val entries = AssetType.entries
    return entries[(entries.indexOf(this) + 1) % entries.size]
}

fun randomColor(): Int {
    val alpha = 128
    val red = Random.nextInt(256)
    val green = Random.nextInt(256)
    val blue = Random.nextInt(256)
    return (alpha shl 24) or (red shl 16) or (green shl 8) or (blue)
}

fun String.toColorInt(): Int {
    // Use a long to avoid rollovers on #FFXXXXXX.
    var color = substring(1).toLong(16)
    if (length == 7) {
        // Set the alpha value.
        color = color or 0x00000000FF000000L
    }
    return color.toInt()
}

fun Image.calculateScaledSize(displayWidth: Int): Pair<Int, Int> {
    val columnWidth = (displayWidth / NUM_COLUMNS.toDouble()).roundToInt()
    val scale = columnWidth / width.toDouble()
    return columnWidth to (scale * height).roundToInt()
}

expect val Extras.Key.Companion.videoFrameMicros: Extras.Key<Long>

const val NUM_COLUMNS = 4
