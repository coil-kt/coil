package coil3.request

import android.app.Activity
import android.widget.ImageView
import android.widget.ImageView.ScaleType.CENTER
import android.widget.ImageView.ScaleType.MATRIX
import coil3.Extras
import coil3.ImageLoader
import coil3.intercept.FakeEngineInterceptor
import coil3.lifecycle.FakeLifecycle
import coil3.size.Precision
import coil3.size.Scale
import coil3.size.Size
import coil3.test.utils.RobolectricTest
import coil3.test.utils.context
import coil3.transition.CrossfadeTransition
import coil3.transition.Transition
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertSame
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ImageRequestTest : RobolectricTest() {

    /** Regression test: https://github.com/coil-kt/coil/issues/221 */
    @Test
    fun `crossfade false creates none transition`() {
        val imageLoader = ImageLoader.Builder(context)
            .crossfade(false)
            .build()

        val request = ImageRequest.Builder(context)
            .crossfade(false)
            .build()

        assertEquals(Transition.Factory.NONE, request.transitionFactory)

        imageLoader.shutdown()
    }

    @Test
    fun `defined values are not replaced`() {
        val lifecycle = FakeLifecycle()
        val imageView = ImageView(context)
        val request = ImageRequest.Builder(context)
            .data("https://example.com/image.jpg")
            .size(100, 100)
            .scale(Scale.FILL)
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
        val defaults = ImageRequest.Defaults(
            decoderCoroutineContext = Dispatchers.Unconfined,
            precision = Precision.EXACT,
            extras = Extras.Builder()
                .set(Extras.Key.transitionFactory, CrossfadeTransition.Factory())
                .build()
        )
        val imageView = ImageView(context)
        val request = ImageRequest.Builder(context)
            .data("https://example.com/image.jpg")
            .target(imageView)
            .defaults(defaults)
            .build()

        assertSame(defaults.decoderCoroutineContext, request.decoderCoroutineContext)
        assertSame(defaults.extras[Extras.Key.transitionFactory], request.transitionFactory)
        assertSame(defaults.precision, request.precision)
    }

    @Test
    fun `equals and hashCode are implemented`() {
        val imageView = ImageView(context)
        val request1 = ImageRequest.Builder(context)
            .data("https://example.com/image.jpg")
            .target(imageView)
            .allowHardware(true)
            .build()
        val request2 = ImageRequest.Builder(context)
            .data("https://example.com/image.jpg")
            .target(imageView)
            .allowHardware(true)
            .build()

        assertEquals(request1, request2)
        assertEquals(request1.hashCode(), request2.hashCode())

        val request3 = request2.newBuilder()
            .allowHardware(false)
            .build()

        assertNotEquals(request1, request3)
        assertNotEquals(request1.hashCode(), request3.hashCode())
    }

    @Test
    fun `request placeholder is not invoked if request target is null`() = runTest {
        val request = ImageRequest.Builder(context)
            .data(Unit)
            .placeholder { error("should not be called") }
            .build()
        val imageLoader = ImageLoader.Builder(context)
            .components {
                add(FakeEngineInterceptor())
            }
            .build()
        val result = imageLoader.execute(request)

        assertIs<SuccessResult>(result)
    }

    @Test
    fun `ImageView with scale type MATRIX or CENTER should default to original size`() = runTest {
        val request1 = ImageRequest.Builder(context)
            .data("https://example.com/image.jpg")
            .target(ImageView(context).apply { scaleType = MATRIX })
            .build()
        val request2 = ImageRequest.Builder(context)
            .data("https://example.com/image.jpg")
            .target(ImageView(context).apply { scaleType = CENTER })
            .build()
        val request3 = ImageRequest.Builder(context)
            .data("https://example.com/image.jpg")
            .target(ImageView(context).apply { scaleType = MATRIX })
            .size(100, 100)
            .build()
        val request4 = ImageRequest.Builder(context)
            .data("https://example.com/image.jpg")
            .target(ImageView(context).apply { scaleType = CENTER })
            .size(100, 100)
            .build()

        assertEquals(Size.ORIGINAL, request1.sizeResolver.size())
        assertEquals(Size.ORIGINAL, request2.sizeResolver.size())
        assertEquals(Size(100, 100), request3.sizeResolver.size())
        assertEquals(Size(100, 100), request4.sizeResolver.size())
    }
}
