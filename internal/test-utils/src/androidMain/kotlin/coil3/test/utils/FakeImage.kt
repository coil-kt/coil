package coil3.test.utils

import android.content.res.Resources
import android.graphics.drawable.Drawable
import coil3.Image

actual class FakeImage actual constructor(
    override val width: Int,
    override val height: Int,
    override val size: Long,
    override val shareable: Boolean,
) : Image {
    override fun asDrawable(resources: Resources): Drawable {
        throw UnsupportedOperationException()
    }
}
