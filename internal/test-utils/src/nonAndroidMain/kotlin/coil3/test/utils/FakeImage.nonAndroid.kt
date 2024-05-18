package coil3.test.utils

import coil3.Canvas
import coil3.Image
import coil3.annotation.Poko

@Poko
actual class FakeImage actual constructor(
    actual override val width: Int,
    actual override val height: Int,
    actual override val size: Long,
    actual override val shareable: Boolean,
) : Image {
    actual override fun draw(canvas: Canvas) {
        // Draw nothing.
    }
}
