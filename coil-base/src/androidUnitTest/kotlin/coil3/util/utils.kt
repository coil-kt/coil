package coil3.util

import android.content.Context
import android.graphics.Bitmap
import androidx.core.graphics.createBitmap
import coil3.request.ImageRequest
import org.robolectric.Shadows

fun createBitmap(
    width: Int = 100,
    height: Int = 100,
    config: Bitmap.Config = Bitmap.Config.ARGB_8888,
    isMutable: Boolean = !config.isHardware
): Bitmap {
    val bitmap = createBitmap(width, height, config)
    Shadows.shadowOf(bitmap).setMutable(isMutable)
    return bitmap
}

inline fun createRequest(
    context: Context,
    builder: ImageRequest.Builder.() -> Unit = {}
): ImageRequest = ImageRequest.Builder(context).data(Unit).apply(builder).build()
