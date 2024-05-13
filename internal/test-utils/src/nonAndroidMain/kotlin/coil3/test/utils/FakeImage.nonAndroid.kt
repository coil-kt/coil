package coil3.test.utils

import coil3.Image
import org.jetbrains.skia.Bitmap

actual class FakeImage actual constructor(
    actual override val width: Int,
    actual override val height: Int,
    actual override val size: Int,
    actual override val shareable: Boolean,
) : Image {
    override fun toBitmap(): Bitmap {
        throw UnsupportedOperationException()
    }
}
