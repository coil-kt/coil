package coil3.transform

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.RectF
import android.graphics.Shader
import androidx.annotation.Px
import androidx.core.graphics.applyCanvas
import androidx.core.graphics.createBitmap
import coil3.decode.DecodeUtils
import coil3.size.Dimension
import coil3.size.Scale
import coil3.size.Size
import coil3.size.isOriginal
import coil3.size.pxOrElse
import coil3.util.safeConfig
import kotlin.math.roundToInt

/**
 * A [Transformation] that crops the image to fit the target's dimensions and rounds the corners of
 * the image.
 *
 * If you're using Jetpack Compose, use `Modifier.clip(RoundedCornerShape(radius))` instead of this
 * transformation as it's more efficient.
 *
 * @param topLeft The radius for the top left corner.
 * @param topRight The radius for the top right corner.
 * @param bottomLeft The radius for the bottom left corner.
 * @param bottomRight The radius for the bottom right corner.
 */
class RoundedCornersTransformation(
    @Px private val topLeft: Float = 0f,
    @Px private val topRight: Float = 0f,
    @Px private val bottomLeft: Float = 0f,
    @Px private val bottomRight: Float = 0f,
) : Transformation() {

    constructor(@Px radius: Float) : this(radius, radius, radius, radius)

    init {
        require(topLeft >= 0 && topRight >= 0 && bottomLeft >= 0 && bottomRight >= 0) {
            "All radii must be >= 0."
        }
    }

    override val cacheKey = "${this::class.qualifiedName}-$topLeft,$topRight,$bottomLeft,$bottomRight"

    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        val (outputWidth, outputHeight) = calculateOutputSize(input, size)

        val output = createBitmap(outputWidth, outputHeight, input.safeConfig)
        output.applyCanvas {
            drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

            val matrix = Matrix()
            val multiplier = DecodeUtils.computeSizeMultiplier(
                srcWidth = input.width,
                srcHeight = input.height,
                dstWidth = outputWidth,
                dstHeight = outputHeight,
                scale = Scale.FILL,
            ).toFloat()
            val dx = (outputWidth - multiplier * input.width) / 2
            val dy = (outputHeight - multiplier * input.height) / 2
            matrix.setTranslate(dx, dy)
            matrix.preScale(multiplier, multiplier)

            val shader = BitmapShader(input, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
            shader.setLocalMatrix(matrix)
            paint.shader = shader

            val radii = floatArrayOf(
                topLeft, topLeft,
                topRight, topRight,
                bottomRight, bottomRight,
                bottomLeft, bottomLeft,
            )
            val rect = RectF(0f, 0f, width.toFloat(), height.toFloat())
            val path = Path().apply { addRoundRect(rect, radii, Path.Direction.CW) }
            drawPath(path, paint)
        }

        return output
    }

    private fun calculateOutputSize(input: Bitmap, size: Size): Pair<Int, Int> {
        if (size.isOriginal) {
            return input.width to input.height
        }

        val (dstWidth, dstHeight) = size
        if (dstWidth is Dimension.Pixels && dstHeight is Dimension.Pixels) {
            return dstWidth.px to dstHeight.px
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
        return outputWidth to outputHeight
    }
}
