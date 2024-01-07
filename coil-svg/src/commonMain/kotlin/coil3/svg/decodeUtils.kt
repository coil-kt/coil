package coil3.svg

import coil3.decode.DecodeUtils
import coil3.svg.internal.indexOf
import okio.BufferedSource
import okio.ByteString.Companion.encodeUtf8

private val SVG_TAG = "<svg".encodeUtf8()
private val LEFT_ANGLE_BRACKET = "<".encodeUtf8()

/**
 * Return 'true' if the [source] contains an SVG image. The [source] is not consumed.
 *
 * NOTE: There's no guaranteed method to determine if a byte stream is an SVG without attempting
 * to decode it. This method uses heuristics.
 */
fun DecodeUtils.isSvg(source: BufferedSource): Boolean {
    return source.rangeEquals(0, LEFT_ANGLE_BRACKET) &&
        source.indexOf(SVG_TAG, 0, 1024) != -1L
}
