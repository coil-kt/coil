@file:Suppress("unused")

package coil.transform

import android.graphics.Bitmap
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import androidx.core.graphics.applyCanvas
import coil.bitmappool.BitmapPool
import coil.size.Size
import coil.util.safeConfig

/**
 * A [Transformation] that converts an image to shades of gray.
 */
class GrayscaleTransformation : Transformation {

    companion object {
        private val COLOR_FILTER = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })
    }

    override fun key(): String = GrayscaleTransformation::class.java.name

    override suspend fun transform(pool: BitmapPool, input: Bitmap, size: Size): Bitmap {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        paint.colorFilter = COLOR_FILTER

        val output = pool.get(input.width, input.height, input.safeConfig)
        output.applyCanvas {
            drawBitmap(input, 0f, 0f, paint)
        }
        pool.put(input)

        return output
    }
}
