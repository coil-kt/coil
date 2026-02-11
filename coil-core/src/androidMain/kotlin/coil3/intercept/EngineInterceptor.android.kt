package coil3.intercept

import android.graphics.Bitmap
import coil3.BitmapImage
import coil3.Image
import coil3.asDrawable
import coil3.intercept.EngineInterceptor.Companion.TAG
import coil3.request.Options
import coil3.request.bitmapConfig
import coil3.request.maxBitmapSize
import coil3.size.Precision
import coil3.transform.Transformation
import coil3.util.DrawableUtils
import coil3.util.Logger
import coil3.util.VALID_TRANSFORMATION_CONFIGS
import coil3.util.log
import coil3.util.safeConfig

internal actual fun convertImageToBitmap(
    image: Image,
    options: Options,
    transformations: List<Transformation>,
    logger: Logger?,
): Bitmap {
    // Fast path: return the existing bitmap.
    if (image is BitmapImage) {
        val bitmap = image.bitmap
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
            "Converting image of type ${image::class.qualifiedName} " +
                "to apply transformations: $transformations."
        }
    }

    // Slow path: draw the drawable on a canvas.
    return DrawableUtils.convertToBitmap(
        drawable = image.asDrawable(options.context.resources),
        config = options.bitmapConfig,
        size = options.size,
        scale = options.scale,
        maxSize = options.maxBitmapSize,
        allowInexactSize = options.precision == Precision.INEXACT,
    )
}
