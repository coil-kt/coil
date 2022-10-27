package coil.decode

import android.graphics.BitmapFactory
import androidx.annotation.Px
import coil.size.Scale

/** A collection of useful utility methods for decoding images. */
object DecodeUtils {

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
        val widthInSampleSize = (srcWidth / dstWidth).takeHighestOneBit()
        val heightInSampleSize = (srcHeight / dstHeight).takeHighestOneBit()
        return when (scale) {
            Scale.FILL -> minOf(widthInSampleSize, heightInSampleSize)
            Scale.FIT -> maxOf(widthInSampleSize, heightInSampleSize)
        }.coerceAtLeast(1)
    }

    /**
     * Calculate the percentage to multiply the source dimensions by to fit/fill the destination
     * dimensions while preserving aspect ratio.
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
            Scale.FILL -> maxOf(widthPercent, heightPercent)
            Scale.FIT -> minOf(widthPercent, heightPercent)
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
            Scale.FILL -> maxOf(widthPercent, heightPercent)
            Scale.FIT -> minOf(widthPercent, heightPercent)
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
            Scale.FILL -> maxOf(widthPercent, heightPercent)
            Scale.FIT -> minOf(widthPercent, heightPercent)
        }
    }
}
