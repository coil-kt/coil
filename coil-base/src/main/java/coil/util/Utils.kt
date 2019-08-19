@file:JvmName("CoilUtils")

package coil.util

import android.app.ActivityManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.O
import android.os.StatFs
import androidx.annotation.Px
import java.io.File

internal object Utils {

    private const val CACHE_DIRECTORY_NAME = "image_cache"

    private const val MIN_DISK_CACHE_SIZE: Long = 10 * 1024 * 1024 // 10MB
    private const val MAX_DISK_CACHE_SIZE: Long = 250 * 1024 * 1024 // 250MB

    private const val DISK_CACHE_PERCENTAGE = 0.02

    private const val STANDARD_MULTIPLIER = 0.25
    private const val LOW_MEMORY_MULTIPLIER = 0.15

    /** Return the in memory size of a [Bitmap] with the given width, height, and [Bitmap.Config]. */
    fun calculateAllocationByteCount(@Px width: Int, @Px height: Int, config: Bitmap.Config?): Int {
        return width * height * config.getBytesPerPixel()
    }

    fun getDefaultCacheDirectory(context: Context): File {
        return File(context.cacheDir, CACHE_DIRECTORY_NAME).apply { mkdirs() }
    }

    /** Modified from Picasso. */
    fun calculateDiskCacheSize(cacheDirectory: File): Long {
        return try {
            val cacheDir = StatFs(cacheDirectory.absolutePath)
            val size = DISK_CACHE_PERCENTAGE * cacheDir.getBlockCountCompat() * cacheDir.getBlockSizeCompat()
            return size.toLong().coerceIn(MIN_DISK_CACHE_SIZE, MAX_DISK_CACHE_SIZE)
        } catch (ignored: Exception) {
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
        return if (activityManager.isLowRawDeviceCompat()) LOW_MEMORY_MULTIPLIER else STANDARD_MULTIPLIER
    }

    fun getDefaultBitmapPoolPercentage(): Double {
        // Allocate less memory for bitmap pooling on Android O and above since we default to
        // hardware bitmaps, which cannot be added to the pool.
        return if (SDK_INT >= O) 0.25 else 0.5
    }

    fun getDefaultBitmapConfig(): Bitmap.Config {
        // Prefer hardware bitmaps on Android O and above since they are optimized for drawing
        // without transformations.
        return if (SDK_INT >= O) Bitmap.Config.HARDWARE else Bitmap.Config.ARGB_8888
    }
}
