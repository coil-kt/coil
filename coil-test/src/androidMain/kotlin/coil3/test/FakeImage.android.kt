package coil3.test

import coil3.Canvas
import coil3.Image
import coil3.annotation.ExperimentalCoilApi
import coil3.annotation.Poko

@ExperimentalCoilApi
@Poko
actual class FakeImage actual constructor(
    actual override val width: Int,
    actual override val height: Int,
    actual override val size: Long,
    actual override val shareable: Boolean,
    actual val color: Int,
) : Image {
    actual override fun draw(canvas: Canvas) {
        canvas.drawColor(color)
    }
}
