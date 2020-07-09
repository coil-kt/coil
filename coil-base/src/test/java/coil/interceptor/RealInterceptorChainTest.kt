@file:Suppress("NOTHING_TO_INLINE", "TestFunctionName")

package coil.interceptor

import android.content.Context
import android.widget.ImageView
import androidx.test.core.app.ApplicationProvider
import coil.EventListener
import coil.annotation.ExperimentalCoilApi
import coil.lifecycle.FakeLifecycle
import coil.request.ImageRequest
import coil.request.RequestResult
import coil.size.PixelSize
import coil.util.Utils.REQUEST_TYPE_ENQUEUE
import coil.util.createRequest
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertFailsWith

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoilApi::class)
class RealInterceptorChainTest {

    private lateinit var context: Context

    @Before
    fun before() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `interceptor cannot set data to null`() {
        val request = createRequest(context) {
            data("https://www.example.com/image.jpg")
        }
        val interceptor = Interceptor { chain ->
            chain.proceed(chain.request.newBuilder().data(null).build())
        }
        assertFailsWith<IllegalStateException> {
            testChain(request, listOf(interceptor))
        }
    }

    @Test
    fun `interceptor cannot modify target`() {
        val request = createRequest(context) {
            target(ImageView(context))
        }
        val interceptor = Interceptor { chain ->
            chain.proceed(chain.request.newBuilder().target(ImageView(context)).build())
        }
        assertFailsWith<IllegalStateException> {
            testChain(request, listOf(interceptor))
        }
    }

    @Test
    fun `interceptor cannot modify lifecycle`() {
        val request = createRequest(context) {
            lifecycle(FakeLifecycle())
        }
        val interceptor = Interceptor { chain ->
            chain.proceed(chain.request.newBuilder().lifecycle(FakeLifecycle()).build())
        }
        assertFailsWith<IllegalStateException> {
            testChain(request, listOf(interceptor))
        }
    }

    @Test
    fun `interceptor cannot modify sizeResolver`() {
        val request = createRequest(context)
        val interceptor = Interceptor { chain ->
            chain.proceed(chain.request.newBuilder().size(PixelSize(100, 100)).build())
        }
        assertFailsWith<IllegalStateException> {
            testChain(request, listOf(interceptor))
        }
    }

    private fun testChain(request: ImageRequest, interceptors: List<Interceptor>): RequestResult {
        val chain = RealInterceptorChain(
            initialRequest = request,
            requestType = REQUEST_TYPE_ENQUEUE,
            interceptors = interceptors + FakeInterceptor(),
            index = 0,
            request = request,
            size = PixelSize(100, 100),
            eventListener = EventListener.NONE
        )
        return runBlocking { chain.proceed(request) }
    }

    private inline fun Interceptor(crossinline block: suspend (Interceptor.Chain) -> RequestResult): Interceptor {
        return object : Interceptor {
            override suspend fun intercept(chain: Interceptor.Chain) = block(chain)
        }
    }
}
