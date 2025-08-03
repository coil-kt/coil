package coil3.svg.skia

import coil3.Canvas

internal actual fun render(image: SvgImage, canvas: Canvas) {
    image.svg.render(canvas)
}
