package coil.request

import android.content.ContentResolver.SCHEME_FILE
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.test.core.app.ApplicationProvider
import coil.ImageLoader
import coil.annotation.ExperimentalCoilApi
import coil.bitmap.BitmapPool
import coil.fetch.AssetUriFetcher.Companion.ASSET_FILE_PATH_ROOT
import coil.size.Size
import coil.transform.Transformation
import coil.util.CoilUtils
import coil.util.requestManager
import coil.util.runBlockingTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoilApi::class, ExperimentalCoroutinesApi::class, FlowPreview::class)
class DisposableTest {

    private lateinit var context: Context
    private lateinit var imageLoader: ImageLoader

    @Before
    fun before() {
        context = ApplicationProvider.getApplicationContext()
        imageLoader = ImageLoader.Builder(context)
            .memoryCachePolicy(CachePolicy.DISABLED)
            .build()
    }

    @After
    fun after() {
        imageLoader.shutdown()
    }

    @Test
    fun baseTargetDisposable_dispose() = runBlockingTest {
        val request = ImageRequest.Builder(context)
            .data("$SCHEME_FILE:///$ASSET_FILE_PATH_ROOT/normal.jpg")
            .size(100, 100)
            .transformations(GateTransformation())
            .target { /* Do nothing. */ }
            .build()
        val disposable = imageLoader.enqueue(request)

        assertTrue(disposable is BaseTargetDisposable)
        assertFalse(disposable.isDisposed)
        disposable.dispose()
        assertTrue(disposable.isDisposed)
    }

    @Test
    fun baseTargetDisposable_await() = runBlockingTest {
        val transformation = GateTransformation()
        var result: Drawable? = null
        val request = ImageRequest.Builder(context)
            .data("$SCHEME_FILE:///$ASSET_FILE_PATH_ROOT/normal.jpg")
            .size(100, 100)
            .transformations(transformation)
            .target { result = it }
            .build()
        val disposable = imageLoader.enqueue(request)

        assertTrue(disposable is BaseTargetDisposable)
        assertNull(result)
        transformation.open()
        disposable.await()
        assertNotNull(result)
    }

    @Test
    fun viewTargetDisposable_dispose() = runBlockingTest {
        val imageView = ImageView(context)
        val request = ImageRequest.Builder(context)
            .data("$SCHEME_FILE:///$ASSET_FILE_PATH_ROOT/normal.jpg")
            // Set a fixed size so we don't suspend indefinitely waiting for the view to be measured.
            .size(100, 100)
            .transformations(GateTransformation())
            .target(imageView)
            .build()
        val disposable = imageLoader.enqueue(request)

        assertTrue(disposable is ViewTargetDisposable)
        assertFalse(disposable.isDisposed)
        disposable.dispose()
        assertTrue(disposable.isDisposed)
    }

    @Test
    fun viewTargetDisposable_await() = runBlockingTest {
        val transformation = GateTransformation()
        val imageView = ImageView(context)
        val request = ImageRequest.Builder(context)
            .data("$SCHEME_FILE:///$ASSET_FILE_PATH_ROOT/normal.jpg")
            // Set a fixed size so we don't suspend indefinitely waiting for the view to be measured.
            .size(100, 100)
            .transformations(transformation)
            .target(imageView)
            .build()
        val disposable = imageLoader.enqueue(request)

        assertTrue(disposable is ViewTargetDisposable)
        assertNull(imageView.drawable)
        transformation.open()
        disposable.await()
        assertNotNull(imageView.drawable)
    }

    @Test
    fun viewTargetDisposable_restart() = runBlockingTest {
        val transformation = GateTransformation()
        val imageView = ImageView(context)
        val request = ImageRequest.Builder(context)
            .data("$SCHEME_FILE:///$ASSET_FILE_PATH_ROOT/normal.jpg")
            // Set a fixed size so we don't suspend indefinitely waiting for the view to be measured.
            .size(100, 100)
            .transformations(transformation)
            .target(imageView)
            .build()
        val disposable = imageLoader.enqueue(request)

        assertTrue(disposable is ViewTargetDisposable)
        assertFalse(disposable.isDisposed)

        transformation.open()
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
    fun viewTargetDisposable_replace() = runBlockingTest {
        val imageView = ImageView(context)

        fun launchNewRequest(): Disposable {
            val request = ImageRequest.Builder(context)
                .data("$SCHEME_FILE:///$ASSET_FILE_PATH_ROOT/normal.jpg")
                // Set a fixed size so we don't suspend indefinitely waiting for the view to be measured.
                .size(100, 100)
                .transformations(GateTransformation())
                .target(imageView)
                .build()
            return imageLoader.enqueue(request)
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
    fun viewTargetDisposable_clear() = runBlockingTest {
        val imageView = ImageView(context)
        val request = ImageRequest.Builder(context)
            .data("$SCHEME_FILE:///$ASSET_FILE_PATH_ROOT/normal.jpg")
            // Set a fixed size so we don't suspend indefinitely waiting for the view to be measured.
            .size(100, 100)
            .transformations(GateTransformation())
            .target(imageView)
            .build()
        val disposable = imageLoader.enqueue(request)

        assertFalse(disposable.isDisposed)
        CoilUtils.clear(imageView)
        assertTrue(disposable.isDisposed)
    }

    /**
     * Prevent completing the [ImageRequest] until [open] is called.
     * This is to avoid our test assertions racing the image request.
     */
    private class GateTransformation : Transformation {

        private val isOpen = MutableStateFlow(false)

        override fun key() = GateTransformation::class.java.name

        override suspend fun transform(pool: BitmapPool, input: Bitmap, size: Size): Bitmap {
            // Suspend until the gate is open.
            isOpen.first { it }

            return input
        }

        fun open() {
            isOpen.value = true
        }
    }
}
