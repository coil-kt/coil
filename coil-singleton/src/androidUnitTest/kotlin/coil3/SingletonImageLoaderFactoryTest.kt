package coil3

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
        assertFalse((context.applicationContext as TestApplication).isInitialized)

        val imageLoader1 = context.imageLoader

        assertTrue((context.applicationContext as TestApplication).isInitialized)

        val imageLoader2 = context.imageLoader

        assertSame(imageLoader1, imageLoader2)
    }

    @Test
    fun `setImageLoader factory is invoked exactly once`() {
        val imageLoader1 = ImageLoader(context)

        val factory = TestSingletonImageLoaderFactory(lazyOf(imageLoader1))
        SingletonImageLoader.set(factory)

        assertFalse(factory.isInitialized)

        val imageLoader2 = context.imageLoader

        assertSame(imageLoader1, imageLoader2)

        assertTrue(factory.isInitialized)

        val imageLoader3 = context.imageLoader

        assertSame(imageLoader1, imageLoader3)
    }

    @Test
    @Config(application = TestApplication::class)
    fun `setImageLoader preempts application factory`() {
        val factory = TestSingletonImageLoaderFactory(context)

        assertFalse(factory.isInitialized)
        assertFalse((context.applicationContext as TestApplication).isInitialized)

        SingletonImageLoader.set(factory)

        context.imageLoader

        assertTrue(factory.isInitialized)
        assertFalse((context.applicationContext as TestApplication).isInitialized)
    }

    class TestSingletonImageLoaderFactory(
        private val imageLoaderLazy: Lazy<ImageLoader>,
    ) : SingletonImageLoader.Factory {

        constructor(context: Context) : this(lazy { ImageLoader(context) })

        private val _isInitialized = atomic(false)
        val isInitialized: Boolean by _isInitialized

        override fun newImageLoader(): ImageLoader {
            check(!_isInitialized.getAndSet(true)) {
                "newImageLoader was invoked more than once."
            }
            return imageLoaderLazy.value
        }
    }

    class TestApplication : Application(), SingletonImageLoader.Factory {
        private val _isInitialized = atomic(false)
        val isInitialized: Boolean by _isInitialized

        override fun newImageLoader(): ImageLoader {
            check(!_isInitialized.getAndSet(true)) {
                "newImageLoader was invoked more than once."
            }
            return ImageLoader(this)
        }
    }
}
