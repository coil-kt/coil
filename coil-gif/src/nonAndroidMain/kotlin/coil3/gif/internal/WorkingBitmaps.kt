package coil3.gif.internal

import coil3.Canvas
import coil3.gif.AnimatedTransformation
import coil3.gif.PixelOpacity
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ImageInfo
import org.jetbrains.skia.SamplingMode
import org.jetbrains.skia.impl.use

internal class WorkingBitmaps(
    decodeImageInfo: ImageInfo,
    private val outputImageInfo: ImageInfo,
    private val animatedTransformation: AnimatedTransformation?,
) {

    val decode = allocateBitmap(decodeImageInfo)
    val output = try {
        if (outputImageInfo != decodeImageInfo || animatedTransformation != null) {
            allocateBitmap(outputImageInfo)
        } else {
            null
        }
    } catch (throwable: Throwable) {
        decode.close()
        throw throwable
    }

    fun prepareOutput(): Bitmap {
        val bitmap = output?.also { output ->
            // A previous transformation may have marked the reused output bitmap opaque.
            output.updateAlphaType(
                if (animatedTransformation == null) {
                    outputImageInfo.colorAlphaType
                } else {
                    ColorAlphaType.PREMUL
                },
            )
            decode.scalePixelsTo(output)
        } ?: decode
        bitmap.applyTransformation(
            transformation = animatedTransformation,
            inputAlphaType = outputImageInfo.colorAlphaType,
        )
        return bitmap
    }

    fun close() = closeExcept(retainedBitmap = null)

    fun closeExcept(retainedBitmap: Bitmap?) {
        try {
            if (output !== retainedBitmap) output?.close()
        } finally {
            if (decode !== retainedBitmap) decode.close()
        }
    }
}

private fun allocateBitmap(imageInfo: ImageInfo): Bitmap {
    val bitmap = Bitmap()
    try {
        check(bitmap.allocPixels(imageInfo)) { "Unable to allocate pixels." }
        return bitmap
    } catch (throwable: Throwable) {
        bitmap.close()
        throw throwable
    }
}

private fun Bitmap.scalePixelsTo(destination: Bitmap) {
    checkNotNull(peekPixels()).use { source ->
        checkNotNull(destination.peekPixels()).use { destinationPixels ->
            check(source.scalePixels(destinationPixels, SamplingMode.DEFAULT)) {
                "Unable to resize image frame."
            }
        }
    }
}

private fun Bitmap.applyTransformation(
    transformation: AnimatedTransformation?,
    inputAlphaType: ColorAlphaType,
) {
    transformation ?: return
    val transformedAlphaType = when (Canvas(this).use(transformation::transform)) {
        PixelOpacity.UNCHANGED -> if (inputAlphaType == ColorAlphaType.OPAQUE) {
            ColorAlphaType.OPAQUE
        } else {
            ColorAlphaType.PREMUL
        }
        PixelOpacity.TRANSLUCENT -> ColorAlphaType.PREMUL
        PixelOpacity.OPAQUE -> ColorAlphaType.OPAQUE
    }
    updateAlphaType(transformedAlphaType)
}

private fun Bitmap.updateAlphaType(alphaType: ColorAlphaType) {
    if (imageInfo.colorAlphaType == alphaType) return
    check(setAlphaType(alphaType)) { "Unable to set image alpha type to $alphaType." }
}
