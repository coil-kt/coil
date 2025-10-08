package coil3.video.internal

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.media.MediaMetadataRetriever.BitmapParams
import android.os.Build.VERSION.SDK_INT
import androidx.annotation.RequiresApi
import kotlin.ExperimentalStdlibApi
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineDispatcher
import okio.BufferedSource
import okio.ByteString

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

internal fun MediaMetadataRetriever.getFrameAtTime(
    timeUs: Long,
    option: Int,
    config: Bitmap.Config,
): Bitmap? = if (SDK_INT >= 30) {
    val params = BitmapParams().apply { preferredConfig = config }
    getFrameAtTime(timeUs, option, params)
} else {
    getFrameAtTime(timeUs, option)
}

@RequiresApi(27)
internal fun MediaMetadataRetriever.getScaledFrameAtTime(
    timeUs: Long,
    option: Int,
    dstWidth: Int,
    dstHeight: Int,
    config: Bitmap.Config,
): Bitmap? = if (SDK_INT >= 30) {
    val params = BitmapParams().apply { preferredConfig = config }
    getScaledFrameAtTime(timeUs, option, dstWidth, dstHeight, params)
} else {
    getScaledFrameAtTime(timeUs, option, dstWidth, dstHeight)
}

@RequiresApi(28)
internal fun MediaMetadataRetriever.getFrameAtIndex(
    frameIndex: Int,
    config: Bitmap.Config,
): Bitmap? = getFrameAtIndex(frameIndex, BitmapParams().apply { preferredConfig = config })

@OptIn(ExperimentalStdlibApi::class)
internal val CoroutineContext.dispatcher: CoroutineDispatcher?
    get() = get(CoroutineDispatcher)
