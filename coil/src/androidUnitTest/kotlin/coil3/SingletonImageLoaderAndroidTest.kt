package coil3

import android.app.Application
import coil3.test.utils.RobolectricTest
import coil3.test.utils.context
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.atomicfu.atomic
import org.junit.After
import org.junit.Test
import org.robolectric.annotation.Config

class SingletonImageLoaderAndroidTest : RobolectricTest() {

    @After
    fun after() {
        SingletonImageLoader.reset()
    }

    @Test
    @Config(application = TestApplication::class)
    fun `application factory is invoked exactly once`() {
        assertFalse((context.applicationContext as TestApplication).isInitialized)

        val imageLoader1 = SingletonImageLoader.get(context)

        assertTrue((context.applicationContext as TestApplication).isInitialized)

        val imageLoader2 = SingletonImageLoader.get(context)

        assertSame(imageLoader1, imageLoader2)
    }

    @Test
    @Config(application = TestApplication::class)
    fun `setImageLoader preempts application factory`() {
        val factory = TestSingletonImageLoaderFactory(context)

        assertFalse(factory.isInitialized)
        assertFalse((context.applicationContext as TestApplication).isInitialized)

        SingletonImageLoader.setSafe(factory)

        SingletonImageLoader.get(context)

        assertTrue(factory.isInitialized)
        assertFalse((context.applicationContext as TestApplication).isInitialized)
    }
}

class TestApplication : Application(), SingletonImageLoader.Factory {
    private val _isInitialized = atomic(false)
    val isInitialized: Boolean by _isInitialized

    override fun newImageLoader(context: PlatformContext): ImageLoader {
        check(!_isInitialized.getAndSet(true)) {
            "newImageLoader was invoked more than once."
        }
        return ImageLoader(this)
    }
}
