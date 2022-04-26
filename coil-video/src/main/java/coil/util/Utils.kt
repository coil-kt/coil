@file:JvmName("-VideoUtils")

package coil.util

import android.media.MediaMetadataRetriever
import android.os.Build.VERSION.SDK_INT
import coil.size.Dimension
import coil.size.Scale
import coil.size.Size
import coil.size.pxOrElse

/** [MediaMetadataRetriever] doesn't implement [AutoCloseable] until API 29. */
internal inline fun <T> MediaMetadataRetriever.use(block: (MediaMetadataRetriever) -> T): T {
    try {
        return block(this)
    } finally {
        // We must call 'close' on API 29+ to avoid a strict mode warning.
        if (SDK_INT >= 29) {
            close()
        } else {
            release()
        }
    }
}

internal typealias PxSize = Pair<Int, Int>

internal fun Size.toPxSize(scale: Scale) = PxSize(width.toPx(scale), height.toPx(scale))

private fun Dimension.toPx(scale: Scale) = pxOrElse {
    when (scale) {
        Scale.FILL -> Int.MIN_VALUE
        Scale.FIT -> Int.MAX_VALUE
    }
}
