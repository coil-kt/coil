package coil3.transform

import android.graphics.Bitmap
import androidx.core.graphics.applyCanvas
import androidx.core.graphics.createBitmap
import coil3.size.Size
import coil3.util.safeConfig

/**
 * A [Transformation] that crops an image using a centered circle as the mask.
 *
 * If you're using Compose, use `Modifier.clip(CircleShape)` instead of this transformation
 * as it's more efficient.
 */
class CircleCropTransformation : Transformation() {

    override val cacheKey = "${this::class.qualifiedName}"

    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        val outputSize = minOf(input.width, input.height)
        return createBitmap(outputSize, outputSize, input.safeConfig).applyCanvas {
            val paint = newScaledShaderPaint(input, outputSize, outputSize)
            val radius = outputSize / 2f
            drawCircle(radius, radius, radius, paint)
        }
    }
}
