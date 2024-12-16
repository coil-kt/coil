package coil3.test.utils

import coil3.Canvas
import coil3.Image
import coil3.annotation.Poko

/** An [Image] that renders nothing. */
@Poko
class FakeImage(
    override val width: Int = 100,
    override val height: Int = 100,
    override val size: Long = 4L * width * height,
    override val shareable: Boolean = true,
) : Image {
    override fun draw(canvas: Canvas) {
        // Draw nothing.
    }
}
