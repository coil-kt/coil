@file:JvmName("-SvgExtensions")

package coil.util

import okio.BufferedSource
import okio.ByteString

internal fun BufferedSource.indexOf(bytes: ByteString, fromIndex: Long, toIndex: Long): Long {
    require(bytes.size > 0) { "bytes is empty" }

    val firstByte = bytes[0]
    var currentIndex = fromIndex
    while (currentIndex < toIndex) {
        currentIndex = indexOf(firstByte, currentIndex)
        if (currentIndex == -1L || rangeEquals(currentIndex, bytes)) {
            return currentIndex
        }
        currentIndex++
    }
    return -1
}
