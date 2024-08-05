package coil3.svg.internal

import coil3.Image
import coil3.request.Options
import coil3.svg.SvgImage
import coil3.svg.css
import com.caverock.androidsvg.RenderOptions
import com.caverock.androidsvg.SVG
import okio.BufferedSource

/**
 * An SVG parser implementation backed by https://bigbadaboom.github.io/androidsvg/.
 */
internal actual fun Svg.Companion.parse(source: BufferedSource): Svg {
    return AndroidSvg(SVG.getFromInputStream(source.inputStream()))
}

private class AndroidSvg(
    private val svg: SVG,
) : Svg {
    private var renderOptions: RenderOptions? = null

    override val viewBox: FloatArray?
        get() = svg.documentViewBox?.run { floatArrayOf(left, top, right, bottom) }
    override val width: Float
        get() = svg.documentWidth
    override val height: Float
        get() = svg.documentHeight

    override fun viewBox(value: FloatArray) {
        svg.setDocumentViewBox(value[0], value[1], value[2] - value[0], value[3] - value[1])
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
