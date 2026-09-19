package coil3.network.okhttp

import coil3.ImageLoader
import coil3.decode.DataSource
import coil3.memory.MemoryCache
import coil3.network.NetworkFetcher
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.test.utils.context
import coil3.util.ServiceLoaderComponentRegistry
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test

class OkHttpAndroidTest {
    private val server = MockWebServer()

    @Before
    fun before() {
        server.start()
    }

    @After
    fun after() {
        server.shutdown()
    }

    @Test
    fun loadedImageIsPresentInMemoryCache() = runTest {
        val imageLoader = ImageLoader(context)
        server.enqueueImage(IMAGE)
        val request = ImageRequest.Builder(context)
            .data(server.url(IMAGE).toString())
            .size(100, 100)
            .build()
        val result = imageLoader.execute(request)

        assertIs<SuccessResult>(result)
        assertEquals(result.image, imageLoader.memoryCache!![result.memoryCacheKey!!]?.image)
    }

    @Test
    fun customMemoryCacheKey() = runTest {
        val fetchers = ServiceLoaderComponentRegistry.fetchers
        assertTrue(fetchers.any { it.factory() is NetworkFetcher.Factory })

        val imageLoader = ImageLoader(context)
        val key = MemoryCache.Key("fake_key")

        server.enqueueImage(IMAGE)
        val request = ImageRequest.Builder(context)
            .data(server.url(IMAGE).toString())
            .memoryCacheKey(key)
            .build()
        val result = imageLoader.execute(request)
        if (result is ErrorResult) throw result.throwable

        assertIs<SuccessResult>(result)
        assertEquals(DataSource.NETWORK, result.dataSource)
        assertEquals(key, result.memoryCacheKey)
        assertSame(imageLoader.memoryCache!![key]!!.image, result.image)
    }

    @Test
    fun customDiskCacheKey() = runTest {
        val imageLoader = ImageLoader(context)
        val key = "fake_key"

        server.enqueueImage(IMAGE)
        val request = ImageRequest.Builder(context)
            .data(server.url(IMAGE).toString())
            .diskCacheKey(key)
            .build()
        val result = imageLoader.execute(request)

        assertIs<SuccessResult>(result)
        assertEquals(DataSource.NETWORK, result.dataSource)
        assertEquals(key, result.diskCacheKey)
        imageLoader.diskCache!!.openSnapshot(key)!!.use { assertNotNull(it) }
    }

    @Test
    fun callFactoryIsInitializedLazily() = runTest {
        var isInitialized = false
        val imageLoader = ImageLoader.Builder(context)
            .components {
                add(
                    OkHttpNetworkFetcherFactory(
                        callFactory = {
                            check(!isInitialized)
                            isInitialized = true
                            OkHttpClient()
                        },
                    ),
                )
            }
            .build()

        assertFalse(isInitialized)

        server.enqueueImage(IMAGE)
        val request = ImageRequest.Builder(context)
            .data(server.url(IMAGE).toString())
            .build()
        val result = imageLoader.execute(request)

        assertIs<SuccessResult>(result)
        assertTrue(isInitialized)
    }

    @Test
    fun memoryCacheIsInitializedLazily() = runTest {
        var isInitialized = false
        val imageLoader = ImageLoader.Builder(context)
            .memoryCache {
                check(!isInitialized)
                isInitialized = true
                null
            }
            .build()

        assertFalse(isInitialized)

        server.enqueueImage(IMAGE)
        val request = ImageRequest.Builder(context)
            .data(server.url(IMAGE).toString())
            .build()
        val result = imageLoader.execute(request)

        assertIs<SuccessResult>(result)
        assertTrue(isInitialized)
    }

    @Test
    fun diskCacheIsInitializedLazily() = runTest {
        var isInitialized = false
        val imageLoader = ImageLoader.Builder(context)
            .diskCache {
                check(!isInitialized)
                isInitialized = true
                null
            }
            .build()

        assertFalse(isInitialized)

        server.enqueueImage(IMAGE)
        val request = ImageRequest.Builder(context)
            .data(server.url(IMAGE).toString())
            .build()
        val result = imageLoader.execute(request)

        assertIs<SuccessResult>(result)
        assertTrue(isInitialized)
    }

    @Test
    fun noMemoryCacheReturnsNoMemoryCacheKey() = runTest {
        val imageLoader = ImageLoader.Builder(context)
            .memoryCache(null)
            .build()

        server.enqueueImage(IMAGE)
        val request = ImageRequest.Builder(context)
            .data(server.url(IMAGE).toString())
            .build()
        val result = imageLoader.execute(request)

        assertIs<SuccessResult>(result)
        assertNull(result.memoryCacheKey)
    }

    @Test
    fun noDiskCacheReturnsNoDiskCacheKey() = runTest {
        val imageLoader = ImageLoader.Builder(context)
            .diskCache(null)
            .build()

        server.enqueueImage(IMAGE)
        val request = ImageRequest.Builder(context)
            .data(server.url(IMAGE).toString())
            .build()
        val result = imageLoader.execute(request)

        assertIs<SuccessResult>(result)
        assertNull(result.diskCacheKey)
    }

    companion object {
        private const val IMAGE = "normal.jpg"
    }
}
