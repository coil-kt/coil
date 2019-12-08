package coil.request

import android.content.ContentResolver
import android.content.Context
import android.graphics.drawable.Drawable
import android.net.Uri
import android.widget.ImageView
import androidx.test.core.app.ApplicationProvider
import coil.ImageLoader
import coil.RealImageLoader
import coil.annotation.ExperimentalCoil
import coil.api.load
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@UseExperimental(ExperimentalCoil::class)
class RequestDisposableTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private lateinit var imageLoader: RealImageLoader

    @Before
    fun before() {
        imageLoader = ImageLoader(context) as RealImageLoader
    }

    @After
    fun after() {
        imageLoader.shutdown()
    }

    @Test
    fun baseTargetRequestDisposable_basicDispose() {
        val data = Uri.parse("${ContentResolver.SCHEME_CONTENT}://coil/normal.jpg")
        val disposable = imageLoader.load(context, data)

        assertTrue(disposable is BaseTargetRequestDisposable)
        assertFalse(disposable.isDisposed)
        disposable.dispose()
        assertTrue(disposable.isDisposed)
    }

    @Test
    fun baseTargetRequestDisposable_await() {
        val data = Uri.parse("${ContentResolver.SCHEME_CONTENT}://coil/normal.jpg")
        var result: Drawable? = null
        val disposable = imageLoader.load(context, data) {
            dispatcher(Dispatchers.Main.immediate)
            size(100)
            target { result = it }
        }

        assertTrue(disposable is BaseTargetRequestDisposable)
        assertNull(result)
        runBlocking {
            disposable.await()
        }
        assertNotNull(result)
    }

    @Test
    fun viewTargetRequestDisposable_basicDispose() {
        val data = Uri.parse("${ContentResolver.SCHEME_CONTENT}://coil/normal.jpg")
        val imageView = ImageView(context)
        val disposable = imageLoader.load(context, data) {
            target(imageView)
            listener(onError = { _, throwable -> throw throwable })
        }

        assertTrue(disposable is ViewTargetRequestDisposable)
        assertFalse(disposable.isDisposed)
        disposable.dispose()
        assertTrue(disposable.isDisposed)
    }

    @Test
    fun viewTargetRequestDisposable_await() {
        val data = Uri.parse("${ContentResolver.SCHEME_CONTENT}://coil/normal.jpg")
        val imageView = ImageView(context)
        val disposable = imageLoader.load(context, data) {
            dispatcher(Dispatchers.Main.immediate)
            size(100)
            target(imageView)
            listener(onError = { _, throwable -> throw throwable })
        }

        assertTrue(disposable is ViewTargetRequestDisposable)
        assertNull(imageView.drawable)
        runBlocking {
            disposable.await()
        }
        assertNotNull(imageView.drawable)
    }
}
