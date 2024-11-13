package coil3.request

import android.graphics.Bitmap
import android.view.View
import android.widget.ImageView
import coil3.ImageLoader
import coil3.RealImageLoader
import coil3.size.Precision
import coil3.size.Scale
import coil3.size.Size
import coil3.size.ViewSizeResolver
import coil3.target.ViewTarget
import coil3.test.utils.RobolectricTest
import coil3.test.utils.context
import coil3.util.SystemCallbacks
import coil3.util.createRequest
import kotlin.test.assertEquals
import org.junit.Before
import org.junit.Test
import org.robolectric.annotation.Config

class RequestServiceTest : RobolectricTest() {

    private lateinit var service: RequestService

    @Before
    fun before() {
        val imageLoader = ImageLoader(context) as RealImageLoader
        service = RequestService(imageLoader, SystemCallbacks(imageLoader), null)
    }

    @Test
    fun `allowInexactSize - exact precision`() {
        val request = createRequest(context) {
            target(ImageView(context))
            precision(Precision.EXACT)
        }
        val options = service.options(service.updateRequest(request), Size(100, 100))
        assertEquals(Precision.EXACT, options.precision)
    }

    @Test
    fun `allowInexactSize - inexact precision`() {
        val request = createRequest(context) {
            precision(Precision.INEXACT)
        }
        val options = service.options(service.updateRequest(request), Size(100, 100))
        assertEquals(Precision.INEXACT, options.precision)
    }

    @Test
    fun `allowInexactSize - ImageViewTarget`() {
        val request = createRequest(context) {
            target(ImageView(context))
        }
        val options = service.options(service.updateRequest(request), Size(100, 100))
        assertEquals(Precision.INEXACT, options.precision)
    }

    @Test
    fun `allowInexactSize - ImageViewTarget explicit size`() {
        val request = createRequest(context) {
            target(ImageView(context))
            size(100, 100)
        }
        val options = service.options(service.updateRequest(request), Size(100, 100))
        assertEquals(Precision.EXACT, options.precision)
    }

    @Test
    fun `allowInexactSize - ImageViewTarget explicit view size resolver`() {
        val request = createRequest(context) {
            val imageView = ImageView(context)
            target(imageView)
            size(ViewSizeResolver(imageView))
        }
        val options = service.options(service.updateRequest(request), Size(100, 100))
        assertEquals(Precision.INEXACT, options.precision)
    }

    @Test
    fun `allowInexactSize - ImageViewTarget explicit view size resolver, different views`() {
        val request = createRequest(context) {
            target(ImageView(context))
            size(ViewSizeResolver(ImageView(context)))
        }
        val options = service.options(service.updateRequest(request), Size(100, 100))
        assertEquals(Precision.EXACT, options.precision)
    }

    @Test
    fun `allowInexactSize - unspecified SizeResolver`() {
        val request = createRequest(context)
        val options = service.options(service.updateRequest(request), Size(100, 100))
        assertEquals(Precision.INEXACT, options.precision)
    }

    @Test
    fun `allowInexactSize - CustomTarget`() {
        val request = createRequest(context) {
            target { /* Empty. */ }
            size(100, 100)
        }
        val options = service.options(service.updateRequest(request), Size(100, 100))
        assertEquals(Precision.EXACT, options.precision)
    }

    @Test
    fun `allowInexactSize - CustomViewTarget`() {
        val request = createRequest(context) {
            target(object : ViewTarget<View> {
                override val view = View(context)
            })
        }
        val options = service.options(service.updateRequest(request), Size(100, 100))
        assertEquals(Precision.EXACT, options.precision)
    }

    @Test
    fun `allowInexactSize - SizeResolver explicit`() {
        val request = createRequest(context) {
            size(100, 100)
        }
        val options = service.options(service.updateRequest(request), Size(100, 100))
        assertEquals(Precision.EXACT, options.precision)
    }

    /** Regression test: https://github.com/coil-kt/coil/issues/1768 */
    @Test
    @Config(sdk = [23])
    fun `RGB_565 is preserved if hardware bitmaps are disabled`() {
        val request = ImageRequest.Builder(context)
            .bitmapConfig(Bitmap.Config.RGB_565)
            .build()
        val options = service.options(service.updateRequest(request), Size(100, 100))
        assertEquals(Bitmap.Config.RGB_565, options.bitmapConfig)
    }

    /** Regression test: https://github.com/coil-kt/coil/issues/2221 */
    @Test
    fun `ImageLoader bitmapConfig is preserved`() {
        val imageLoader = ImageLoader.Builder(context)
            .bitmapConfig(Bitmap.Config.RGB_565)
            .build() as RealImageLoader
        val request = ImageRequest.Builder(context)
            .defaults(imageLoader.defaults)
            .build()
        val service = RequestService(imageLoader, SystemCallbacks(imageLoader), null)
        val options = service.options(service.updateRequest(request), Size(100, 100))
        assertEquals(Bitmap.Config.RGB_565, options.bitmapConfig)
    }

    /** Regression test: https://github.com/coil-kt/coil/pull/2669 */
    @Test
    fun `ImageView scaleType sets scale`() {
        val imageView = ImageView(context)
        imageView.scaleType = ImageView.ScaleType.FIT_XY
        val request = ImageRequest.Builder(context)
            .target(imageView)
            .build()
        val options = service.options(service.updateRequest(request), Size(100, 100))
        assertEquals(Scale.FILL, options.scale)
    }
}
