package coil3.memory

import android.graphics.Bitmap
import coil3.EventListener
import coil3.ImageLoader
import coil3.RealImageLoader
import coil3.asCoilImage
import coil3.key.Keyer
import coil3.memory.MemoryCacheService.Companion.EXTRA_IS_SAMPLED
import coil3.memory.MemoryCacheService.Companion.EXTRA_TRANSFORMATION_INDEX
import coil3.memory.MemoryCacheService.Companion.EXTRA_TRANSFORMATION_SIZE
import coil3.request.ImageRequest
import coil3.request.Options
import coil3.request.RequestService
import coil3.request.allowHardware
import coil3.request.transformations
import coil3.size.Dimension
import coil3.size.Precision
import coil3.size.Scale
import coil3.size.Size
import coil3.test.utils.RobolectricTest
import coil3.test.utils.context
import coil3.transform.CircleCropTransformation
import coil3.transform.Transformation
import coil3.util.SystemCallbacks
import coil3.util.createBitmap
import coil3.util.createRequest
import coil3.util.forEachIndexedIndices
import coil3.util.toDrawable
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail
import org.junit.Test

class MemoryCacheServiceTest : RobolectricTest() {

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
        val memoryCacheKeyExtras = createFakeMemoryCacheKeyExtras()
        val request = createRequest(context) {
            memoryCacheKeyExtras(memoryCacheKeyExtras)
        }
        val options = Options(context, size = Size.ORIGINAL)
        val actual = service.newCacheKey(request, Unit, options, EventListener.NONE)

        assertEquals(newMemoryCacheKey(memoryCacheKeyExtras = memoryCacheKeyExtras), actual)
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

        val expected = newMemoryCacheKey(
            transformations = transformations,
            size = size,
        )
        assertEquals(expected, actual)
    }

    @Test
    fun `newCacheKey - complex key`() {
        val service = newService(key = TEST_KEY)
        val memoryCacheKeyExtras = createFakeMemoryCacheKeyExtras()
        val transformations = createFakeTransformations()
        val request = createRequest(context) {
            memoryCacheKeyExtras(memoryCacheKeyExtras)
            transformations(transformations)
        }
        val options = Options(context, size = Size.ORIGINAL)
        val actual = service.newCacheKey(request, Unit, options, EventListener.NONE)

        val expected = newMemoryCacheKey(
            transformations = transformations,
            memoryCacheKeyExtras = memoryCacheKeyExtras,
        )
        assertEquals(expected, actual)
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
            image = createBitmap(width = 200, height = 200)
                .toDrawable(context).asCoilImage(), // The small cached bitmap.
            extras = mapOf(EXTRA_IS_SAMPLED to true)
        )
        val request = createRequest(context)

        assertFalse(service.isCacheValueValid(
            cacheKey = key,
            cacheValue = value,
            request = request.newBuilder().precision(Precision.INEXACT).build(),
            size = Size(650, 400),
            scale = Scale.FIT
        ))

        assertTrue(service.isCacheValueValid(
            cacheKey = key,
            cacheValue = value,
            request = request.newBuilder().precision(Precision.EXACT).build(),
            size = Size(1000, 500),
            scale = Scale.FIT
        ))

        assertFalse(service.isCacheValueValid(
            cacheKey = key,
            cacheValue = value,
            request = request.newBuilder().precision(Precision.INEXACT).build(),
            size = Size(1500, 1000),
            scale = Scale.FIT
        ))

        assertFalse(service.isCacheValueValid(
            cacheKey = key,
            cacheValue = value,
            request = request.newBuilder().precision(Precision.EXACT).build(),
            size = Size(800, 500),
            scale = Scale.FIT
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
            size = Size(400, Dimension.Undefined)
        ))
        assertTrue(service.isCacheValueValid(
            request = request,
            cached = createBitmap(width = 400, height = 200),
            isSampled = true,
            size = Size(Dimension.Undefined, 200)
        ))
        assertFalse(service.isCacheValueValid(
            request = request,
            cached = createBitmap(width = 400, height = 200),
            isSampled = true,
            size = Size(450, Dimension.Undefined)
        ))
        assertFalse(service.isCacheValueValid(
            request = request,
            cached = createBitmap(width = 400, height = 200),
            isSampled = true,
            size = Size(Dimension.Undefined, 250)
        ))
        assertTrue(service.isCacheValueValid(
            request = request,
            cached = createBitmap(width = 400, height = 200),
            isSampled = false,
            size = Size(450, Dimension.Undefined)
        ))
        assertTrue(service.isCacheValueValid(
            request = request,
            cached = createBitmap(width = 400, height = 200),
            isSampled = false,
            size = Size(Dimension.Undefined, 250)
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
        cacheValue = MemoryCache.Value(
            image = cached.toDrawable(context).asCoilImage(),
            extras = mapOf(EXTRA_IS_SAMPLED to isSampled),
        ),
        size = size,
        scale = request.scale
    )

    private fun createFakeTransformations(): List<Transformation> {
        return listOf(
            object : Transformation() {
                override val cacheKey = "key1"
                override suspend fun transform(input: Bitmap, size: Size) = fail()
            },
            object : Transformation() {
                override val cacheKey = "key2"
                override suspend fun transform(input: Bitmap, size: Size) = fail()
            }
        )
    }

    private fun createFakeMemoryCacheKeyExtras(): Map<String, String> {
        return buildMap {
            put("key1", "cached1")
            put("key2", "cached2")
        }
    }

    private fun newService(key: String? = TEST_KEY): MemoryCacheService {
        val imageLoader = ImageLoader.Builder(context)
            .components {
                add(Keyer { _: Any, _ -> key })
            }
            .build() as RealImageLoader
        val systemCallbacks = SystemCallbacks(imageLoader)
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
        memoryCacheKeyExtras: Map<String, String> = emptyMap(),
    ): MemoryCache.Key {
        val extras = memoryCacheKeyExtras.toMutableMap()
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
