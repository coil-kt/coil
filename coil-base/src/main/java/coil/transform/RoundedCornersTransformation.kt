@file:Suppress("unused")

package coil.transform

import android.graphics.*
import androidx.core.graphics.applyCanvas
import coil.bitmappool.BitmapPool

/**
 * A [Transformation] that rounds the corners of an image.
 */
class RoundedCornersTransformation(private vararg val radii: Float) : Transformation {

    init {
        require(radii.size == 1 || radii.size == 4) { "Radii size has to be either 1 or 4" }
        require(radii.all { it >= 0 }) { "All radius must be >= 0." }
    }

    override fun key() = "${RoundedCornersTransformation::class.java}-${radii.joinToString(",")}"

    override suspend fun transform(pool: BitmapPool, input: Bitmap): Bitmap {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        paint.shader = BitmapShader(input, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)

        val output = pool.get(input.width, input.height, input.config)
        val rect = RectF(0f, 0f, output.width.toFloat(), output.height.toFloat())
        output.applyCanvas {
            drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
            when (radii.size) {
                1 -> drawRoundRect(rect, radii[0], radii[0], paint)
                else -> drawPath(Path().apply {
                    addRoundRect(rect, floatArrayOf(radii[0], radii[0], radii[1], radii[1], radii[2], radii[2], radii[3], radii[3]), Path.Direction.CW)
                }, paint)
            }
        }
        pool.put(input)
        return output
    }
}
