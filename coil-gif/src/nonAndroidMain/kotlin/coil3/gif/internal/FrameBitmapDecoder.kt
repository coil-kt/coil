package coil3.gif.internal

import coil3.BitmapImage
import coil3.Canvas
import coil3.asImage
import coil3.gif.AnimatedTransformation
import coil3.gif.PixelOpacity
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Codec
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo
import org.jetbrains.skia.SamplingMode
import org.jetbrains.skia.impl.use

internal class FrameBitmapDecoder(
    private val decodeImageInfo: ImageInfo,
    private val outputImageInfo: ImageInfo,
    private val animatedTransformation: AnimatedTransformation?,
    needsPriorFrame: Boolean = false,
) {

    private val separateOutputInfo = when {
        animatedTransformation != null -> outputImageInfo.withColorAlphaType(ColorAlphaType.PREMUL)
        outputImageInfo != decodeImageInfo -> outputImageInfo
        else -> null
    }
    private val workingBitmap = if (needsPriorFrame || separateOutputInfo != null) {
        allocateBitmap(decodeImageInfo)
    } else {
        null
    }
    val workingBitmapSize = workingBitmap?.imageInfo?.byteSize() ?: 0L

    fun decodeStaticFrame(codec: Codec): BitmapImage {
        val bitmap = decodeFrame { codec.readPixels(it) }
        check(bitmap !== workingBitmap)
        try {
            bitmap.setImmutable()
            return bitmap.asImage()
        } catch (throwable: Throwable) {
            bitmap.close()
            throw throwable
        }
    }

    fun decodeAnimatedFrame(codec: Codec, frame: Int, priorFrame: Int): Image {
        val bitmap = decodeFrame { codec.readPixels(it, frame = frame, priorFrame = priorFrame) }
        if (bitmap === workingBitmap) {
            // Skia copies mutable pixels here, so the codec can reuse them to composite later frames.
            return Image.makeFromBitmap(bitmap)
        }
        return bitmap.use {
            // Transfer these pixels to the image without copying. Never write them again: a
            // recorded draw can keep the image alive even after we evict it from the frame buffer.
            it.setImmutable()
            Image.makeFromBitmap(it)
        }
    }

    private inline fun decodeFrame(readPixels: (Bitmap) -> Unit): Bitmap {
        val decode = workingBitmap ?: return allocateBitmap(decodeImageInfo, readPixels)
        readPixels(decode)
        val outputInfo = separateOutputInfo ?: return decode
        return allocateBitmap(outputInfo) { output ->
            decode.scalePixelsTo(output)
            output.applyTransformation(animatedTransformation, outputImageInfo.colorAlphaType)
        }
    }

    fun close() {
        workingBitmap?.close()
    }
}

private inline fun allocateBitmap(imageInfo: ImageInfo, initialize: (Bitmap) -> Unit = {}): Bitmap {
    val bitmap = Bitmap()
    try {
        check(bitmap.allocPixels(imageInfo)) { "Unable to allocate pixels." }
        initialize(bitmap)
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
