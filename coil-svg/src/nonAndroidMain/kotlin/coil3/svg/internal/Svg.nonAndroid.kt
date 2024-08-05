package coil3.svg.internal

import coil3.Image
import coil3.request.Options
import coil3.svg.SvgImage
import kotlin.jvm.JvmInline
import okio.BufferedSource
import org.jetbrains.skia.Data
import org.jetbrains.skia.Rect
import org.jetbrains.skia.svg.SVGDOM
import org.jetbrains.skia.svg.SVGLength
import org.jetbrains.skia.svg.SVGLengthUnit

/**
 * An SVG parser implementation backed by https://api.skia.org/classSkSVGDOM.html/.
 */
internal actual fun parseSvg(source: BufferedSource): Svg {
    return SkiaSvg(SVGDOM(Data.makeFromBytes(source.readByteArray())))
}

@JvmInline
private value class SkiaSvg(
    private val svg: SVGDOM,
) : Svg {
    override val viewBox: FloatArray?
        get() = svg.root?.viewBox?.run { floatArrayOf(left, top, right, bottom) }
    override val width: Float
        get() = svg.root?.width.toFloat()
    override val height: Float
        get() = svg.root?.height.toFloat()

    override fun viewBox(value: FloatArray) {
        svg.root?.viewBox = Rect.makeLTRB(value[0], value[1], value[2], value[3])
    }

    override fun width(value: String) {
        svg.root?.width = parseSVGLength(value)
    }

    override fun height(value: String) {
        svg.root?.height = parseSVGLength(value)
    }

    override fun options(options: Options) {
        // Unsupported.
    }

    override fun asImage(width: Int, height: Int): Image {
        return SvgImage(svg, width, height)
    }

    private fun SVGLength?.toFloat(): Float {
        return when (this?.unit) {
            SVGLengthUnit.NUMBER,
            SVGLengthUnit.PX -> value
            else -> -1f
        }
    }

    private fun parseSVGLength(value: String): SVGLength {
        val suffix: String
        val unit: SVGLengthUnit

        if (value.endsWith("%")) {
            suffix = "%"
            unit = SVGLengthUnit.PERCENTAGE
        } else if (value.endsWith("px")) {
            suffix = "px"
            unit = SVGLengthUnit.PX
        } else {
            throw UnsupportedOperationException()
        }

        return SVGLength(
            value = value.substringBefore(suffix).toFloat(),
            unit = unit,
        )
    }
}
