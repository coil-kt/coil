package coil.util

import android.app.ActivityManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.os.Build.VERSION.SDK_INT
import android.os.StatFs
import androidx.annotation.Px
import java.io.File

/** Private utility methods for Coil. */
internal object Utils {

    private const val CACHE_DIRECTORY_NAME = "image_cache"

    private const val MIN_DISK_CACHE_SIZE_BYTES = 10L * 1024 * 1024 // 10MB
    private const val MAX_DISK_CACHE_SIZE_BYTES = 250L * 1024 * 1024 // 250MB

    private const val DISK_CACHE_PERCENTAGE = 0.02

    private const val STANDARD_MULTIPLIER = 0.2
    private const val LOW_MEMORY_MULTIPLIER = 0.15

    private const val DEFAULT_MEMORY_CLASS_MEGABYTES = 256

    const val REQUEST_TYPE_ENQUEUE = 0
    const val REQUEST_TYPE_EXECUTE = 1

    /** Prefer hardware bitmaps on API 26 and above since they are optimized for drawing without transformations. */
    val DEFAULT_BITMAP_CONFIG get() = if (SDK_INT >= 26) Bitmap.Config.HARDWARE else Bitmap.Config.ARGB_8888

    /** Return the in memory size of a [Bitmap] with the given width, height, and [Bitmap.Config]. */
    fun calculateAllocationByteCount(@Px width: Int, @Px height: Int, config: Bitmap.Config?): Int {
        return width * height * config.bytesPerPixel
    }

    fun getDefaultCacheDirectory(context: Context): File {
        return File(context.cacheDir, CACHE_DIRECTORY_NAME).apply { mkdirs() }
    }

    /** Modified from Picasso. */
    fun calculateDiskCacheSize(cacheDirectory: File): Long {
        return try {
            val cacheDir = StatFs(cacheDirectory.absolutePath)
            val size = DISK_CACHE_PERCENTAGE * cacheDir.blockCountCompat * cacheDir.blockSizeCompat
            return size.toLong().coerceIn(MIN_DISK_CACHE_SIZE_BYTES, MAX_DISK_CACHE_SIZE_BYTES)
        } catch (_: Exception) {
            MIN_DISK_CACHE_SIZE_BYTES
        }
    }

    /** Modified from Picasso. */
    fun calculateAvailableMemorySize(context: Context, percentage: Double): Long {
        val memoryClassMegabytes = try {
            val activityManager: ActivityManager = context.requireSystemService()
            val isLargeHeap = (context.applicationInfo.flags and ApplicationInfo.FLAG_LARGE_HEAP) != 0
            if (isLargeHeap) activityManager.largeMemoryClass else activityManager.memoryClass
        } catch (_: Exception) {
            DEFAULT_MEMORY_CLASS_MEGABYTES
        }
        return (percentage * memoryClassMegabytes * 1024 * 1024).toLong()
    }

    fun getDefaultAvailableMemoryPercentage(context: Context): Double {
        return try {
            val activityManager: ActivityManager = context.requireSystemService()
            if (activityManager.isLowRamDeviceCompat) LOW_MEMORY_MULTIPLIER else STANDARD_MULTIPLIER
        } catch (_: Exception) {
            STANDARD_MULTIPLIER
        }
    }

    fun getDefaultBitmapPoolPercentage(): Double {
        return when {
            // Prefer immutable bitmaps (which cannot be pooled) on API 24 and greater.
            SDK_INT >= 24 -> 0.0
            // Bitmap pooling is most effective on APIs 19 to 23.
            SDK_INT >= 19 -> 0.5
            // The requirements for bitmap reuse are strict below API 19.
            else -> 0.25
        }
    }
}
