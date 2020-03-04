package coil

import android.content.ContentResolver.SCHEME_FILE
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.drawable.BitmapDrawable
import androidx.test.core.app.ApplicationProvider
import coil.annotation.ExperimentalCoilApi
import coil.api.newLoadBuilder
import coil.bitmappool.BitmapPool
import coil.decode.Options
import coil.fetch.AssetUriFetcher.Companion.ASSET_FILE_PATH_ROOT
import coil.fetch.Fetcher
import coil.memory.BitmapReferenceCounter
import coil.memory.EmptyTargetDelegate
import coil.memory.MemoryCache
import coil.request.Parameters
import coil.size.OriginalSize
import coil.size.PixelSize
import coil.size.Precision
import coil.size.Scale
import coil.size.Size
import coil.size.SizeResolver
import coil.transform.Transformation
import coil.util.createBitmap
import coil.util.createGetRequest
import coil.util.createLoadRequest
import coil.util.decodeBitmapAsset
import coil.util.error
import coil.util.toDrawable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Basic tests for [RealImageLoader] that don't touch Android's graphics pipeline ([BitmapFactory], [ImageDecoder], etc.).
 */
@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoilApi::class)
class RealImageLoaderBasicTest {

    private lateinit var context: Context
    private lateinit var bitmapPool: BitmapPool
    private lateinit var referenceCounter: BitmapReferenceCounter
    private lateinit var memoryCache: MemoryCache
    private lateinit var imageLoader: RealImageLoader

    @Before
    fun before() {
        context = ApplicationProvider.getApplicationContext()
        bitmapPool = BitmapPool(Long.MAX_VALUE)
        referenceCounter = BitmapReferenceCounter(bitmapPool)
        memoryCache = MemoryCache(referenceCounter, Int.MAX_VALUE)
        imageLoader = RealImageLoader(
            context,
            DefaultRequestOptions(),
            bitmapPool,
            referenceCounter,
            memoryCache,
            OkHttpClient(),
            ComponentRegistry()
        )
    }

    @After
    fun after() {
        imageLoader.shutdown()
    }

    @Test
    fun `isCachedDrawableValid - fill`() {
        val request = createGetRequest {
            size(100, 100)
            precision(Precision.INEXACT)
        }
        val cached = createBitmap().toDrawable(context)
        assertFalse(imageLoader.isCachedDrawableValid(
            cached = cached,
            isSampled = true,
            size = PixelSize(200, 200),
            scale = Scale.FILL,
            request = request
        ))
        assertFalse(imageLoader.isCachedDrawableValid(
            cached = cached,
            isSampled = true,
            size = PixelSize(150, 50),
            scale = Scale.FILL,
            request = request
        ))
        assertTrue(imageLoader.isCachedDrawableValid(
            cached = cached,
            isSampled = true,
            size = PixelSize(100, 100),
            scale = Scale.FILL,
            request = request
        ))
        assertTrue(imageLoader.isCachedDrawableValid(
            cached = cached,
            isSampled = true,
            size = PixelSize(50, 100),
            scale = Scale.FILL,
            request = request
        ))
        assertTrue(imageLoader.isCachedDrawableValid(
            cached = cached,
            isSampled = true,
            size = PixelSize(50, 50),
            scale = Scale.FILL,
            request = request
        ))
        assertTrue(imageLoader.isCachedDrawableValid(
            cached = createBitmap(width = 400, height = 200).toDrawable(context),
            isSampled = true,
            size = PixelSize(400, 200),
            scale = Scale.FILL,
            request = request
        ))
    }

    @Test
    fun `isCachedDrawableValid - fit`() {
        val request = createGetRequest {
            size(100, 100)
            precision(Precision.INEXACT)
        }
        val cached = createBitmap().toDrawable(context)
        assertFalse(imageLoader.isCachedDrawableValid(
            cached = cached,
            isSampled = true,
            size = PixelSize(200, 200),
            scale = Scale.FIT,
            request = request
        ))
        assertTrue(imageLoader.isCachedDrawableValid(
            cached = cached,
            isSampled = true,
            size = PixelSize(150, 50),
            scale = Scale.FIT,
            request = request
        ))
        assertTrue(imageLoader.isCachedDrawableValid(
            cached = cached,
            isSampled = true,
            size = PixelSize(100, 100),
            scale = Scale.FIT,
            request = request
        ))
        assertTrue(imageLoader.isCachedDrawableValid(
            cached = cached,
            isSampled = true,
            size = PixelSize(50, 100),
            scale = Scale.FIT,
            request = request
        ))
        assertTrue(imageLoader.isCachedDrawableValid(
            cached = cached,
            isSampled = true,
            size = PixelSize(50, 50),
            scale = Scale.FIT,
            request = request
        ))
        assertFalse(imageLoader.isCachedDrawableValid(
            cached = createBitmap(width = 200, height = 400).toDrawable(context),
            isSampled = true,
            size = PixelSize(400, 800),
            scale = Scale.FIT,
            request = request
        ))
    }

    @Test
    fun `isCachedDrawableValid - small not sampled cached drawable is valid`() {
        val request = createGetRequest {
            precision(Precision.INEXACT)
        }
        val cached = createBitmap().toDrawable(context)
        val isValid = imageLoader.isCachedDrawableValid(
            cached = cached,
            isSampled = false,
            size = PixelSize(200, 200),
            scale = Scale.FILL,
            request = request
        )
        assertTrue(isValid)
    }

    @Test
    fun `isCachedDrawableValid - bitmap config must be equal`() {
        val request = createGetRequest()

        fun isBitmapConfigValid(config: Bitmap.Config): Boolean {
            val cached = createBitmap(config = config).toDrawable(context)
            return imageLoader.isCachedDrawableValid(
                cached = cached,
                isSampled = true,
                size = PixelSize(100, 100),
                scale = Scale.FILL,
                request = request
            )
        }

        assertFalse(isBitmapConfigValid(Bitmap.Config.RGBA_F16))
        assertTrue(isBitmapConfigValid(Bitmap.Config.HARDWARE))
        assertTrue(isBitmapConfigValid(Bitmap.Config.ARGB_8888))
        assertFalse(isBitmapConfigValid(Bitmap.Config.RGB_565))
        assertFalse(isBitmapConfigValid(Bitmap.Config.ALPHA_8))
    }

    @Test
    fun `isCachedDrawableValid - allowRgb565=true allows matching any config if cached bitmap is RGB_565`() {
        fun isBitmapConfigValid(
            cachedConfig: Bitmap.Config,
            requestedConfig: Bitmap.Config
        ): Boolean {
            val request = createGetRequest {
                allowRgb565(true)
                bitmapConfig(requestedConfig)
            }
            return imageLoader.isCachedDrawableValid(
                cached = createBitmap(config = cachedConfig).toDrawable(context),
                isSampled = true,
                size = PixelSize(100, 100),
                scale = Scale.FILL,
                request = request
            )
        }

        assertTrue(isBitmapConfigValid(Bitmap.Config.RGB_565, Bitmap.Config.HARDWARE))
        assertTrue(isBitmapConfigValid(Bitmap.Config.RGB_565, Bitmap.Config.RGBA_F16))
        assertTrue(isBitmapConfigValid(Bitmap.Config.RGB_565, Bitmap.Config.ARGB_8888))
        assertTrue(isBitmapConfigValid(Bitmap.Config.RGB_565, Bitmap.Config.RGB_565))
        assertTrue(isBitmapConfigValid(Bitmap.Config.RGB_565, Bitmap.Config.ALPHA_8))

        assertFalse(isBitmapConfigValid(Bitmap.Config.ARGB_8888, Bitmap.Config.RGBA_F16))
        assertTrue(isBitmapConfigValid(Bitmap.Config.ARGB_8888, Bitmap.Config.HARDWARE))
        assertTrue(isBitmapConfigValid(Bitmap.Config.ARGB_8888, Bitmap.Config.ARGB_8888))
        assertFalse(isBitmapConfigValid(Bitmap.Config.ARGB_8888, Bitmap.Config.RGB_565))
        assertFalse(isBitmapConfigValid(Bitmap.Config.ARGB_8888, Bitmap.Config.ALPHA_8))
    }

    @Test
    fun `isCachedDrawableValid - allowHardware=false prevents using cached hardware bitmap`() {
        val request = createGetRequest {
            allowHardware(false)
        }

        fun isBitmapConfigValid(config: Bitmap.Config): Boolean {
            val cached = createBitmap(config = config).toDrawable(context)
            return imageLoader.isCachedDrawableValid(
                cached = cached,
                isSampled = true,
                size = PixelSize(100, 100),
                scale = Scale.FILL,
                request = request
            )
        }

        assertFalse(isBitmapConfigValid(Bitmap.Config.HARDWARE))
        assertTrue(isBitmapConfigValid(Bitmap.Config.ARGB_8888))
        assertFalse(isBitmapConfigValid(Bitmap.Config.RGB_565))
    }

    @Test
    fun `isCachedDrawableValid - exact precision`() {
        val request = createLoadRequest(context) {
            precision(Precision.EXACT)
        }
        assertFalse(imageLoader.isCachedDrawableValid(
            cached = createBitmap(width = 100, height = 100).toDrawable(context),
            isSampled = true,
            size = PixelSize(50, 50),
            scale = Scale.FILL,
            request = request
        ))
        assertFalse(imageLoader.isCachedDrawableValid(
            cached = createBitmap(width = 100, height = 100).toDrawable(context),
            isSampled = true,
            size = PixelSize(50, 50),
            scale = Scale.FIT,
            request = request
        ))
        assertTrue(imageLoader.isCachedDrawableValid(
            cached = createBitmap(width = 100, height = 100).toDrawable(context),
            isSampled = true,
            size = PixelSize(100, 50),
            scale = Scale.FILL,
            request = request
        ))
        assertFalse(imageLoader.isCachedDrawableValid(
            cached = createBitmap(width = 100, height = 100).toDrawable(context),
            isSampled = true,
            size = PixelSize(100, 50),
            scale = Scale.FIT,
            request = request
        ))
        assertTrue(imageLoader.isCachedDrawableValid(
            cached = createBitmap(width = 100, height = 100).toDrawable(context),
            isSampled = true,
            size = PixelSize(100, 100),
            scale = Scale.FILL,
            request = request
        ))
        assertTrue(imageLoader.isCachedDrawableValid(
            cached = createBitmap(width = 100, height = 100).toDrawable(context),
            isSampled = true,
            size = PixelSize(100, 100),
            scale = Scale.FIT,
            request = request
        ))
        assertTrue(imageLoader.isCachedDrawableValid(
            cached = createBitmap(width = 400, height = 200).toDrawable(context),
            isSampled = true,
            size = PixelSize(400, 200),
            scale = Scale.FILL,
            request = request
        ))
        assertFalse(imageLoader.isCachedDrawableValid(
            cached = createBitmap(width = 200, height = 400).toDrawable(context),
            isSampled = true,
            size = PixelSize(400, 800),
            scale = Scale.FIT,
            request = request
        ))
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
        val size = createFakeLazySizeResolver()
        val result = runBlocking {
            imageLoader.computeCacheKey(fetcher, Unit, Parameters.EMPTY, emptyList(), size)
        }

        assertEquals("base_key", result)
    }

    @Test
    fun `computeCacheKey - params only`() {
        val fetcher = createFakeFetcher()
        val parameters = createFakeParameters()
        val size = createFakeLazySizeResolver()
        val result = runBlocking {
            imageLoader.computeCacheKey(fetcher, Unit, parameters, emptyList(), size)
        }

        assertEquals("base_key#key2=cached2#key3=cached3", result)
    }

    @Test
    fun `computeCacheKey - transformations only`() {
        val fetcher = createFakeFetcher()
        val transformations = createFakeTransformations()
        val size = createFakeLazySizeResolver { PixelSize(123, 332) }
        val result = runBlocking {
            imageLoader.computeCacheKey(fetcher, Unit, Parameters.EMPTY, transformations, size)
        }

        assertEquals("base_key#key1#key2#PixelSize(width=123, height=332)", result)
    }

    @Test
    fun `computeCacheKey - complex key`() {
        val fetcher = createFakeFetcher()
        val parameters = createFakeParameters()
        val transformations = createFakeTransformations()
        val size = createFakeLazySizeResolver { OriginalSize }
        val result = runBlocking {
            imageLoader.computeCacheKey(fetcher, Unit, parameters, transformations, size)
        }

        assertEquals("base_key#key2=cached2#key3=cached3#key1#key2#OriginalSize", result)
    }

    @Test
    fun `lazySizeResolver - resolves at most once`() {
        var isFirstResolve = true
        val lazySizeResolver = createFakeLazySizeResolver {
            if (isFirstResolve) {
                isFirstResolve = false
                PixelSize(100, 100)
            } else {
                throw IllegalStateException()
            }
        }

        runBlocking {
            assertEquals(lazySizeResolver.size(), lazySizeResolver.size())
        }
    }

    @Test
    fun cachedHardwareBitmap_disallowHardware() {
        val key = "fake_key"
        val fileName = "normal.jpg"
        val bitmap = decodeAssetAndAddToMemoryCache(key, fileName)

        runBlocking {
            var error: Throwable? = null
            val request = imageLoader.newLoadBuilder(context)
                .key(key)
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
                    onError = { _, throwable -> error = throwable }
                )
                .build()
            imageLoader.load(request).await()

            // Rethrow any errors that occurred while loading.
            error?.let { throw it }
        }
    }

    @Test
    fun cachedHardwareBitmap_allowHardware() {
        val key = "fake_key"
        val fileName = "normal.jpg"
        val bitmap = decodeAssetAndAddToMemoryCache(key, fileName)

        runBlocking {
            var error: Throwable? = null
            val request = imageLoader.newLoadBuilder(context)
                .key(key)
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
                    onError = { _, throwable -> error = throwable }
                )
                .build()
            imageLoader.load(request).await()

            // Rethrow any errors that occurred while loading.
            error?.let { throw it }
        }
    }

    @Suppress("SameParameterValue")
    private fun decodeAssetAndAddToMemoryCache(key: String, fileName: String): Bitmap {
        val options = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.HARDWARE }
        val bitmap = context.decodeBitmapAsset(fileName, options)
        assertEquals(Bitmap.Config.HARDWARE, bitmap.config)
        memoryCache.set(key, bitmap, false)
        return bitmap
    }

    private fun createFakeTransformations(): List<Transformation> {
        return listOf(
            object : Transformation {
                override fun key() = "key1"
                override suspend fun transform(pool: BitmapPool, input: Bitmap, size: Size) = error()
            },
            object : Transformation {
                override fun key() = "key2"
                override suspend fun transform(pool: BitmapPool, input: Bitmap, size: Size) = error()
            }
        )
    }

    private fun createFakeParameters(): Parameters {
        return Parameters.Builder()
            .set("key1", "no_cache")
            .set("key2", "cached2", "cached2")
            .set("key3", "cached3", "cached3")
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
            ) = error()
        }
    }

    private fun createFakeLazySizeResolver(
        block: suspend () -> Size = { error() }
    ): RealImageLoader.LazySizeResolver {
        return RealImageLoader.LazySizeResolver(
            scope = CoroutineScope(Job()), // Pass a fake scope.
            sizeResolver = object : SizeResolver {
                override suspend fun size() = block()
            },
            targetDelegate = EmptyTargetDelegate,
            request = createLoadRequest(context)
        )
    }
}
