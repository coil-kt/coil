package coil

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.atomicfu.atomic
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class SingletonImageLoaderFactoryTest {

    private lateinit var context: Context

    @Before
    fun before() {
        context = ApplicationProvider.getApplicationContext()
    }

    @After
    fun after() {
        SingletonImageLoader.reset()
    }

    @Test
    @Config(application = TestApplication::class)
    fun `application factory is invoked exactly once`() {
        assertFalse((context.applicationContext as TestApplication).isInitialized.value)

        val imageLoader1 = context.imageLoader

        assertTrue((context.applicationContext as TestApplication).isInitialized.value)

        val imageLoader2 = context.imageLoader

        assertSame(imageLoader1, imageLoader2)
    }

    @Test
    fun `setImageLoader factory is invoked exactly once`() {
        val imageLoader1 = ImageLoader(context)

        val isInitialized = atomic(false)
        SingletonImageLoader.set {
            check(!isInitialized.getAndSet(true)) {
                "newImageLoader was invoked more than once."
            }
            imageLoader1
        }

        assertFalse(isInitialized.value)

        val imageLoader2 = context.imageLoader

        assertSame(imageLoader1, imageLoader2)

        assertTrue(isInitialized.value)

        val imageLoader3 = context.imageLoader

        assertSame(imageLoader1, imageLoader3)
    }

    @Test
    @Config(application = TestApplication::class)
    fun `setImageLoader preempts application factory`() {
        val isInitialized = atomic(false)

        assertFalse(isInitialized.value)
        assertFalse((context.applicationContext as TestApplication).isInitialized.value)

        SingletonImageLoader.set {
            check(!isInitialized.getAndSet(true)) {
                "newImageLoader was invoked more than once."
            }
            ImageLoader(context)
        }

        context.imageLoader

        assertTrue(isInitialized.value)
        assertFalse((context.applicationContext as TestApplication).isInitialized.value)
    }

    class TestApplication : Application(), SingletonImageLoader.Factory {
        val isInitialized = atomic(false)

        override fun newImageLoader(): ImageLoader {
            check(!isInitialized.getAndSet(true)) {
                "newImageLoader was invoked more than once."
            }
            return ImageLoader(this)
        }
    }
}
