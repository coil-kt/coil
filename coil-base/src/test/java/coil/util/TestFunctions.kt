@file:Suppress("EXPERIMENTAL_API_USAGE", "NOTHING_TO_INLINE", "unused")

package coil.util

import android.graphics.Bitmap
import android.os.Build.VERSION.SDK_INT
import android.os.Looper
import androidx.core.graphics.createBitmap
import org.robolectric.Shadows

internal const val DEFAULT_BITMAP_SIZE = 40000 // 4 * 100 * 100

internal fun createBitmap(
    width: Int = 100,
    height: Int = 100,
    config: Bitmap.Config = Bitmap.Config.ARGB_8888,
    isMutable: Boolean = SDK_INT < 26 || config != Bitmap.Config.HARDWARE
): Bitmap {
    val bitmap = createBitmap(width, height, config)
    Shadows.shadowOf(bitmap).setMutable(isMutable)
    return bitmap
}

internal fun executeQueuedMainThreadTasks() {
    Shadows.shadowOf(Looper.getMainLooper()).idle()
}
