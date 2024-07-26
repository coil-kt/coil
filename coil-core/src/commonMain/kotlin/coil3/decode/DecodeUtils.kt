package coil3.decode

import coil3.annotation.ExperimentalCoilApi
import coil3.size.Dimension
import coil3.size.Scale
import coil3.size.Size
import coil3.size.isOriginal
import coil3.size.pxOrElse
import coil3.util.IntPair
import coil3.util.isMinOrMax
import kotlin.jvm.JvmStatic

/** A collection of useful utility methods for decoding images. */
object DecodeUtils {

    /**
     * Calculate the `BitmapFactory.Options.inSampleSize` given the source dimensions of the image
     * ([srcWidth] and [srcHeight]), the output dimensions ([dstWidth], [dstHeight]), and the [scale].
     */
    @JvmStatic
    fun calculateInSampleSize(
        srcWidth: Int,
        srcHeight: Int,
        dstWidth: Int,
        dstHeight: Int,
        scale: Scale,
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
        srcWidth: Int,
        srcHeight: Int,
        dstWidth: Int,
        dstHeight: Int,
        scale: Scale,
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
        srcWidth: Float,
        srcHeight: Float,
        dstWidth: Float,
        dstHeight: Float,
        scale: Scale,
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
        srcWidth: Double,
        srcHeight: Double,
        dstWidth: Double,
        dstHeight: Double,
        scale: Scale,
    ): Double {
        val widthPercent = dstWidth / srcWidth
        val heightPercent = dstHeight / srcHeight
        return when (scale) {
            Scale.FILL -> maxOf(widthPercent, heightPercent)
            Scale.FIT -> minOf(widthPercent, heightPercent)
        }
    }

    @ExperimentalCoilApi
    @JvmStatic
    fun computeDstSize(
        srcWidth: Int,
        srcHeight: Int,
        targetSize: Size,
        scale: Scale,
        maxSize: Size,
    ): IntPair {
        var dstWidth: Int
        var dstHeight: Int
        if (targetSize.isOriginal) {
            dstWidth = srcWidth
            dstHeight = srcHeight
        } else {
            dstWidth = targetSize.width.toPx(scale)
            dstHeight = targetSize.height.toPx(scale)
        }
        if (maxSize.width is Dimension.Pixels && !maxSize.width.px.isMinOrMax()) {
            dstWidth = dstWidth.coerceAtMost(maxSize.width.px)
        }
        if (maxSize.height is Dimension.Pixels && !maxSize.height.px.isMinOrMax()) {
            dstHeight = dstHeight.coerceAtMost(maxSize.height.px)
        }
        return IntPair(dstWidth, dstHeight)
    }

    private fun Dimension.toPx(scale: Scale): Int = pxOrElse {
        when (scale) {
            Scale.FILL -> Int.MIN_VALUE
            Scale.FIT -> Int.MAX_VALUE
        }
    }
}
