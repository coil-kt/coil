package coil3.test

import coil3.Canvas
import coil3.Image
import coil3.annotation.Poko
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Rect

@Poko
actual class FakeImage actual constructor(
    actual override val width: Int,
    actual override val height: Int,
    actual override val size: Long,
    actual override val shareable: Boolean,
    actual val color: Int,
) : Image {
    private var lazyPaint: Paint? = null
    private var lazyRect: Rect? = null

    actual override fun draw(canvas: Canvas) {
        val paint = lazyPaint ?: run {
            Paint()
                .apply { color = this@FakeImage.color }
                .also { lazyPaint = it }
        }
        val rect = lazyRect ?: run {
            Rect.makeWH(width.toFloat(), height.toFloat())
                .also { lazyRect = it }
        }
        canvas.drawRect(rect, paint)
    }
}
