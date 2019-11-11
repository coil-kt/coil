package coil.memory

import android.content.Context
import android.widget.ImageView
import androidx.test.core.app.ApplicationProvider
import coil.ImageLoader
import coil.annotation.ExperimentalCoil
import coil.bitmappool.FakeBitmapPool
import coil.drawable.CrossfadeDrawable
import coil.lifecycle.FakeLifecycle
import coil.target.FakeTarget
import coil.target.ImageViewTarget
import coil.transition.CrossfadeTransition
import coil.util.createBitmap
import coil.util.createGetRequest
import coil.util.createLoadRequest
import coil.util.createTestMainDispatcher
import coil.util.toDrawable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@UseExperimental(ExperimentalCoil::class, ExperimentalCoroutinesApi::class)
class TargetDelegateTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private lateinit var mainDispatcher: TestCoroutineDispatcher
    private lateinit var imageLoader: ImageLoader
    private lateinit var pool: FakeBitmapPool
    private lateinit var counter: BitmapReferenceCounter
    private lateinit var delegateService: DelegateService

    @Before
    fun before() {
        mainDispatcher = createTestMainDispatcher()
        imageLoader = ImageLoader(context)
        pool = FakeBitmapPool()
        counter = BitmapReferenceCounter(pool)
        delegateService = DelegateService(imageLoader, counter)
    }

    @After
    fun after() {
        Dispatchers.resetMain()
        imageLoader.shutdown()
    }

    @Test
    fun `empty target does not invalidate`() {
        val request = createLoadRequest(context)
        val delegate = delegateService.createTargetDelegate(request)

        runBlocking {
            val bitmap = createBitmap()
            val drawable = bitmap.toDrawable(context)
            delegate.start(drawable, drawable)
            assertFalse(counter.invalid(bitmap))
        }

        runBlocking {
            val bitmap = createBitmap()
            delegate.success(bitmap.toDrawable(context), null)
            assertFalse(counter.invalid(bitmap))
        }
    }

    @Test
    fun `get request invalidates the success bitmap`() {
        val request = createGetRequest()
        val delegate = delegateService.createTargetDelegate(request)

        runBlocking {
            val bitmap = createBitmap()
            delegate.success(bitmap.toDrawable(context), null)
            assertTrue(counter.invalid(bitmap))
        }
    }

    @Test
    fun `target methods are called and bitmaps are invalidated`() {
        val target = FakeTarget()
        val request = createLoadRequest(context) {
            target(target)
        }
        val delegate = delegateService.createTargetDelegate(request)

        runBlocking {
            val bitmap = createBitmap()
            val drawable = bitmap.toDrawable(context)
            delegate.start(drawable, null)
            assertTrue(target.start)
            assertTrue(counter.invalid(bitmap))
        }

        runBlocking {
            val bitmap = createBitmap()
            delegate.success(bitmap.toDrawable(context), null)
            assertTrue(target.success)
            assertTrue(counter.invalid(bitmap))
        }

        runBlocking {
            val bitmap = createBitmap()
            delegate.error(bitmap.toDrawable(context), null)
            assertTrue(target.error)
            assertFalse(counter.invalid(bitmap))
        }
    }

    @Test
    fun `request with poolable target returns previous bitmap to pool`() {
        val request = createLoadRequest(context) {
            target(ImageViewTarget(ImageView(context)))
        }
        val delegate = delegateService.createTargetDelegate(request)

        val initialBitmap = createBitmap()
        runBlocking {
            val drawable = initialBitmap.toDrawable(context)
            delegate.start(drawable, drawable)
            assertFalse(counter.invalid(initialBitmap))
            assertFalse(pool.bitmaps.contains(initialBitmap))
        }

        runBlocking {
            val bitmap = createBitmap()
            delegate.success(bitmap.toDrawable(context), null)
            assertFalse(counter.invalid(bitmap))
            assertTrue(pool.bitmaps.contains(initialBitmap))
        }
    }

    @Test
    fun `request suspends until transition is complete`() {
        val imageView = ImageView(context)
        val target = ImageViewTarget(imageView)
        val request = createLoadRequest(context) {
            target(target)
        }
        val delegate = delegateService.createTargetDelegate(request)
        delegate.start(null, null)

        runBlocking {
            val bitmap = createBitmap()
            target.onStart { FakeLifecycle() }
            delegate.success(bitmap.toDrawable(context), CrossfadeTransition())

            // If we start the drawable and it does not run, it must have already been run.
            val drawable = imageView.drawable
            assertTrue(drawable is CrossfadeDrawable)
            assertFalse(drawable.isRunning)
            drawable.start()
            assertFalse(drawable.isRunning)
        }
    }
}
