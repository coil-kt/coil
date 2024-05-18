package coil3.svg

import coil3.Canvas
import coil3.Image
import coil3.annotation.ExperimentalCoilApi
import coil3.annotation.Poko
import org.jetbrains.skia.svg.SVGDOM

@ExperimentalCoilApi
@Poko
internal class SvgImage(
    val svg: SVGDOM,
    override val width: Int,
    override val height: Int,
) : Image {

    override val size: Long
        get() = 4L * width * height

    override val shareable: Boolean
        get() = true

    override fun draw(canvas: Canvas) {
        svg.render(canvas)
    }
}
