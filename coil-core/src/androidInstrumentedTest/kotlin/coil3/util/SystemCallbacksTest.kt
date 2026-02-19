package coil3.util

import android.app.Application
import android.content.ComponentCallbacks2.TRIM_MEMORY_BACKGROUND
import android.content.ComponentCallbacks2.TRIM_MEMORY_COMPLETE
import android.content.ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN
import android.graphics.Bitmap
import androidx.core.graphics.createBitmap
import coil3.ImageLoader
import coil3.RealImageLoader
import coil3.asImage
import coil3.memory.MemoryCache
import coil3.memoryCacheMaxSizePercentWhileInBackground
import coil3.test.utils.ViewTestActivity
import coil3.test.utils.context
import coil3.test.utils.launchActivity
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Test

class SystemCallbacksTest {

    @Test
    fun imageLoaderIsFreedWithoutShutdown() {
        var imageLoader: RealImageLoader?
        imageLoader = ImageLoader(context) as RealImageLoader
        val systemCallbacks = SystemCallbacks(imageLoader) as AndroidSystemCallbacks
        systemCallbacks.registerMemoryPressureCallbacks()

        // Clear the local reference.
        @Suppress("UNUSED_VALUE")
        imageLoader = null

        val bitmaps = mutableListOf<Bitmap>()
        while (systemCallbacks.imageLoader.get() != null) {
            // Request that garbage collection occur.
            Runtime.getRuntime().gc()

            // Keep allocating bitmaps until either the image loader is freed or we run out of memory.
            bitmaps += createBitmap(500, 500)
        }
        bitmaps.clear()

        // Ensure that the next system callback is called.
        systemCallbacks.componentCallbacks.onTrimMemory(TRIM_MEMORY_BACKGROUND)

        assertTrue(systemCallbacks.shutdown)
    }

    @Test
    fun trimMemoryCompleteClearsMemoryCache() {
        val memoryCache = MemoryCache.Builder()
            .maxSizePercent(context)
            .build()
        val imageLoader = ImageLoader.Builder(context)
            .memoryCache(memoryCache)
            .build() as RealImageLoader
        val systemCallbacks = SystemCallbacks(imageLoader) as AndroidSystemCallbacks
        systemCallbacks.registerMemoryPressureCallbacks()

        memoryCache[MemoryCache.Key("1")] = MemoryCache.Value(
            image = createBitmap(1000, 1000, Bitmap.Config.ARGB_8888)
                .toDrawable(context).asImage(),
        )
        memoryCache[MemoryCache.Key("2")] = MemoryCache.Value(
            image = createBitmap(1000, 1000, Bitmap.Config.ARGB_8888)
                .toDrawable(context).asImage(),
        )

        assertEquals(8_000_000, memoryCache.size)

        @Suppress("DEPRECATION")
        systemCallbacks.componentCallbacks.onTrimMemory(TRIM_MEMORY_COMPLETE)

        assertEquals(0, memoryCache.size)
    }

    @Test
    fun trimMemoryUiHiddenReducesMemoryCacheMaxSize() {
        val memoryCacheMaxSize = 1_048_576L
        val memoryCacheMaxSizePercent = 0.5
        val memoryCache = MemoryCache.Builder()
            .maxSizeBytes(memoryCacheMaxSize)
            .build()
        val imageLoader = ImageLoader.Builder(context)
            .memoryCache(memoryCache)
            .memoryCacheMaxSizePercentWhileInBackground(memoryCacheMaxSizePercent)
            .build() as RealImageLoader
        val systemCallbacks = SystemCallbacks(imageLoader) as AndroidSystemCallbacks
        systemCallbacks.registerMemoryPressureCallbacks()
        val application = context.applicationContext as TestApplication

        assertTrue(application.activityLifecycleCallbacks.isEmpty())
        assertEquals(memoryCacheMaxSize, memoryCache.initialMaxSize)
        assertEquals(memoryCacheMaxSize, memoryCache.maxSize)

        @Suppress("DEPRECATION")
        systemCallbacks.componentCallbacks.onTrimMemory(TRIM_MEMORY_UI_HIDDEN)

        assertEquals(
            setOf<Application.ActivityLifecycleCallbacks>(systemCallbacks.activityCallbacks),
            application.activityLifecycleCallbacks,
        )
        assertEquals(
            (memoryCacheMaxSizePercent * memoryCacheMaxSize).toLong(),
            memoryCache.maxSize,
        )

        launchActivity { activity: ViewTestActivity ->
            assertTrue(application.activityLifecycleCallbacks.isEmpty())
            assertEquals(memoryCacheMaxSize, memoryCache.initialMaxSize)
            assertEquals(memoryCacheMaxSize, memoryCache.maxSize)
        }
    }
}
