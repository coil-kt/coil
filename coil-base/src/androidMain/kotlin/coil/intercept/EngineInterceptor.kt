package coil.intercept

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import coil.EventListener
import coil.Image
import coil.asCoilImage
import coil.drawable
import coil.intercept.EngineInterceptor.Companion.TAG
import coil.intercept.EngineInterceptor.ExecuteResult
import coil.request.ImageRequest
import coil.request.Options
import coil.request.allowConversionToBitmap
import coil.request.bitmapConfig
import coil.request.transformations
import coil.transform.Transformation
import coil.util.DrawableUtils
import coil.util.Logger
import coil.util.VALID_TRANSFORMATION_CONFIGS
import coil.util.foldIndices
import coil.util.log
import coil.util.safeConfig
import coil.util.toDrawable
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
    if (result.image !is BitmapDrawable && !request.allowConversionToBitmap) {
        logger?.log(TAG, Logger.Level.Info) {
            val type = result.image::class.java.canonicalName
            "allowConversionToBitmap=false, skipping transformations for type $type."
        }
        return result
    }

    // Apply the transformations.
    val input = convertDrawableToBitmap(result.image.drawable, options, transformations, logger)
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
            "Converting drawable of type ${drawable::class.java.canonicalName} " +
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
    (image.drawable as? BitmapDrawable)?.bitmap?.prepareToDraw()
}
