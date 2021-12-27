package coil.coil.memory

import android.content.Context
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import coil.EventListener
import coil.ImageLoader
import coil.RealImageLoader
import coil.key.Keyer
import coil.memory.MemoryCache
import coil.memory.MemoryCacheService
import coil.memory.MemoryCacheService.Companion.EXTRA_IS_SAMPLED
import coil.memory.MemoryCacheService.Companion.EXTRA_TRANSFORMATION_INDEX
import coil.memory.MemoryCacheService.Companion.EXTRA_TRANSFORMATION_SIZE
import coil.request.ImageRequest
import coil.request.Options
import coil.request.Parameters
import coil.request.RequestService
import coil.size.Dimension
import coil.size.Precision
import coil.size.Scale
import coil.size.Size
import coil.transform.CircleCropTransformation
import coil.transform.Transformation
import coil.util.SystemCallbacks
import coil.util.createBitmap
import coil.util.createRequest
import coil.util.forEachIndexedIndices
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
class MemoryCacheServiceTest {

    private lateinit var context: Context

    @Before
    fun before() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `newCacheKey - null key`() {
        val service = newService(key = null)
        val request = createRequest(context)
        val options = Options(context, size = Size.ORIGINAL)
        val key = service.newCacheKey(request, Unit, options, EventListener.NONE)

        assertNull(key)
    }

    @Test
    fun `newCacheKey - simple key`() {
        val service = newService()
        val request = createRequest(context)
        val options = Options(context, size = Size.ORIGINAL)
        val actual = service.newCacheKey(request, Unit, options, EventListener.NONE)

        assertEquals(newMemoryCacheKey(), actual)
    }

    @Test
    fun `newCacheKey - params only`() {
        val service = newService()
        val parameters = createFakeParameters()
        val request = createRequest(context) {
            parameters(parameters)
        }
        val options = Options(context, size = Size.ORIGINAL)
        val actual = service.newCacheKey(request, Unit, options, EventListener.NONE)

        assertEquals(newMemoryCacheKey(parameters = parameters), actual)
    }

    @Test
    fun `newCacheKey - transformations only`() {
        val service = newService()
        val transformations = createFakeTransformations()
        val request = createRequest(context) {
            transformations(transformations)
        }
        val size = Size(123, 332)
        val options = Options(context, size = size)
        val actual = service.newCacheKey(request, Unit, options, EventListener.NONE)

        assertEquals(newMemoryCacheKey(transformations = transformations, size = size), actual)
    }

    @Test
    fun `newCacheKey - complex key`() {
        val service = newService(key = TEST_KEY)
        val parameters = createFakeParameters()
        val transformations = createFakeTransformations()
        val request = createRequest(context) {
            parameters(parameters)
            transformations(transformations)
        }
        val options = Options(context, size = Size.ORIGINAL)
        val actual = service.newCacheKey(request, Unit, options, EventListener.NONE)

        assertEquals(newMemoryCacheKey(transformations = transformations, parameters = parameters), actual)
    }

    @Test
    fun `isCacheValueValid - fill`() {
        val service = newService()
        val request = createRequest(context) {
            size(100, 100)
            precision(Precision.INEXACT)
            scale(Scale.FILL)
        }
        val cached = createBitmap()
        assertFalse(service.isCacheValueValid(
            cached = cached,
            isSampled = true,
            request = request,
            size = Size(200, 200)
        ))
        assertFalse(service.isCacheValueValid(
            cached = cached,
            isSampled = true,
            request = request,
            size = Size(150, 50)
        ))
        assertTrue(service.isCacheValueValid(
            cached = cached,
            isSampled = true,
            request = request,
            size = Size(100, 100)
        ))
        assertTrue(service.isCacheValueValid(
            cached = cached,
            isSampled = true,
            request = request,
            size = Size(50, 100)
        ))
        assertTrue(service.isCacheValueValid(
            cached = cached,
            isSampled = true,
            request = request,
            size = Size(50, 50)
        ))
        assertTrue(service.isCacheValueValid(
            cached = createBitmap(width = 400, height = 200),
            isSampled = true,
            request = request,
            size = Size(400, 200)
        ))
    }

    @Test
    fun `isCacheValueValid - fit`() {
        val service = newService()
        val request = createRequest(context) {
            size(100, 100)
            precision(Precision.INEXACT)
            scale(Scale.FIT)
        }
        val cached = createBitmap()
        assertFalse(service.isCacheValueValid(
            cached = cached,
            isSampled = true,
            request = request,
            size = Size(200, 200)
        ))
        assertTrue(service.isCacheValueValid(
            cached = cached,
            isSampled = true,
            request = request,
            size = Size(150, 50)
        ))
        assertTrue(service.isCacheValueValid(
            cached = cached,
            isSampled = true,
            request = request,
            size = Size(100, 100)
        ))
        assertTrue(service.isCacheValueValid(
            cached = cached,
            isSampled = true,
            request = request,
            size = Size(50, 100)
        ))
        assertTrue(service.isCacheValueValid(
            cached = cached,
            isSampled = true,
            request = request,
            size = Size(50, 50)
        ))
        assertFalse(service.isCacheValueValid(
            cached = createBitmap(width = 200, height = 400),
            isSampled = true,
            request = request,
            size = Size(400, 800)
        ))
    }

    @Test
    fun `isCacheValueValid - small not sampled cached drawable is valid`() {
        val service = newService()
        val cached = createBitmap()
        val isValid = service.isCacheValueValid(
            cached = cached,
            isSampled = false,
            request = createRequest(context) {
                precision(Precision.INEXACT)
                scale(Scale.FILL)
            },
            size = Size(200, 200)
        )
        assertTrue(isValid)
    }

    @Test
    fun `isCacheValueValid - allowHardware=false prevents using cached hardware bitmap`() {
        val service = newService()
        fun isBitmapConfigValid(config: Bitmap.Config): Boolean {
            val cached = createBitmap(config = config)
            return service.isCacheValueValid(
                cached = cached,
                isSampled = true,
                request = createRequest(context) {
                    allowHardware(false)
                    scale(Scale.FILL)
                },
                size = Size(100, 100)
            )
        }

        assertFalse(isBitmapConfigValid(Bitmap.Config.HARDWARE))
        assertTrue(isBitmapConfigValid(Bitmap.Config.RGBA_F16))
        assertTrue(isBitmapConfigValid(Bitmap.Config.ARGB_8888))
        assertTrue(isBitmapConfigValid(Bitmap.Config.RGB_565))
    }

    @Test
    fun `isCacheValueValid - exact precision`() {
        val service = newService()
        assertFalse(service.isCacheValueValid(
            cached = createBitmap(width = 100, height = 100),
            isSampled = true,
            request = createRequest(context) {
                precision(Precision.EXACT)
                scale(Scale.FILL)
            },
            size = Size(50, 50)
        ))
        assertFalse(service.isCacheValueValid(
            cached = createBitmap(width = 100, height = 100),
            isSampled = true,
            request = createRequest(context) {
                precision(Precision.EXACT)
                scale(Scale.FIT)
            },
            size = Size(50, 50)
        ))
        assertTrue(service.isCacheValueValid(
            cached = createBitmap(width = 100, height = 100),
            isSampled = true,
            request = createRequest(context) {
                precision(Precision.EXACT)
                scale(Scale.FILL)
            },
            size = Size(100, 50)
        ))
        assertFalse(service.isCacheValueValid(
            cached = createBitmap(width = 100, height = 100),
            isSampled = true,
            request = createRequest(context) {
                precision(Precision.EXACT)
                scale(Scale.FIT)
            },
            size = Size(100, 50)
        ))
        assertTrue(service.isCacheValueValid(
            cached = createBitmap(width = 100, height = 100),
            isSampled = true,
            request = createRequest(context) {
                precision(Precision.EXACT)
                scale(Scale.FILL)
            },
            size = Size(100, 100)
        ))
        assertTrue(service.isCacheValueValid(
            cached = createBitmap(width = 100, height = 100),
            isSampled = true,
            request = createRequest(context) {
                precision(Precision.EXACT)
                scale(Scale.FIT)
            },
            size = Size(100, 100)
        ))
        assertTrue(service.isCacheValueValid(
            cached = createBitmap(width = 400, height = 200),
            isSampled = true,
            request = createRequest(context) {
                precision(Precision.EXACT)
                scale(Scale.FILL)
            },
            size = Size(400, 200)
        ))
        assertFalse(service.isCacheValueValid(
            cached = createBitmap(width = 200, height = 400),
            isSampled = true,
            request = createRequest(context) {
                precision(Precision.EXACT)
                scale(Scale.FIT)
            },
            size = Size(400, 800)
        ))
    }

    @Test
    fun `isCacheValueValid - one pixel off`() {
        val service = newService()
        assertTrue(service.isCacheValueValid(
            cached = createBitmap(width = 244, height = 600),
            isSampled = true,
            request = createRequest(context) {
                precision(Precision.EXACT)
                scale(Scale.FIT)
            },
            size = Size(245, 600)
        ))
        assertTrue(service.isCacheValueValid(
            cached = createBitmap(width = 244, height = 600),
            isSampled = true,
            request = createRequest(context) {
                precision(Precision.INEXACT)
                scale(Scale.FIT)
            },
            size = Size(245, 600)
        ))
        assertFalse(service.isCacheValueValid(
            cached = createBitmap(width = 243, height = 595),
            isSampled = true,
            request = createRequest(context) {
                precision(Precision.EXACT)
                scale(Scale.FIT)
            },
            size = Size(245, 600)
        ))
        assertFalse(service.isCacheValueValid(
            cached = createBitmap(width = 243, height = 595),
            isSampled = true,
            request = createRequest(context) {
                precision(Precision.INEXACT)
                scale(Scale.FIT)
            },
            size = Size(245, 600)
        ))
        // Regression test: https://github.com/coil-kt/coil/issues/817
        assertTrue(service.isCacheValueValid(
            cached = createBitmap(width = 175, height = 117),
            isSampled = true,
            request = createRequest(context) {
                precision(Precision.INEXACT)
                scale(Scale.FIT)
            },
            size = Size(176, 176)
        ))
    }

    @Test
    fun `isCacheValueValid - transformation that reduces size of output bitmap`() {
        val service = newService()
        val key = newMemoryCacheKey(
            key = "key",
            transformations = listOf(CircleCropTransformation()),
            size = Size(1000, 500) // The size of the previous request.
        )
        val value = MemoryCache.Value(
            bitmap = createBitmap(width = 200, height = 200), // The small cached bitmap.
            extras = mapOf(EXTRA_IS_SAMPLED to true)
        )
        val request = createRequest(context)

        assertFalse(service.isCacheValueValid(
            cacheKey = key,
            cacheValue = value,
            request = request.newBuilder().precision(Precision.INEXACT).scale(Scale.FIT).build(),
            size = Size(650, 400)
        ))

        assertTrue(service.isCacheValueValid(
            cacheKey = key,
            cacheValue = value,
            request = request.newBuilder().precision(Precision.EXACT).scale(Scale.FIT).build(),
            size = Size(1000, 500)
        ))

        assertFalse(service.isCacheValueValid(
            cacheKey = key,
            cacheValue = value,
            request = request.newBuilder().precision(Precision.INEXACT).scale(Scale.FIT).build(),
            size = Size(1500, 1000)
        ))

        assertFalse(service.isCacheValueValid(
            cacheKey = key,
            cacheValue = value,
            request = request.newBuilder().precision(Precision.EXACT).scale(Scale.FIT).build(),
            size = Size(800, 500)
        ))
    }

    @Test
    fun `isCacheValueValid - Size_ORIGINAL`() {
        val service = newService()

        assertFalse(service.isCacheValueValid(
            cached = createBitmap(width = 200, height = 400),
            isSampled = true,
            request = createRequest(context),
            size = Size.ORIGINAL
        ))
        assertTrue(service.isCacheValueValid(
            cached = createBitmap(width = 200, height = 400),
            isSampled = false,
            request = createRequest(context),
            size = Size.ORIGINAL
        ))
    }

    @Test
    fun `isCacheValueValid - Dimension_Original`() {
        val service = newService()
        val request = createRequest(context) {
            precision(Precision.INEXACT)
        }

        assertTrue(service.isCacheValueValid(
            request = request,
            cached = createBitmap(width = 400, height = 200),
            isSampled = true,
            size = Size(400, Dimension.Original)
        ))
        assertTrue(service.isCacheValueValid(
            request = request,
            cached = createBitmap(width = 400, height = 200),
            isSampled = true,
            size = Size(Dimension.Original, 200)
        ))
        assertFalse(service.isCacheValueValid(
            request = request,
            cached = createBitmap(width = 400, height = 200),
            isSampled = true,
            size = Size(450, Dimension.Original)
        ))
        assertFalse(service.isCacheValueValid(
            request = request,
            cached = createBitmap(width = 400, height = 200),
            isSampled = true,
            size = Size(Dimension.Original, 250)
        ))
        assertTrue(service.isCacheValueValid(
            request = request,
            cached = createBitmap(width = 400, height = 200),
            isSampled = false,
            size = Size(450, Dimension.Original)
        ))
        assertTrue(service.isCacheValueValid(
            request = request,
            cached = createBitmap(width = 400, height = 200),
            isSampled = false,
            size = Size(Dimension.Original, 250)
        ))
    }

    private fun MemoryCacheService.isCacheValueValid(
        request: ImageRequest,
        cached: Bitmap,
        isSampled: Boolean,
        size: Size
    ) = isCacheValueValid(
        request = request,
        cacheKey = MemoryCache.Key("key"),
        cacheValue = MemoryCache.Value(cached, mapOf(EXTRA_IS_SAMPLED to isSampled)),
        size = size
    )

    private fun createFakeTransformations(): List<Transformation> {
        return listOf(
            object : Transformation {
                override val cacheKey = "key1"
                override suspend fun transform(input: Bitmap, size: Size) = fail()
            },
            object : Transformation {
                override val cacheKey = "key2"
                override suspend fun transform(input: Bitmap, size: Size) = fail()
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

    private fun newService(key: String? = TEST_KEY): MemoryCacheService {
        val imageLoader = ImageLoader.Builder(context)
            .components {
                add(Keyer { _: Any, _ -> key })
            }
            .build()
        val systemCallbacks = SystemCallbacks(imageLoader as RealImageLoader, context, true)
        return MemoryCacheService(
            imageLoader = imageLoader,
            requestService = RequestService(imageLoader, systemCallbacks, null),
            logger = null
        )
    }

    private fun newMemoryCacheKey(
        key: String = TEST_KEY,
        transformations: List<Transformation> = emptyList(),
        size: Size = Size.ORIGINAL,
        parameters: Parameters = Parameters.EMPTY
    ): MemoryCache.Key {
        val extras = parameters.memoryCacheKeys().toMutableMap()
        if (transformations.isNotEmpty()) {
            transformations.forEachIndexedIndices { index, transformation ->
                extras[EXTRA_TRANSFORMATION_INDEX + index] = transformation.cacheKey
            }
            extras[EXTRA_TRANSFORMATION_SIZE] = size.toString()
        }
        return MemoryCache.Key(key, extras)
    }

    companion object {
        private const val TEST_KEY = "test_key"
    }
}
