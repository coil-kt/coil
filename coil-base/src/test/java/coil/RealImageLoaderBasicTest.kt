@file:Suppress("SameParameterValue")

package coil

import android.content.ContentResolver.SCHEME_FILE
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.drawable.BitmapDrawable
import androidx.test.core.app.ApplicationProvider
import coil.annotation.ExperimentalCoilApi
import coil.bitmappool.BitmapPool
import coil.decode.Options
import coil.fetch.AssetUriFetcher.Companion.ASSET_FILE_PATH_ROOT
import coil.fetch.Fetcher
import coil.memory.BitmapReferenceCounter
import coil.memory.EmptyTargetDelegate
import coil.memory.MemoryCache
import coil.memory.MemoryCache.Key
import coil.memory.RealWeakMemoryCache
import coil.request.ImageRequest
import coil.request.Parameters
import coil.size.OriginalSize
import coil.size.PixelSize
import coil.size.Precision
import coil.size.Size
import coil.size.SizeResolver
import coil.transform.Transformation
import coil.util.createRequest
import coil.util.decodeBitmapAsset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.fail

/**
 * Basic tests for [RealImageLoader] that don't touch Android's graphics pipeline ([BitmapFactory], [ImageDecoder], etc.).
 */
@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoilApi::class)
class RealImageLoaderBasicTest {

    private lateinit var context: Context
    private lateinit var memoryCache: MemoryCache
    private lateinit var imageLoader: RealImageLoader

    @Before
    fun before() {
        context = ApplicationProvider.getApplicationContext()
        val bitmapPool = BitmapPool(Int.MAX_VALUE)
        val weakMemoryCache = RealWeakMemoryCache()
        val referenceCounter = BitmapReferenceCounter(weakMemoryCache, bitmapPool, null)
        memoryCache = MemoryCache(weakMemoryCache, referenceCounter, Int.MAX_VALUE, null)
        imageLoader = RealImageLoader(
            context = context,
            defaults = DefaultRequestOptions(),
            bitmapPool = bitmapPool,
            referenceCounter = referenceCounter,
            memoryCache = memoryCache,
            weakMemoryCache = weakMemoryCache,
            callFactory = OkHttpClient(),
            eventListenerFactory = EventListener.Factory.NONE,
            registry = ComponentRegistry(),
            addLastModifiedToFileCacheKey = true,
            logger = null
        )
    }

    @After
    fun after() {
        imageLoader.shutdown()
    }

    @Test
    fun `cachedHardwareBitmap - disallowHardware`() {
        val key = Key("fake_key")
        val fileName = "normal.jpg"
        val bitmap = decodeAssetAndAddToMemoryCache(key, fileName)

        runBlocking {
            suspendCancellableCoroutine<Unit> { continuation ->
                val request = ImageRequest.Builder(context)
                    .key(key.baseKey)
                    .data("$SCHEME_FILE:///$ASSET_FILE_PATH_ROOT/$fileName")
                    .size(100, 100)
                    .precision(Precision.INEXACT)
                    .allowHardware(false)
                    .dispatcher(Dispatchers.Main.immediate)
                    .target(
                        onStart = {
                            // The hardware bitmap should not be returned as a placeholder.
                            assertNull(it)
                        },
                        onSuccess = {
                            // The hardware bitmap should not be returned as the result.
                            assertNotEquals(bitmap, (it as BitmapDrawable).bitmap)
                        }
                    )
                    .listener(
                        onSuccess = { _, _ -> continuation.resume(Unit) },
                        onError = { _, throwable -> continuation.resumeWithException(throwable) },
                        onCancel = { continuation.cancel() }
                    )
                    .build()
                imageLoader.enqueue(request)
            }
        }
    }

    @Test
    fun `cachedHardwareBitmap - allowHardware`() {
        val key = Key("fake_key")
        val fileName = "normal.jpg"
        val bitmap = decodeAssetAndAddToMemoryCache(key, fileName)

        runBlocking {
            suspendCancellableCoroutine<Unit> { continuation ->
                val request = ImageRequest.Builder(context)
                    .key(key.baseKey)
                    .data("$SCHEME_FILE:///$ASSET_FILE_PATH_ROOT/$fileName")
                    .size(100, 100)
                    .precision(Precision.INEXACT)
                    .allowHardware(true)
                    .dispatcher(Dispatchers.Main.immediate)
                    .target(
                        onStart = {
                            assertEquals(bitmap, (it as BitmapDrawable).bitmap)
                        },
                        onSuccess = {
                            assertEquals(bitmap, (it as BitmapDrawable).bitmap)
                        }
                    )
                    .listener(
                        onSuccess = { _, _ -> continuation.resume(Unit) },
                        onError = { _, throwable -> continuation.resumeWithException(throwable) },
                        onCancel = { continuation.cancel() }
                    )
                    .build()
                imageLoader.enqueue(request)
            }
        }
    }

    @Test
    fun `computeCacheKey - null key`() {
        val fetcher = createFakeFetcher(key = null)
        val size = createFakeLazySizeResolver()
        val key = runBlocking {
            imageLoader.computeCacheKey(fetcher, Unit, Parameters.EMPTY, emptyList(), size)
        }

        assertNull(key)
    }

    @Test
    fun `computeCacheKey - basic key`() {
        val fetcher = createFakeFetcher()
        val sizeResolver = createFakeLazySizeResolver()
        val result = runBlocking {
            imageLoader.computeCacheKey(fetcher, Unit, Parameters.EMPTY, emptyList(), sizeResolver)
        }

        assertEquals(Key("base_key"), result)
    }

    @Test
    fun `computeCacheKey - params only`() {
        val fetcher = createFakeFetcher()
        val parameters = createFakeParameters()
        val sizeResolver = createFakeLazySizeResolver()
        val result = runBlocking {
            imageLoader.computeCacheKey(fetcher, Unit, parameters, emptyList(), sizeResolver)
        }

        assertEquals(Key("base_key", parameters), result)
    }

    @Test
    fun `computeCacheKey - transformations only`() {
        val fetcher = createFakeFetcher()
        val transformations = createFakeTransformations()
        val size = PixelSize(123, 332)
        val sizeResolver = createFakeLazySizeResolver { size }
        val result = runBlocking {
            imageLoader.computeCacheKey(fetcher, Unit, Parameters.EMPTY, transformations, sizeResolver)
        }

        assertEquals(Key("base_key", transformations, size), result)
    }

    @Test
    fun `computeCacheKey - complex key`() {
        val fetcher = createFakeFetcher()
        val parameters = createFakeParameters()
        val transformations = createFakeTransformations()
        val size = OriginalSize
        val sizeResolver = createFakeLazySizeResolver { size }
        val result = runBlocking {
            imageLoader.computeCacheKey(fetcher, Unit, parameters, transformations, sizeResolver)
        }

        assertEquals(Key("base_key", transformations, size, parameters), result)
    }

    @Test
    fun `lazySizeResolver - resolves at most once`() {
        var isFirstResolve = true
        val lazySizeResolver = createFakeLazySizeResolver {
            check(isFirstResolve)
            isFirstResolve = false
            PixelSize(100, 100)
        }

        runBlocking {
            assertEquals(lazySizeResolver.size(), lazySizeResolver.size())
        }
    }

    private fun createFakeTransformations(): List<Transformation> {
        return listOf(
            object : Transformation {
                override fun key() = "key1"
                override suspend fun transform(pool: BitmapPool, input: Bitmap, size: Size) = fail()
            },
            object : Transformation {
                override fun key() = "key2"
                override suspend fun transform(pool: BitmapPool, input: Bitmap, size: Size) = fail()
            }
        )
    }

    private fun createFakeParameters(): Parameters {
        return Parameters.Builder()
            .set("key1", "no_cache", cacheKey = null)
            .set("key2", "cached2")
            .set("key3", "cached3")
            .build()
    }

    private fun createFakeFetcher(key: String? = "base_key"): Fetcher<Any> {
        return object : Fetcher<Any> {
            override fun key(data: Any) = key

            override suspend fun fetch(
                pool: BitmapPool,
                data: Any,
                size: Size,
                options: Options
            ) = fail()
        }
    }

    private fun createFakeLazySizeResolver(
        block: suspend () -> Size = { fail() }
    ): RealImageLoader.LazySizeResolver {
        return RealImageLoader.LazySizeResolver(
            coroutineContext = CoroutineScope(Job()).coroutineContext, // Pass a fake context.
            sizeResolver = object : SizeResolver {
                override suspend fun size() = block()
            },
            targetDelegate = EmptyTargetDelegate,
            request = createRequest(context),
            defaults = DefaultRequestOptions(),
            eventListener = EventListener.NONE
        )
    }

    private fun decodeAssetAndAddToMemoryCache(key: Key, fileName: String): Bitmap {
        val options = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.HARDWARE }
        val bitmap = context.decodeBitmapAsset(fileName, options)
        assertEquals(Bitmap.Config.HARDWARE, bitmap.config)
        memoryCache.set(key, bitmap, false)
        return bitmap
    }
}
