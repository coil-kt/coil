package coil.util

import android.content.ComponentCallbacks2.TRIM_MEMORY_BACKGROUND
import android.content.ComponentCallbacks2.TRIM_MEMORY_COMPLETE
import android.content.Context
import android.graphics.Bitmap
import androidx.core.graphics.createBitmap
import androidx.test.core.app.ApplicationProvider
import coil.ComponentRegistry
import coil.EventListener
import coil.ImageLoader
import coil.RealImageLoader
import coil.memory.MemoryCache
import coil.memory.MemoryCache.Key
import coil.memory.MemoryCache.Value
import coil.request.DefaultRequestOptions
import okhttp3.OkHttpClient
import org.junit.Before
import org.junit.Test
import kotlin.test.assertTrue

class SystemCallbacksTest {

    private lateinit var context: Context

    @Before
    fun before() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun imageLoaderIsFreedWithoutShutdown() {
        val systemCallbacks = SystemCallbacks(ImageLoader(context) as RealImageLoader, context, true)

        val bitmaps = mutableListOf<Bitmap>()
        while (systemCallbacks.imageLoader.get() != null) {
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
        val memoryCache = MemoryCache(context)
        val imageLoader = RealImageLoader(
            context = context,
            defaults = DefaultRequestOptions(),
            memoryCache = memoryCache,
            callFactory = OkHttpClient(),
            eventListenerFactory = EventListener.Factory.NONE,
            componentRegistry = ComponentRegistry(),
            options = ImageLoaderOptions(),
            logger = null
        )
        val systemCallbacks = SystemCallbacks(imageLoader, context, true)

        memoryCache[Key("1")] = Value(createBitmap(1000, 1000, Bitmap.Config.ARGB_8888))
        memoryCache[Key("2")] = Value(createBitmap(1000, 1000, Bitmap.Config.ARGB_8888))

        assertTrue(memoryCache.size == 8000000)

        systemCallbacks.onTrimMemory(TRIM_MEMORY_COMPLETE)

        assertTrue(memoryCache.size == 0)
    }
}
