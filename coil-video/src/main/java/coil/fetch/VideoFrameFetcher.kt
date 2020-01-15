@file:Suppress("unused")

package coil.fetch

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Paint
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.O_MR1
import androidx.core.graphics.applyCanvas
import androidx.core.graphics.drawable.toDrawable
import coil.bitmappool.BitmapPool
import coil.decode.DataSource
import coil.decode.Decoder
import coil.decode.Options
import coil.extension.videoFrameMicros
import coil.extension.videoFrameOption
import coil.size.PixelSize
import coil.size.Size
import java.io.File

/**
 * A [VideoFrameFetcher] that supports fetching and decoding a video frame from a [File].
 */
class VideoFrameFileFetcher(context: Context) : VideoFrameFetcher<File>(context) {

    override fun key(data: File) = "${data.path}:${data.lastModified()}"

    override fun handles(data: File): Boolean {
        val fileName = data.name
        return SUPPORTED_FILE_FORMATS.any { fileName.endsWith(it, true) }
    }

    override fun MediaMetadataRetriever.setDataSource(data: File) = setDataSource(data.path)
}

/**
 * A [VideoFrameFetcher] that supports fetching and decoding a video frame from a [Uri].
 */
class VideoFrameUriFetcher(private val context: Context) : VideoFrameFetcher<Uri>(context) {

    override fun key(data: Uri) = data.toString()

    override fun handles(data: Uri): Boolean {
        val fileName = data.lastPathSegment
        return fileName != null && SUPPORTED_FILE_FORMATS.any { fileName.endsWith(it, true) }
    }

    override fun MediaMetadataRetriever.setDataSource(data: Uri) {
        if (data.scheme == ContentResolver.SCHEME_FILE && data.pathSegments.firstOrNull() == ASSET_FILE_PATH_ROOT) {
            // Work around setDataSource(Context, Uri) not properly handling android_asset uris.
            val path = data.pathSegments.drop(1).joinToString("/")
            context.assets.openFd(path).use { setDataSource(it.fileDescriptor, it.startOffset, it.length) }
        } else {
            setDataSource(context, data)
        }
    }
}

/**
 * A [Fetcher] that uses [MediaMetadataRetriever] to fetch and decode a frame from a video.
 *
 * Due to [MediaMetadataRetriever] requiring non-sequential reads into the data source it's not
 * possible to make this a [Decoder]. Use the [VideoFrameFileFetcher] and [VideoFrameUriFetcher] implementations.
 */
abstract class VideoFrameFetcher<T : Any>(private val context: Context) : Fetcher<T> {

    companion object {
        // https://developer.android.com/guide/topics/media/media-formats#video-formats
        internal val SUPPORTED_FILE_FORMATS = arrayOf(".3gp", ".mkv", ".mp4", ".ts", ".webm")

        internal const val ASSET_FILE_PATH_ROOT = "android_asset"

        const val VIDEO_FRAME_MICROS_KEY = "coil#video_frame_micros"
        const val VIDEO_FRAME_OPTION_KEY = "coil#video_frame_option"
    }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    abstract fun MediaMetadataRetriever.setDataSource(data: T)

    override suspend fun fetch(
        pool: BitmapPool,
        data: T,
        size: Size,
        options: Options
    ): FetchResult {
        val retriever = MediaMetadataRetriever()

        try {
            retriever.setDataSource(data)

            val option = options.parameters.videoFrameOption() ?: MediaMetadataRetriever.OPTION_CLOSEST_SYNC
            val frameMicros = options.parameters.videoFrameMicros() ?: 0L

            // Frame sampling is only supported on O_MR1 and above.
            if (SDK_INT >= O_MR1 && size is PixelSize) {
                val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
                val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
                if (width > 0 && height > 0 && size.width < width && size.height < height) {
                    val bitmap = retriever.getScaledFrameAtTime(frameMicros, option, size.width, size.height)
                    if (bitmap != null) {
                        return DrawableResult(
                            drawable = ensureValidConfig(pool, bitmap, options).toDrawable(context.resources),
                            isSampled = true,
                            dataSource = DataSource.DISK
                        )
                    }
                }
            }

            // Read the frame at its full size.
            val bitmap = checkNotNull(retriever.getFrameAtTime(frameMicros, option)) {
                "Failed to decode frame at $frameMicros microseconds."
            }

            return DrawableResult(
                drawable = ensureValidConfig(pool, bitmap, options).toDrawable(context.resources),
                isSampled = false,
                dataSource = DataSource.DISK
            )
        } finally {
            retriever.release()
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
