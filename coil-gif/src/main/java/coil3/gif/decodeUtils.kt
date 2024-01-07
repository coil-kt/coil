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
        source.request(17) &&
        (source.buffer[16] and 0b00000010) > 0
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
