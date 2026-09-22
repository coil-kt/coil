package sample.common

import coil3.Extras
import coil3.request.ImageRequest
import kotlin.random.Random

expect fun assetTypes(): List<AssetType>

fun AssetType.next(): AssetType {
    val types = assetTypes()
    return types[(types.indexOf(this) + 1) % types.size]
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

fun ImageRequest.Builder.extras(other: Extras) = apply {
    extras.setAll(other)
    other.asMap().forEach { (key, value) ->
        memoryCacheKeyExtra(key.toString(), value.toString())
    }
}

expect val Extras.Key.Companion.videoFrameMicros: Extras.Key<Long>

const val MIN_COLUMN_WIDTH_DP = 100
