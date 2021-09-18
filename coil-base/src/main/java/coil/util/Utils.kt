package coil.util

import android.app.ActivityManager
import android.content.Context
import android.content.pm.ApplicationInfo.FLAG_LARGE_HEAP
import android.graphics.Bitmap
import android.os.Build.VERSION.SDK_INT
import coil.disk.DiskCache
import coil.transform.Transformation

/** Private utility methods for Coil. */
internal object Utils {

    private const val STANDARD_MEMORY_MULTIPLIER = 0.2
    private const val LOW_MEMORY_MULTIPLIER = 0.15
    private const val DEFAULT_MEMORY_CLASS_MEGABYTES = 256
    private const val CACHE_DIRECTORY_NAME = "image_cache"

    const val REQUEST_TYPE_ENQUEUE = 0
    const val REQUEST_TYPE_EXECUTE = 1

    /**
     * An allowlist of valid bitmap configs for the input and output bitmaps of
     * [Transformation.transform].
     */
    @JvmField val VALID_TRANSFORMATION_CONFIGS = if (SDK_INT >= 26) {
        arrayOf(Bitmap.Config.ARGB_8888, Bitmap.Config.RGBA_F16)
    } else {
        arrayOf(Bitmap.Config.ARGB_8888)
    }

    /**
     * Prefer hardware bitmaps on API 26 and above since they are optimized for drawing without
     * transformations.
     */
    @JvmField val DEFAULT_BITMAP_CONFIG = if (SDK_INT >= 26) {
        Bitmap.Config.HARDWARE
    } else {
        Bitmap.Config.ARGB_8888
    }

    private var defaultDiskCache: DiskCache? = null

    @Synchronized
    fun defaultDiskCache(context: Context): DiskCache {
        defaultDiskCache?.let { return it }
        return DiskCache.Builder(context)
            .directory(context.safeCacheDir.resolve(CACHE_DIRECTORY_NAME))
            .build()
            .also { defaultDiskCache = it }
    }

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
