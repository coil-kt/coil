package coil3

import coil3.annotation.ExperimentalCoilApi
import coil3.annotation.Poko
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Rect

@ExperimentalCoilApi
@Poko
actual class ColorImage actual constructor(
    actual val color: Int,
    actual override val width: Int,
    actual override val height: Int,
    actual override val size: Long,
    actual override val shareable: Boolean,
) : Image {
    private var lazyPaint: Paint? = null
    private var lazyRect: Rect? = null

    actual override fun draw(canvas: Canvas) {
        val paint = lazyPaint ?: run {
            Paint()
                .apply { color = this@ColorImage.color }
                .also { lazyPaint = it }
        }
        if (width >= 0 && height >= 0) {
            val rect = lazyRect ?: run {
                Rect.makeWH(width.toFloat(), height.toFloat())
                    .also { lazyRect = it }
            }
            canvas.drawRect(rect, paint)
        } else {
            canvas.drawPaint(paint)
        }
    }
}