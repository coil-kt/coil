package coil.util

import coil.Uri
import coil.decode.DecodeUtils
import coil.request.ImageRequest
import coil.size.Precision
import coil.size.Scale
import coil.size.Size
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

/** Create a [Bitmap] from [image] that fits the given [size] and [scale]. */
internal fun Bitmap.Companion.makeFromImage(
    image: Image,
    size: Size,
    scale: Scale,
): Bitmap {
    val srcWidth = image.width
    val srcHeight = image.height
    val multiplier = DecodeUtils.computeSizeMultiplier(
        srcWidth = srcWidth,
        srcHeight = srcHeight,
        dstWidth = size.widthPx(scale) { srcWidth },
        dstHeight = size.heightPx(scale) { srcHeight },
        scale = scale,
    )
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
