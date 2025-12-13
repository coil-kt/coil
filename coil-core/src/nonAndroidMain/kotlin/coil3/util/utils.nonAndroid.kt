package coil3.util

import coil3.Image
import coil3.Uri
import coil3.decode.DecodeUtils
import coil3.request.Options
import coil3.request.maxBitmapSize
import coil3.size.Precision
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Image as SkiaImage
import org.jetbrains.skia.Rect
import org.jetbrains.skia.impl.use

internal actual fun println(
    level: Logger.Level,
    tag: String,
    message: String,
) {
    println(message)
}

/** Create a [Bitmap] from [image] for the given [options]. */
internal fun Bitmap.Companion.makeFromImage(
    image: SkiaImage,
    options: Options,
): Bitmap {
    val srcWidth = image.width
    val srcHeight = image.height
    val (dstWidth, dstHeight) = DecodeUtils.computeDstSize(
        srcWidth = srcWidth,
        srcHeight = srcHeight,
        targetSize = options.size,
        scale = options.scale,
        maxSize = options.maxBitmapSize,
    )
    var multiplier = DecodeUtils.computeSizeMultiplier(
        srcWidth = srcWidth,
        srcHeight = srcHeight,
        dstWidth = dstWidth,
        dstHeight = dstHeight,
        scale = options.scale,
        maxSize = options.maxBitmapSize,
    )

    // Only upscale the image if the options require an exact size.
    if (options.precision == Precision.INEXACT) {
        multiplier = multiplier.coerceAtMost(1.0)
    }

    val outWidth = (multiplier * srcWidth).toInt()
    val outHeight = (multiplier * srcHeight).toInt()

    val bitmap = Bitmap()
    bitmap.allocN32Pixels(outWidth, outHeight)
    Canvas(bitmap).use { canvas ->
        canvas.drawImageRect(
            image = image,
            src = Rect.makeWH(srcWidth.toFloat(), srcHeight.toFloat()),
            dst = Rect.makeWH(outWidth.toFloat(), outHeight.toFloat()),
        )
    }
    return bitmap
}

internal actual fun isAssetUri(uri: Uri): Boolean {
    // Asset URIs are only supported on Android.
    return false
}

internal actual fun Image.prepareToDraw() {
    // Do nothing.
}
