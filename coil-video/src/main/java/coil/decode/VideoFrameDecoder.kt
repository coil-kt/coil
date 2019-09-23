@file:Suppress("unused")

package coil.decode

import android.content.Context
import android.media.MediaMetadataRetriever
import android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT
import android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH
import android.media.MediaMetadataRetriever.OPTION_CLOSEST_SYNC
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.M
import android.os.Build.VERSION_CODES.O_MR1
import androidx.core.graphics.drawable.toDrawable
import coil.bitmappool.BitmapPool
import coil.request.videoFrame
import coil.size.PixelSize
import coil.size.Size
import okio.BufferedSource
import okio.buffer
import okio.sink
import java.io.File

/**
 * A [Decoder] that decodes a frame from a video. Use [videoFrame] to specify the time of the frame to extract.
 */
class VideoFrameDecoder(private val context: Context) : Decoder {

    companion object {
        internal const val VIDEO_FRAME_MILLIS_KEY = "coil.decode.VideoFrameDecoder#video_frame_millis"
    }

    /** TODO: Check the file headers for any of Android's supported video formats instead of relying on the MIME type. */
    override fun handles(source: BufferedSource, mimeType: String?) = mimeType?.startsWith("video") == true

    override suspend fun decode(
        pool: BitmapPool,
        source: BufferedSource,
        size: Size,
        options: Options
    ): DecodeResult {
        var tempFile: File? = null
        val retriever = MediaMetadataRetriever()

        try {
            if (SDK_INT >= M) {
                retriever.setDataSource(BufferedMediaDataSource(source))
            } else {
                // Write the source to disk so it can be read on pre-M.
                tempFile = createTempFile()
                tempFile.sink().buffer().writeAll(source)

                retriever.setDataSource(tempFile.path)
            }

            val frameMicros = TODO()

            // Frame sampling is only supported on O_MR1 and above.
            if (SDK_INT >= O_MR1 && size is PixelSize) {
                val width = retriever.extractMetadata(METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
                val height = retriever.extractMetadata(METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
                if (width > 0 && height > 0 && size.width < width && size.height < height) {
                    val bitmap = retriever.getScaledFrameAtTime(frameMicros, OPTION_CLOSEST_SYNC, size.width, size.height)
                    if (bitmap != null) {
                        return DecodeResult(
                            drawable = bitmap.toDrawable(context.resources),
                            isSampled = true
                        )
                    }
                }
            }

            // Read the frame at its full size.
            val bitmap = checkNotNull(retriever.getFrameAtTime(frameMicros, OPTION_CLOSEST_SYNC)) { "Failed to decode frame." }

            return DecodeResult(
                drawable = bitmap.toDrawable(context.resources),
                isSampled = false
            )
        } finally {
            retriever.release()
            tempFile?.delete()
        }
    }
}
