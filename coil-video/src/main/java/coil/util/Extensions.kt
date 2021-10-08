@file:JvmName("-VideoExtensions")

package coil.util

import android.media.MediaMetadataRetriever
import android.os.Build.VERSION.SDK_INT

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
