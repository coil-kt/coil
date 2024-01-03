package coil3.util.svg

import coil3.size.Dimension
import coil3.size.Scale
import coil3.size.Size
import coil3.size.isOriginal
import okio.BufferedSource
import okio.ByteString

internal fun BufferedSource.indexOf(bytes: ByteString, fromIndex: Long, toIndex: Long): Long {
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

internal fun Dimension.toPx(scale: Scale): Float {
    if (this is Dimension.Pixels) {
        return px.toFloat()
    } else {
        return when (scale) {
            Scale.FILL -> Float.MIN_VALUE
            Scale.FIT -> Float.MAX_VALUE
        }
    }
}

internal inline fun Size.widthPx(scale: Scale, original: () -> Float): Float {
    return if (isOriginal) original() else width.toPx(scale)
}

internal inline fun Size.heightPx(scale: Scale, original: () -> Float): Float {
    return if (isOriginal) original() else height.toPx(scale)
}
