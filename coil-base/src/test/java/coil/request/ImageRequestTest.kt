package coil.request

import android.app.Activity
import android.content.Context
import android.widget.ImageView
import androidx.test.core.app.ApplicationProvider
import coil.ImageLoader
import coil.annotation.ExperimentalCoilApi
import coil.lifecycle.FakeLifecycle
import coil.size.Precision
import coil.size.Scale
import coil.size.ViewSizeResolver
import coil.transition.CrossfadeTransition
import coil.transition.Transition
import coil.util.scale
import kotlinx.coroutines.Dispatchers
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoilApi::class)
class ImageRequestTest {

    private lateinit var context: Context

    @Before
    fun before() {
        context = ApplicationProvider.getApplicationContext()
    }

    /** Regression test: https://github.com/coil-kt/coil/issues/221 */
    @Test
    fun `crossfade false creates none transition`() {
        val loader = ImageLoader.Builder(context)
            .crossfade(false)
            .build()

        val request = ImageRequest.Builder(context)
            .crossfade(false)
            .build()

        assertEquals(Transition.NONE, request.transition)

        loader.shutdown()
    }

    @Test
    fun `changing context resets resolved values`() {
        val request = ImageRequest.Builder(context)
            .data("https://www.example.com/image.jpg")
            .build()

        assertSame(request.sizeResolver, request.newBuilder().build().sizeResolver)
        assertNotSame(request.sizeResolver, request.newBuilder(Activity()).build().sizeResolver)
    }

    @Test
    fun `changing target resets resolved values`() {
        val imageView = ImageView(context)
        val request = ImageRequest.Builder(context)
            .data("https://www.example.com/image.jpg")
            .target(imageView)
            .build()

        val sizeResolver = request.sizeResolver
        assertTrue(sizeResolver is ViewSizeResolver<*> && sizeResolver.view === imageView)
        assertSame(sizeResolver, request.newBuilder().build().sizeResolver)
        assertNotSame(sizeResolver, request.newBuilder().target(ImageView(context)).build().sizeResolver)
    }

    @Test
    fun `changing size resolver resets resolved values`() {
        val imageView = ImageView(context)
        val request = ImageRequest.Builder(context)
            .data("https://www.example.com/image.jpg")
            .size(100, 100)
            .target(imageView)
            .build()

        assertEquals(Scale.FIT, imageView.scale)

        imageView.scaleType = ImageView.ScaleType.CENTER_CROP

        assertSame(request.scale, request.newBuilder().build().scale)
        assertNotSame(request.scale, request.newBuilder().size(200, 200).build().scale)
    }

    @Test
    fun `setting defaults resets resolved scale only`() {
        val imageView = ImageView(context)
        val request = ImageRequest.Builder(context)
            .data("https://www.example.com/image.jpg")
            .size(100, 100)
            .target(imageView)
            .build()

        assertEquals(Scale.FIT, imageView.scale)

        imageView.scaleType = ImageView.ScaleType.CENTER_CROP

        val newRequest = request.newBuilder().defaults(DefaultRequestOptions()).build()
        assertSame(request.lifecycle, newRequest.lifecycle)
        assertSame(request.sizeResolver, newRequest.sizeResolver)
        assertEquals(Scale.FILL, newRequest.scale)
    }

    @Test
    fun `defined values are not replaced`() {
        val lifecycle = FakeLifecycle()
        val imageView = ImageView(context)
        val request = ImageRequest.Builder(context)
            .data("https://www.example.com/image.jpg")
            .size(100, 100)
            .lifecycle(lifecycle)
            .target(imageView)
            .build()

        val newRequest = request.newBuilder(Activity()).build()
        assertSame(request.lifecycle, newRequest.lifecycle)
        assertSame(request.sizeResolver, newRequest.sizeResolver)
        assertSame(request.scale, newRequest.scale)
    }

    @Test
    fun `defaults fill unset values`() {
        val defaults = DefaultRequestOptions(
            dispatcher = Dispatchers.Unconfined,
            transition = CrossfadeTransition(),
            precision = Precision.EXACT
        )
        val imageView = ImageView(context)
        val request = ImageRequest.Builder(context)
            .data("https://www.example.com/image.jpg")
            .target(imageView)
            .defaults(defaults)
            .build()

        assertSame(defaults.dispatcher, request.dispatcher)
        assertSame(defaults.transition, request.transition)
        assertSame(defaults.precision, request.precision)
    }

    @Test
    fun `test equality`() {
        val imageView = ImageView(context)
        val request1 = ImageRequest.Builder(context)
            .data("https://www.example.com/image.jpg")
            .target(imageView)
            .allowHardware(true)
            .build()
        val request2 = ImageRequest.Builder(context)
            .data("https://www.example.com/image.jpg")
            .target(imageView)
            .allowHardware(true)
            .build()

        assertEquals(request1, request2)

        val request3 = request2.newBuilder()
            .allowHardware(false)
            .build()

        assertNotEquals(request1, request3)
    }
}
