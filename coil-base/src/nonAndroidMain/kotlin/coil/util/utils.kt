package coil.util

import coil.decode.DecodeUtils
import coil.request.ImageRequest
import coil.size.Precision
import coil.size.Scale
import coil.size.Size
import kotlin.math.roundToInt
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Codec
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

/** Returns true if the image is not animated. */
internal val Codec.static: Boolean
    get() = frameCount <= 1

/** Create a [Bitmap] from [image] that fits the given [size] and [scale]. */
internal fun Bitmap.Companion.makeFromImage(
    image: Image,
    size: Size,
    scale: Scale,
): Bitmap {
    val bitmap = Bitmap()
    val srcWidth = image.width
    val srcHeight = image.height
    val targetWidth = size.widthPx(scale) { srcWidth }
    val targetHeight = size.heightPx(scale) { srcHeight }
    val multiplier = DecodeUtils.computeSizeMultiplier(
        srcWidth = srcWidth,
        srcHeight = srcHeight,
        dstWidth = targetWidth,
        dstHeight = targetHeight,
        scale = scale,
    )
    val dstWidth = (multiplier * targetWidth).roundToInt()
    val dstHeight = (multiplier * targetHeight).roundToInt()

    bitmap.allocN32Pixels(dstWidth, dstHeight)
    bitmap.applyCanvas {
        drawImageRect(
            image,
            Rect.makeWH(srcWidth.toFloat(), srcHeight.toFloat()),
            Rect.makeWH(dstWidth.toFloat(), dstHeight.toFloat()),
        )
    }
    return bitmap
}

internal inline fun Bitmap.applyCanvas(block: Canvas.() -> Unit) {
    Canvas(this).use(block)
}
