package coil3.decode

import coil3.ImageLoader
import coil3.asCoilImage
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import coil3.size.Scale
import coil3.size.isOriginal
import coil3.util.toPx
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import org.jetbrains.skia.*
import org.jetbrains.skia.svg.SVGDOM

/**
 * A [Decoder] that uses [AndroidSVG](https://bigbadaboom.github.io/androidsvg/) to decode SVG
 * files.
 *
 * @param useViewBoundsAsIntrinsicSize If true, uses the SVG's view bounds as the intrinsic size for
 *  the SVG. If false, uses the SVG's width/height as the intrinsic size for the SVG.
 */
class SvgDecoder(
    private val source: ImageSource,
    private val options: Options,
    val useViewBoundsAsIntrinsicSize: Boolean = true,
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
            svg.root?.viewBox = Rect.makeWH(svgWidth, svgHeight)
        }

        val bitmap = Bitmap().apply {
            allocN32Pixels(bitmapWidth, bitmapHeight)
        }

        val svgRootWidth = svg.root?.width?.value ?: svgWidth
        val svgRootHeight = svg.root?.height?.value ?: svgHeight

        val xScale: Float = bitmapWidth / svgRootWidth
        val yScale: Float = bitmapHeight / svgRootHeight

        val canvas = Canvas(bitmap)
        canvas.scale(xScale, yScale)
        svg.render(canvas)

        return DecodeResult(
            image = bitmap.asCoilImage(),
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

//    private fun calculateViewBoxTransform(
//        viewPort: com.caverock.androidsvg.SVG.Box,
//        viewBox: com.caverock.androidsvg.SVG.Box,
//        positioning: PreserveAspectRatio?,
//    ): android.graphics.Matrix {
//        val m: android.graphics.Matrix = android.graphics.Matrix()
//        if (positioning == null || positioning.getAlignment() == null) return m
//        val xScale: Float = viewPort.width / viewBox.width
//        val yScale: Float = viewPort.height / viewBox.height
//        var xOffset: Float = -viewBox.minX
//        var yOffset: Float = -viewBox.minY
//
//        // 'none' means scale both dimensions to fit the viewport
//        if (positioning == PreserveAspectRatio.STRETCH) {
//            m.preTranslate(viewPort.minX, viewPort.minY)
//            m.preScale(xScale, yScale)
//            m.preTranslate(xOffset, yOffset)
//            return m
//        }
//
//        // Otherwise, the aspect ratio of the image is kept.
//        // What scale are we going to use?
//        val scale = if (positioning.getScale() == PreserveAspectRatio.Scale.slice) max(
//            xScale.toDouble(),
//            yScale.toDouble(),
//        )
//            .toFloat() else min(xScale.toDouble(), yScale.toDouble()).toFloat()
//        // What size will the image end up being?
//        val imageW: Float = viewPort.width / scale
//        val imageH: Float = viewPort.height / scale
//        when (positioning.getAlignment()) {
//            PreserveAspectRatio.Alignment.xMidYMin, PreserveAspectRatio.Alignment.xMidYMid, PreserveAspectRatio.Alignment.xMidYMax -> xOffset -= (viewBox.width - imageW) / 2
//            PreserveAspectRatio.Alignment.xMaxYMin, PreserveAspectRatio.Alignment.xMaxYMid, PreserveAspectRatio.Alignment.xMaxYMax -> xOffset -= viewBox.width - imageW
//            else -> {}
//        }
//        when (positioning.getAlignment()) {
//            PreserveAspectRatio.Alignment.xMinYMid, PreserveAspectRatio.Alignment.xMidYMid, PreserveAspectRatio.Alignment.xMaxYMid -> yOffset -= (viewBox.height - imageH) / 2
//            PreserveAspectRatio.Alignment.xMinYMax, PreserveAspectRatio.Alignment.xMidYMax, PreserveAspectRatio.Alignment.xMaxYMax -> yOffset -= viewBox.height - imageH
//            else -> {}
//        }
//        m.preTranslate(viewPort.minX, viewPort.minY)
//        m.preScale(scale, scale)
//        m.preTranslate(xOffset, yOffset)
//        return m
//    }

    class Factory(
        val useViewBoundsAsIntrinsicSize: Boolean = true,
    ) : Decoder.Factory {

        override fun create(
            result: SourceFetchResult,
            options: Options,
            imageLoader: ImageLoader,
        ): Decoder? {
            if (!isApplicable(result).also { println("isApplicable svg: $it") }) return null
            return SvgDecoder(result.source, options, useViewBoundsAsIntrinsicSize)
        }

        private fun isApplicable(result: SourceFetchResult): Boolean {
            return result.mimeType == MIME_TYPE_SVG || DecodeUtils.isSvg(result.source.source())
        }
    }

    companion object {
        private const val MIME_TYPE_SVG = "image/svg+xml"
        private const val DEFAULT_SIZE = 512f
    }
}
