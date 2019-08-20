@file:JvmName("CoilUtils")

package coil.util

import android.app.ActivityManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.O
import android.os.StatFs
import androidx.annotation.Px
import coil.size.PixelSize
import coil.size.Size
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream

/** Private utility methods for Coil. */
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

    /** Return bitmap from drawable. */
    fun getBitmapFromDrawable(drawable: Drawable, size: Size? = null): Bitmap {
        if (drawable is BitmapDrawable) {
            return if (size is PixelSize) getScaledBitmap(drawable.bitmap, size.width, size.height) else drawable.bitmap
        }

        val bitmap = Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return if (size is PixelSize) getScaledBitmap(bitmap, size.width, size.height) else bitmap
    }

    /** Return InputStream from bitmap. */
    fun getInputStreamFromBitmap(bitmap: Bitmap): InputStream {
        val bos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 0 /*ignored for PNG*/, bos)
        return ByteArrayInputStream(bos.toByteArray())
    }

    /** Return scaled bitmap from bitmap. */
    fun getScaledBitmap(source: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        var image = source
        if (maxHeight > 0 && maxWidth > 0) {
            val width = image.width
            val height = image.height
            val ratioBitmap = width.toFloat() / height.toFloat()
            val ratioMax = maxWidth.toFloat() / maxHeight.toFloat()

            var finalWidth = maxWidth
            var finalHeight = maxHeight
            if (ratioMax > 1) {
                finalWidth = (maxHeight.toFloat() * ratioBitmap).toInt()
            } else {
                finalHeight = (maxWidth.toFloat() / ratioBitmap).toInt()
            }
            image = Bitmap.createScaledBitmap(image, finalWidth, finalHeight, true)
            return image
        } else {
            return image
        }
    }
}
