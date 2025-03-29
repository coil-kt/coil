package coil3.transform

import android.graphics.Bitmap
import android.graphics.Path
import android.graphics.RectF
import androidx.annotation.Px
import androidx.core.graphics.applyCanvas
import androidx.core.graphics.createBitmap
import coil3.decode.DecodeUtils
import coil3.size.Dimension
import coil3.size.Scale
import coil3.size.Size
import coil3.size.isOriginal
import coil3.size.pxOrElse
import coil3.util.IntPair
import coil3.util.component1
import coil3.util.component2
import coil3.util.safeConfig
import kotlin.math.roundToInt

/**
 * A [Transformation] that crops the image to fit the target's dimensions and rounds the corners of
 * the image.
 *
 * If you're using Compose, use `Modifier.clip(RoundedCornerShape(radius))` instead of this
 * transformation as it's more efficient.
 *
 * @param topLeft The radius for the top left corner.
 * @param topRight The radius for the top right corner.
 * @param bottomLeft The radius for the bottom left corner.
 * @param bottomRight The radius for the bottom right corner.
 */
class RoundedCornersTransformation(
    @param:Px private val topLeft: Float = 0f,
    @param:Px private val topRight: Float = 0f,
    @param:Px private val bottomLeft: Float = 0f,
    @param:Px private val bottomRight: Float = 0f,
) : Transformation() {

    constructor(@Px radius: Float) : this(radius, radius, radius, radius)

    init {
        require(topLeft >= 0 && topRight >= 0 && bottomLeft >= 0 && bottomRight >= 0) {
            "All radii must be >= 0."
        }
    }

    override val cacheKey = "${this::class.qualifiedName}-$topLeft,$topRight,$bottomLeft,$bottomRight"

    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        val (outputWidth, outputHeight) = calculateOutputSize(input, size)
        return createBitmap(outputWidth, outputHeight, input.safeConfig).applyCanvas {
            val paint = newScaledShaderPaint(input, outputWidth, outputHeight)
            if (topLeft == topRight && topRight == bottomLeft && bottomLeft == bottomRight) {
                drawRoundRect(0f, 0f, outputWidth.toFloat(), outputHeight.toFloat(), topLeft, topLeft, paint)
            } else {
                val radii = floatArrayOf(
                    topLeft, topLeft,
                    topRight, topRight,
                    bottomRight, bottomRight,
                    bottomLeft, bottomLeft,
                )
                val rect = RectF(0f, 0f, outputWidth.toFloat(), outputHeight.toFloat())
                val path = Path().apply { addRoundRect(rect, radii, Path.Direction.CW) }
                drawPath(path, paint)
            }
        }
    }

    private fun calculateOutputSize(input: Bitmap, size: Size): IntPair {
        if (size.isOriginal) {
            return IntPair(input.width, input.height)
        }

        val (dstWidth, dstHeight) = size
        if (dstWidth is Dimension.Pixels && dstHeight is Dimension.Pixels) {
            return IntPair(dstWidth.px, dstHeight.px)
        }

        val multiplier = DecodeUtils.computeSizeMultiplier(
            srcWidth = input.width,
            srcHeight = input.height,
            dstWidth = size.width.pxOrElse { Int.MIN_VALUE },
            dstHeight = size.height.pxOrElse { Int.MIN_VALUE },
            scale = Scale.FILL,
        )
        val outputWidth = (multiplier * input.width).roundToInt()
        val outputHeight = (multiplier * input.height).roundToInt()
        return IntPair(outputWidth, outputHeight)
    }
}
