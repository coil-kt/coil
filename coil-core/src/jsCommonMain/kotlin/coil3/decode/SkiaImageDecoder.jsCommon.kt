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
        maxSize = options.maxBitmapSize,
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
internal fun getOriginalSize(bytes: ByteArray): Pair<Int, Int> { // (w,h)
    val pngSize = getPngSizeOrNull(bytes)
    if (pngSize != null) return pngSize

    val jpegSize = getJpegSizeOrNull(bytes)
    if (jpegSize != null) return jpegSize

    val webpSize = getWebpSizeOrNull(bytes)
    if (webpSize != null) return webpSize

    // Fallback for others
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
        var orientation = 0
        while (offset < bytes.size - 6) {
            val marker = int16(bytes[offset], bytes[offset + 1])
            offset += 2

            if (marker in 0xFFC0u..0xFFCFu && marker != 0xFFC4u && marker != 0xFFC8u && marker != 0xFFCCu) {
                val height = int16(bytes[offset + 3], bytes[offset + 4])
                val width = int16(bytes[offset + 5], bytes[offset + 6])
                return if (orientation.isSwapped) {
                    height.toInt() to width.toInt()
                } else {
                    width.toInt() to height.toInt()
                }
            }

            val segmentLength = int16(bytes[offset], bytes[offset + 1])
            if (marker == 0xFFE1u) {
                val exifOrientation = getExifOrientation(
                    bytes = bytes,
                    offset = offset + 2,
                    length = segmentLength.toInt() - 2,
                )
                if (exifOrientation > 0) {
                    orientation = exifOrientation
                }
            }
            offset += segmentLength.toInt()
        }
    }
    return null
}

private val Int.isSwapped: Boolean
    get() = this in 5..8

private fun getExifOrientation(
    bytes: ByteArray,
    offset: Int,
    length: Int,
): Int {
    if (length < 14 || offset < 0 || offset + length > bytes.size) return 0
    if (bytes[offset + 0].asInt() != 0x45u || // E
        bytes[offset + 1].asInt() != 0x78u || // x
        bytes[offset + 2].asInt() != 0x69u || // i
        bytes[offset + 3].asInt() != 0x66u || // f
        bytes[offset + 4].asInt() != 0u ||
        bytes[offset + 5].asInt() != 0u
    ) {
        return 0
    }

    val tiffOffset = offset + 6
    val isLittleEndian = when (int16(bytes[tiffOffset], bytes[tiffOffset + 1])) {
        0x4949u -> true
        0x4D4Du -> false
        else -> return 0
    }
    val read16 = if (isLittleEndian) ::int16LE else ::int16
    val read32 = if (isLittleEndian) ::int32LE else ::int32
    if (read16(bytes[tiffOffset + 2], bytes[tiffOffset + 3]) != 42u) return 0

    val ifdOffset = read32(
        bytes[tiffOffset + 4],
        bytes[tiffOffset + 5],
        bytes[tiffOffset + 6],
        bytes[tiffOffset + 7],
    ).toInt()
    val ifdStart = tiffOffset + ifdOffset
    if (ifdStart < tiffOffset || ifdStart + 2 > offset + length) return 0

    val entryCount = read16(bytes[ifdStart], bytes[ifdStart + 1]).toInt()
    var entryOffset = ifdStart + 2
    repeat(entryCount) {
        if (entryOffset + 12 > offset + length) return 0

        val tag = read16(bytes[entryOffset], bytes[entryOffset + 1])
        if (tag == 0x0112u) {
            val type = read16(bytes[entryOffset + 2], bytes[entryOffset + 3])
            val count = read32(
                bytes[entryOffset + 4],
                bytes[entryOffset + 5],
                bytes[entryOffset + 6],
                bytes[entryOffset + 7],
            )
            if (type == 3u && count == 1u) {
                return read16(bytes[entryOffset + 8], bytes[entryOffset + 9]).toInt()
            }
            return 0
        }

        entryOffset += 12
    }
    return 0
}

internal fun getWebpSizeOrNull(bytes: ByteArray): Pair<Int, Int>? {
    if (bytes.size < 30) return null

    // check "RIFF" and "WEBP" signatures
    if (
        int32(bytes[0], bytes[1], bytes[2], bytes[3]) != 0x5249_4646u ||
        int32(bytes[8], bytes[9], bytes[10], bytes[11]) != 0x5745_4250u
    ) {
        return null
    }

    val chunkType = int32(bytes[12], bytes[13], bytes[14], bytes[15])

    return when (chunkType) {
        0x5650_3858u -> { // "VP8X" (Extended WebP)
            val w = int24LE(bytes[24], bytes[25], bytes[26]).toInt() + 1
            val h = int24LE(bytes[27], bytes[28], bytes[29]).toInt() + 1
            w to h
        }
        0x5650_3820u -> { // "VP8" (Lossy WebP)
            // Check Sync Code "0x9D 0x01 0x2A"
            if (bytes[23].asInt() != 0x9Du || bytes[24].asInt() != 0x01u || bytes[25].asInt() != 0x2Au) return null
            val w = (int16LE(bytes[26], bytes[27]) and 0x3FFFu).toInt()
            val h = (int16LE(bytes[28], bytes[29]) and 0x3FFFu).toInt()
            w to h
        }
        0x5650_384Cu -> { // "VP8L" (Lossless WebP)
            // Check Lossless
            if (bytes[20].asInt() != 0x2Fu) return null
            val bits = int32LE(bytes[21], bytes[22], bytes[23], bytes[24])
            val w = (bits and 0x3FFFu).toInt() + 1
            val h = ((bits shr 14) and 0x3FFFu).toInt() + 1
            w to h
        }
        else -> null
    }
}

private fun int16(b1: Byte, b2: Byte): UInt =
    (b1.asInt() shl 8) or b2.asInt()

private fun int24(b1: Byte, b2: Byte, b3: Byte): UInt =
    (b1.asInt() shl 16) or (b2.asInt() shl 8) or b3.asInt()

private fun int32(b1: Byte, b2: Byte, b3: Byte, b4: Byte): UInt =
    (int16(b1, b2) shl 16) or int16(b3, b4)

private fun int16LE(b1: Byte, b2: Byte): UInt =
    int16(b2, b1)

private fun int24LE(b1: Byte, b2: Byte, b3: Byte): UInt =
    int24(b3, b2, b1)

private fun int32LE(b1: Byte, b2: Byte, b3: Byte, b4: Byte): UInt =
    int32(b4, b3, b2, b1)

private fun Byte.asInt() = this.toUInt() and 0xFFu
