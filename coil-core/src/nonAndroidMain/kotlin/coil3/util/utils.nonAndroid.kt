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
import org.jetbrains.skia.SamplingMode
import org.jetbrains.skia.impl.use

internal actual fun println(
    level: Logger.Level,
    tag: String,
    message: String,
) {
    println(message)
}

internal fun Options.allocDecodeBitmap(srcWidth: Int, srcHeight: Int): Bitmap {
    val (dstWidth, dstHeight) = DecodeUtils.computeDstSize(
        srcWidth = srcWidth,
        srcHeight = srcHeight,
        targetSize = size,
        scale = scale,
        maxSize = maxBitmapSize,
    )
    var multiplier = DecodeUtils.computeSizeMultiplier(
        srcWidth = srcWidth,
        srcHeight = srcHeight,
        dstWidth = dstWidth,
        dstHeight = dstHeight,
        scale = scale,
        maxSize = maxBitmapSize,
    )

    // Only upscale the image if the options require an exact size.
    if (precision == Precision.INEXACT) {
        multiplier = multiplier.coerceAtMost(1.0)
    }

    val outWidth = (multiplier * srcWidth).toInt()
    val outHeight = (multiplier * srcHeight).toInt()

    val bitmap = Bitmap()
    bitmap.allocN32Pixels(outWidth, outHeight)
    return bitmap
}

/** Create a [Bitmap] from [image] for the given [options]. */
internal fun Bitmap.Companion.makeFromImage(
    image: SkiaImage,
    options: Options,
): Bitmap {
    val bitmap = options.allocDecodeBitmap(image.width, image.height)
    Canvas(bitmap).use { canvas ->
        canvas.drawImageRect(
            image = image,
            srcLeft = 0f,
            srcTop = 0f,
            srcRight = image.width.toFloat(),
            srcBottom = image.height.toFloat(),
            dstLeft = 0f,
            dstTop = 0f,
            dstRight = bitmap.width.toFloat(),
            dstBottom = bitmap.height.toFloat(),
            samplingMode = SamplingMode.DEFAULT,
            paint = null,
            strict = false,
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
