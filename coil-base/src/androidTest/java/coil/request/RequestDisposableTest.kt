package coil.request

import android.content.ContentResolver.SCHEME_CONTENT
import android.content.Context
import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.test.core.app.ApplicationProvider
import coil.ImageLoader
import coil.RealImageLoader
import coil.annotation.ExperimentalCoilApi
import coil.transition.Transition
import coil.transition.TransitionTarget
import coil.util.CoilUtils
import coil.util.requestManager
import coil.util.runBlockingTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoilApi::class, ExperimentalCoroutinesApi::class, FlowPreview::class)
class RequestDisposableTest {

    private lateinit var context: Context
    private lateinit var imageLoader: RealImageLoader

    @Before
    fun before() {
        context = ApplicationProvider.getApplicationContext()
        imageLoader = ImageLoader(context) as RealImageLoader
    }

    @After
    fun after() {
        imageLoader.shutdown()
    }

    @Test
    fun baseTargetRequestDisposable_dispose() {
        val request = LoadRequest.Builder(context)
            .data("$SCHEME_CONTENT://coil/normal.jpg")
            .target { /** Do nothing. */ }
            .build()
        val disposable = imageLoader.execute(request)

        assertTrue(disposable is BaseTargetRequestDisposable)
        assertFalse(disposable.isDisposed)
        disposable.dispose()
        assertTrue(disposable.isDisposed)
    }

    @Test
    fun baseTargetRequestDisposable_await() {
        var result: Drawable? = null
        val request = LoadRequest.Builder(context)
            .data("$SCHEME_CONTENT://coil/normal.jpg")
            .target { result = it }
            .build()
        val disposable = imageLoader.execute(request)

        assertTrue(disposable is BaseTargetRequestDisposable)
        assertNull(result)
        runBlocking { disposable.await() }
        assertNotNull(result)
    }

    @Test
    fun viewTargetRequestDisposable_dispose() {
        val transition = GateTransition()
        val imageView = ImageView(context)
        val request = LoadRequest.Builder(context)
            .data("$SCHEME_CONTENT://coil/normal.jpg")
            // Set a fixed size so we don't suspend indefinitely waiting for the view to be measured.
            .size(100, 100)
            .transition(transition)
            .target(imageView)
            .build()
        val disposable = imageLoader.execute(request)

        assertTrue(disposable is ViewTargetRequestDisposable)
        assertFalse(disposable.isDisposed)
        disposable.dispose()
        assertTrue(disposable.isDisposed)
    }

    @Test
    fun viewTargetRequestDisposable_await() {
        val transition = GateTransition()
        val imageView = ImageView(context)
        val request = LoadRequest.Builder(context)
            .data("$SCHEME_CONTENT://coil/normal.jpg")
            // Set a fixed size so we don't suspend indefinitely waiting for the view to be measured.
            .size(100, 100)
            .transition(transition)
            .target(imageView)
            .build()
        val disposable = imageLoader.execute(request)

        assertTrue(disposable is ViewTargetRequestDisposable)
        assertNull(imageView.drawable)
        transition.open()
        runBlocking { disposable.await() }
        assertNotNull(imageView.drawable)
    }

    @Test
    fun viewTargetRequestDisposable_restart() = runBlockingTest {
        val transition = GateTransition()
        val imageView = ImageView(context)
        val request = LoadRequest.Builder(context)
            .data("$SCHEME_CONTENT://coil/normal.jpg")
            // Set a fixed size so we don't suspend indefinitely waiting for the view to be measured.
            .size(100, 100)
            .transition(transition)
            .target(imageView)
            .build()
        val disposable = imageLoader.execute(request)

        assertTrue(disposable is ViewTargetRequestDisposable)
        assertFalse(disposable.isDisposed)

        transition.open()
        disposable.await()
        assertFalse(disposable.isDisposed)

        imageView.requestManager.onViewDetachedFromWindow(imageView)
        assertFalse(disposable.isDisposed)

        imageView.requestManager.onViewAttachedToWindow(imageView)
        assertFalse(disposable.isDisposed)

        disposable.dispose()
        assertTrue(disposable.isDisposed)
    }

    @Test
    fun viewTargetRequestDisposable_replace() = runBlockingTest {
        val transition = GateTransition()
        val imageView = ImageView(context)

        fun launchNewRequest(): RequestDisposable {
            val request = LoadRequest.Builder(context)
                .data("$SCHEME_CONTENT://coil/normal.jpg")
                // Set a fixed size so we don't suspend indefinitely waiting for the view to be measured.
                .size(100, 100)
                .transition(transition)
                .target(imageView)
                .build()
            return imageLoader.execute(request)
        }

        val disposable1 = launchNewRequest()
        assertFalse(disposable1.isDisposed)

        val disposable2 = launchNewRequest()
        assertTrue(disposable1.isDisposed)
        assertFalse(disposable2.isDisposed)

        disposable2.dispose()
        assertTrue(disposable1.isDisposed)
        assertTrue(disposable2.isDisposed)
    }

    @Test
    fun viewTargetRequestDisposable_clear() = runBlockingTest {
        val transition = GateTransition()
        val imageView = ImageView(context)
        val request = LoadRequest.Builder(context)
            .data("$SCHEME_CONTENT://coil/normal.jpg")
            // Set a fixed size so we don't suspend indefinitely waiting for the view to be measured.
            .size(100, 100)
            .transition(transition)
            .target(imageView)
            .build()
        val disposable = imageLoader.execute(request)

        assertFalse(disposable.isDisposed)
        CoilUtils.clear(imageView)
        assertTrue(disposable.isDisposed)
    }

    /**
     * Prevent completing the [LoadRequest] until [open] is called.
     * This is to avoid our test assertions racing the image request.
     */
    private class GateTransition : Transition {

        private val isOpen = ConflatedBroadcastChannel(false)

        override suspend fun transition(
            target: TransitionTarget<*>,
            result: RequestResult
        ) {
            // Suspend until the gate is open.
            isOpen.asFlow().first { it }

            // Delegate to the empty transition.
            Transition.NONE.transition(target, result)
        }

        fun open() = isOpen.offer(true)
    }
}
