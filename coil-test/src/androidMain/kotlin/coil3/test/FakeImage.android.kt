package coil3.test

import android.graphics.Paint
import coil3.Canvas
import coil3.Image
import coil3.annotation.Poko

@Deprecated(
    message = "ColorImage supports the same functionality, has `color` as the first argument, " +
        "and is more easily accessible in coil-core instead of coil-test.",
    replaceWith = ReplaceWith("ColorImage", "coil3.ColorImage"),
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
