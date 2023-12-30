package coil3.transform

import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import androidx.core.graphics.applyCanvas
import androidx.core.graphics.createBitmap
import coil3.size.Size
import coil3.util.safeConfig

/**
 * A [Transformation] that crops an image using a centered circle as the mask.
 *
 * If you're using Jetpack Compose, use `Modifier.clip(CircleShape)` instead of this transformation
 * as it's more efficient.
 */
class CircleCropTransformation : Transformation() {

    override val cacheKey = "${this::class.qualifiedName}"

    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        val minSize = minOf(input.width, input.height)
        val radius = minSize / 2f
        val output = createBitmap(minSize, minSize, input.safeConfig)
        output.applyCanvas {
            drawCircle(radius, radius, radius, paint)
            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
            drawBitmap(input, radius - input.width / 2f, radius - input.height / 2f, paint)
        }

        return output
    }
}
