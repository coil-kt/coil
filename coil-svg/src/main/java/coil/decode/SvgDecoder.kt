package coil.decode

import android.graphics.Canvas
import android.graphics.RectF
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toDrawable
import coil.ImageLoader
import coil.fetch.SourceResult
import coil.request.Options
import coil.request.css
import coil.size.Scale
import coil.size.isOriginal
import coil.util.toPx
import coil.util.toSoftware
import com.caverock.androidsvg.RenderOptions
import com.caverock.androidsvg.SVG
import kotlin.math.roundToInt
import kotlinx.coroutines.runInterruptible

/**
 * A [Decoder] that uses [AndroidSVG](https://bigbadaboom.github.io/androidsvg/) to decode SVG
 * files.
 *
 * @param useViewBoundsAsIntrinsicSize If true, uses the SVG's view bounds as the intrinsic size for
 *  the SVG. If false, uses the SVG's width/height as the intrinsic size for the SVG.
 */
class SvgDecoder @JvmOverloads constructor(
    private val source: ImageSource,
    private val options: Options,
    val useViewBoundsAsIntrinsicSize: Boolean = true
) : Decoder {

    override suspend fun decode() = runInterruptible {
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
        val (dstWidth, dstHeight) = getDstSize(svgWidth, svgHeight, options.scale)
        if (svgWidth > 0 && svgHeight > 0) {
            val multiplier = DecodeUtils.computeSizeMultiplier(
                srcWidth = svgWidth,
                srcHeight = svgHeight,
                dstWidth = dstWidth,
                dstHeight = dstHeight,
                scale = options.scale
            )
            bitmapWidth = (multiplier * svgWidth).toInt()
            bitmapHeight = (multiplier * svgHeight).toInt()
        } else {
            bitmapWidth = dstWidth.roundToInt()
            bitmapHeight = dstHeight.roundToInt()
        }

        // Set the SVG's view box to enable scaling if it is not set.
        if (viewBox == null && svgWidth > 0 && svgHeight > 0) {
            svg.setDocumentViewBox(0f, 0f, svgWidth, svgHeight)
        }

        svg.setDocumentWidth("100%")
        svg.setDocumentHeight("100%")

        val bitmap = createBitmap(bitmapWidth, bitmapHeight, options.config.toSoftware())
        val renderOptions = options.parameters.css()?.let { RenderOptions().css(it) }
        svg.renderToCanvas(Canvas(bitmap), renderOptions)

        DecodeResult(
            drawable = bitmap.toDrawable(options.context.resources),
            isSampled = true // SVGs can always be re-decoded at a higher resolution.
        )
    }

    private fun getDstSize(srcWidth: Float, srcHeight: Float, scale: Scale): Pair<Float, Float> {
        if (options.size.isOriginal) {
            val dstWidth = if (srcWidth > 0) srcWidth else DEFAULT_SIZE
            val dstHeight = if (srcHeight > 0) srcHeight else DEFAULT_SIZE
            return dstWidth to dstHeight
        } else {
            val (dstWidth, dstHeight) = options.size
            return dstWidth.toPx(scale) to dstHeight.toPx(scale)
        }
    }

    class Factory @JvmOverloads constructor(
        val useViewBoundsAsIntrinsicSize: Boolean = true
    ) : Decoder.Factory {

        override fun create(result: SourceResult, options: Options, imageLoader: ImageLoader): Decoder? {
            if (!isApplicable(result)) return null
            return SvgDecoder(result.source, options, useViewBoundsAsIntrinsicSize)
        }

        private fun isApplicable(result: SourceResult): Boolean {
            return result.mimeType == MIME_TYPE_SVG || DecodeUtils.isSvg(result.source.source())
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            return other is Factory &&
                useViewBoundsAsIntrinsicSize == other.useViewBoundsAsIntrinsicSize
        }

        override fun hashCode() = useViewBoundsAsIntrinsicSize.hashCode()
    }

    companion object {
        private const val MIME_TYPE_SVG = "image/svg+xml"
        private const val DEFAULT_SIZE = 512f
        const val CSS_KEY = "coil#css"
    }
}
