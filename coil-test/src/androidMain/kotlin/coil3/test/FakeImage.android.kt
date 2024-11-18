package coil3.test

import android.graphics.Paint
import coil3.Canvas
import coil3.Image
import coil3.annotation.Poko

@Poko
actual class FakeImage actual constructor(
    actual val color: Int,
    actual override val width: Int,
    actual override val height: Int,
    actual override val size: Long,
    actual override val shareable: Boolean,
) : Image {

    @Deprecated("Kept for binary compatibility.", level = DeprecationLevel.HIDDEN)
    actual constructor(
        width: Int,
        height: Int,
        size: Long,
        shareable: Boolean,
        color: Int,
    ) : this(color, width, height, size, shareable)

    private var lazyPaint: Paint? = null

    actual override fun draw(canvas: Canvas) {
        val paint = lazyPaint ?: run {
            Paint()
                .apply { color = this@FakeImage.color }
                .also { lazyPaint = it }
        }

        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
    }
}
