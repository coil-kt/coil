package coil3.svg

import android.graphics.RectF
import coil3.ImageLoader
import coil3.decode.DecodeResult
import coil3.decode.DecodeUtils
import coil3.decode.Decoder
import coil3.decode.ImageSource
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import coil3.svg.internal.MIME_TYPE_SVG
import coil3.svg.internal.getDstSize
import com.caverock.androidsvg.RenderOptions
import com.caverock.androidsvg.SVG
import kotlinx.coroutines.runInterruptible

/**
 * A [Decoder] that uses [AndroidSVG](https://bigbadaboom.github.io/androidsvg/) to decode SVG
 * files.
 *
 * @param useViewBoundsAsIntrinsicSize If true, uses the SVG's view bounds as the intrinsic size for
 *  the SVG. If false, uses the SVG's width/height as the intrinsic size for the SVG.
 */
actual class SvgDecoder @JvmOverloads actual constructor(
    private val source: ImageSource,
    private val options: Options,
    val useViewBoundsAsIntrinsicSize: Boolean,
) : Decoder {

    actual override suspend fun decode(): DecodeResult? = runInterruptible {
        val svg = source.source().use { SVG.getFromInputStream(it.inputStream()) }

        val svgWidth: Float
        val svgHeight: Float
        val viewBox: RectF? = svg.documentViewBox
        if (useViewBoundsAsIntrinsicSize && viewBox != null) {
            svgWidth = viewBox.width()
            svgHeight = viewBox.height()
        } else {
            svgWidth = svg.documentWidth
            svgHeight = svg.documentHeight
        }

        val bitmapWidth: Int
        val bitmapHeight: Int
        val (dstWidth, dstHeight) = getDstSize(options, svgWidth, svgHeight, options.scale)
        if (svgWidth > 0 && svgHeight > 0) {
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
        if (viewBox == null && svgWidth > 0 && svgHeight > 0) {
            svg.setDocumentViewBox(0f, 0f, svgWidth, svgHeight)
        }

        svg.setDocumentWidth("100%")
        svg.setDocumentHeight("100%")

        val renderOptions = options.css?.let { RenderOptions().css(it) }

        DecodeResult(
            image = SvgImage(svg, renderOptions, bitmapWidth, bitmapHeight),
            isSampled = true, // SVGs can always be re-decoded at a higher resolution.
        )
    }

    actual class Factory @JvmOverloads actual constructor(
        val useViewBoundsAsIntrinsicSize: Boolean,
    ) : Decoder.Factory {

        actual override fun create(
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
