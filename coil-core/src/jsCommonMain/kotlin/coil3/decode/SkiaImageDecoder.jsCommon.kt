package coil3.decode

import coil3.asImage
import coil3.request.Options
import coil3.request.maxBitmapSize
import coil3.size.Precision
import coil3.util.component1
import coil3.util.component2
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Image

internal actual suspend fun decodeBitmap(
    options: Options,
    bytes: ByteArray,
): DecodeResult {
    val (srcWidth, srcHeight) = getOriginalSize(bytes)
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
    )

    // Only upscale the image if the options require an exact size.
    if (options.precision == Precision.INEXACT) {
        multiplier = multiplier.coerceAtMost(1.0)
    }

    val outWidth = (multiplier * srcWidth).toInt()
    val outHeight = (multiplier * srcHeight).toInt()

    val bitmap: Bitmap = decodeImageAsync(bytes, outWidth, outHeight)

    return DecodeResult(
        image = bitmap.asImage(),
        isSampled = bitmap.width < srcWidth || bitmap.height < srcHeight,
    )
}

// try to read a size directly for PNG and JPEG images.
// It is faster than Image.makeFromEncoded(bytes)
private fun getOriginalSize(bytes: ByteArray): Pair<Int, Int> { //(w,h)
    val pngSize = getPngSizeOrNull(bytes)
    if (pngSize != null) return pngSize

    val jpegSize = getJpegSizeOrNull(bytes)
    if (jpegSize != null) return jpegSize

    // Fallback for WebP and others
    val image = Image.makeFromEncoded(bytes)
    return image.width to image.height
}

internal fun getPngSizeOrNull(bytes: ByteArray): Pair<Int, Int>? {
    if (bytes.size < 24) return null
    if (
        int32(bytes[0], bytes[1], bytes[2], bytes[3]) == 0x8950_4E47u &&
        int32(bytes[4], bytes[5], bytes[6], bytes[7]) == 0x0D0A_1A0Au
    ) {
        val width = int32(bytes[16], bytes[17], bytes[18], bytes[19])
        val height = int32(bytes[20], bytes[21], bytes[22], bytes[23])
        return width.toInt() to height.toInt()
    }
    return null
}

internal fun getJpegSizeOrNull(bytes: ByteArray): Pair<Int, Int>? {
    if (bytes.size < 10) return null
    if (int16(bytes[0], bytes[1]) == 0xFFD8u) {
        var offset = 2
        while (offset < bytes.size - 6) {
            val marker = int16(bytes[offset], bytes[offset + 1])
            offset += 2

            if (marker in 0xFFC0u..0xFFCFu && marker != 0xFFC4u && marker != 0xFFC8u && marker != 0xFFCCu) {
                val height = int16(bytes[offset + 3], bytes[offset + 4])
                val width = int16(bytes[offset + 5], bytes[offset + 6])
                return width.toInt() to height.toInt()
            }

            val segmentLength = int16(bytes[offset], bytes[offset + 1])
            offset += segmentLength.toInt()
        }
    }
    return null
}

private fun int16(b1: Byte, b2: Byte): UInt =
    (b1.asInt() shl 8) or b2.asInt()

private fun int32(b1: Byte, b2: Byte, b3: Byte, b4: Byte): UInt =
    (int16(b1, b2) shl 16) or int16(b3, b4)

private fun Byte.asInt() = this.toUInt() and 0xFFu
