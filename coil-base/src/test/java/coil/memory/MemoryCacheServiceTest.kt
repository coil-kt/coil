package coil.memory

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.test.core.app.ApplicationProvider
import coil.DefaultRequestOptions
import coil.EventListener
import coil.RealImageLoader
import coil.annotation.ExperimentalCoilApi
import coil.bitmappool.BitmapPool
import coil.decode.Options
import coil.fetch.Fetcher
import coil.request.Parameters
import coil.request.Request
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
import coil.util.toDrawable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoilApi::class)
class MemoryCacheServiceTest {

    private lateinit var context: Context
    private lateinit var service: MemoryCacheService

    @Before
    fun before() {
        context = ApplicationProvider.getApplicationContext()
        val defaults = DefaultRequestOptions()
        val requestService = RequestService(defaults, null)
        service = MemoryCacheService(requestService, defaults, null)
    }

    @Test
    fun `computeCacheKey - null key`() {
        val fetcher = createFakeFetcher(key = null)
        val size = createFakeLazySizeResolver()
        val key = runBlocking {
            service.computeCacheKey(fetcher, Unit, Parameters.EMPTY, emptyList(), size)
        }

        assertNull(key)
    }

    @Test
    fun `computeCacheKey - basic key`() {
        val fetcher = createFakeFetcher()
        val size = createFakeLazySizeResolver()
        val result = runBlocking {
            service.computeCacheKey(fetcher, Unit, Parameters.EMPTY, emptyList(), size)
        }

        assertEquals("base_key", result)
    }

    @Test
    fun `computeCacheKey - params only`() {
        val fetcher = createFakeFetcher()
        val parameters = createFakeParameters()
        val size = createFakeLazySizeResolver()
        val result = runBlocking {
            service.computeCacheKey(fetcher, Unit, parameters, emptyList(), size)
        }

        assertEquals("base_key#key2=cached2#key3=cached3", result)
    }

    @Test
    fun `computeCacheKey - transformations only`() {
        val fetcher = createFakeFetcher()
        val transformations = createFakeTransformations()
        val size = createFakeLazySizeResolver { PixelSize(123, 332) }
        val result = runBlocking {
            service.computeCacheKey(fetcher, Unit, Parameters.EMPTY, transformations, size)
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
            service.computeCacheKey(fetcher, Unit, parameters, transformations, size)
        }

        assertEquals("base_key#key2=cached2#key3=cached3#key1#key2#coil.size.OriginalSize", result)
    }

    @Test
    fun `isCachedDrawableValid - fill`() {
        val request = createGetRequest {
            size(100, 100)
            precision(Precision.INEXACT)
        }
        val cached = createBitmap().toDrawable(context)
        assertFalse(service.isCachedDrawableValid(
            cached = cached,
            isSampled = true,
            request = request,
            size = PixelSize(200, 200),
            scale = Scale.FILL
        ))
        assertFalse(service.isCachedDrawableValid(
            cached = cached,
            isSampled = true,
            request = request,
            size = PixelSize(150, 50),
            scale = Scale.FILL
        ))
        assertTrue(service.isCachedDrawableValid(
            cached = cached,
            isSampled = true,
            request = request,
            size = PixelSize(100, 100),
            scale = Scale.FILL
        ))
        assertTrue(service.isCachedDrawableValid(
            cached = cached,
            isSampled = true,
            request = request,
            size = PixelSize(50, 100),
            scale = Scale.FILL
        ))
        assertTrue(service.isCachedDrawableValid(
            cached = cached,
            isSampled = true,
            request = request,
            size = PixelSize(50, 50),
            scale = Scale.FILL
        ))
        assertTrue(service.isCachedDrawableValid(
            cached = createBitmap(width = 400, height = 200).toDrawable(context),
            isSampled = true,
            request = request,
            size = PixelSize(400, 200),
            scale = Scale.FILL
        ))
    }

    @Test
    fun `isCachedDrawableValid - fit`() {
        val request = createGetRequest {
            size(100, 100)
            precision(Precision.INEXACT)
        }
        val cached = createBitmap().toDrawable(context)
        assertFalse(service.isCachedDrawableValid(
            cached = cached,
            isSampled = true,
            request = request,
            size = PixelSize(200, 200),
            scale = Scale.FIT
        ))
        assertTrue(service.isCachedDrawableValid(
            cached = cached,
            isSampled = true,
            request = request,
            size = PixelSize(150, 50),
            scale = Scale.FIT
        ))
        assertTrue(service.isCachedDrawableValid(
            cached = cached,
            isSampled = true,
            request = request,
            size = PixelSize(100, 100),
            scale = Scale.FIT
        ))
        assertTrue(service.isCachedDrawableValid(
            cached = cached,
            isSampled = true,
            request = request,
            size = PixelSize(50, 100),
            scale = Scale.FIT
        ))
        assertTrue(service.isCachedDrawableValid(
            cached = cached,
            isSampled = true,
            request = request,
            size = PixelSize(50, 50),
            scale = Scale.FIT
        ))
        assertFalse(service.isCachedDrawableValid(
            cached = createBitmap(width = 200, height = 400).toDrawable(context),
            isSampled = true,
            request = request,
            size = PixelSize(400, 800),
            scale = Scale.FIT
        ))
    }

    @Test
    fun `isCachedDrawableValid - small not sampled cached drawable is valid`() {
        val request = createGetRequest {
            precision(Precision.INEXACT)
        }
        val cached = createBitmap().toDrawable(context)
        val isValid = service.isCachedDrawableValid(
            cached = cached,
            isSampled = false,
            request = request,
            size = PixelSize(200, 200),
            scale = Scale.FILL
        )
        assertTrue(isValid)
    }

    @Test
    fun `isCachedDrawableValid - bitmap config must be equal`() {
        val request = createGetRequest()

        fun isBitmapConfigValid(config: Bitmap.Config): Boolean {
            val cached = createBitmap(config = config).toDrawable(context)
            return service.isCachedDrawableValid(
                cached = cached,
                isSampled = true,
                request = request,
                size = PixelSize(100, 100),
                scale = Scale.FILL
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
            return service.isCachedDrawableValid(
                cached = createBitmap(config = cachedConfig).toDrawable(context),
                isSampled = true,
                request = request,
                size = PixelSize(100, 100),
                scale = Scale.FILL
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
            return service.isCachedDrawableValid(
                cached = cached,
                isSampled = true,
                request = request,
                size = PixelSize(100, 100),
                scale = Scale.FILL
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
        assertFalse(service.isCachedDrawableValid(
            cached = createBitmap(width = 100, height = 100).toDrawable(context),
            isSampled = true,
            request = request,
            size = PixelSize(50, 50),
            scale = Scale.FILL
        ))
        assertFalse(service.isCachedDrawableValid(
            cached = createBitmap(width = 100, height = 100).toDrawable(context),
            isSampled = true,
            request = request,
            size = PixelSize(50, 50),
            scale = Scale.FIT
        ))
        assertTrue(service.isCachedDrawableValid(
            cached = createBitmap(width = 100, height = 100).toDrawable(context),
            isSampled = true,
            request = request,
            size = PixelSize(100, 50),
            scale = Scale.FILL
        ))
        assertFalse(service.isCachedDrawableValid(
            cached = createBitmap(width = 100, height = 100).toDrawable(context),
            isSampled = true,
            request = request,
            size = PixelSize(100, 50),
            scale = Scale.FIT
        ))
        assertTrue(service.isCachedDrawableValid(
            cached = createBitmap(width = 100, height = 100).toDrawable(context),
            isSampled = true,
            request = request,
            size = PixelSize(100, 100),
            scale = Scale.FILL
        ))
        assertTrue(service.isCachedDrawableValid(
            cached = createBitmap(width = 100, height = 100).toDrawable(context),
            isSampled = true,
            request = request,
            size = PixelSize(100, 100),
            scale = Scale.FIT
        ))
        assertTrue(service.isCachedDrawableValid(
            cached = createBitmap(width = 400, height = 200).toDrawable(context),
            isSampled = true,
            request = request,
            size = PixelSize(400, 200),
            scale = Scale.FILL
        ))
        assertFalse(service.isCachedDrawableValid(
            cached = createBitmap(width = 200, height = 400).toDrawable(context),
            isSampled = true,
            request = request,
            size = PixelSize(400, 800),
            scale = Scale.FIT
        ))
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
            scope = CoroutineScope(Job()), // Pass a fake scope.
            sizeResolver = object : SizeResolver {
                override suspend fun size() = block()
            },
            targetDelegate = EmptyTargetDelegate,
            request = createLoadRequest(context),
            defaults = DefaultRequestOptions(),
            eventListener = EventListener.NONE
        )
    }

    /** Convenience function to avoid having to specify the [SizeResolver] explicitly. */
    private fun MemoryCacheService.isCachedDrawableValid(
        cached: BitmapDrawable,
        isSampled: Boolean,
        request: Request,
        size: Size,
        scale: Scale
    ): Boolean = isCachedDrawableValid(cached, isSampled, request, SizeResolver(size), size, scale)
}
