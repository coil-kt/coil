package coil3.svg

import coil3.ImageLoader
import coil3.decode.DecodeResult
import coil3.decode.Decoder
import coil3.decode.ImageSource
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import kotlin.jvm.JvmOverloads

/**
 * A [Decoder] that uses [AndroidSVG](https://bigbadaboom.github.io/androidsvg/)
 * and [SVGDOM](https://api.skia.org/classSkSVGDOM.html/) to decode SVG
 * files.
 *
 * @param useViewBoundsAsIntrinsicSize If true, uses the SVG's view bounds as the intrinsic size for
 *  the SVG. If false, uses the SVG's width/height as the intrinsic size for the SVG.
 * @param renderToBitmap If true, renders the SVG to a bitmap immediately after decoding. Else, the
 *  SVG will be rendered at draw time. Rendering at draw time is more memory efficient, but
 *  depending on the complexity of the SVG, can be slow.
 */
expect class SvgDecoder(
    source: ImageSource,
    options: Options,
    useViewBoundsAsIntrinsicSize: Boolean = true,
    renderToBitmap: Boolean = true,
) : Decoder {

    override suspend fun decode(): DecodeResult?

    class Factory @JvmOverloads constructor(
        useViewBoundsAsIntrinsicSize: Boolean = true,
        renderToBitmap: Boolean = true,
    ) : Decoder.Factory {
        override fun create(
            result: SourceFetchResult,
            options: Options,
            imageLoader: ImageLoader,
        ): Decoder?
    }
}
