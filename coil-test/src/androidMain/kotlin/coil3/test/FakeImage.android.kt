package coil3.test

import android.graphics.Paint
import coil3.Canvas
import coil3.Image
import coil3.annotation.Poko

@Deprecated(
    message = "This use case is better fulfilled by ColorImage.",
    replaceWith = ReplaceWith("ColorImage", ["coil3.test.ColorImage"]),
    level = DeprecationLevel.WARNING,
)
@Poko
actual class FakeImage actual constructor(
    actual override val width: Int,
    actual override val height: Int,
    actual override val size: Long,
    actual override val shareable: Boolean,
    actual val color: Int,
) : Image {
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
