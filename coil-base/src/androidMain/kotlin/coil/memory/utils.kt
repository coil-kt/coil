package coil.memory

import android.app.ActivityManager
import android.content.pm.ApplicationInfo
import androidx.core.content.getSystemService
import coil.PlatformContext
import coil.asAndroidContext

private const val STANDARD_MEMORY_MULTIPLIER = 0.2
private const val LOW_MEMORY_MULTIPLIER = 0.15

/** Return the default percent of the application's total memory to use for the memory cache. */
internal actual fun PlatformContext.defaultMemoryCacheSizePercent(): Double {
    return try {
        val context = asAndroidContext()
        val activityManager: ActivityManager = context.getSystemService()!!
        if (activityManager.isLowRamDevice) LOW_MEMORY_MULTIPLIER else STANDARD_MEMORY_MULTIPLIER
    } catch (_: Exception) {
        STANDARD_MEMORY_MULTIPLIER
    }
}

private const val DEFAULT_MEMORY_CLASS_MEGABYTES = 256

/** Return the application's total memory in bytes. */
internal fun PlatformContext.totalAvailableMemoryBytes(): Long {
    val memoryClassMegabytes = try {
        val context = asAndroidContext()
        val activityManager: ActivityManager = context.getSystemService()!!
        val isLargeHeap = (context.applicationInfo.flags and ApplicationInfo.FLAG_LARGE_HEAP) != 0
        if (isLargeHeap) activityManager.largeMemoryClass else activityManager.memoryClass
    } catch (_: Exception) {
        DEFAULT_MEMORY_CLASS_MEGABYTES
    }
    return memoryClassMegabytes * 1024L * 1024L
}
