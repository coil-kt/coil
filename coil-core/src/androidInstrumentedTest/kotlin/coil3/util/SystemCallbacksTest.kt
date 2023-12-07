package coil3.util

import android.content.ComponentCallbacks2.TRIM_MEMORY_BACKGROUND
import android.content.ComponentCallbacks2.TRIM_MEMORY_COMPLETE
import android.graphics.Bitmap
import androidx.core.graphics.createBitmap
import coil3.ImageLoader
import coil3.RealImageLoader
import coil3.asCoilImage
import coil3.memory.MemoryCache
import coil3.test.context
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Test

class SystemCallbacksTest {

    @Test
    fun imageLoaderIsFreedWithoutShutdown() {
        val systemCallbacks = SystemCallbacks() as AndroidSystemCallbacks
        systemCallbacks.register(ImageLoader(context) as RealImageLoader)

        val bitmaps = mutableListOf<Bitmap>()
        while (systemCallbacks.imageLoader?.get() != null) {
            // Request that garbage collection occur.
            Runtime.getRuntime().gc()

            // Keep allocating bitmaps until either the image loader is freed or we run out of memory.
            bitmaps += createBitmap(500, 500)
        }
        bitmaps.clear()

        // Ensure that the next system callback is called.
        systemCallbacks.onTrimMemory(TRIM_MEMORY_BACKGROUND)

        assertTrue(systemCallbacks.isShutdown)
    }

    @Test
    fun trimMemoryCallsArePassedThrough() {
        val memoryCache = MemoryCache.Builder()
            .maxSizePercent(context)
            .build()
        val imageLoader = ImageLoader.Builder(context)
            .memoryCache(memoryCache)
            .build()
        val systemCallbacks = SystemCallbacks() as AndroidSystemCallbacks
        systemCallbacks.register(imageLoader as RealImageLoader)

        memoryCache[MemoryCache.Key("1")] = MemoryCache.Value(
            image = createBitmap(1000, 1000, Bitmap.Config.ARGB_8888)
                .toDrawable(context).asCoilImage()
        )
        memoryCache[MemoryCache.Key("2")] = MemoryCache.Value(
            image = createBitmap(1000, 1000, Bitmap.Config.ARGB_8888)
                .toDrawable(context).asCoilImage()
        )

        assertEquals(8_000_000, memoryCache.size)

        systemCallbacks.onTrimMemory(TRIM_MEMORY_COMPLETE)

        assertEquals(0, memoryCache.size)
    }
}
