@file:JvmName("-FakeUtils")

package coil

import android.graphics.Bitmap
import android.os.Build.VERSION.SDK_INT
import java.util.Collections

internal fun <T> Collection<T>.toImmutableSet(): Set<T> = when (size) {
    0 -> emptySet()
    1 -> Collections.singleton(first())
    else -> Collections.unmodifiableSet(LinkedHashSet(this))
}

internal fun <K, V> Map<K, V>.toImmutableMap(): Map<K, V> = when (size) {
    0 -> emptyMap()
    1 -> entries.first().let { (key, value) -> Collections.singletonMap(key, value) }
    else -> Collections.unmodifiableMap(LinkedHashMap(this))
}

@Suppress("DEPRECATION")
internal val Bitmap.Config?.bytesPerPixel: Int
    get() = when {
        this == Bitmap.Config.ALPHA_8 -> 1
        this == Bitmap.Config.RGB_565 -> 2
        this == Bitmap.Config.ARGB_4444 -> 2
        SDK_INT >= 26 && this == Bitmap.Config.RGBA_F16 -> 8
        else -> 4
    }

internal val Bitmap.allocationByteCountCompat: Int
    get() {
        check(!isRecycled) {
            "Cannot obtain size for recycled bitmap: $this [$width x $height] + $config"
        }

        return try {
            allocationByteCount
        } catch (_: Exception) {
            width * height * config.bytesPerPixel
        }
    }
