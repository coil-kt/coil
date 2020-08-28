package coil

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import coil.request.ImageRequest
import coil.util.createTestMainDispatcher
import coil.util.runBlockingTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class ImageLoaderFactoryTest {

    private lateinit var context: Context
    private lateinit var mainDispatcher: TestCoroutineDispatcher

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

        val imageLoader1 = Coil.imageLoader(context)

        assertTrue((context.applicationContext as TestApplication).isInitialized.get())

        val imageLoader2 = Coil.imageLoader(context)

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

        val imageLoader2 = Coil.imageLoader(context)

        assertSame(imageLoader1, imageLoader2)

        assertTrue(isInitialized.get())

        val imageLoader3 = Coil.imageLoader(context)

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

        Coil.imageLoader(context)

        assertTrue(isInitialized.get())
        assertFalse((context.applicationContext as TestApplication).isInitialized.get())
    }

    @Test
    fun `setImageLoader shuts down previous instance`() = runBlockingTest {
        val imageLoader1 = ImageLoader(context)

        Coil.setImageLoader(imageLoader1)

        val imageLoader2 = ImageLoader(context)

        Coil.setImageLoader(imageLoader2)

        // The request should fail instantly since imageLoader1 is shut down.
        assertFailsWith<IllegalStateException> {
            imageLoader1.execute(ImageRequest.Builder(context).data(Unit).build())
        }
    }

    class TestApplication : Application(), ImageLoaderFactory {

        val isInitialized = AtomicBoolean(false)

        override fun newImageLoader(): ImageLoader {
            check(!isInitialized.getAndSet(true)) { "newImageLoader was invoked more than once." }
            return ImageLoader(this)
        }
    }
}
