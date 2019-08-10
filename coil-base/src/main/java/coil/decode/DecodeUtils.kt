package coil.decode

import android.graphics.BitmapFactory
import androidx.annotation.Px
import coil.size.Scale
import okio.BufferedSource
import okio.ByteString.Companion.encodeUtf8
import kotlin.experimental.and
import kotlin.math.max
import kotlin.math.min

/**
 * A collection of useful utility methods for decoding images.
 */
object DecodeUtils {

    // https://www.onicos.com/staff/iz/formats/gif.html
    private val GIF_HEADER = "GIF".encodeUtf8()

    // https://developers.google.com/speed/webp/docs/riff_container
    private val WEBP_HEADER_RIFF = "RIFF".encodeUtf8()
    private val WEBP_HEADER_WEBP = "WEBP".encodeUtf8()
    private val WEBP_HEADER_VPX8 = "VP8X".encodeUtf8()

    /** Return true if the [source] contains a GIF image. The [source] is not consumed. */
    @JvmStatic
    fun isGif(source: BufferedSource): Boolean {
        return source.rangeEquals(0, GIF_HEADER)
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

    /**
     * Calculate the [BitmapFactory.Options.inSampleSize] given the source dimensions of the image ([inWidth] and [inHeight]),
     * the output dimensions ([outWidth], [outHeight]), and the [scale].
     */
    @JvmStatic
    fun calculateInSampleSize(
        @Px inWidth: Int,
        @Px inHeight: Int,
        @Px outWidth: Int,
        @Px outHeight: Int,
        scale: Scale
    ): Int {
        val widthInSampleSize = max(1, Integer.highestOneBit(inWidth / outWidth))
        val heightInSampleSize = max(1, Integer.highestOneBit(inHeight / outHeight))
        return when (scale) {
            Scale.FILL -> min(widthInSampleSize, heightInSampleSize)
            Scale.FIT -> max(widthInSampleSize, heightInSampleSize)
        }
    }
}
