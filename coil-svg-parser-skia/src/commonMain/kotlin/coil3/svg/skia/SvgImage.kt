package coil3.svg.skia

import coil3.Canvas
import coil3.Image
import coil3.annotation.Poko
import org.jetbrains.skia.svg.SVGDOM

@Poko
class SvgImage(
    val svg: SVGDOM,
    override val width: Int,
    override val height: Int,
) : Image {

    init {
        // TODO: Use `canvas.width/height` in `draw` once it's public.
        svg.setContainerSize(width.toFloat(), height.toFloat())
    }

    override val size: Long
        get() = 2048L

    override val shareable: Boolean
        get() = true

    override fun draw(canvas: Canvas) {
        render(this, canvas)
    }
}

internal expect fun render(image: SvgImage, canvas: Canvas)
