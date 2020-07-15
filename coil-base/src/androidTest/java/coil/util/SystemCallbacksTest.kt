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
import coil.annotation.ExperimentalCoilApi
import coil.bitmap.BitmapPool
import coil.bitmap.BitmapReferenceCounter
import coil.memory.MemoryCache
import coil.memory.RealWeakMemoryCache
import coil.memory.StrongMemoryCache
import coil.request.DefaultRequestOptions
import okhttp3.OkHttpClient
import org.junit.Before
import org.junit.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalCoilApi::class)
class SystemCallbacksTest {

    private lateinit var context: Context

    @Before
    fun before() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun imageLoaderIsFreedWithoutShutdown() {
        val systemCallbacks = SystemCallbacks(ImageLoader(context) as RealImageLoader, context)

        val bitmaps = mutableListOf<Bitmap>()
        while (systemCallbacks.imageLoader.get() != null) {
            // Request that garbage collection occur.
            Runtime.getRuntime().gc()

            // Keep allocating bitmaps until either the image loader is freed or we run out of memory.
            bitmaps += createBitmap(500, 500, Bitmap.Config.ARGB_8888)
        }
        bitmaps.clear()

        // Ensure that the next system callback is called.
        systemCallbacks.onTrimMemory(TRIM_MEMORY_BACKGROUND)

        assertTrue(systemCallbacks.isShutdown)
    }

    @Test
    fun trimMemoryCallsArePassedThrough() {
        val bitmapPool = BitmapPool(Int.MAX_VALUE)
        val weakMemoryCache = RealWeakMemoryCache(null)
        val referenceCounter = BitmapReferenceCounter(weakMemoryCache, bitmapPool, null)
        val memoryCache = StrongMemoryCache(weakMemoryCache, referenceCounter, Int.MAX_VALUE, null)
        val imageLoader = RealImageLoader(
            context = context,
            defaults = DefaultRequestOptions(),
            bitmapPool = bitmapPool,
            referenceCounter = referenceCounter,
            strongMemoryCache = memoryCache,
            weakMemoryCache = weakMemoryCache,
            callFactory = OkHttpClient(),
            eventListenerFactory = EventListener.Factory.NONE,
            componentRegistry = ComponentRegistry(),
            logger = null
        )
        val systemCallbacks = SystemCallbacks(imageLoader, context)

        memoryCache.set(
            key = MemoryCache.Key("1"),
            bitmap = createBitmap(1000, 1000, Bitmap.Config.ARGB_8888),
            isSampled = false
        )

        memoryCache.set(
            key = MemoryCache.Key("2"),
            bitmap = createBitmap(1000, 1000, Bitmap.Config.ARGB_8888),
            isSampled = false
        )

        assertTrue(memoryCache.size == 8000000)

        systemCallbacks.onTrimMemory(TRIM_MEMORY_COMPLETE)

        assertTrue(memoryCache.size == 0)
    }
}
