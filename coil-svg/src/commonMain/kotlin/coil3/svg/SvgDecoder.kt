package coil3.svg

import coil3.ImageLoader
import coil3.PlatformContext
import coil3.asImage
import coil3.decode.DecodeResult
import coil3.decode.DecodeUtils
import coil3.decode.Decoder
import coil3.decode.ImageSource
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import coil3.request.maxBitmapSize
import coil3.size.Size
import coil3.size.isOriginal
import coil3.svg.Svg.ViewBox
import coil3.svg.internal.MIME_TYPE_SVG
import coil3.svg.internal.SVG_DEFAULT_SIZE
import coil3.svg.internal.density
import coil3.svg.internal.runInterruptible
import coil3.toBitmap
import coil3.util.component1
import coil3.util.component2
import kotlin.jvm.JvmField
import kotlin.math.roundToInt
import okio.use

/**
 * A [Decoder] that decodes SVGs. Relies on external dependencies to parse and decode the SVGs.
 *
 * @param parser An [Svg.Parser] that converts bytes into an [Svg]. This property can be used
 *  to replace the default SVG parser with a different library.
 * @param density A function whose return value will be used to scale the output dimensions of the
 *  image if the target size is [Size.ORIGINAL].
 * @param useViewBoundsAsIntrinsicSize If true, uses the SVG's view bounds as the intrinsic size for
 *  the SVG. If false, uses the SVG's width/height as the intrinsic size for the SVG.
 * @param renderToBitmap If true, renders the SVG to a bitmap immediately after decoding. Else, the
 *  SVG will be rendered at draw time. Rendering at draw time is more memory efficient, but
 *  depending on the complexity of the SVG, can be slow.
 */
class SvgDecoder(
    private val source: ImageSource,
    private val options: Options,
    val parser: Svg.Parser = Svg.Parser.DEFAULT,
    val density: PlatformContext.() -> Float = NO_DENSITY,
    val useViewBoundsAsIntrinsicSize: Boolean = true,
    val renderToBitmap: Boolean = true,
) : Decoder {

    constructor(
        source: ImageSource,
        options: Options,
        useViewBoundsAsIntrinsicSize: Boolean = true,
        renderToBitmap: Boolean = true,
        scaleToDensity: Boolean = false,
    ) : this(
        source = source,
        options = options,
        parser = Svg.Parser.DEFAULT,
        density = if (scaleToDensity) PLATFORM_DENSITY else NO_DENSITY,
        useViewBoundsAsIntrinsicSize = useViewBoundsAsIntrinsicSize,
        renderToBitmap = renderToBitmap,
    )

    @Deprecated("Migrate to density.")
    val scaleToDensity: Boolean
        get() = density != NO_DENSITY

    override suspend fun decode() = runInterruptible {
        val svg = source.source().use(parser::parse)

        var svgWidth: Float
        var svgHeight: Float
        val viewBox: ViewBox? = svg.viewBox

        if (useViewBoundsAsIntrinsicSize && viewBox != null) {
            svgWidth = viewBox.width
            svgHeight = viewBox.height
        } else {
            svgWidth = svg.width
            svgHeight = svg.height
        }

        if (options.size.isOriginal) {
            val density = options.context.density()
            if (svgWidth > 0f) svgWidth *= density
            if (svgHeight > 0f) svgHeight *= density
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
                svg.viewBox = ViewBox(0f, 0f, svgWidth, svgHeight)
            }
        } else {
            bitmapWidth = dstWidth
            bitmapHeight = dstHeight
        }

        svg.width("100%")
        svg.height("100%")
        svg.options(options)

        var image = svg.asImage(bitmapWidth, bitmapHeight)
        if (renderToBitmap) {
            image = image.toBitmap().asImage()
        }

        DecodeResult(
            image = image,
            isSampled = renderToBitmap, // Rasterized SVGs can always be re-decoded at a higher resolution.
        )
    }

    class Factory(
        val parser: Svg.Parser = Svg.Parser.DEFAULT,
        val density: PlatformContext.() -> Float = NO_DENSITY,
        val useViewBoundsAsIntrinsicSize: Boolean = true,
        val renderToBitmap: Boolean = true,
    ) : Decoder.Factory {

        constructor(
            useViewBoundsAsIntrinsicSize: Boolean = true,
            renderToBitmap: Boolean = true,
            scaleToDensity: Boolean = false,
        ) : this(
            parser = Svg.Parser.DEFAULT,
            density = if (scaleToDensity) PLATFORM_DENSITY else NO_DENSITY,
            useViewBoundsAsIntrinsicSize = useViewBoundsAsIntrinsicSize,
            renderToBitmap = renderToBitmap,
        )

        @Deprecated("Migrate to density.")
        val scaleToDensity: Boolean
            get() = density != NO_DENSITY

        override fun create(
            result: SourceFetchResult,
            options: Options,
            imageLoader: ImageLoader,
        ): Decoder? {
            if (!isApplicable(result)) return null
            return SvgDecoder(
                source = result.source,
                options = options,
                parser = parser,
                density = density,
                useViewBoundsAsIntrinsicSize = useViewBoundsAsIntrinsicSize,
                renderToBitmap = renderToBitmap,
            )
        }

        private fun isApplicable(result: SourceFetchResult): Boolean {
            return result.mimeType == MIME_TYPE_SVG || DecodeUtils.isSvg(result.source.source())
        }
    }

    companion object {
        /**
         * A density implementation that always returns 1 (no scaling).
         */
        @JvmField val NO_DENSITY: PlatformContext.() -> Float = { 1f }

        /**
         * A density implementation that returns the platform's display density.
         *
         * NOTE: This currently behaves the same as [NO_DENSITY] on non-Android platforms.
         */
        @JvmField val PLATFORM_DENSITY: PlatformContext.() -> Float = { density }
    }
}
