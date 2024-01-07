package coil3.svg.internal

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

internal const val MIME_TYPE_SVG = "image/svg+xml"
internal const val SVG_DEFAULT_SIZE = 512
