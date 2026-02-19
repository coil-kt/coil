package coil3

import android.app.Application
import coil3.test.utils.RobolectricTest
import coil3.test.utils.ViewTestActivity
import coil3.test.utils.context
import coil3.test.utils.launchActivity
import kotlin.test.assertEquals
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

    @Test
    @Config(application = TestApplication::class)
    fun `GIVEN activity context, WHEN image loader is set, THEN it uses application context`() {
        // GIVEN
        var capturedContext: PlatformContext? = null
        val factory = SingletonImageLoader.Factory { ctx ->
            capturedContext = ctx
            ImageLoader(context = capturedContext)
        }

        SingletonImageLoader.setSafe(factory)

        // WHEN
        launchActivity { activity: ViewTestActivity ->
            SingletonImageLoader.get(activity)
        }

        // THEN
        assertEquals(
            expected = context.applicationContext,
            actual = capturedContext,
            message = "Expected factory to receive Application context, not Activity",
        )
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
