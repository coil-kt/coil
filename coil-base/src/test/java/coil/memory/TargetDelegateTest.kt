package coil.memory

import android.content.Context
import android.widget.ImageView
import androidx.test.core.app.ApplicationProvider
import coil.ImageLoader
import coil.annotation.ExperimentalCoilApi
import coil.bitmappool.FakeBitmapPool
import coil.target.FakeTarget
import coil.target.ImageViewTarget
import coil.transition.Transition
import coil.transition.TransitionResult
import coil.transition.TransitionTarget
import coil.util.createBitmap
import coil.util.createGetRequest
import coil.util.createLoadRequest
import coil.util.createTestMainDispatcher
import coil.util.toDrawable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
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
@OptIn(ExperimentalCoilApi::class, ExperimentalCoroutinesApi::class)
class TargetDelegateTest {

    private lateinit var context: Context
    private lateinit var mainDispatcher: TestCoroutineDispatcher
    private lateinit var imageLoader: ImageLoader
    private lateinit var pool: FakeBitmapPool
    private lateinit var counter: BitmapReferenceCounter
    private lateinit var delegateService: DelegateService

    @Before
    fun before() {
        context = ApplicationProvider.getApplicationContext()
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
            delegate.success(bitmap.toDrawable(context), false, null)
            assertFalse(counter.invalid(bitmap))
        }
    }

    @Test
    fun `get request invalidates the success bitmap`() {
        val request = createGetRequest()
        val delegate = delegateService.createTargetDelegate(request)

        runBlocking {
            val bitmap = createBitmap()
            delegate.success(bitmap.toDrawable(context), false, null)
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
            delegate.success(bitmap.toDrawable(context), false, null)
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
        val initialDrawable = initialBitmap.toDrawable(context)
        delegate.start(initialDrawable, initialDrawable)
        assertFalse(counter.invalid(initialBitmap))
        assertFalse(pool.bitmaps.contains(initialBitmap))

        runBlocking {
            val bitmap = createBitmap()
            delegate.success(bitmap.toDrawable(context), false, null)
            assertFalse(counter.invalid(bitmap))
            assertTrue(pool.bitmaps.contains(initialBitmap))
        }
    }

    @Test
    fun `request suspends until transition is complete`() {
        val request = createLoadRequest(context) {
            target(ImageViewTarget(ImageView(context)))
        }
        val delegate = delegateService.createTargetDelegate(request)

        val initialBitmap = createBitmap()
        val initialDrawable = initialBitmap.toDrawable(context)
        delegate.start(initialDrawable, initialDrawable)
        assertFalse(counter.invalid(initialBitmap))
        assertFalse(pool.bitmaps.contains(initialBitmap))

        runBlocking {
            val bitmap = createBitmap()
            var isRunning = true
            val transition = object : Transition {
                override suspend fun transition(target: TransitionTarget<*>, result: TransitionResult) {
                    assertFalse(pool.bitmaps.contains(initialBitmap))
                    delay(100) // Simulate an animation.
                    assertFalse(pool.bitmaps.contains(initialBitmap))
                    isRunning = false
                }
            }
            delegate.success(bitmap.toDrawable(context), false, transition)

            // Ensure that the animation completed and the initial bitmap was not pooled until this method completes.
            assertFalse(isRunning)
            assertTrue(pool.bitmaps.contains(initialBitmap))
        }
    }
}
