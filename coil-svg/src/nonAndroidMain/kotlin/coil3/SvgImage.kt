package coil3

import coil3.annotation.ExperimentalCoilApi
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.svg.SVGDOM

@ExperimentalCoilApi
internal fun SVGDOM.asCoilImage(
    width: Int,
    height: Int,
): Image = SvgImage(
    svg = this,
    width = width,
    height = height,
)

@ExperimentalCoilApi
private class SvgImage(
    val svg: SVGDOM,
    override val width: Int,
    override val height: Int,
) : DrawableImage() {
    override val size: Long
        get() = 4L * width * height

    override val shareable: Boolean = true

    override fun Canvas.onDraw() {
        svg.render(this)
    }
}
