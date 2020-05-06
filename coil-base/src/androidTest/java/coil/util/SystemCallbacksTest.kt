package coil.util

import android.content.ComponentCallbacks2.TRIM_MEMORY_BACKGROUND
import android.content.Context
import android.graphics.Bitmap
import androidx.core.graphics.createBitmap
import androidx.test.core.app.ApplicationProvider
import coil.ImageLoader
import coil.RealImageLoader
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
        val systemCallbacks = SystemCallbacks(ImageLoader(context) as RealImageLoader, context)

        // Keep allocating bitmaps until either the image loader is freed or we run out of memory.
        val bitmaps = mutableListOf<Bitmap>()
        while (systemCallbacks.imageLoader.get() != null) {
            bitmaps += createBitmap(1000, 1000, Bitmap.Config.ARGB_8888)
        }

        // Ensure that the next system callback is called.
        systemCallbacks.onTrimMemory(TRIM_MEMORY_BACKGROUND)

        assertTrue(systemCallbacks.isShutdown)
    }
}
