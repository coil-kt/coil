package coil3.svg

import coil3.Canvas
import coil3.Image
import coil3.annotation.Poko
import coil3.svg.internal.SVG_SIZE_BYTES
import org.jetbrains.skia.svg.SVGDOM

@Poko
class SvgImage(
    val svg: SVGDOM,
    override val width: Int,
    override val height: Int,
) : Image {

    override val size: Long
        get() = SVG_SIZE_BYTES

    override val shareable: Boolean
        get() = true

    override fun draw(canvas: Canvas) {
        svg.render(canvas)
    }
}
