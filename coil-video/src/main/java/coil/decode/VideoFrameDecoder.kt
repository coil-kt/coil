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
import coil.extension.videoFrameMicros
import coil.extension.videoFrameMillis
import coil.size.PixelSize
import coil.size.Size
import okio.BufferedSource
import okio.buffer
import okio.sink
import java.io.File

/**
 * A [Decoder] that decodes a frame from a video.
 * Use [videoFrameMillis] or [videoFrameMicros] to specify the time of the frame to extract.
 */
class VideoFrameDecoder(private val context: Context) : Decoder {

    /** TODO: Check the file headers for any of Android's supported video formats instead of relying on the MIME type. */
    override fun handles(source: BufferedSource, mimeType: String?) = mimeType?.startsWith("video") == true

    override suspend fun decode(
        pool: BitmapPool,
        source: BufferedSource,
        size: Size,
        options: Options
    ): DecodeResult {
        var tempFile: File? = null
        var retriever: MediaMetadataRetriever? = null

        try {
            retriever = MediaMetadataRetriever()
            if (SDK_INT >= M) {
                source.use { retriever.setDataSource(BufferedMediaDataSource(it)) }
            } else {
                // Write the source to disk so it can be read on pre-M.
                tempFile = createTempFile()
                source.use { tempFile.sink().buffer().writeAll(it) }

                retriever.setDataSource(tempFile.path)
            }

            val frameMicros = options.parameters.videoFrameMicros() ?: 0L

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
            val bitmap = checkNotNull(retriever.getFrameAtTime(frameMicros, OPTION_CLOSEST_SYNC)) {
                "Failed to decode frame at $frameMicros microseconds."
            }

            return DecodeResult(
                drawable = bitmap.toDrawable(context.resources),
                isSampled = false
            )
        } finally {
            retriever?.release()
            tempFile?.delete()
        }
    }
}
