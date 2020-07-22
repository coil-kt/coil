package coil.memory

import android.content.Context
import android.widget.ImageView
import androidx.test.core.app.ApplicationProvider
import coil.EventListener
import coil.ImageLoader
import coil.annotation.ExperimentalCoilApi
import coil.bitmap.BitmapReferenceCounter
import coil.bitmap.FakeBitmapPool
import coil.decode.DataSource
import coil.request.ErrorResult
import coil.request.Metadata
import coil.request.RequestResult
import coil.request.SuccessResult
import coil.target.FakeTarget
import coil.target.ImageViewTarget
import coil.transition.Transition
import coil.transition.TransitionTarget
import coil.util.Utils.REQUEST_TYPE_ENQUEUE
import coil.util.Utils.REQUEST_TYPE_EXECUTE
import coil.util.createBitmap
import coil.util.createRequest
import coil.util.createTestMainDispatcher
import coil.util.isInvalid
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
        counter = BitmapReferenceCounter(EmptyWeakMemoryCache, pool, null)
        delegateService = DelegateService(imageLoader, counter, null)
    }

    @After
    fun after() {
        Dispatchers.resetMain()
        imageLoader.shutdown()
    }

    @Test
    fun `empty target does not invalidate`() {
        val request = createRequest(context)
        val delegate = delegateService.createTargetDelegate(request.target, REQUEST_TYPE_ENQUEUE, EventListener.NONE)

        runBlocking {
            val bitmap = createBitmap()
            val drawable = bitmap.toDrawable(context)
            delegate.start(drawable, drawable)
            assertFalse(counter.isInvalid(bitmap))
        }

        runBlocking {
            val bitmap = createBitmap()
            val result = SuccessResult(
                drawable = bitmap.toDrawable(context),
                request = request,
                metadata = Metadata(null, false, DataSource.DISK)
            )
            delegate.success(result)
            assertFalse(counter.isInvalid(bitmap))
        }
    }

    @Test
    fun `get request invalidates the success bitmap`() {
        val request = createRequest(context)
        val delegate = delegateService.createTargetDelegate(request.target, REQUEST_TYPE_EXECUTE, EventListener.NONE)

        runBlocking {
            val bitmap = createBitmap()
            val result = SuccessResult(
                drawable = bitmap.toDrawable(context),
                request = request,
                metadata = Metadata(null, false, DataSource.DISK)
            )
            delegate.success(result)
            assertTrue(counter.isInvalid(bitmap))
        }
    }

    @Test
    fun `target methods are called and bitmaps are invalidated`() {
        val target = FakeTarget()
        val request = createRequest(context) {
            target(target)
        }
        val delegate = delegateService.createTargetDelegate(request.target, REQUEST_TYPE_ENQUEUE, EventListener.NONE)

        runBlocking {
            val bitmap = createBitmap()
            val drawable = bitmap.toDrawable(context)
            delegate.start(drawable, null)
            assertTrue(target.start)
            assertTrue(counter.isInvalid(bitmap))
        }

        runBlocking {
            val bitmap = createBitmap()
            val result = SuccessResult(
                drawable = bitmap.toDrawable(context),
                request = request,
                metadata = Metadata(null, false, DataSource.DISK)
            )
            delegate.success(result)
            assertTrue(target.success)
            assertTrue(counter.isInvalid(bitmap))
        }

        runBlocking {
            val bitmap = createBitmap()
            val result = ErrorResult(
                drawable = bitmap.toDrawable(context),
                request = request,
                throwable = Throwable()
            )
            delegate.error(result)
            assertTrue(target.error)
            assertFalse(counter.isInvalid(bitmap))
        }
    }

    @Test
    fun `request with poolable target returns previous bitmap to pool`() {
        val request = createRequest(context) {
            target(ImageViewTarget(ImageView(context)))
        }
        val delegate = delegateService.createTargetDelegate(request.target, REQUEST_TYPE_ENQUEUE, EventListener.NONE)

        val initialBitmap = createBitmap()
        val initialDrawable = initialBitmap.toDrawable(context)
        delegate.start(initialDrawable, initialDrawable)
        assertFalse(counter.isInvalid(initialBitmap))
        assertFalse(initialBitmap in pool.bitmaps)

        runBlocking {
            val bitmap = createBitmap()
            val result = SuccessResult(
                drawable = bitmap.toDrawable(context),
                request = request,
                metadata = Metadata(null, false, DataSource.DISK)
            )
            delegate.success(result)
            assertFalse(counter.isInvalid(bitmap))
            assertTrue(initialBitmap in pool.bitmaps)
        }
    }

    @Test
    fun `request suspends until transition is complete`() {
        val request = createRequest(context) {
            target(ImageViewTarget(ImageView(context)))
        }
        val delegate = delegateService.createTargetDelegate(request.target, REQUEST_TYPE_ENQUEUE, EventListener.NONE)

        val initialBitmap = createBitmap()
        val initialDrawable = initialBitmap.toDrawable(context)
        delegate.start(initialDrawable, initialDrawable)
        assertFalse(counter.isInvalid(initialBitmap))
        assertFalse(initialBitmap in pool.bitmaps)

        runBlocking {
            val bitmap = createBitmap()
            var isRunning = true
            val transition = object : Transition {
                override suspend fun transition(target: TransitionTarget<*>, result: RequestResult) {
                    assertFalse(initialBitmap in pool.bitmaps)
                    delay(100) // Simulate an animation.
                    assertFalse(initialBitmap in pool.bitmaps)
                    isRunning = false
                }
            }
            val result = SuccessResult(
                drawable = bitmap.toDrawable(context),
                request = request.newBuilder().transition(transition).build(),
                metadata = Metadata(null, false, DataSource.DISK)
            )
            delegate.success(result)

            // Ensure that the animation completed and the initial bitmap was not pooled until this method completes.
            assertFalse(isRunning)
            assertTrue(initialBitmap in pool.bitmaps)
        }
    }
}
