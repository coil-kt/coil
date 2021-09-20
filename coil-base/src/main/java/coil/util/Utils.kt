@file:JvmName("-Utils")

package coil.util

import android.app.ActivityManager
import android.content.Context
import android.content.pm.ApplicationInfo.FLAG_LARGE_HEAP
import android.graphics.Bitmap
import android.os.Build.VERSION.SDK_INT
import coil.ImageLoader
import coil.disk.DiskCache
import coil.transform.Transformation

/** Private utility methods for Coil. */
internal object Utils {

    private const val STANDARD_MEMORY_MULTIPLIER = 0.2
    private const val LOW_MEMORY_MULTIPLIER = 0.15
    private const val DEFAULT_MEMORY_CLASS_MEGABYTES = 256

    const val REQUEST_TYPE_ENQUEUE = 0
    const val REQUEST_TYPE_EXECUTE = 1

    fun calculateMemoryCacheSize(context: Context, percent: Double): Int {
        val memoryClassMegabytes = try {
            val activityManager: ActivityManager = context.requireSystemService()
            val isLargeHeap = (context.applicationInfo.flags and FLAG_LARGE_HEAP) != 0
            if (isLargeHeap) activityManager.largeMemoryClass else activityManager.memoryClass
        } catch (_: Exception) {
            DEFAULT_MEMORY_CLASS_MEGABYTES
        }
        return (percent * memoryClassMegabytes * 1024 * 1024).toInt()
    }

    fun defaultMemoryCacheSizePercent(context: Context): Double {
        return try {
            val activityManager: ActivityManager = context.requireSystemService()
            if (activityManager.isLowRamDevice) LOW_MEMORY_MULTIPLIER else STANDARD_MEMORY_MULTIPLIER
        } catch (_: Exception) {
            STANDARD_MEMORY_MULTIPLIER
        }
    }
}

/**
 * An allowlist of valid bitmap configs for the input and output bitmaps of
 * [Transformation.transform].
 */
@JvmField internal val VALID_TRANSFORMATION_CONFIGS = if (SDK_INT >= 26) {
    arrayOf(Bitmap.Config.ARGB_8888, Bitmap.Config.RGBA_F16)
} else {
    arrayOf(Bitmap.Config.ARGB_8888)
}

/**
 * Prefer hardware bitmaps on API 26 and above since they are optimized for drawing without
 * transformations.
 */
@JvmField internal val DEFAULT_BITMAP_CONFIG = if (SDK_INT >= 26) {
    Bitmap.Config.HARDWARE
} else {
    Bitmap.Config.ARGB_8888
}

internal class Option<T : Any>(val value: T?)

/**
 * Holds the singleton instance of the disk cache. We need to have a singleton disk cache instance
 * to support creating multiple [ImageLoader]s without specifying the disk cache directory.
 *
 * @see DiskCache.Builder.directory
 */
private var singletonDiskCache: DiskCache? = null

@Synchronized
internal fun singletonDiskCache(context: Context): DiskCache {
    singletonDiskCache?.let { return it }
    val diskCache = DiskCache.Builder(context)
        .directory(context.safeCacheDir.resolve("coil_image_cache"))
        .build()
    return diskCache.also { singletonDiskCache = it }
}
