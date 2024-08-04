package coil3.svg

import android.graphics.Canvas
import android.graphics.RectF
import androidx.core.graphics.createBitmap
import coil3.Image
import coil3.ImageLoader
import coil3.asImage
import coil3.decode.DecodeResult
import coil3.decode.DecodeUtils
import coil3.decode.Decoder
import coil3.decode.ImageSource
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import coil3.request.bitmapConfig
import coil3.request.maxBitmapSize
import coil3.svg.internal.MIME_TYPE_SVG
import coil3.svg.internal.SVG_DEFAULT_SIZE
import coil3.toBitmap
import coil3.util.component1
import coil3.util.component2
import coil3.util.toSoftware
import com.caverock.androidsvg.RenderOptions
import com.caverock.androidsvg.SVG
import kotlin.math.roundToInt
import kotlinx.coroutines.runInterruptible

/**
 * A [Decoder] that uses [AndroidSVG](https://bigbadaboom.github.io/androidsvg/) to decode SVG files.
 */
actual class SvgDecoder actual constructor(
    private val source: ImageSource,
    private val options: Options,
    val useViewBoundsAsIntrinsicSize: Boolean,
    val renderToBitmap: Boolean,
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
        val (dstWidth, dstHeight) = DecodeUtils.computeDstSize(
            srcWidth = if (svgWidth > 0) svgWidth.roundToInt() else SVG_DEFAULT_SIZE,
            srcHeight = if (svgHeight > 0) svgHeight.roundToInt() else SVG_DEFAULT_SIZE,
            targetSize = options.size,
            scale = options.scale,
            maxSize = options.maxBitmapSize,
        )
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

            // Set the SVG's view box to enable scaling if it is not set.
            if (viewBox == null) {
                svg.setDocumentViewBox(0f, 0f, svgWidth, svgHeight)
            }
        } else {
            bitmapWidth = dstWidth
            bitmapHeight = dstHeight
        }

        svg.setDocumentWidth("100%")
        svg.setDocumentHeight("100%")

        val renderOptions = options.css?.let { RenderOptions().css(it) }

        var image: Image = SvgImage(svg, renderOptions, bitmapWidth, bitmapHeight)
        if (renderToBitmap) {
            image = image.toBitmap().asImage()
        }

        DecodeResult(
            image = image,
            isSampled = true, // SVGs can always be re-decoded at a higher resolution.
        )
    }

    actual class Factory @JvmOverloads actual constructor(
        val useViewBoundsAsIntrinsicSize: Boolean,
        val renderToBitmap: Boolean,
    ) : Decoder.Factory {

        actual override fun create(
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
