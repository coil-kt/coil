package coil3.intercept

import coil3.EventListener
import coil3.memory.MemoryCache
import coil3.request.ImageRequest
import coil3.request.ImageResult
import coil3.size.Size
import coil3.target.Target
import coil3.test.utils.RobolectricTest
import coil3.test.utils.context
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertSame
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest

class RealInterceptorChainTests : RobolectricTest() {

    @Test
    fun interceptorCannotSetDataToNull() = runTest {
        val initialRequest = ImageRequest.Builder(context)
            .data("https://example.com/image.jpg")
            .build()
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
    fun interceptorCannotModifyTarget() = runTest {
        val initialRequest = ImageRequest.Builder(context)
            .data("https://example.com/image.jpg")
            .target(object : Target {})
            .build()
        var request: ImageRequest
        val interceptor = Interceptor { chain ->
            request = chain.request.newBuilder()
                .target(object : Target {})
                .build()
            chain.withRequest(request).proceed()
        }
        assertFailsWith<IllegalStateException> {
            testChain(initialRequest, listOf(interceptor))
        }
    }

    @Test
    fun interceptorCannotModifySizeResolver() = runTest {
        val initialRequest = ImageRequest.Builder(context)
            .data("https://example.com/image.jpg")
            .build()
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
    fun requestModificationsArePassedToSubsequentInterceptors() = runTest {
        val initialRequest = ImageRequest.Builder(context)
            .data("https://example.com/image.jpg")
            .build()
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
                .listener(object : ImageRequest.Listener {})
                .build()
            chain.withRequest(request).proceed()
        }
        val interceptor3 = Interceptor { chain ->
            assertSame(request, chain.request)
            request = chain.request.newBuilder()
                .fetcherDispatcher(Dispatchers.Default)
                .build()
            chain.withRequest(request).proceed()
        }
        val result = testChain(request, listOf(interceptor1, interceptor2, interceptor3))

        assertNotEquals(initialRequest, result.request)
        assertSame(request, result.request)
    }

    @Test
    fun withSizeIsPassedToSubsequentInterceptors() = runTest {
        var size = Size(100, 100)
        val request = ImageRequest.Builder(context)
            .data("https://example.com/image.jpg")
            .size(size)
            .build()
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
    fun withRequestModifiesTheChainsRequest() = runTest {
        val initialRequest = ImageRequest.Builder(context)
            .data("https://example.com/image.jpg")
            .build()
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
            interceptors = interceptors + FakeEngineInterceptor(),
            index = 0,
            request = request,
            size = Size(100, 100),
            eventListener = EventListener.NONE,
            isPlaceholderCached = false
        )
        return chain.withRequest(request).proceed()
    }
}
