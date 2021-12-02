package coil

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import coil.util.createTestMainDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ImageLoaderFactoryTest {

    private lateinit var context: Context
    private lateinit var mainDispatcher: TestDispatcher

    @Before
    fun before() {
        context = ApplicationProvider.getApplicationContext()
        mainDispatcher = createTestMainDispatcher()
    }

    @After
    fun after() {
        Dispatchers.resetMain()
        Coil.reset()
    }

    @Test
    @Config(application = TestApplication::class)
    fun `application factory is invoked exactly once`() {
        assertFalse((context.applicationContext as TestApplication).isInitialized.get())

        val imageLoader1 = context.imageLoader

        assertTrue((context.applicationContext as TestApplication).isInitialized.get())

        val imageLoader2 = context.imageLoader

        assertSame(imageLoader1, imageLoader2)
    }

    @Test
    fun `setImageLoader factory is invoked exactly once`() {
        val imageLoader1 = ImageLoader(context)

        val isInitialized = AtomicBoolean(false)
        Coil.setImageLoader {
            check(!isInitialized.getAndSet(true)) { "newImageLoader was invoked more than once." }
            imageLoader1
        }

        assertFalse(isInitialized.get())

        val imageLoader2 = context.imageLoader

        assertSame(imageLoader1, imageLoader2)

        assertTrue(isInitialized.get())

        val imageLoader3 = context.imageLoader

        assertSame(imageLoader1, imageLoader3)
    }

    @Test
    @Config(application = TestApplication::class)
    fun `setImageLoader preempts application factory`() {
        val isInitialized = AtomicBoolean(false)

        assertFalse(isInitialized.get())
        assertFalse((context.applicationContext as TestApplication).isInitialized.get())

        Coil.setImageLoader {
            check(!isInitialized.getAndSet(true)) { "newImageLoader was invoked more than once." }
            ImageLoader(context)
        }

        context.imageLoader

        assertTrue(isInitialized.get())
        assertFalse((context.applicationContext as TestApplication).isInitialized.get())
    }

    class TestApplication : Application(), ImageLoaderFactory {

        val isInitialized = AtomicBoolean(false)

        override fun newImageLoader(): ImageLoader {
            check(!isInitialized.getAndSet(true)) { "newImageLoader was invoked more than once." }
            return ImageLoader(this)
        }
    }
}
