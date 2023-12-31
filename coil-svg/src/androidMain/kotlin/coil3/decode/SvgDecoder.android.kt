package coil3.decode

import android.graphics.Canvas
import android.graphics.RectF
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toDrawable
import coil3.ImageLoader
import coil3.asCoilImage
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import coil3.request.bitmapConfig
import coil3.request.css
import coil3.size.Scale
import coil3.size.isOriginal
import coil3.util.toPx
import coil3.util.toSoftware
import com.caverock.androidsvg.RenderOptions
import com.caverock.androidsvg.SVG
import kotlin.math.roundToInt
import kotlinx.coroutines.runInterruptible

/**
 * A [Decoder] that uses [AndroidSVG](https://bigbadaboom.github.io/androidsvg/) and  to decode SVG
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
                scale = options.scale,
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

        val bitmap = createBitmap(bitmapWidth, bitmapHeight, options.bitmapConfig.toSoftware())
        val renderOptions = options.css?.let { RenderOptions().css(it) }
        svg.renderToCanvas(Canvas(bitmap), renderOptions)

        DecodeResult(
            image = bitmap.toDrawable(options.context.resources).asCoilImage(),
            isSampled = true, // SVGs can always be re-decoded at a higher resolution.
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

    actual class Factory @JvmOverloads actual constructor(
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
