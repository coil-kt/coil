@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

package coil3.svg

import coil3.ImageLoader
import coil3.asCoilImage
import coil3.decode.DecodeResult
import coil3.decode.DecodeUtils
import coil3.decode.Decoder
import coil3.decode.ImageSource
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import coil3.size.Scale
import coil3.size.isOriginal
import coil3.svg.internal.MIME_TYPE_SVG
import coil3.svg.internal.SVG_DEFAULT_SIZE
import coil3.util.toPx
import kotlin.math.roundToInt
import org.jetbrains.skia.Data
import org.jetbrains.skia.Rect
import org.jetbrains.skia.svg.SVGDOM
import org.jetbrains.skia.svg.SVGLength
import org.jetbrains.skia.svg.SVGLengthUnit

/**
 * A [Decoder] that uses [SVGDOM](https://api.skia.org/classSkSVGDOM.html/) to decode SVG
 * files.
 *
 * @param useViewBoundsAsIntrinsicSize If true, uses the SVG's view bounds as the intrinsic size for
 *  the SVG. If false, uses the SVG's width/height as the intrinsic size for the SVG.
 */
actual class SvgDecoder actual constructor(
    private val source: ImageSource,
    private val options: Options,
    val useViewBoundsAsIntrinsicSize: Boolean,
) : Decoder {

    override suspend fun decode(): DecodeResult {
        val bytes = source.source().readByteArray()
        val svg = SVGDOM(Data.makeFromBytes(bytes))

        val svgWidth: Float
        val svgHeight: Float
        val viewBox: Rect? = svg.root?.viewBox

        if (useViewBoundsAsIntrinsicSize && viewBox != null) {
            svgWidth = viewBox.width
            svgHeight = viewBox.height
        } else {
            svgWidth = svg.root?.width?.value ?: 0f
            svgHeight = svg.root?.height?.value ?: 0f
        }

        val bitmapWidth: Int
        val bitmapHeight: Int
        val (dstWidth, dstHeight) = getDstSize(svgWidth, svgHeight, options.scale)
        if (svgWidth > 0f && svgHeight > 0f) {
            val multiplier = DecodeUtils.computeSizeMultiplier(
                srcWidth = svgWidth,
                srcHeight = svgHeight,
                dstWidth = dstWidth.toFloat(),
                dstHeight = dstHeight.toFloat(),
                scale = options.scale,
            )
            bitmapWidth = (multiplier * svgWidth).toInt()
            bitmapHeight = (multiplier * svgHeight).toInt()
        } else {
            bitmapWidth = dstWidth
            bitmapHeight = dstHeight
        }

        // Set the SVG's view box to enable scaling if it is not set.
        if (viewBox == null && svgWidth > 0f && svgHeight > 0f) {
            svg.root?.viewBox = Rect.makeWH(svgWidth, svgHeight)
        }

        svg.root?.width = SVGLength(
            value = 100f,
            unit = SVGLengthUnit.PERCENTAGE,
        )

        svg.root?.height = SVGLength(
            value = 100f,
            unit = SVGLengthUnit.PERCENTAGE,
        )

        svg.setContainerSize(bitmapWidth.toFloat(), bitmapHeight.toFloat())

        return DecodeResult(
            image = svg.asCoilImage(bitmapWidth, bitmapHeight),
            isSampled = true, // SVGs can always be re-decoded at a higher resolution.
        )
    }

    private fun getDstSize(srcWidth: Float, srcHeight: Float, scale: Scale): Pair<Int, Int> {
        if (options.size.isOriginal) {
            val dstWidth = if (srcWidth > 0) srcWidth.roundToInt() else SVG_DEFAULT_SIZE
            val dstHeight = if (srcHeight > 0) srcHeight.roundToInt() else SVG_DEFAULT_SIZE
            return dstWidth to dstHeight
        } else {
            val (dstWidth, dstHeight) = options.size
            return dstWidth.toPx(scale) to dstHeight.toPx(scale)
        }
    }

    actual class Factory actual constructor(
        val useViewBoundsAsIntrinsicSize: Boolean,
    ) : Decoder.Factory {

        override fun create(
            result: SourceFetchResult,
            options: Options,
            imageLoader: ImageLoader,
        ): Decoder? {
            if (!isApplicable(result)) return null
            return SvgDecoder(result.source, options, useViewBoundsAsIntrinsicSize)
        }

        private fun isApplicable(result: SourceFetchResult): Boolean {
            return result.mimeType == MIME_TYPE_SVG || DecodeUtils.isSvg(result.source.source())
        }
    }
}
