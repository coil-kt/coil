package coil.transform

import android.graphics.Bitmap
import android.graphics.Paint
import androidx.core.graphics.applyCanvas
import androidx.core.graphics.scaleMatrix
import androidx.core.graphics.times
import androidx.core.graphics.translationMatrix
import coil.bitmappool.BitmapPool

class CenterCropTransformation(private val width: Int, private val height: Int) : Transformation {

    constructor(sideLength: Int) : this(sideLength, sideLength)

    override fun key() = "${CenterCropTransformation::class.java.canonicalName}-{$width,$height}"

    override suspend fun transform(pool: BitmapPool, input: Bitmap): Bitmap {
        if (input.width == width && input.height == height) {
            return input
        }

        val scale: Float
        val dx: Float
        val dy: Float
        if (input.width * height > width * input.height) {
            scale = height.toFloat() / input.height.toFloat()
            dx = (width - input.width * scale) * 0.5f
            dy = 0f
        } else {
            scale = width.toFloat() / input.width.toFloat()
            dx = 0f
            dy = (height - input.height * scale) * 0.5f
        }

        val matrix = translationMatrix(dx + 0.5f, dy + 0.5f) * scaleMatrix(scale, scale)

        val result = pool.get(width, height, input.config ?: Bitmap.Config.ARGB_8888)

        result.setHasAlpha(input.hasAlpha())

        result.applyCanvas {
            drawBitmap(input, matrix, DEFAULT_PAINT)
            setBitmap(null)
        }

        return result
    }

    companion object {
        private val DEFAULT_PAINT = Paint(Paint.DITHER_FLAG or Paint.FILTER_BITMAP_FLAG)
    }
}
