package coil.util

import coil.Uri
import coil.decode.DecodeUtils
import coil.request.ImageRequest
import coil.request.Options
import coil.size.Precision
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
    bitmap.applyCanvas {
        drawImageRect(
            image = image,
            src = Rect.makeWH(srcWidth.toFloat(), srcHeight.toFloat()),
            dst = Rect.makeWH(dstWidth.toFloat(), dstHeight.toFloat()),
        )
    }
    return bitmap
}

internal inline fun Bitmap.applyCanvas(block: Canvas.() -> Unit) {
    Canvas(this).use(block)
}

internal actual fun isFileUri(uri: Uri): Boolean {
    return uri.scheme == SCHEME_FILE
}
