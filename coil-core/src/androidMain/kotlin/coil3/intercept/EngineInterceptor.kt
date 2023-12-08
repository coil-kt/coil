package coil3.intercept

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import coil3.BitmapImage
import coil3.EventListener
import coil3.Image
import coil3.asCoilImage
import coil3.intercept.EngineInterceptor.Companion.TAG
import coil3.intercept.EngineInterceptor.ExecuteResult
import coil3.request.ImageRequest
import coil3.request.Options
import coil3.request.allowConversionToBitmap
import coil3.request.bitmapConfig
import coil3.request.transformations
import coil3.transform.Transformation
import coil3.util.DrawableUtils
import coil3.util.Logger
import coil3.util.VALID_TRANSFORMATION_CONFIGS
import coil3.util.foldIndices
import coil3.util.log
import coil3.util.safeConfig
import coil3.util.toDrawable
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.ensureActive

internal actual suspend fun transform(
    result: ExecuteResult,
    request: ImageRequest,
    options: Options,
    eventListener: EventListener,
    logger: Logger?,
): ExecuteResult {
    val transformations = request.transformations
    if (transformations.isEmpty()) return result

    // Skip the transformations as converting to a bitmap is disabled.
    val drawable = result.image.asDrawable(request.context.resources)
    if (drawable !is BitmapDrawable && !request.allowConversionToBitmap) {
        logger?.log(TAG, Logger.Level.Info) {
            val type = result.image::class.qualifiedName
            "allowConversionToBitmap=false, skipping transformations for type $type."
        }
        return result
    }

    // Apply the transformations.
    val input = convertDrawableToBitmap(drawable, options, transformations, logger)
    eventListener.transformStart(request, input)
    val output = transformations.foldIndices(input) { bitmap, transformation ->
        transformation.transform(bitmap, options.size).also { coroutineContext.ensureActive() }
    }
    eventListener.transformEnd(request, output)
    return result.copy(image = output.toDrawable(request.context).asCoilImage())
}

/** Convert [drawable] to a [Bitmap]. */
private fun convertDrawableToBitmap(
    drawable: Drawable,
    options: Options,
    transformations: List<Transformation>,
    logger: Logger?,
): Bitmap {
    // Fast path: return the existing bitmap.
    if (drawable is BitmapDrawable) {
        val bitmap = drawable.bitmap
        val config = bitmap.safeConfig
        if (config in VALID_TRANSFORMATION_CONFIGS) {
            return bitmap
        } else {
            logger?.log(TAG, Logger.Level.Info) {
                "Converting bitmap with config $config " +
                    "to apply transformations: $transformations."
            }
        }
    } else {
        logger?.log(TAG, Logger.Level.Info) {
            "Converting drawable of type ${drawable::class.qualifiedName} " +
                "to apply transformations: $transformations."
        }
    }

    // Slow path: draw the drawable on a canvas.
    return DrawableUtils.convertToBitmap(
        drawable = drawable,
        config = options.bitmapConfig,
        size = options.size,
        scale = options.scale,
        allowInexactSize = options.allowInexactSize,
    )
}

internal actual fun prepareToDraw(image: Image) {
    (image as? BitmapImage)?.bitmap?.prepareToDraw()
}
