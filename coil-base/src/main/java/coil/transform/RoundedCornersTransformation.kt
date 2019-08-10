@file:Suppress("unused")

package coil.transform

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.RectF
import android.graphics.Shader
import androidx.core.graphics.applyCanvas
import coil.bitmappool.BitmapPool

/**
 * A [Transformation] that rounds the corners of an image.
 */
class RoundedCornersTransformation(private val radius: Float) : Transformation {

    init {
        require(radius >= 0) { "Radius must be >= 0." }
    }

    override fun key() = "${RoundedCornersTransformation::class.java}-$radius"

    override suspend fun transform(pool: BitmapPool, input: Bitmap): Bitmap {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        paint.shader = BitmapShader(input, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)

        val output = pool.get(input.width, input.height, input.config)
        val rect = RectF(0f, 0f, output.width.toFloat(), output.height.toFloat())
        output.applyCanvas {
            drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
            drawRoundRect(rect, radius, radius, paint)
        }
        pool.put(input)
        return output
    }
}
