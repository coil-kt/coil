package coil3.gif

import coil3.decode.DecodeUtils
import kotlin.experimental.and
import okio.BufferedSource
import okio.ByteString.Companion.encodeUtf8

// https://www.matthewflickinger.com/lab/whatsinagif/bits_and_bytes.asp
private val GIF_HEADER_87A = "GIF87a".encodeUtf8()
private val GIF_HEADER_89A = "GIF89a".encodeUtf8()

// https://developers.google.com/speed/webp/docs/riff_container
private val WEBP_HEADER_RIFF = "RIFF".encodeUtf8()
private val WEBP_HEADER_WEBP = "WEBP".encodeUtf8()
private val WEBP_HEADER_VPX8 = "VP8X".encodeUtf8()

// https://nokiatech.github.io/heif/technical.html
private val HEIF_HEADER_FTYP = "ftyp".encodeUtf8()
private val HEIF_HEADER_MSF1 = "msf1".encodeUtf8()
private val HEIF_HEADER_HEVC = "hevc".encodeUtf8()
private val HEIF_HEADER_HEVX = "hevx".encodeUtf8()

/**
 * Return 'true' if the [source] contains a GIF image. The [source] is not consumed.
 */
fun DecodeUtils.isGif(source: BufferedSource): Boolean {
    return source.rangeEquals(0, GIF_HEADER_89A) ||
        source.rangeEquals(0, GIF_HEADER_87A)
}

/**
 * Return 'true' if the [source] contains a WebP image. The [source] is not consumed.
 */
fun DecodeUtils.isWebP(source: BufferedSource): Boolean {
    return source.rangeEquals(0, WEBP_HEADER_RIFF) &&
        source.rangeEquals(8, WEBP_HEADER_WEBP)
}

/**
 * Return 'true' if the [source] contains an animated WebP image. The [source] is not consumed.
 */
fun DecodeUtils.isAnimatedWebP(source: BufferedSource): Boolean {
    return isWebP(source) &&
        source.rangeEquals(12, WEBP_HEADER_VPX8) &&
        source.request(21) &&
        (source.buffer[20] and 0b00000010) > 0
}

/**
 * Return 'true' if the [source] contains an HEIF image. The [source] is not consumed.
 */
fun DecodeUtils.isHeif(source: BufferedSource): Boolean {
    return source.rangeEquals(4, HEIF_HEADER_FTYP)
}

/**
 * Return 'true' if the [source] contains an animated HEIF image sequence. The [source] is not
 * consumed.
 */
fun DecodeUtils.isAnimatedHeif(source: BufferedSource): Boolean {
    return isHeif(source) &&
        (source.rangeEquals(8, HEIF_HEADER_MSF1) ||
            source.rangeEquals(8, HEIF_HEADER_HEVC) ||
            source.rangeEquals(8, HEIF_HEADER_HEVX))
}

internal fun extractLoopCountFromGif(source: BufferedSource): Int? {
    return try {
        parseLoopCount(source)
    } catch (e: Exception) {
        null
    }
}

private fun parseLoopCount(source: BufferedSource): Int? {
    val headerBytes = ByteArray(6)
    if (source.read(headerBytes) != 6) return null

    val screenDescriptorBytes = ByteArray(7)
    if (source.read(screenDescriptorBytes) != 7) return null

    if ((screenDescriptorBytes[4] and 0b10000000.toByte()) != 0.toByte()) {
        val colorTableSize = 3 * (1 shl ((screenDescriptorBytes[4].toInt() and 0b00000111) + 1))
        source.skip(colorTableSize.toLong())
    }

    // Handle Application Extension Block
    while (!source.exhausted()) {
        val blockType = source.readByte().toInt() and 0xFF
        if (blockType == 0x21) { // Extension Introducer
            val label = source.readByte().toInt() and 0xFF
            if (label == 0xFF) { // Application Extension
                val blockSize = source.readByte().toInt() and 0xFF
                val appIdentifier = source.readUtf8(8)
                val appAuthCode = source.readUtf8(3)

                if (appIdentifier == "NETSCAPE" && appAuthCode == "2.0") {
                    val dataBlockSize = source.readByte().toInt() and 0xFF
                    val loopCountIndicator = source.readByte().toInt() and 0xFF

                    // Read low and high bytes for loop count
                    val loopCountLow = source.readByte().toInt() and 0xFF
                    val loopCountHigh = source.readByte().toInt() and 0xFF
                    val loopCount = (loopCountHigh shl 8) or loopCountLow
                    val blockTerminator = source.readByte().toInt() and 0xFF
                    return if (loopCount == 0) null else loopCount
                } else {
                    skipExtensionBlock(source) // Skip if not NETSCAPE
                }
            } else {
                skipExtensionBlock(source) // Skip other extension blocks
            }
        }
    }
    return null
}

// Extension Blocks always terminate with a zero
private fun skipExtensionBlock(source: BufferedSource) {
    while (true) {
        val size = source.readByte().toInt() and 0xFF
        if (size == 0) break
        source.skip(size.toLong())
    }
}
