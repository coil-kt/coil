package coil.intercept

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import androidx.test.core.app.ApplicationProvider
import coil.ComponentRegistry
import coil.EventListener
import coil.ImageLoader
import coil.RealImageLoader
import coil.bitmap.BitmapPool
import coil.bitmap.RealBitmapReferenceCounter
import coil.decode.DataSource
import coil.decode.DrawableDecoderService
import coil.decode.Options
import coil.fetch.DrawableResult
import coil.fetch.Fetcher
import coil.memory.MemoryCache.Key
import coil.memory.MemoryCacheService
import coil.memory.RealMemoryCache
import coil.memory.RealWeakMemoryCache
import coil.memory.RequestService
import coil.memory.StrongMemoryCache
import coil.request.ImageRequest
import coil.request.Parameters
import coil.size.OriginalSize
import coil.size.PixelSize
import coil.size.Precision
import coil.size.Scale
import coil.size.Size
import coil.transform.CircleCropTransformation
import coil.transform.Transformation
import coil.util.SystemCallbacks
import coil.util.createBitmap
import coil.util.createRequest
import coil.util.invoke
import coil.util.size
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.test.fail

@RunWith(RobolectricTestRunner::class)
class EngineInterceptorTest {

    private lateinit var context: Context
    private lateinit var strongMemoryCache: StrongMemoryCache
    private lateinit var interceptor: EngineInterceptor

    @Before
    fun before() {
        context = ApplicationProvider.getApplicationContext()
        val bitmapPool = BitmapPool(Int.MAX_VALUE)
        val weakMemoryCache = RealWeakMemoryCache(null)
        val referenceCounter = RealBitmapReferenceCounter(weakMemoryCache, bitmapPool, null)
        strongMemoryCache = StrongMemoryCache(weakMemoryCache, referenceCounter, Int.MAX_VALUE, null)
        interceptor = EngineInterceptor(
            registry = ComponentRegistry(),
            bitmapPool = bitmapPool,
            referenceCounter = referenceCounter,
            strongMemoryCache = strongMemoryCache,
            memoryCacheService = MemoryCacheService(referenceCounter, strongMemoryCache, weakMemoryCache),
            requestService = RequestService(null),
            systemCallbacks = SystemCallbacks(ImageLoader(context) as RealImageLoader, context),
            drawableDecoder = DrawableDecoderService(bitmapPool),
            logger = null
        )
    }

    @Test
    fun `computeMemoryCacheKey - null key`() {
        val request = createRequest(context)
        val fetcher = createFakeFetcher(key = null)
        val size = OriginalSize
        val key = runBlocking {
            interceptor.computeMemoryCacheKey(request, Unit, fetcher, size)
        }

        assertNull(key)
    }

    @Test
    fun `computeMemoryCacheKey - simple key`() {
        val request = createRequest(context)
        val fetcher = createFakeFetcher()
        val size = OriginalSize
        val result = runBlocking {
            interceptor.computeMemoryCacheKey(request, Unit, fetcher, size)
        }

        assertEquals(Key("base_key", Parameters.EMPTY), result)
    }

    @Test
    fun `computeMemoryCacheKey - params only`() {
        val parameters = createFakeParameters()
        val request = createRequest(context) {
            parameters(parameters)
        }
        val fetcher = createFakeFetcher()
        val size = OriginalSize
        val result = runBlocking {
            interceptor.computeMemoryCacheKey(request, Unit, fetcher, size)
        }

        assertEquals(Key("base_key", parameters), result)
    }

    @Test
    fun `computeMemoryCacheKey - transformations only`() {
        val transformations = createFakeTransformations()
        val request = createRequest(context) {
            transformations(transformations)
        }
        val fetcher = createFakeFetcher()
        val size = PixelSize(123, 332)
        val result = runBlocking {
            interceptor.computeMemoryCacheKey(request, Unit, fetcher, size)
        }

        assertEquals(Key("base_key", transformations, size, Parameters.EMPTY), result)
    }

    @Test
    fun `computeMemoryCacheKey - complex key`() {
        val parameters = createFakeParameters()
        val transformations = createFakeTransformations()
        val request = createRequest(context) {
            parameters(parameters)
            transformations(transformations)
        }
        val fetcher = createFakeFetcher()
        val size = OriginalSize
        val result = runBlocking {
            interceptor.computeMemoryCacheKey(request, Unit, fetcher, size)
        }

        assertEquals(Key("base_key", transformations, size, parameters), result)
    }

    @Test
    fun `isCachedValueValid - fill`() {
        val request = createRequest(context) {
            size(100, 100)
            precision(Precision.INEXACT)
            scale(Scale.FILL)
        }
        val cached = createBitmap()
        assertFalse(interceptor.isCachedValueValid(
            cached = cached,
            isSampled = true,
            request = request,
            size = PixelSize(200, 200)
        ))
        assertFalse(interceptor.isCachedValueValid(
            cached = cached,
            isSampled = true,
            request = request,
            size = PixelSize(150, 50)
        ))
        assertTrue(interceptor.isCachedValueValid(
            cached = cached,
            isSampled = true,
            request = request,
            size = PixelSize(100, 100)
        ))
        assertTrue(interceptor.isCachedValueValid(
            cached = cached,
            isSampled = true,
            request = request,
            size = PixelSize(50, 100)
        ))
        assertTrue(interceptor.isCachedValueValid(
            cached = cached,
            isSampled = true,
            request = request,
            size = PixelSize(50, 50)
        ))
        assertTrue(interceptor.isCachedValueValid(
            cached = createBitmap(width = 400, height = 200),
            isSampled = true,
            request = request,
            size = PixelSize(400, 200)
        ))
    }

    @Test
    fun `isCachedValueValid - fit`() {
        val request = createRequest(context) {
            size(100, 100)
            precision(Precision.INEXACT)
            scale(Scale.FIT)
        }
        val cached = createBitmap()
        assertFalse(interceptor.isCachedValueValid(
            cached = cached,
            isSampled = true,
            request = request,
            size = PixelSize(200, 200)
        ))
        assertTrue(interceptor.isCachedValueValid(
            cached = cached,
            isSampled = true,
            request = request,
            size = PixelSize(150, 50)
        ))
        assertTrue(interceptor.isCachedValueValid(
            cached = cached,
            isSampled = true,
            request = request,
            size = PixelSize(100, 100)
        ))
        assertTrue(interceptor.isCachedValueValid(
            cached = cached,
            isSampled = true,
            request = request,
            size = PixelSize(50, 100)
        ))
        assertTrue(interceptor.isCachedValueValid(
            cached = cached,
            isSampled = true,
            request = request,
            size = PixelSize(50, 50)
        ))
        assertFalse(interceptor.isCachedValueValid(
            cached = createBitmap(width = 200, height = 400),
            isSampled = true,
            request = request,
            size = PixelSize(400, 800)
        ))
    }

    @Test
    fun `isCachedValueValid - small not sampled cached drawable is valid`() {
        val cached = createBitmap()
        val isValid = interceptor.isCachedValueValid(
            cached = cached,
            isSampled = false,
            request = createRequest(context) {
                precision(Precision.INEXACT)
                scale(Scale.FILL)
            },
            size = PixelSize(200, 200)
        )
        assertTrue(isValid)
    }

    @Test
    fun `isCachedValueValid - allowHardware=false prevents using cached hardware bitmap`() {
        fun isBitmapConfigValid(config: Bitmap.Config): Boolean {
            val cached = createBitmap(config = config)
            return interceptor.isCachedValueValid(
                cached = cached,
                isSampled = true,
                request = createRequest(context) {
                    allowHardware(false)
                    scale(Scale.FILL)
                },
                size = PixelSize(100, 100)
            )
        }

        assertFalse(isBitmapConfigValid(Bitmap.Config.HARDWARE))
        assertTrue(isBitmapConfigValid(Bitmap.Config.RGBA_F16))
        assertTrue(isBitmapConfigValid(Bitmap.Config.ARGB_8888))
        assertTrue(isBitmapConfigValid(Bitmap.Config.RGB_565))
    }

    @Test
    fun `isCachedValueValid - exact precision`() {
        assertFalse(interceptor.isCachedValueValid(
            cached = createBitmap(width = 100, height = 100),
            isSampled = true,
            request = createRequest(context) {
                precision(Precision.EXACT)
                scale(Scale.FILL)
            },
            size = PixelSize(50, 50)
        ))
        assertFalse(interceptor.isCachedValueValid(
            cached = createBitmap(width = 100, height = 100),
            isSampled = true,
            request = createRequest(context) {
                precision(Precision.EXACT)
                scale(Scale.FIT)
            },
            size = PixelSize(50, 50)
        ))
        assertTrue(interceptor.isCachedValueValid(
            cached = createBitmap(width = 100, height = 100),
            isSampled = true,
            request = createRequest(context) {
                precision(Precision.EXACT)
                scale(Scale.FILL)
            },
            size = PixelSize(100, 50)
        ))
        assertFalse(interceptor.isCachedValueValid(
            cached = createBitmap(width = 100, height = 100),
            isSampled = true,
            request = createRequest(context) {
                precision(Precision.EXACT)
                scale(Scale.FIT)
            },
            size = PixelSize(100, 50)
        ))
        assertTrue(interceptor.isCachedValueValid(
            cached = createBitmap(width = 100, height = 100),
            isSampled = true,
            request = createRequest(context) {
                precision(Precision.EXACT)
                scale(Scale.FILL)
            },
            size = PixelSize(100, 100)
        ))
        assertTrue(interceptor.isCachedValueValid(
            cached = createBitmap(width = 100, height = 100),
            isSampled = true,
            request = createRequest(context) {
                precision(Precision.EXACT)
                scale(Scale.FIT)
            },
            size = PixelSize(100, 100)
        ))
        assertTrue(interceptor.isCachedValueValid(
            cached = createBitmap(width = 400, height = 200),
            isSampled = true,
            request = createRequest(context) {
                precision(Precision.EXACT)
                scale(Scale.FILL)
            },
            size = PixelSize(400, 200)
        ))
        assertFalse(interceptor.isCachedValueValid(
            cached = createBitmap(width = 200, height = 400),
            isSampled = true,
            request = createRequest(context) {
                precision(Precision.EXACT)
                scale(Scale.FIT)
            },
            size = PixelSize(400, 800)
        ))
    }

    @Test
    fun `isCachedValueValid - one pixel off`() {
        assertTrue(interceptor.isCachedValueValid(
            cached = createBitmap(width = 244, height = 600),
            isSampled = true,
            request = createRequest(context) {
                precision(Precision.EXACT)
                scale(Scale.FIT)
            },
            size = PixelSize(245, 600)
        ))
        assertTrue(interceptor.isCachedValueValid(
            cached = createBitmap(width = 244, height = 600),
            isSampled = true,
            request = createRequest(context) {
                precision(Precision.INEXACT)
                scale(Scale.FIT)
            },
            size = PixelSize(245, 600)
        ))
        assertFalse(interceptor.isCachedValueValid(
            cached = createBitmap(width = 243, height = 599),
            isSampled = true,
            request = createRequest(context) {
                precision(Precision.EXACT)
                scale(Scale.FIT)
            },
            size = PixelSize(245, 600)
        ))
        assertFalse(interceptor.isCachedValueValid(
            cached = createBitmap(width = 243, height = 599),
            isSampled = true,
            request = createRequest(context) {
                precision(Precision.INEXACT)
                scale(Scale.FIT)
            },
            size = PixelSize(245, 600)
        ))
    }

    @Test
    fun `isCachedValueValid - transformation that reduces size of output bitmap`() {
        val transformations = listOf(CircleCropTransformation())
        val cachedSize = PixelSize(1000, 500) // The size of the previous request.
        val key = Key("key", transformations, cachedSize, Parameters.EMPTY)
        val value = object : RealMemoryCache.Value {
            override val bitmap = createBitmap(width = 200, height = 200) // The small cached bitmap.
            override val isSampled = true
        }
        val request = createRequest(context)

        assertTrue(interceptor.isCachedValueValid(
            cacheKey = key,
            cacheValue = value,
            request = request.newBuilder().precision(Precision.INEXACT).scale(Scale.FIT).build(),
            size = PixelSize(650, 400)
        ))

        assertTrue(interceptor.isCachedValueValid(
            cacheKey = key,
            cacheValue = value,
            request = request.newBuilder().precision(Precision.EXACT).scale(Scale.FIT).build(),
            size = PixelSize(1000, 500)
        ))

        assertFalse(interceptor.isCachedValueValid(
            cacheKey = key,
            cacheValue = value,
            request = request.newBuilder().precision(Precision.INEXACT).scale(Scale.FIT).build(),
            size = PixelSize(1500, 1000)
        ))

        assertFalse(interceptor.isCachedValueValid(
            cacheKey = key,
            cacheValue = value,
            request = request.newBuilder().precision(Precision.EXACT).scale(Scale.FIT).build(),
            size = PixelSize(800, 500)
        ))
    }

    private fun EngineInterceptor.isCachedValueValid(
        cached: Bitmap,
        isSampled: Boolean,
        request: ImageRequest,
        size: Size
    ): Boolean {
        val key = Key("key")
        val value = object : RealMemoryCache.Value {
            override val bitmap = cached
            override val isSampled = isSampled
        }
        return isCachedValueValid(key, value, request, size)
    }

    @Test
    fun `applyTransformations - transformations convert drawable to bitmap`() {
        val drawable = ColorDrawable(Color.BLACK)
        val size = PixelSize(100, 100)
        val result = runBlocking {
            interceptor.applyTransformations(
                result = DrawableResult(
                    drawable = drawable,
                    isSampled = false,
                    dataSource = DataSource.MEMORY
                ),
                request = createRequest(context) { transformations(CircleCropTransformation()) },
                size = size,
                options = Options(context),
                eventListener = EventListener.NONE
            )
        }

        val resultDrawable = result.drawable
        assertTrue(resultDrawable is BitmapDrawable)
        assertEquals(resultDrawable.bitmap.size, size)
    }

    @Test
    fun `applyTransformations - empty transformations does not convert drawable to bitmap`() {
        val drawable = ColorDrawable(Color.BLACK)
        val size = PixelSize(100, 100)
        val result = runBlocking {
            interceptor.applyTransformations(
                result = DrawableResult(
                    drawable = drawable,
                    isSampled = false,
                    dataSource = DataSource.MEMORY
                ),
                request = createRequest(context) { transformations(emptyList()) },
                size = size,
                options = Options(context),
                eventListener = EventListener.NONE
            )
        }

        assertSame(drawable, result.drawable)
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
}
