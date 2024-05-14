package coil3.test

import android.content.res.Resources
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.core.graphics.applyCanvas
import androidx.core.graphics.createBitmap
import coil3.Image
import coil3.annotation.Poko
import coil3.annotation.ExperimentalCoilApi

@ExperimentalCoilApi
@Poko
actual class FakeImage actual constructor(
    actual override val width: Int,
    actual override val height: Int,
    actual override val size: Long,
    actual override val shareable: Boolean,
    actual val color: Int,
) : Image {
    override fun asDrawable(resources: Resources): Drawable {
        val bitmap = createBitmap(width, height)
        bitmap.applyCanvas { drawColor(color) }
        return BitmapDrawable(resources, bitmap)
    }
}
