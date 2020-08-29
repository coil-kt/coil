@file:Suppress("EXPERIMENTAL_API_USAGE", "NOTHING_TO_INLINE", "unused")

package coil.util

import android.graphics.Bitmap
import android.os.Looper
import androidx.core.graphics.createBitmap
import org.robolectric.Shadows

const val DEFAULT_BITMAP_SIZE = 40000 // 4 * 100 * 100

fun createBitmap(
    width: Int = 100,
    height: Int = 100,
    config: Bitmap.Config = Bitmap.Config.ARGB_8888,
    isMutable: Boolean = true
): Bitmap {
    val bitmap = createBitmap(width, height, config)
    Shadows.shadowOf(bitmap).setMutable(isMutable)
    return bitmap
}

fun executeQueuedMainThreadTasks() {
    Shadows.shadowOf(Looper.getMainLooper()).idle()
}
