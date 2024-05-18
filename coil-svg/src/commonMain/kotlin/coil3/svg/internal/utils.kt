package coil3.svg.internal

import coil3.request.Options
import coil3.size.Scale
import coil3.size.isOriginal
import coil3.util.toPx
import kotlin.math.roundToInt
import okio.BufferedSource
import okio.ByteString

internal fun BufferedSource.indexOf(
    bytes: ByteString,
    fromIndex: Long,
    toIndex: Long,
): Long {
    require(bytes.size > 0) { "bytes is empty" }

    val firstByte = bytes[0]
    val lastIndex = toIndex - bytes.size
    var currentIndex = fromIndex
    while (currentIndex < lastIndex) {
        currentIndex = indexOf(firstByte, currentIndex, lastIndex)
        if (currentIndex == -1L || rangeEquals(currentIndex, bytes)) {
            return currentIndex
        }
        currentIndex++
    }
    return -1
}

internal fun getDstSize(
    options: Options,
    srcWidth: Float,
    srcHeight: Float,
    scale: Scale,
): Pair<Int, Int> {
    if (options.size.isOriginal) {
        val dstWidth = if (srcWidth > 0) srcWidth.roundToInt() else SVG_DEFAULT_SIZE
        val dstHeight = if (srcHeight > 0) srcHeight.roundToInt() else SVG_DEFAULT_SIZE
        return dstWidth to dstHeight
    } else {
        val (dstWidth, dstHeight) = options.size
        return dstWidth.toPx(scale) to dstHeight.toPx(scale)
    }
}

internal const val MIME_TYPE_SVG = "image/svg+xml"
internal const val SVG_DEFAULT_SIZE = 512

// Use a default 2KB value for the SVG's size in memory.
internal const val SVG_SIZE_BYTES = 2048L
