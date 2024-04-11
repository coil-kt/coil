package coil3.test.utils

import android.content.res.Resources
import android.graphics.drawable.Drawable
import coil3.Image

actual class FakeImage actual constructor(
    actual override val width: Int,
    actual override val height: Int,
    actual override val size: Long,
    actual override val shareable: Boolean,
) : Image {
    override fun asDrawable(resources: Resources): Drawable {
        throw UnsupportedOperationException()
    }
}
