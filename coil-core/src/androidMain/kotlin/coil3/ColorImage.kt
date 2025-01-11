package coil3

import android.graphics.Paint
import coil3.annotation.ExperimentalCoilApi
import coil3.annotation.Poko

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

    actual override fun draw(canvas: Canvas) {
        val paint = lazyPaint ?: run {
            Paint()
                .apply { color = this@ColorImage.color }
                .also { lazyPaint = it }
        }
        if (width >= 0 && height >= 0) {
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        } else {
            canvas.drawPaint(paint)
        }
    }
}
