package coil3.decode

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

    @Deprecated("Kept for binary compatibility.", level = DeprecationLevel.HIDDEN)
    @JvmStatic
    fun computeSizeMultiplier(
        srcWidth: Int,
        srcHeight: Int,
        dstWidth: Int,
        dstHeight: Int,
        scale: Scale,
    ) = computeSizeMultiplier(srcWidth, srcHeight, dstWidth, dstHeight, scale)

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
        maxSize: Size = Size.ORIGINAL,
    ): Double {
        val srcWidthDouble = srcWidth.toDouble()
        val srcHeightDouble = srcHeight.toDouble()
        val widthPercent = dstWidth / srcWidthDouble
        val heightPercent = dstHeight / srcHeightDouble
        var percent = when (scale) {
            Scale.FILL -> maxOf(widthPercent, heightPercent)
            Scale.FIT -> minOf(widthPercent, heightPercent)
        }
        if (maxSize.width is Dimension.Pixels) {
            val maxWidth = maxSize.width.px
            val maxWidthPercent = maxWidth / srcWidthDouble
            percent = percent.coerceAtMost(maxWidthPercent)
        }
        if (maxSize.height is Dimension.Pixels) {
            val maxHeight = maxSize.height.px
            val maxHeightPercent = maxHeight / srcHeightDouble
            percent = percent.coerceAtMost(maxHeightPercent)
        }
        return percent
    }

    @Deprecated("Kept for binary compatibility.", level = DeprecationLevel.HIDDEN)
    @JvmStatic
    fun computeSizeMultiplier(
        srcWidth: Float,
        srcHeight: Float,
        dstWidth: Float,
        dstHeight: Float,
        scale: Scale,
    ) = computeSizeMultiplier(srcWidth, srcHeight, dstWidth, dstHeight, scale)

    /** @see computeSizeMultiplier */
    @JvmStatic
    fun computeSizeMultiplier(
        srcWidth: Float,
        srcHeight: Float,
        dstWidth: Float,
        dstHeight: Float,
        scale: Scale,
        maxSize: Size = Size.ORIGINAL,
    ): Float {
        val widthPercent = dstWidth / srcWidth
        val heightPercent = dstHeight / srcHeight
        var percent = when (scale) {
            Scale.FILL -> maxOf(widthPercent, heightPercent)
            Scale.FIT -> minOf(widthPercent, heightPercent)
        }
        if (maxSize.width is Dimension.Pixels) {
            val maxWidth = maxSize.width.px
            val maxWidthPercent = maxWidth / srcWidth
            percent = percent.coerceAtMost(maxWidthPercent)
        }
        if (maxSize.height is Dimension.Pixels) {
            val maxHeight = maxSize.height.px
            val maxHeightPercent = maxHeight / srcHeight
            percent = percent.coerceAtMost(maxHeightPercent)
        }
        return percent
    }

    @Deprecated("Kept for binary compatibility.", level = DeprecationLevel.HIDDEN)
    @JvmStatic
    fun computeSizeMultiplier(
        srcWidth: Double,
        srcHeight: Double,
        dstWidth: Double,
        dstHeight: Double,
        scale: Scale,
    ) = computeSizeMultiplier(srcWidth, srcHeight, dstWidth, dstHeight, scale)

    /** @see computeSizeMultiplier */
    @JvmStatic
    fun computeSizeMultiplier(
        srcWidth: Double,
        srcHeight: Double,
        dstWidth: Double,
        dstHeight: Double,
        scale: Scale,
        maxSize: Size = Size.ORIGINAL,
    ): Double {
        val widthPercent = dstWidth / srcWidth
        val heightPercent = dstHeight / srcHeight
        var percent = when (scale) {
            Scale.FILL -> maxOf(widthPercent, heightPercent)
            Scale.FIT -> minOf(widthPercent, heightPercent)
        }
        if (maxSize.width is Dimension.Pixels) {
            val maxWidth = maxSize.width.px
            val maxWidthPercent = maxWidth / srcWidth
            percent = percent.coerceAtMost(maxWidthPercent)
        }
        if (maxSize.height is Dimension.Pixels) {
            val maxHeight = maxSize.height.px
            val maxHeightPercent = maxHeight / srcHeight
            percent = percent.coerceAtMost(maxHeightPercent)
        }
        return percent
    }

    /**
     * Parse [targetSize] and return the destination dimensions that the source image should be
     * scaled into. The returned dimensions can be passed to [computeSizeMultiplier] to get the
     * final size multiplier.
     */
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
        if (maxSize.width is Dimension.Pixels && !dstWidth.isMinOrMax()) {
            dstWidth = dstWidth.coerceAtMost(maxSize.width.px)
        }
        if (maxSize.height is Dimension.Pixels && !dstHeight.isMinOrMax()) {
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
