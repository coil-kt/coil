package coil.request

import android.content.Context
import android.view.View
import android.widget.ImageView
import androidx.test.core.app.ApplicationProvider
import coil.memory.RequestService
import coil.size.Precision
import coil.target.ViewTarget
import coil.util.createGetRequest
import coil.util.createLoadRequest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class RequestServiceTest {

    private lateinit var context: Context
    private lateinit var service: RequestService

    @Before
    fun before() {
        context = ApplicationProvider.getApplicationContext()
        service = RequestService(null)
    }

    @Test
    fun `allowInexactSize - exact precision`() {
        val request = createLoadRequest(context) {
            target(ImageView(context))
            precision(Precision.EXACT)
        }
        assertFalse(service.allowInexactSize(request))
    }

    @Test
    fun `allowInexactSize - inexact precision`() {
        val request = createGetRequest {
            precision(Precision.INEXACT)
        }
        assertTrue(service.allowInexactSize(request))
    }

    @Test
    fun `allowInexactSize - ImageViewTarget`() {
        val request = createLoadRequest(context) {
            target(ImageView(context))
        }
        assertTrue(service.allowInexactSize(request))
    }

    @Test
    fun `allowInexactSize - NullSizeResolver`() {
        val request = createLoadRequest(context)
        assertTrue(service.allowInexactSize(request))
    }

    @Test
    fun `allowInexactSize - CustomTarget`() {
        val request = createLoadRequest(context) {
            target { /** Empty. */ }
            size(100)
        }
        assertFalse(service.allowInexactSize(request))
    }

    @Test
    fun `allowInexactSize - CustomViewTarget`() {
        val request = createLoadRequest(context) {
            target(object : ViewTarget<View> {
                override val view = View(context)
            })
        }
        assertFalse(service.allowInexactSize(request))
    }

    @Test
    fun `allowInexactSize - GetRequest`() {
        val request = createGetRequest {
            size(100)
        }
        assertFalse(service.allowInexactSize(request))
    }
}
