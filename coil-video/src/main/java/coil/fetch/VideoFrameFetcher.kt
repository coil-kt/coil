@file:Suppress("unused")

package coil.fetch

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Paint
import android.media.MediaMetadataRetriever
import android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT
import android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH
import android.media.MediaMetadataRetriever.OPTION_CLOSEST_SYNC
import android.net.Uri
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.O
import android.os.Build.VERSION_CODES.O_MR1
import androidx.core.graphics.applyCanvas
import androidx.core.graphics.drawable.toDrawable
import coil.bitmappool.BitmapPool
import coil.decode.DataSource
import coil.decode.DecodeUtils
import coil.decode.Decoder
import coil.decode.Options
import coil.extension.videoFrameMicros
import coil.extension.videoFrameOption
import coil.size.OriginalSize
import coil.size.PixelSize
import coil.size.Size
import java.io.File
import kotlin.math.roundToInt

/**
 * A [VideoFrameFetcher] that supports fetching and decoding a video frame from a [File].
 */
class VideoFrameFileFetcher(context: Context) : VideoFrameFetcher<File>(context) {

    override fun key(data: File) = "${data.path}:${data.lastModified()}"

    override fun handles(data: File): Boolean {
        val fileName = data.name
        return SUPPORTED_FILE_EXTENSIONS.any { fileName.endsWith(it, true) }
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
        return fileName != null && SUPPORTED_FILE_EXTENSIONS.any { fileName.endsWith(it, true) }
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
        internal val SUPPORTED_FILE_EXTENSIONS = arrayOf(".3gp", ".mkv", ".mp4", ".ts", ".webm")

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

            val option = options.parameters.videoFrameOption() ?: OPTION_CLOSEST_SYNC
            val frameMicros = options.parameters.videoFrameMicros() ?: 0L

            // Resolve the dimensions to decode the video frame at
            // accounting for the source's aspect ratio and the target's size.
            val destSize = when (size) {
                is PixelSize -> {
                    val srcWidth = retriever.extractMetadata(METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
                    val srcHeight = retriever.extractMetadata(METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0

                    if (srcWidth > 0 && srcHeight > 0) {
                        val rawScale = DecodeUtils.computeSizeMultiplier(
                            srcWidth = srcWidth,
                            srcHeight = srcHeight,
                            destWidth = size.width,
                            destHeight = size.height,
                            scale = options.scale
                        )
                        val scale = if (options.allowInexactSize) rawScale.coerceAtMost(1.0) else rawScale
                        val width = (scale * srcWidth).roundToInt()
                        val height = (scale * srcHeight).roundToInt()
                        PixelSize(width, height)
                    } else {
                        // We were unable to decode the video's dimensions.
                        // Fall back to decoding the video frame at the target's size.
                        // We'll scale the resulting bitmap after decoding, if necessary.
                        size
                    }
                }
                is OriginalSize -> OriginalSize
            }

            val rawBitmap = if (destSize is PixelSize && SDK_INT >= O_MR1) {
                retriever.getScaledFrameAtTime(frameMicros, option, destSize.width, destSize.height)
            } else {
                retriever.getFrameAtTime(frameMicros, option)
            }
            checkNotNull(rawBitmap) { "Failed to decode frame at $frameMicros microseconds." }

            val config = getTargetConfig(options, rawBitmap)
            val bitmap = validateBitmap(pool, rawBitmap, destSize, config, options)

            return DrawableResult(
                drawable = bitmap.toDrawable(context.resources),
                isSampled = true,
                dataSource = DataSource.DISK
            )
        } finally {
            retriever.release()
        }
    }

    /** Validate that [inBitmap] matches [destSize] and [destConfig]. */
    private fun validateBitmap(
        pool: BitmapPool,
        inBitmap: Bitmap,
        destSize: Size,
        destConfig: Bitmap.Config,
        options: Options
    ): Bitmap {
        // If the input bitmap is valid, return it.
        if (destConfig == inBitmap.config &&
            (options.allowInexactSize ||
                destSize !is PixelSize ||
                (destSize.width == inBitmap.width && destSize.height == inBitmap.height))) {
            return inBitmap
        }

        // Else, re-render the bitmap with the correct size + config.
        val scale: Float
        val destWidth: Int
        val destHeight: Int
        when (destSize) {
            is PixelSize -> {
                scale = DecodeUtils.computeSizeMultiplier(
                    srcWidth = inBitmap.width,
                    srcHeight = inBitmap.height,
                    destWidth = destSize.width,
                    destHeight = destSize.height,
                    scale = options.scale
                ).toFloat()
                destWidth = (scale * inBitmap.width).roundToInt()
                destHeight = (scale * inBitmap.height).roundToInt()
            }
            is OriginalSize -> {
                scale = 1f
                destWidth = inBitmap.width
                destHeight = inBitmap.height
            }
        }
        val outBitmap = pool.get(destWidth, destHeight, destConfig)
        outBitmap.applyCanvas {
            scale(scale, scale)
            drawBitmap(inBitmap, 0f, 0f, paint)
        }
        pool.put(inBitmap)
        return outBitmap
    }

    private fun getTargetConfig(options: Options, bitmap: Bitmap): Bitmap.Config {
        val srcConfig = bitmap.config.takeIf { SDK_INT < O || it != Bitmap.Config.HARDWARE } ?: Bitmap.Config.ARGB_8888
        val destConfig = options.config.takeIf { SDK_INT < O || it != Bitmap.Config.HARDWARE } ?: Bitmap.Config.ARGB_8888
        return if (options.allowRgb565 &&
            srcConfig == Bitmap.Config.RGB_565 &&
            destConfig == Bitmap.Config.ARGB_8888) {
            Bitmap.Config.RGB_565
        } else {
            destConfig
        }
    }
}
