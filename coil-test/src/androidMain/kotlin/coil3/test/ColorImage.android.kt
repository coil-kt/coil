package coil3.test

import android.graphics.Paint
import coil3.Canvas
import coil3.Image
import coil3.annotation.Poko

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

    actual companion object {
        actual const val Black = 0xFF000000.toInt()
        actual const val White = 0xFFFFFFFF.toInt()
        actual const val Transparent = 0x00000000.toInt()
        actual const val Red = 0xFFFF0000.toInt()
        actual const val Green = 0xFF00FF00.toInt()
        actual const val Blue = 0xFF0000FF.toInt()
    }
}
