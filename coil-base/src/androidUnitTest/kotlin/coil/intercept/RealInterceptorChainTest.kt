package coil.intercept

import android.content.Context
import android.graphics.Bitmap
import android.widget.ImageView
import androidx.test.core.app.ApplicationProvider
import coil.EventListener
import coil.lifecycle.FakeLifecycle
import coil.memory.MemoryCache
import coil.request.ImageRequest
import coil.request.ImageResult
import coil.request.bitmapConfig
import coil.request.lifecycle
import coil.request.target
import coil.request.transformations
import coil.size.Size
import coil.transform.CircleCropTransformation
import coil.util.createRequest
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertSame
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RealInterceptorChainTest {

    private lateinit var context: Context

    @Before
    fun before() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `interceptor cannot set data to null`() = runTest {
        val initialRequest = createRequest(context) {
            data("https://example.com/image.jpg")
        }
        var request: ImageRequest
        val interceptor = Interceptor { chain ->
            request = chain.request.newBuilder()
                .data(null)
                .build()
            chain.withRequest(request).proceed()
        }
        assertFailsWith<IllegalStateException> {
            testChain(initialRequest, listOf(interceptor))
        }
    }

    @Test
    fun `interceptor cannot modify target`() = runTest {
        val initialRequest = createRequest(context) {
            target(ImageView(context))
        }
        var request: ImageRequest
        val interceptor = Interceptor { chain ->
            request = chain.request.newBuilder()
                .target(ImageView(context))
                .build()
            chain.withRequest(request).proceed()
        }
        assertFailsWith<IllegalStateException> {
            testChain(initialRequest, listOf(interceptor))
        }
    }

    @Test
    fun `interceptor cannot modify lifecycle`() = runTest {
        val initialRequest = createRequest(context) {
            lifecycle(FakeLifecycle())
        }
        var request: ImageRequest
        val interceptor = Interceptor { chain ->
            request = chain.request.newBuilder()
                .lifecycle(FakeLifecycle())
                .build()
            chain.withRequest(request).proceed()
        }
        assertFailsWith<IllegalStateException> {
            testChain(initialRequest, listOf(interceptor))
        }
    }

    @Test
    fun `interceptor cannot modify sizeResolver`() = runTest {
        val initialRequest = createRequest(context)
        var request: ImageRequest
        val interceptor = Interceptor { chain ->
            request = chain.request.newBuilder()
                .size(Size(100, 100))
                .build()
            chain.withRequest(request).proceed()
        }
        assertFailsWith<IllegalStateException> {
            testChain(initialRequest, listOf(interceptor))
        }
    }

    @Test
    fun `request modifications are passed to subsequent interceptors`() = runTest {
        val initialRequest = createRequest(context)
        var request = initialRequest
        val interceptor1 = Interceptor { chain ->
            assertSame(request, chain.request)
            request = chain.request.newBuilder()
                .memoryCacheKey(MemoryCache.Key("test"))
                .build()
            chain.withRequest(request).proceed()
        }
        val interceptor2 = Interceptor { chain ->
            assertSame(request, chain.request)
            request = chain.request.newBuilder()
                .bitmapConfig(Bitmap.Config.RGB_565)
                .build()
            chain.withRequest(request).proceed()
        }
        val interceptor3 = Interceptor { chain ->
            assertSame(request, chain.request)
            request = chain.request.newBuilder()
                .transformations(CircleCropTransformation())
                .build()
            chain.withRequest(request).proceed()
        }
        val result = testChain(request, listOf(interceptor1, interceptor2, interceptor3))

        assertNotEquals(initialRequest, result.request)
        assertSame(request, result.request)
    }

    @Test
    fun `withSize is passed to subsequent interceptors`() = runTest {
        var size = Size(100, 100)
        val request = createRequest(context) {
            size(size)
        }
        val interceptor1 = Interceptor { chain ->
            assertEquals(size, chain.size)
            size = Size(123, 456)
            chain.withSize(size).withRequest(chain.request).proceed()
        }
        val interceptor2 = Interceptor { chain ->
            assertEquals(size, chain.size)
            size = Size(1728, 400)
            chain.withSize(size).withRequest(chain.request).proceed()
        }
        val interceptor3 = Interceptor { chain ->
            assertEquals(size, chain.size)
            size = Size.ORIGINAL
            chain.withSize(size).withRequest(chain.request).proceed()
        }
        val result = testChain(request, listOf(interceptor1, interceptor2, interceptor3))

        assertEquals(Size.ORIGINAL, size)
        assertSame(request, result.request)
    }

    @Test
    fun `withRequest modifies the chain's request`() = runTest {
        val initialRequest = createRequest(context)
        val interceptor1 = Interceptor { chain ->
            val request = chain.request.newBuilder()
                .memoryCacheKey(MemoryCache.Key("test"))
                .build()
            assertEquals(chain.withRequest(request).request, request)
            chain.withRequest(request).proceed()
        }

        testChain(initialRequest, listOf(interceptor1))
    }

    private suspend fun testChain(
        request: ImageRequest,
        interceptors: List<Interceptor>
    ): ImageResult {
        val chain = RealInterceptorChain(
            initialRequest = request,
            interceptors = interceptors + FakeInterceptor(),
            index = 0,
            request = request,
            size = Size(100, 100),
            eventListener = EventListener.NONE,
            isPlaceholderCached = false
        )
        return chain.withRequest(request).proceed()
    }
}
