@file:Suppress("unused")

package coil.transform

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Paint
import androidx.core.graphics.applyCanvas
import coil.bitmappool.BitmapPool

/**
 * A [Transformation] that rotate an image from rotation angle.
 */
class RotateTransformation(
        private val rotateRotationAngle: Float
) : Transformation {

    override fun key(): String = RotateTransformation::class.java.name

    override suspend fun transform(pool: BitmapPool, input: Bitmap): Bitmap {
        val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        val matrix = Matrix().apply {
            postRotate(rotateRotationAngle, input.width / 2f, input.height / 2f)
        }

        val output = pool.getFromMatrix(input.width, input.height, input.config, matrix)
        output.applyCanvas {
            drawBitmap(input, matrix, bitmapPaint)
        }
        pool.put(input)
        return output
    }
}
