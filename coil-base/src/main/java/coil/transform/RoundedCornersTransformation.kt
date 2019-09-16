@file:Suppress("unused")

package coil.transform

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.RectF
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.Path
import androidx.core.graphics.applyCanvas
import coil.bitmappool.BitmapPool

/**
 * A [Transformation] that rounds the corners of an image.
 */
class RoundedCornersTransformation(
    private val topLeft: Float,
    private val topRight: Float,
    private val bottomRight: Float,
    private val bottomLeft: Float
) : Transformation {

    constructor(radius: Float) : this(radius, radius, radius, radius)

    init {
        require(topLeft >= 0 && topRight >= 0 && bottomRight >= 0 && bottomLeft >= 0) { "All radii must be >= 0." }
    }

    override fun key() = "${RoundedCornersTransformation::class.java}-${topLeft},${topRight},${bottomLeft},${bottomRight}"

    override suspend fun transform(pool: BitmapPool, input: Bitmap): Bitmap {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        paint.shader = BitmapShader(input, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)

        val output = pool.get(input.width, input.height, input.config)
        val rect = RectF(0f, 0f, output.width.toFloat(), output.height.toFloat())
        output.applyCanvas {
            drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
            drawPath(Path().apply {
                addRoundRect(
                    rect,
                    floatArrayOf(
                        topLeft, topLeft, topRight, topRight, bottomLeft, bottomLeft, bottomRight, bottomRight
                    ),
                    Path.Direction.CW
                )
            }, paint)
        }

        pool.put(input)
        return output
    }
}
