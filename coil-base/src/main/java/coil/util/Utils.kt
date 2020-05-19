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

    private const val MIN_DISK_CACHE_SIZE: Long = 10 * 1024 * 1024 // 10MB
    private const val MAX_DISK_CACHE_SIZE: Long = 250 * 1024 * 1024 // 250MB

    private const val DISK_CACHE_PERCENTAGE = 0.02

    private const val STANDARD_MULTIPLIER = 0.2
    private const val LOW_MEMORY_MULTIPLIER = 0.15

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
            return size.toLong().coerceIn(MIN_DISK_CACHE_SIZE, MAX_DISK_CACHE_SIZE)
        } catch (_: Exception) {
            MIN_DISK_CACHE_SIZE
        }
    }

    /** Modified from Picasso. */
    fun calculateAvailableMemorySize(context: Context, percentage: Double): Long {
        val activityManager: ActivityManager = context.requireSystemService()
        val isLargeHeap = (context.applicationInfo.flags and ApplicationInfo.FLAG_LARGE_HEAP) != 0
        val memoryClassMegabytes = if (isLargeHeap) activityManager.largeMemoryClass else activityManager.memoryClass
        return (percentage * memoryClassMegabytes * 1024 * 1024).toLong()
    }

    fun getDefaultAvailableMemoryPercentage(context: Context): Double {
        val activityManager: ActivityManager = context.requireSystemService()
        return if (activityManager.isLowRamDeviceCompat) LOW_MEMORY_MULTIPLIER else STANDARD_MULTIPLIER
    }

    fun getDefaultBitmapPoolPercentage(): Double {
        // Allocate less memory for bitmap pooling on API 18 and below as the requirements
        // for bitmap reuse are quite strict.
        // Allocate less memory for bitmap pooling on API 26 and above since we default to
        // hardware bitmaps, which cannot be reused.
        return if (SDK_INT in 19..25) 0.5 else 0.25
    }

    fun getDefaultBitmapConfig(): Bitmap.Config {
        // Prefer hardware bitmaps on API 26 and above since they are optimized for drawing
        // without transformations.
        return if (SDK_INT >= 26) Bitmap.Config.HARDWARE else Bitmap.Config.ARGB_8888
    }
}
