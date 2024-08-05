package coil3.svg

import coil3.Image
import coil3.ImageLoader
import coil3.asImage
import coil3.decode.DecodeResult
import coil3.decode.DecodeUtils
import coil3.decode.Decoder
import coil3.decode.ImageSource
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import coil3.request.maxBitmapSize
import coil3.svg.internal.MIME_TYPE_SVG
import coil3.svg.internal.SVG_DEFAULT_SIZE
import coil3.svg.internal.Svg
import coil3.svg.internal.parse
import coil3.svg.internal.runInterruptible
import coil3.toBitmap
import coil3.util.component1
import coil3.util.component2
import kotlin.math.roundToInt
import okio.use

/**
 * A [Decoder] that decodes SVGs. Relies on external dependencies to parse and decode the SVGs
 * (see [Svg]).
 *
 * @param useViewBoundsAsIntrinsicSize If true, uses the SVG's view bounds as the intrinsic size for
 *  the SVG. If false, uses the SVG's width/height as the intrinsic size for the SVG.
 * @param renderToBitmap If true, renders the SVG to a bitmap immediately after decoding. Else, the
 *  SVG will be rendered at draw time. Rendering at draw time is more memory efficient, but
 *  depending on the complexity of the SVG, can be slow.
 */
class SvgDecoder(
    private val source: ImageSource,
    private val options: Options,
    val useViewBoundsAsIntrinsicSize: Boolean,
    val renderToBitmap: Boolean,
) : Decoder {

    override suspend fun decode() = runInterruptible {
        val svg = source.source().use(Svg::parse)

        val svgWidth: Float
        val svgHeight: Float
        val viewBox: FloatArray? = svg.viewBox

        if (useViewBoundsAsIntrinsicSize && viewBox != null) {
            svgWidth = viewBox[2] - viewBox[0]
            svgHeight = viewBox[3] - viewBox[1]
        } else {
            svgWidth = svg.width
            svgHeight = svg.height
        }

        val bitmapWidth: Int
        val bitmapHeight: Int
        val (dstWidth, dstHeight) = DecodeUtils.computeDstSize(
            srcWidth = if (svgWidth > 0f) svgWidth.roundToInt() else SVG_DEFAULT_SIZE,
            srcHeight = if (svgHeight > 0f) svgHeight.roundToInt() else SVG_DEFAULT_SIZE,
            targetSize = options.size,
            scale = options.scale,
            maxSize = options.maxBitmapSize,
        )
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

            // Set the SVG's view box to enable scaling if it is not set.
            if (viewBox == null) {
                svg.viewBox(floatArrayOf(0f, 0f, svgWidth, svgHeight))
            }
        } else {
            bitmapWidth = dstWidth
            bitmapHeight = dstHeight
        }

        svg.width("100%")
        svg.height("100%")
        svg.options(options)

        var image: Image = svg.asImage(bitmapWidth, bitmapHeight)
        if (renderToBitmap) {
            image = image.toBitmap().asImage()
        }

        DecodeResult(
            image = image,
            isSampled = true, // SVGs can always be re-decoded at a higher resolution.
        )
    }

    class Factory(
        val useViewBoundsAsIntrinsicSize: Boolean = true,
        val renderToBitmap: Boolean = true,
    ) : Decoder.Factory {

        override fun create(
            result: SourceFetchResult,
            options: Options,
            imageLoader: ImageLoader,
        ): Decoder? {
            if (!isApplicable(result)) return null
            return SvgDecoder(
                source = result.source,
                options = options,
                useViewBoundsAsIntrinsicSize = useViewBoundsAsIntrinsicSize,
                renderToBitmap = renderToBitmap,
            )
        }

        private fun isApplicable(result: SourceFetchResult): Boolean {
            return result.mimeType == MIME_TYPE_SVG || DecodeUtils.isSvg(result.source.source())
        }
    }
}
