package coil3.gif.internal

import org.jetbrains.skia.AnimationFrameInfo
import org.jetbrains.skia.ImageInfo

internal fun ImageInfo.byteSize(): Long {
    val computedSize = computeMinByteSize().toLong()
    return if (computedSize > 0L) {
        computedSize
    } else {
        (4L * width * height).coerceAtLeast(0L)
    }
}

internal val AnimationFrameInfo?.safeFrameDuration: Int
    get() = if (this == null || duration <= 0) DEFAULT_FRAME_DURATION else duration

private const val DEFAULT_FRAME_DURATION = 100
