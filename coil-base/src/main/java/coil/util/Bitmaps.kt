@file:JvmName("-Bitmaps")
@file:Suppress("NOTHING_TO_INLINE")

package coil.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Build.VERSION.SDK_INT
import androidx.core.graphics.drawable.toDrawable

@Suppress("DEPRECATION")
internal val Bitmap.Config?.bytesPerPixel: Int
    get() = when {
        this == Bitmap.Config.ALPHA_8 -> 1
        this == Bitmap.Config.RGB_565 -> 2
        this == Bitmap.Config.ARGB_4444 -> 2
        SDK_INT >= 26 && this == Bitmap.Config.RGBA_F16 -> 8
        else -> 4
    }

/**
 * Returns the in memory size of this [Bitmap] in bytes.
 * This value will not change over the lifetime of a bitmap.
 */
internal val Bitmap.allocationByteCountCompat: Int
    get() {
        check(!isRecycled) { "Cannot obtain size for recycled bitmap: $this [$width x $height] + $config" }

        return try {
            if (SDK_INT >= 19) {
                allocationByteCount
            } else {
                rowBytes * height
            }
        } catch (_: Exception) {
            Utils.calculateAllocationByteCount(width, height, config)
        }
    }

internal val Bitmap.Config.isHardware: Boolean
    get() = SDK_INT >= 26 && this == Bitmap.Config.HARDWARE

/** Guard against null bitmap configs. */
internal val Bitmap.safeConfig: Bitmap.Config
    get() = config ?: Bitmap.Config.ARGB_8888

internal inline fun Bitmap.toDrawable(context: Context): BitmapDrawable = toDrawable(context.resources)

/** Convert null and [Bitmap.Config.HARDWARE] configs to [Bitmap.Config.ARGB_8888]. */
internal fun Bitmap.Config?.toSoftware(): Bitmap.Config {
    return if (this == null || isHardware) Bitmap.Config.ARGB_8888 else this
}
