package coil.decode

import android.graphics.Canvas
import android.graphics.RectF
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toDrawable
import coil.ImageLoader
import coil.fetch.SourceResult
import coil.request.Options
import coil.size.OriginalSize
import coil.size.PixelSize
import coil.util.indexOf
import coil.util.toSoftware
import com.caverock.androidsvg.SVG
import kotlinx.coroutines.runInterruptible
import okio.BufferedSource
import okio.ByteString.Companion.encodeUtf8

/**
 * A [Decoder] that uses [AndroidSVG](https://bigbadaboom.github.io/androidsvg/) to decode SVG files.
 *
 * @param useViewBoundsAsIntrinsicSize If true, uses the SVG's view bounds as the intrinsic size for the SVG.
 *  If false, uses the SVG's width/height as the intrinsic size for the SVG.
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
        val bitmapWidth: Int
        val bitmapHeight: Int
        val viewBox: RectF? = svg.documentViewBox
        when (val size = options.size) {
            is PixelSize -> {
                if (useViewBoundsAsIntrinsicSize && viewBox != null) {
                    svgWidth = viewBox.width()
                    svgHeight = viewBox.height()
                } else {
                    svgWidth = svg.documentWidth
                    svgHeight = svg.documentHeight
                }

                if (svgWidth > 0 && svgHeight > 0) {
                    val multiplier = DecodeUtils.computeSizeMultiplier(
                        srcWidth = svgWidth,
                        srcHeight = svgHeight,
                        dstWidth = size.width.toFloat(),
                        dstHeight = size.height.toFloat(),
                        scale = options.scale
                    )
                    bitmapWidth = (multiplier * svgWidth).toInt()
                    bitmapHeight = (multiplier * svgHeight).toInt()
                } else {
                    bitmapWidth = size.width
                    bitmapHeight = size.height
                }
            }
            is OriginalSize -> {
                svgWidth = svg.documentWidth
                svgHeight = svg.documentHeight

                if (svgWidth > 0 && svgHeight > 0) {
                    bitmapWidth = svgWidth.toInt()
                    bitmapHeight = svgHeight.toInt()
                } else if (useViewBoundsAsIntrinsicSize && viewBox != null) {
                    bitmapWidth = viewBox.width().toInt()
                    bitmapHeight = viewBox.height().toInt()
                } else {
                    bitmapWidth = DEFAULT_SIZE
                    bitmapHeight = DEFAULT_SIZE
                }
            }
        }

        // Set the SVG's view box to enable scaling if it is not set.
        if (viewBox == null && svgWidth > 0 && svgHeight > 0) {
            svg.setDocumentViewBox(0f, 0f, svgWidth, svgHeight)
        }

        svg.setDocumentWidth("100%")
        svg.setDocumentHeight("100%")

        val bitmap = createBitmap(bitmapWidth, bitmapHeight, options.config.toSoftware())
        svg.renderToCanvas(Canvas(bitmap))

        DecodeResult(
            drawable = bitmap.toDrawable(options.context.resources),
            isSampled = true // SVGs can always be re-decoded at a higher resolution.
        )
    }

    class Factory @JvmOverloads constructor(
        val useViewBoundsAsIntrinsicSize: Boolean = true
    ) : Decoder.Factory {

        override fun create(result: SourceResult, options: Options, imageLoader: ImageLoader): Decoder? {
            if (!isApplicable(result)) return null
            return SvgDecoder(result.source, options, useViewBoundsAsIntrinsicSize)
        }

        private fun isApplicable(result: SourceResult): Boolean {
            return result.mimeType == MIME_TYPE_SVG || containsSvgTag(result.source.source())
        }

        private fun containsSvgTag(source: BufferedSource): Boolean {
            return source.rangeEquals(0, LEFT_ANGLE_BRACKET) &&
                source.indexOf(SVG_TAG, 0, SVG_TAG_SEARCH_THRESHOLD_BYTES) != -1L
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            return other is Factory && useViewBoundsAsIntrinsicSize == other.useViewBoundsAsIntrinsicSize
        }

        override fun hashCode() = useViewBoundsAsIntrinsicSize.hashCode()
    }

    private companion object {
        private const val MIME_TYPE_SVG = "image/svg+xml"
        private const val DEFAULT_SIZE = 512
        private const val SVG_TAG_SEARCH_THRESHOLD_BYTES = 1024L
        private val SVG_TAG = "<svg ".encodeUtf8()
        private val LEFT_ANGLE_BRACKET = "<".encodeUtf8()
    }
}
