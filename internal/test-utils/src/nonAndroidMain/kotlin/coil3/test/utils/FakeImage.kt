package coil3.test.utils

import coil3.CoilPainter
import coil3.Image

actual class FakeImage actual constructor(
    override val width: Int,
    override val height: Int,
    override val size: Long,
    override val shareable: Boolean,
) : Image {
    override fun asPainter(): CoilPainter {
        throw UnsupportedOperationException()
    }
}
