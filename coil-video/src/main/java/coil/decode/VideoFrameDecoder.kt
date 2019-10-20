@file:Suppress("unused")

package coil.decode

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Paint
import android.media.MediaMetadataRetriever
import android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT
import android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH
import android.media.MediaMetadataRetriever.OPTION_CLOSEST_SYNC
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.M
import android.os.Build.VERSION_CODES.O_MR1
import androidx.core.graphics.applyCanvas
import androidx.core.graphics.drawable.toDrawable
import coil.bitmappool.BitmapPool
import coil.extension.videoFrameMicros
import coil.extension.videoFrameMillis
import coil.extension.videoFrameOption
import coil.size.PixelSize
import coil.size.Size
import okio.BufferedSource
import okio.sink
import java.io.File

/**
 * A [Decoder] that decodes a frame from a video.
 * Use [videoFrameMillis] or [videoFrameMicros] to specify the time of the frame to extract.
 */
class VideoFrameDecoder(private val context: Context) : Decoder {

    companion object {
        const val VIDEO_FRAME_MICROS_KEY = "coil#video_frame_micros"
        const val VIDEO_FRAME_OPTION_KEY = "coil#video_frame_option"
    }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    override fun handles(source: BufferedSource, mimeType: String?) = mimeType?.startsWith("video") == true

    override suspend fun decode(
        pool: BitmapPool,
        source: BufferedSource,
        size: Size,
        options: Options
    ): DecodeResult {
        var tempFile: File? = null
        var mediaDataSource: BufferedMediaDataSource? = null
        val retriever = MediaMetadataRetriever()

        try {
            if (SDK_INT >= M) {
                mediaDataSource = BufferedMediaDataSource(source)
                retriever.setDataSource(mediaDataSource)
            } else {
                // Write the source to a temp file so it can be read on pre-M.
                tempFile = createTempFile()
                source.use { tempFile.sink().use(source::readAll) }
                retriever.setDataSource(tempFile.path)
            }

            val option = options.parameters.videoFrameOption() ?: OPTION_CLOSEST_SYNC
            val frameMicros = options.parameters.videoFrameMicros() ?: 0L

            // Frame sampling is only supported on O_MR1 and above.
            if (SDK_INT >= O_MR1 && size is PixelSize) {
                val width = retriever.extractMetadata(METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
                val height = retriever.extractMetadata(METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
                if (width > 0 && height > 0 && size.width < width && size.height < height) {
                    val bitmap = retriever.getScaledFrameAtTime(frameMicros, option, size.width, size.height)
                    if (bitmap != null) {
                        return DecodeResult(
                            drawable = ensureValidConfig(pool, bitmap, options).toDrawable(context.resources),
                            isSampled = true
                        )
                    }
                }
            }

            // Read the frame at its full size.
            val bitmap = checkNotNull(retriever.getFrameAtTime(frameMicros, option)) {
                "Failed to decode frame at $frameMicros microseconds."
            }

            return DecodeResult(
                drawable = ensureValidConfig(pool, bitmap, options).toDrawable(context.resources),
                isSampled = false
            )
        } finally {
            retriever.release()
            if (SDK_INT >= M) {
                mediaDataSource?.close()
            } else {
                tempFile?.delete()
            }
        }
    }

    /** Copy the input [Bitmap] to a non-hardware [Bitmap.Config] if necessary. */
    private fun ensureValidConfig(pool: BitmapPool, bitmap: Bitmap, options: Options): Bitmap {
        if (options.config == bitmap.config || (options.allowRgb565 && bitmap.config == Bitmap.Config.RGB_565)) {
            return bitmap
        }

        val safeBitmap = pool.get(bitmap.width, bitmap.height, options.config)
        safeBitmap.applyCanvas {
            drawBitmap(bitmap, 0f, 0f, paint)
        }
        return safeBitmap
    }
}
