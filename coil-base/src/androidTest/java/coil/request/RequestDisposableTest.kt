package coil.request

import android.content.ContentResolver.SCHEME_CONTENT
import android.content.Context
import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.core.net.toUri
import androidx.test.core.app.ApplicationProvider
import coil.ImageLoader
import coil.RealImageLoader
import coil.annotation.ExperimentalCoilApi
import coil.api.load
import coil.util.CoilUtils
import coil.util.requestManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoilApi::class)
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
        val data = "$SCHEME_CONTENT://coil/normal.jpg".toUri()
        val disposable = imageLoader.load(context, data) {
            target { /* Do nothing. */ }
            listener(onError = { _, throwable -> throw throwable })
        }

        assertTrue(disposable is BaseTargetRequestDisposable)
        assertFalse(disposable.isDisposed)
        disposable.dispose()
        assertTrue(disposable.isDisposed)
    }

    @Test
    fun baseTargetRequestDisposable_await() {
        val data = "$SCHEME_CONTENT://coil/normal.jpg".toUri()
        var result: Drawable? = null
        val disposable = imageLoader.load(context, data) {
            target { result = it }
            listener(onError = { _, throwable -> throw throwable })
        }

        assertTrue(disposable is BaseTargetRequestDisposable)
        assertNull(result)
        runBlocking {
            disposable.await()
        }
        assertNotNull(result)
    }

    @Test
    fun viewTargetRequestDisposable_dispose() {
        val data = "$SCHEME_CONTENT://coil/normal.jpg".toUri()
        val imageView = ImageView(context)
        val disposable = imageLoader.load(context, data) {
            target(imageView)
            size(100) // Set a fixed size so we don't suspend indefinitely waiting for the view to be measured.
            listener(onError = { _, throwable -> throw throwable })
        }

        assertTrue(disposable is ViewTargetRequestDisposable)
        assertFalse(disposable.isDisposed)
        disposable.dispose()
        assertTrue(disposable.isDisposed)
    }

    @Test
    fun viewTargetRequestDisposable_await() {
        val data = "$SCHEME_CONTENT://coil/normal.jpg".toUri()
        val imageView = ImageView(context)
        val disposable = imageLoader.load(context, data) {
            target(imageView)
            size(100) // Set a fixed size so we don't suspend indefinitely waiting for the view to be measured.
            listener(onError = { _, throwable -> throw throwable })
        }

        assertTrue(disposable is ViewTargetRequestDisposable)
        assertNull(imageView.drawable)
        runBlocking {
            disposable.await()
        }
        assertNotNull(imageView.drawable)
    }

    @Test
    fun viewTargetRequestDisposable_restart() {
        val data = "$SCHEME_CONTENT://coil/normal.jpg".toUri()
        val imageView = ImageView(context)
        val disposable = imageLoader.load(context, data) {
            target(imageView)
            size(100) // Set a fixed size so we don't suspend indefinitely waiting for the view to be measured.
            listener(onError = { _, throwable -> throw throwable })
        }

        runBlocking(Dispatchers.Main.immediate) {
            assertTrue(disposable is ViewTargetRequestDisposable)
            assertFalse(disposable.isDisposed)

            disposable.await()
            assertFalse(disposable.isDisposed)

            imageView.requestManager.onViewDetachedFromWindow(imageView)
            assertFalse(disposable.isDisposed)

            imageView.requestManager.onViewAttachedToWindow(imageView)
            assertFalse(disposable.isDisposed)

            disposable.dispose()
            assertTrue(disposable.isDisposed)
        }
    }

    @Test
    fun viewTargetRequestDisposable_replace() {
        val data = "$SCHEME_CONTENT://coil/normal.jpg".toUri()
        val imageView = ImageView(context)

        fun launchNewRequest(): RequestDisposable {
            return imageLoader.load(context, data) {
                target(imageView)
                size(100) // Set a fixed size so we don't suspend indefinitely waiting for the view to be measured.
                listener(onError = { _, throwable -> throw throwable })
            }
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
    fun viewTargetRequestDisposable_clear() {
        val data = "$SCHEME_CONTENT://coil/normal.jpg".toUri()
        val imageView = ImageView(context)
        val disposable = imageLoader.load(context, data) {
            target(imageView)
            size(100) // Set a fixed size so we don't suspend indefinitely waiting for the view to be measured.
            listener(onError = { _, throwable -> throw throwable })
        }

        assertFalse(disposable.isDisposed)
        CoilUtils.clear(imageView)
        assertTrue(disposable.isDisposed)
    }
}
