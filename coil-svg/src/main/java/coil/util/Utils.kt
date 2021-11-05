@file:JvmName("-SvgUtils")

package coil.util

import android.graphics.Bitmap
import android.os.Build.VERSION.SDK_INT
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

internal val Bitmap.Config.isHardware: Boolean
    get() = SDK_INT >= 26 && this == Bitmap.Config.HARDWARE

/** Convert null and [Bitmap.Config.HARDWARE] configs to [Bitmap.Config.ARGB_8888]. */
internal fun Bitmap.Config?.toSoftware(): Bitmap.Config {
    return if (this == null || isHardware) Bitmap.Config.ARGB_8888 else this
}
