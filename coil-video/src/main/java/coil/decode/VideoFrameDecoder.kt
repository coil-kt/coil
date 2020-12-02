@file:Suppress("unused")

package coil.decode

import android.content.Context
import android.media.MediaMetadataRetriever
import coil.bitmap.BitmapPool
import coil.size.Size
import okio.BufferedSource
import okio.sink
import java.io.File

/**
 * A [Decoder] that uses [MediaMetadataRetriever] to fetch and decode a frame from a video.
 *
 * NOTE: [VideoFrameDecoder] creates a temporary copy of the video on the file system. This may cause the decode
 * process to fail if the video being decoded is very large and/or the device is very low on disk space.
 */
class VideoFrameDecoder(private val context: Context) : Decoder {

    private val delegate = VideoFrameDecoderDelegate(context)

    override fun handles(source: BufferedSource, mimeType: String?): Boolean {
        return mimeType != null && mimeType.startsWith("video/")
    }

    override suspend fun decode(
        pool: BitmapPool,
        source: BufferedSource,
        size: Size,
        options: Options
    ): DecodeResult {
        val tempFile = File.createTempFile("tmp", null, context.cacheDir)
        source.use { tempFile.sink().use(it::readAll) }

        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(tempFile.path)
            return delegate.decode(pool, retriever, size, options)
        } finally {
            tempFile.delete()
            retriever.release()
        }
    }

    companion object {
        const val VIDEO_FRAME_MICROS_KEY = VideoFrameDecoderDelegate.VIDEO_FRAME_MICROS_KEY
        const val VIDEO_FRAME_OPTION_KEY = VideoFrameDecoderDelegate.VIDEO_FRAME_OPTION_KEY
    }
}
