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
import androidx.core.graphics.applyCanvas
import androidx.core.graphics.drawable.toDrawable
import coil.bitmap.BitmapPool
import coil.decode.DataSource
import coil.decode.DecodeUtils
import coil.decode.Decoder
import coil.decode.Options
import coil.request.videoFrameMicros
import coil.request.videoFrameOption
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
            // Work around setDataSource(Context, Uri) not handling android_asset uris properly.
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

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    protected abstract fun MediaMetadataRetriever.setDataSource(data: T)

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

            // Resolve the dimensions to decode the video frame at accounting
            // for the source's aspect ratio and the target's size.
            var srcWidth = 0
            var srcHeight = 0
            val destSize = when (size) {
                is PixelSize -> {
                    srcWidth = retriever.extractMetadata(METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
                    srcHeight = retriever.extractMetadata(METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0

                    if (srcWidth > 0 && srcHeight > 0) {
                        val rawScale = DecodeUtils.computeSizeMultiplier(
                            srcWidth = srcWidth,
                            srcHeight = srcHeight,
                            dstWidth = size.width,
                            dstHeight = size.height,
                            scale = options.scale
                        )
                        val scale = if (options.allowInexactSize) rawScale.coerceAtMost(1.0) else rawScale
                        val width = (scale * srcWidth).roundToInt()
                        val height = (scale * srcHeight).roundToInt()
                        PixelSize(width, height)
                    } else {
                        // We were unable to decode the video's dimensions.
                        // Fall back to decoding the video frame at the original size.
                        // We'll scale the resulting bitmap after decoding if necessary.
                        OriginalSize
                    }
                }
                is OriginalSize -> OriginalSize
            }

            val rawBitmap: Bitmap? = if (SDK_INT >= 27 && destSize is PixelSize) {
                retriever.getScaledFrameAtTime(frameMicros, option, destSize.width, destSize.height)
            } else {
                retriever.getFrameAtTime(frameMicros, option)?.also {
                    srcWidth = it.width
                    srcHeight = it.height
                }
            }

            // If you encounter this exception make sure your video is encoded in a supported codec.
            // https://developer.android.com/guide/topics/media/media-formats#video-formats
            checkNotNull(rawBitmap) { "Failed to decode frame at $frameMicros microseconds." }

            val bitmap = normalizeBitmap(pool, rawBitmap, destSize, options)

            val isSampled = if (srcWidth > 0 && srcHeight > 0) {
                DecodeUtils.computeSizeMultiplier(srcWidth, srcHeight, bitmap.width, bitmap.height, options.scale) < 1.0
            } else {
                // We were unable to determine the original size of the video. Assume it is sampled.
                true
            }

            return DrawableResult(
                drawable = bitmap.toDrawable(context.resources),
                isSampled = isSampled,
                dataSource = DataSource.DISK
            )
        } finally {
            retriever.release()
        }
    }

    /** Return [inBitmap] or a copy of [inBitmap] that is valid for the input [options] and [size]. */
    private fun normalizeBitmap(
        pool: BitmapPool,
        inBitmap: Bitmap,
        size: Size,
        options: Options
    ): Bitmap {
        // Fast path: if the input bitmap is valid, return it.
        if (isConfigValid(inBitmap, options) && isSizeValid(inBitmap, options, size)) {
            return inBitmap
        }

        // Slow path: re-render the bitmap with the correct size + config.
        val scale: Float
        val dstWidth: Int
        val dstHeight: Int
        when (size) {
            is PixelSize -> {
                scale = DecodeUtils.computeSizeMultiplier(
                    srcWidth = inBitmap.width,
                    srcHeight = inBitmap.height,
                    dstWidth = size.width,
                    dstHeight = size.height,
                    scale = options.scale
                ).toFloat()
                dstWidth = (scale * inBitmap.width).roundToInt()
                dstHeight = (scale * inBitmap.height).roundToInt()
            }
            is OriginalSize -> {
                scale = 1f
                dstWidth = inBitmap.width
                dstHeight = inBitmap.height
            }
        }
        val safeConfig = when {
            SDK_INT >= 26 && options.config == Bitmap.Config.HARDWARE -> Bitmap.Config.ARGB_8888
            else -> options.config
        }

        val outBitmap = pool.get(dstWidth, dstHeight, safeConfig)
        outBitmap.applyCanvas {
            scale(scale, scale)
            drawBitmap(inBitmap, 0f, 0f, paint)
        }
        pool.put(inBitmap)

        return outBitmap
    }

    private fun isConfigValid(bitmap: Bitmap, options: Options): Boolean {
        return SDK_INT < 26 || bitmap.config != Bitmap.Config.HARDWARE || options.config == Bitmap.Config.HARDWARE
    }

    private fun isSizeValid(bitmap: Bitmap, options: Options, size: Size): Boolean {
        return options.allowInexactSize || size is OriginalSize ||
            size == DecodeUtils.computePixelSize(bitmap.width, bitmap.height, size, options.scale)
    }

    companion object {
        // https://developer.android.com/guide/topics/media/media-formats#video-formats
        @JvmField internal val SUPPORTED_FILE_EXTENSIONS = arrayOf(".3gp", ".mkv", ".mp4", ".ts", ".webm")

        internal const val ASSET_FILE_PATH_ROOT = "android_asset"

        const val VIDEO_FRAME_MICROS_KEY = "coil#video_frame_micros"
        const val VIDEO_FRAME_OPTION_KEY = "coil#video_frame_option"
    }
}
