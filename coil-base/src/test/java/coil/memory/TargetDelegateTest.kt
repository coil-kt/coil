package coil.memory

import android.content.Context
import android.widget.ImageView
import androidx.test.core.app.ApplicationProvider
import coil.EventListener
import coil.ImageLoader
import coil.bitmap.FakeBitmapPool
import coil.bitmap.RealBitmapReferenceCounter
import coil.decode.DataSource
import coil.request.ErrorResult
import coil.request.ImageResult
import coil.request.ImageResult.Metadata
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
import coil.util.executeQueuedMainThreadTasks
import coil.util.isValid
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
@OptIn(ExperimentalCoroutinesApi::class)
class TargetDelegateTest {

    private lateinit var context: Context
    private lateinit var mainDispatcher: TestCoroutineDispatcher
    private lateinit var imageLoader: ImageLoader
    private lateinit var pool: FakeBitmapPool
    private lateinit var counter: RealBitmapReferenceCounter
    private lateinit var delegateService: DelegateService

    @Before
    fun before() {
        context = ApplicationProvider.getApplicationContext()
        mainDispatcher = createTestMainDispatcher()
        imageLoader = ImageLoader(context)
        pool = FakeBitmapPool()
        counter = RealBitmapReferenceCounter(EmptyWeakMemoryCache, pool, null)
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
            counter.setValid(bitmap, true)
            val drawable = bitmap.toDrawable(context)
            delegate.start(drawable, bitmap)
            assertTrue(counter.isValid(bitmap))
        }

        runBlocking {
            val bitmap = createBitmap()
            counter.setValid(bitmap, true)
            val result = SuccessResult(
                drawable = bitmap.toDrawable(context),
                request = request,
                metadata = Metadata(
                    memoryCacheKey = null,
                    isSampled = false,
                    dataSource = DataSource.DISK,
                    isPlaceholderMemoryCacheKeyPresent = false
                )
            )
            delegate.success(result)
            assertTrue(counter.isValid(bitmap))
        }
    }

    @Test
    fun `get request invalidates the success bitmap`() {
        val request = createRequest(context)
        val delegate = delegateService.createTargetDelegate(request.target, REQUEST_TYPE_EXECUTE, EventListener.NONE)

        runBlocking {
            val bitmap = createBitmap()
            counter.setValid(bitmap, true)
            val result = SuccessResult(
                drawable = bitmap.toDrawable(context),
                request = request,
                metadata = Metadata(
                    memoryCacheKey = null,
                    isSampled = false,
                    dataSource = DataSource.DISK,
                    isPlaceholderMemoryCacheKeyPresent = false
                )
            )
            delegate.success(result)
            assertFalse(counter.isValid(bitmap))
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
            counter.setValid(bitmap, true)
            delegate.start(bitmap.toDrawable(context), bitmap)
            assertTrue(target.start)
            assertFalse(counter.isValid(bitmap))
        }

        runBlocking {
            val bitmap = createBitmap()
            counter.setValid(bitmap, true)
            val result = SuccessResult(
                drawable = bitmap.toDrawable(context),
                request = request,
                metadata = Metadata(
                    memoryCacheKey = null,
                    isSampled = false,
                    dataSource = DataSource.DISK,
                    isPlaceholderMemoryCacheKeyPresent = false
                )
            )
            delegate.success(result)
            assertTrue(target.success)
            assertFalse(counter.isValid(bitmap))
        }

        runBlocking {
            val bitmap = createBitmap()
            counter.setValid(bitmap, true)
            val result = ErrorResult(
                drawable = bitmap.toDrawable(context),
                request = request,
                throwable = Throwable()
            )
            delegate.error(result)
            assertTrue(target.error)
            assertTrue(counter.isValid(bitmap))
        }
    }

    @Test
    fun `request with poolable target returns previous bitmap to pool`() {
        val request = createRequest(context) {
            target(ImageViewTarget(ImageView(context)))
        }
        val delegate = delegateService.createTargetDelegate(request.target, REQUEST_TYPE_ENQUEUE, EventListener.NONE)

        val initialBitmap = createBitmap()
        counter.setValid(initialBitmap, true)
        delegate.start(initialBitmap.toDrawable(context), initialBitmap)
        assertTrue(counter.isValid(initialBitmap))
        assertFalse(initialBitmap in pool.bitmaps)

        runBlocking {
            val bitmap = createBitmap()
            counter.setValid(bitmap, true)
            val result = SuccessResult(
                drawable = bitmap.toDrawable(context),
                request = request,
                metadata = Metadata(
                    memoryCacheKey = null,
                    isSampled = false,
                    dataSource = DataSource.DISK,
                    isPlaceholderMemoryCacheKeyPresent = false
                )
            )
            delegate.success(result)
            executeQueuedMainThreadTasks()
            assertTrue(counter.isValid(bitmap))
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
        counter.setValid(initialBitmap, true)
        delegate.start(initialBitmap.toDrawable(context), initialBitmap)
        assertTrue(counter.isValid(initialBitmap))
        assertFalse(initialBitmap in pool.bitmaps)

        runBlocking {
            val bitmap = createBitmap()
            counter.setValid(bitmap, true)
            var isRunning = true
            val transition = Transition { _, _ ->
                assertFalse(initialBitmap in pool.bitmaps)
                delay(100) // Simulate an animation.
                assertFalse(initialBitmap in pool.bitmaps)
                isRunning = false
            }
            val result = SuccessResult(
                drawable = bitmap.toDrawable(context),
                request = request.newBuilder().transition(transition).build(),
                metadata = Metadata(
                    memoryCacheKey = null,
                    isSampled = false,
                    dataSource = DataSource.DISK,
                    isPlaceholderMemoryCacheKeyPresent = false
                )
            )
            delegate.success(result)

            // Ensure that the animation completed and the initial bitmap was not pooled until this method completes.
            executeQueuedMainThreadTasks()
            assertFalse(isRunning)
            assertTrue(initialBitmap in pool.bitmaps)
        }
    }

    @Suppress("TestFunctionName")
    private inline fun Transition(crossinline block: suspend (TransitionTarget, ImageResult) -> Unit): Transition {
        return object : Transition {
            override suspend fun transition(target: TransitionTarget, result: ImageResult) = block(target, result)
        }
    }
}
