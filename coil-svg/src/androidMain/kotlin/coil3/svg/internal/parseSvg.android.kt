package coil3.svg.internal

import coil3.Image
import coil3.request.Options
import coil3.svg.Svg
import coil3.svg.Svg.ViewBox
import coil3.svg.SvgImage
import coil3.svg.css
import coil3.svg.height
import coil3.svg.width
import com.caverock.androidsvg.RenderOptions
import com.caverock.androidsvg.SVG
import okio.BufferedSource

/**
 * An SVG parser implementation backed by https://bigbadaboom.github.io/androidsvg/.
 */
internal actual fun parseSvg(source: BufferedSource): Svg {
    return AndroidSvg(SVG.getFromInputStream(source.inputStream()))
}

private class AndroidSvg(
    private val svg: SVG,
) : Svg {
    private var renderOptions: RenderOptions? = null

    override val viewBox: ViewBox?
        get() = svg.documentViewBox?.run { ViewBox(left, top, right, bottom) }
    override val width: Float
        get() = svg.documentWidth
    override val height: Float
        get() = svg.documentHeight

    override fun viewBox(value: ViewBox) {
        svg.setDocumentViewBox(value.left, value.top, value.width, value.height)
    }

    override fun width(value: String) {
        svg.setDocumentWidth(value)
    }

    override fun height(value: String) {
        svg.setDocumentHeight(value)
    }

    override fun options(options: Options) {
        options.css?.let { css ->
            renderOptions = RenderOptions().apply { css(css) }
        }
    }

    override fun asImage(width: Int, height: Int): Image {
        return SvgImage(svg, renderOptions, width, height)
    }
}
