package coil3.test

import android.content.Context
import android.graphics.drawable.Drawable
import coil3.Image

actual class FakeImage actual constructor(
    override val width: Int,
    override val height: Int,
    override val size: Long,
    override val shareable: Boolean,
) : Image {
    override fun asDrawable(context: Context): Drawable {
        throw UnsupportedOperationException()
    }
}
