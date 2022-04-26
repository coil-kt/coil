package coil.decode

import android.graphics.Bitmap
import android.graphics.Paint
import android.media.MediaMetadataRetriever
import android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT
import android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION
import android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH
import android.media.MediaMetadataRetriever.OPTION_CLOSEST_SYNC
import android.os.Build.VERSION.SDK_INT
import androidx.core.graphics.applyCanvas
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toDrawable
import coil.ImageLoader
import coil.fetch.SourceResult
import coil.request.Options
import coil.request.videoFrameMicros
import coil.request.videoFrameOption
import coil.size.Dimension.Pixels
import coil.size.Size
import coil.size.pxOrElse
import coil.util.heightPx
import coil.util.use
import coil.util.widthPx
import kotlin.math.roundToInt

/**
 * A [Decoder] that uses [MediaMetadataRetriever] to fetch and decode a frame from a video.
 */
class VideoFrameDecoder(
    private val source: ImageSource,
    private val options: Options
) : Decoder {

    override suspend fun decode() = MediaMetadataRetriever().use { retriever ->
        retriever.setDataSource(source)
        val option = options.parameters.videoFrameOption() ?: OPTION_CLOSEST_SYNC
        val frameMicros = options.parameters.videoFrameMicros() ?: 0L

        // Resolve the dimensions to decode the video frame at accounting
        // for the source's aspect ratio and the target's size.
        var srcWidth: Int
        var srcHeight: Int
        val rotation = retriever.extractMetadata(METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
        if (rotation == 90 || rotation == 270) {
            srcWidth = retriever.extractMetadata(METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
            srcHeight = retriever.extractMetadata(METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
        } else {
            srcWidth = retriever.extractMetadata(METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
            srcHeight = retriever.extractMetadata(METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
        }

        val dstSize = if (srcWidth > 0 && srcHeight > 0) {
            val dstWidth = options.size.widthPx(options.scale) { srcWidth }
            val dstHeight = options.size.heightPx(options.scale) { srcHeight }
            val rawScale = DecodeUtils.computeSizeMultiplier(
                srcWidth = srcWidth,
                srcHeight = srcHeight,
                dstWidth = dstWidth,
                dstHeight = dstHeight,
                scale = options.scale
            )
            val scale = if (options.allowInexactSize) {
                rawScale.coerceAtMost(1.0)
            } else {
                rawScale
            }
            val width = (scale * srcWidth).roundToInt()
            val height = (scale * srcHeight).roundToInt()
            Size(width, height)
        } else {
            // We were unable to decode the video's dimensions.
            // Fall back to decoding the video frame at the original size.
            // We'll scale the resulting bitmap after decoding if necessary.
            Size.ORIGINAL
        }

        val (dstWidth, dstHeight) = dstSize
        val rawBitmap: Bitmap? = if (SDK_INT >= 27 && dstWidth is Pixels && dstHeight is Pixels) {
            retriever.getScaledFrameAtTime(frameMicros, option, dstWidth.px, dstHeight.px)
        } else {
            retriever.getFrameAtTime(frameMicros, option)?.also {
                srcWidth = it.width
                srcHeight = it.height
            }
        }

        // If you encounter this exception make sure your video is encoded in a supported codec.
        // https://developer.android.com/guide/topics/media/media-formats#video-formats
        checkNotNull(rawBitmap) { "Failed to decode frame at $frameMicros microseconds." }

        val bitmap = normalizeBitmap(rawBitmap, dstSize)

        val isSampled = if (srcWidth > 0 && srcHeight > 0) {
            DecodeUtils.computeSizeMultiplier(
                srcWidth = srcWidth,
                srcHeight = srcHeight,
                dstWidth = bitmap.width,
                dstHeight = bitmap.height,
                scale = options.scale
            ) < 1.0
        } else {
            // We were unable to determine the original size of the video. Assume it is sampled.
            true
        }

        DecodeResult(
            drawable = bitmap.toDrawable(options.context.resources),
            isSampled = isSampled
        )
    }

    /** Return [inBitmap] or a copy of [inBitmap] that is valid for the input [options] and [size]. */
    private fun normalizeBitmap(inBitmap: Bitmap, size: Size): Bitmap {
        // Fast path: if the input bitmap is valid, return it.
        if (isConfigValid(inBitmap, options) && isSizeValid(inBitmap, options, size)) {
            return inBitmap
        }

        // Slow path: re-render the bitmap with the correct size + config.
        val scale = DecodeUtils.computeSizeMultiplier(
            srcWidth = inBitmap.width,
            srcHeight = inBitmap.height,
            dstWidth = size.width.pxOrElse { inBitmap.width },
            dstHeight = size.height.pxOrElse { inBitmap.height },
            scale = options.scale
        ).toFloat()
        val dstWidth = (scale * inBitmap.width).roundToInt()
        val dstHeight = (scale * inBitmap.height).roundToInt()
        val safeConfig = when {
            SDK_INT >= 26 && options.config == Bitmap.Config.HARDWARE -> Bitmap.Config.ARGB_8888
            else -> options.config
        }

        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        val outBitmap = createBitmap(dstWidth, dstHeight, safeConfig)
        outBitmap.applyCanvas {
            scale(scale, scale)
            drawBitmap(inBitmap, 0f, 0f, paint)
        }
        inBitmap.recycle()

        return outBitmap
    }

    private fun isConfigValid(bitmap: Bitmap, options: Options): Boolean {
        return SDK_INT < 26 ||
            bitmap.config != Bitmap.Config.HARDWARE ||
            options.config == Bitmap.Config.HARDWARE
    }

    private fun isSizeValid(bitmap: Bitmap, options: Options, size: Size): Boolean {
        if (options.allowInexactSize) return true
        val multiplier = DecodeUtils.computeSizeMultiplier(
            srcWidth = bitmap.width,
            srcHeight = bitmap.height,
            dstWidth = size.width.pxOrElse { bitmap.width },
            dstHeight = size.height.pxOrElse { bitmap.height },
            scale = options.scale
        )
        return multiplier == 1.0
    }

    private fun MediaMetadataRetriever.setDataSource(source: ImageSource) {
        when (val metadata = source.metadata) {
            is AssetMetadata -> {
                options.context.assets.openFd(metadata.fileName).use {
                    setDataSource(it.fileDescriptor, it.startOffset, it.length)
                }
            }
            is ContentMetadata -> {
                setDataSource(options.context, metadata.uri)
            }
            is ResourceMetadata -> {
                setDataSource("android.resource://${metadata.packageName}/${metadata.resId}")
            }
            else -> {
                setDataSource(source.file().toFile().path)
            }
        }
    }

    class Factory : Decoder.Factory {

        override fun create(result: SourceResult, options: Options, imageLoader: ImageLoader): Decoder? {
            if (!isApplicable(result.mimeType)) return null
            return VideoFrameDecoder(result.source, options)
        }

        private fun isApplicable(mimeType: String?): Boolean {
            return mimeType != null && mimeType.startsWith("video/")
        }

        override fun equals(other: Any?) = other is Factory

        override fun hashCode() = javaClass.hashCode()
    }

    companion object {
        const val VIDEO_FRAME_MICROS_KEY = "coil#video_frame_micros"
        const val VIDEO_FRAME_OPTION_KEY = "coil#video_frame_option"
    }
}
