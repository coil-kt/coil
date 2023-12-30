package coil3.util

import coil3.Uri
import coil3.decode.DecodeUtils
import coil3.request.ImageRequest
import coil3.request.Options
import coil3.size.Precision
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Image
import org.jetbrains.skia.Rect
import org.jetbrains.skia.impl.use

internal actual fun println(
    level: Logger.Level,
    tag: String,
    message: String,
) {
    println(message)
}

internal actual val ImageRequest.allowInexactSize: Boolean
    get() = when (precision) {
        Precision.EXACT -> false
        Precision.INEXACT,
        Precision.AUTOMATIC -> true
    }

/** Create a [Bitmap] from [image] for the given [options]. */
internal fun Bitmap.Companion.makeFromImage(
    image: Image,
    options: Options,
): Bitmap {
    val srcWidth = image.width
    val srcHeight = image.height
    var multiplier = DecodeUtils.computeSizeMultiplier(
        srcWidth = srcWidth,
        srcHeight = srcHeight,
        dstWidth = options.size.widthPx(options.scale) { srcWidth },
        dstHeight = options.size.heightPx(options.scale) { srcHeight },
        scale = options.scale,
    )

    // Only upscale the image if the options require an exact size.
    if (options.allowInexactSize) {
        multiplier = multiplier.coerceAtMost(1.0)
    }

    val dstWidth = (multiplier * srcWidth).toInt()
    val dstHeight = (multiplier * srcHeight).toInt()

    val bitmap = Bitmap()
    bitmap.allocN32Pixels(dstWidth, dstHeight)
    Canvas(bitmap).use { canvas ->
        canvas.drawImageRect(
            image = image,
            src = Rect.makeWH(srcWidth.toFloat(), srcHeight.toFloat()),
            dst = Rect.makeWH(dstWidth.toFloat(), dstHeight.toFloat()),
        )
    }
    return bitmap
}

internal actual fun isAssetUri(uri: Uri): Boolean {
    // Asset URIs are only supported on Android.
    return false
}
