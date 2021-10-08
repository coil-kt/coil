@file:Suppress("unused")

package coil.transform

import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import androidx.core.graphics.applyCanvas
import androidx.core.graphics.createBitmap
import coil.size.Size
import coil.util.safeConfig
import kotlin.math.min

/**
 * A [Transformation] that crops an image using a centered circle as the mask.
 */
class CircleCropTransformation : Transformation {

    override val cacheKey: String = javaClass.name

    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        val minSize = min(input.width, input.height)
        val radius = minSize / 2f
        val output = createBitmap(minSize, minSize, input.safeConfig)
        output.applyCanvas {
            drawCircle(radius, radius, radius, paint)
            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
            drawBitmap(input, radius - input.width / 2f, radius - input.height / 2f, paint)
        }

        return output
    }

    override fun equals(other: Any?) = other is CircleCropTransformation

    override fun hashCode() = javaClass.hashCode()
}
