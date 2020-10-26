package coil.decode

import android.graphics.BitmapFactory
import androidx.annotation.Px
import coil.size.OriginalSize
import coil.size.PixelSize
import coil.size.Scale
import coil.size.Size
import okio.BufferedSource
import okio.ByteString.Companion.encodeUtf8
import kotlin.experimental.and
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** A collection of useful utility methods for decoding images. */
object DecodeUtils {

    // https://www.matthewflickinger.com/lab/whatsinagif/bits_and_bytes.asp
    private val GIF_HEADER_87A = "GIF87a".encodeUtf8()
    private val GIF_HEADER_89A = "GIF89a".encodeUtf8()

    // https://developers.google.com/speed/webp/docs/riff_container
    private val WEBP_HEADER_RIFF = "RIFF".encodeUtf8()
    private val WEBP_HEADER_WEBP = "WEBP".encodeUtf8()
    private val WEBP_HEADER_VPX8 = "VP8X".encodeUtf8()

    // https://nokiatech.github.io/heif/technical.html
    private val HEIF_HEADER_FTYP = "ftyp".encodeUtf8()
    private val HEIF_HEADER_MSF1 = "msf1".encodeUtf8()
    private val HEIF_HEADER_HEVC = "hevc".encodeUtf8()
    private val HEIF_HEADER_HEVX = "hevx".encodeUtf8()

    /** Return true if the [source] contains a GIF image. The [source] is not consumed. */
    @JvmStatic
    fun isGif(source: BufferedSource): Boolean {
        return source.rangeEquals(0, GIF_HEADER_89A) || source.rangeEquals(0, GIF_HEADER_87A)
    }

    /** Return true if the [source] contains a WebP image. The [source] is not consumed. */
    @JvmStatic
    fun isWebP(source: BufferedSource): Boolean {
        return source.rangeEquals(0, WEBP_HEADER_RIFF) && source.rangeEquals(8, WEBP_HEADER_WEBP)
    }

    /** Return true if the [source] contains an animated WebP image. The [source] is not consumed. */
    @JvmStatic
    fun isAnimatedWebP(source: BufferedSource): Boolean {
        return isWebP(source) &&
            source.rangeEquals(12, WEBP_HEADER_VPX8) &&
            source.request(17) &&
            (source.buffer[16] and 0b00000010) > 0
    }

    /** Return true if the [source] contains an HEIF image. The [source] is not consumed. */
    @JvmStatic
    fun isHeif(source: BufferedSource): Boolean {
        return source.rangeEquals(4, HEIF_HEADER_FTYP)
    }

    /** Return true if the [source] contains an animated HEIF image sequence. The [source] is not consumed. */
    @JvmStatic
    fun isAnimatedHeif(source: BufferedSource): Boolean {
        return isHeif(source) &&
            (source.rangeEquals(8, HEIF_HEADER_MSF1) ||
                source.rangeEquals(8, HEIF_HEADER_HEVC) ||
                source.rangeEquals(8, HEIF_HEADER_HEVX))
    }

    /**
     * Calculate the [BitmapFactory.Options.inSampleSize] given the source dimensions of the image
     * ([srcWidth] and [srcHeight]), the output dimensions ([dstWidth], [dstHeight]), and the [scale].
     */
    @JvmStatic
    fun calculateInSampleSize(
        @Px srcWidth: Int,
        @Px srcHeight: Int,
        @Px dstWidth: Int,
        @Px dstHeight: Int,
        scale: Scale
    ): Int {
        val widthInSampleSize = Integer.highestOneBit(srcWidth / dstWidth).coerceAtLeast(1)
        val heightInSampleSize = Integer.highestOneBit(srcHeight / dstHeight).coerceAtLeast(1)
        return when (scale) {
            Scale.FILL -> min(widthInSampleSize, heightInSampleSize)
            Scale.FIT -> max(widthInSampleSize, heightInSampleSize)
        }
    }

    /**
     * Calculate the percentage to multiply the source dimensions by to fit/fill the
     * destination dimensions while preserving aspect ratio.
     */
    @JvmStatic
    fun computeSizeMultiplier(
        @Px srcWidth: Int,
        @Px srcHeight: Int,
        @Px dstWidth: Int,
        @Px dstHeight: Int,
        scale: Scale
    ): Double {
        val widthPercent = dstWidth / srcWidth.toDouble()
        val heightPercent = dstHeight / srcHeight.toDouble()
        return when (scale) {
            Scale.FILL -> max(widthPercent, heightPercent)
            Scale.FIT -> min(widthPercent, heightPercent)
        }
    }

    /** @see computeSizeMultiplier */
    @JvmStatic
    fun computeSizeMultiplier(
        @Px srcWidth: Float,
        @Px srcHeight: Float,
        @Px dstWidth: Float,
        @Px dstHeight: Float,
        scale: Scale
    ): Float {
        val widthPercent = dstWidth / srcWidth
        val heightPercent = dstHeight / srcHeight
        return when (scale) {
            Scale.FILL -> max(widthPercent, heightPercent)
            Scale.FIT -> min(widthPercent, heightPercent)
        }
    }

    /** @see computeSizeMultiplier */
    @JvmStatic
    fun computeSizeMultiplier(
        @Px srcWidth: Double,
        @Px srcHeight: Double,
        @Px dstWidth: Double,
        @Px dstHeight: Double,
        scale: Scale
    ): Double {
        val widthPercent = dstWidth / srcWidth
        val heightPercent = dstHeight / srcHeight
        return when (scale) {
            Scale.FILL -> max(widthPercent, heightPercent)
            Scale.FIT -> min(widthPercent, heightPercent)
        }
    }

    /**
     * Calculate the pixel size required to fit/fill the source dimensions inside the
     * destination size while preserving aspect ratio.
     */
    @JvmStatic
    fun computePixelSize(srcWidth: Int, srcHeight: Int, dstSize: Size, scale: Scale): PixelSize {
        return when (dstSize) {
            is OriginalSize -> PixelSize(srcWidth, srcHeight)
            is PixelSize -> {
                val multiplier = computeSizeMultiplier(srcWidth, srcHeight, dstSize.width, dstSize.height, scale)
                PixelSize(
                    width = (multiplier * srcWidth).roundToInt(),
                    height = (multiplier * srcHeight).roundToInt()
                )
            }
        }
    }
}
