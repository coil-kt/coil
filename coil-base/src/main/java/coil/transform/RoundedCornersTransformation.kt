@file:Suppress("unused")

package coil.transform

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.RectF
import android.graphics.Shader
import androidx.annotation.FloatRange
import androidx.core.graphics.applyCanvas
import coil.bitmappool.BitmapPool
import coil.decode.DecodeUtils
import coil.size.OriginalSize
import coil.size.PixelSize
import coil.size.Scale
import coil.size.Size
import kotlin.math.roundToInt

/**
 * A [Transformation] that rounds the corners of an image.
 *
 * @param topLeft The radius for the top left corner.
 * @param topRight The radius for the top right corner.
 * @param bottomLeft The radius for the bottom left corner.
 * @param bottomRight The radius for the bottom right corner.
 */
class RoundedCornersTransformation(
    @FloatRange(from = 0.0, to = 1.0) private val topLeft: Float = 0f,
    @FloatRange(from = 0.0, to = 1.0) private val topRight: Float = 0f,
    @FloatRange(from = 0.0, to = 1.0) private val bottomLeft: Float = 0f,
    @FloatRange(from = 0.0, to = 1.0) private val bottomRight: Float = 0f
) : Transformation {

    companion object {
        private const val DEFAULT_RADIUS = 0.05f
    }

    constructor(@FloatRange(from = 0.0, to = 1.0) radius: Float = DEFAULT_RADIUS) : this(radius, radius, radius, radius)

    init {
        require(topLeft >= 0 && topRight >= 0 && bottomLeft >= 0 && bottomRight >= 0) { "All radii must be >= 0." }
    }

    override fun key() = "${RoundedCornersTransformation::class.java}-$topLeft,$topRight,$bottomLeft,$bottomRight"

    override suspend fun transform(pool: BitmapPool, input: Bitmap, size: Size): Bitmap {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        paint.shader = BitmapShader(input, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)

        val outputWidth: Int
        val outputHeight: Int
        when (size) {
            is PixelSize -> {
                val multiplier = DecodeUtils.computeSizeMultiplier(
                    srcWidth = input.width.toFloat(),
                    srcHeight = input.height.toFloat(),
                    destWidth = size.width.toFloat(),
                    destHeight = size.height.toFloat(),
                    scale = Scale.FILL
                )
                outputWidth = (multiplier * size.width).roundToInt()
                outputHeight = (multiplier * size.height).roundToInt()
            }
            is OriginalSize -> {
                outputWidth = input.width
                outputHeight = input.height
            }
        }

        val output = pool.get(outputWidth, outputHeight, input.config)
        output.applyCanvas {
            drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

            val normalizedTopLeft = 0
            val radii = floatArrayOf(topLeft, topLeft, topRight, topRight, bottomRight, bottomRight, bottomLeft, bottomLeft)
            val rect = RectF(0f, 0f, output.width.toFloat(), output.height.toFloat())
            val path = Path().apply { addRoundRect(rect, radii, Path.Direction.CW) }
            drawPath(path, paint)
        }
        pool.put(input)

        return output
    }
}
