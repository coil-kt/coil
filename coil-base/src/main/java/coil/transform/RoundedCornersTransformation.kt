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
import androidx.core.graphics.applyCanvas
import coil.bitmappool.BitmapPool

/**
 * A [Transformation] that rounds the corners of an image.
 */
class RoundedCornersTransformation(
    private val topLeft: Float = 0f,
    private val topRight: Float = 0f,
    private val bottomRight: Float = 0f,
    private val bottomLeft: Float = 0f
) : Transformation {

    constructor(radius: Float) : this(radius, radius, radius, radius)

    init {
        require(topLeft >= 0 && topRight >= 0 && bottomRight >= 0 && bottomLeft >= 0) { "All radii must be >= 0." }
    }

    override fun key() = "${RoundedCornersTransformation::class.java}-$topLeft,$topRight,$bottomLeft,$bottomRight"

    override suspend fun transform(pool: BitmapPool, input: Bitmap): Bitmap {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        paint.shader = BitmapShader(input, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)

        val output = pool.get(input.width, input.height, input.config)
        val rect = RectF(0f, 0f, output.width.toFloat(), output.height.toFloat())
        output.applyCanvas {
            drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

            val radii = floatArrayOf(topLeft, topLeft, topRight, topRight, bottomRight, bottomRight, bottomLeft, bottomLeft)
            val path = Path().apply { addRoundRect(rect, radii, Path.Direction.CW) }
            drawPath(path, paint)
        }
        pool.put(input)
        return output
    }
}
