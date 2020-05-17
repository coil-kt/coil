package coil.memory

import android.content.Context
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import coil.DefaultRequestOptions
import coil.annotation.ExperimentalCoilApi
import coil.memory.StrongMemoryCache.Key
import coil.memory.StrongMemoryCache.Value
import coil.request.ImageRequest
import coil.size.PixelSize
import coil.size.Precision
import coil.size.Scale
import coil.size.Size
import coil.size.SizeResolver
import coil.transform.CircleCropTransformation
import coil.util.createBitmap
import coil.util.createRequest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertFalse
import kotlin.test.assertTrue

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
        service = MemoryCacheService(requestService, null)
    }

    @Test
    fun `isCachedValueValid - fill`() {
        val request = createRequest(context) {
            size(100, 100)
            precision(Precision.INEXACT)
        }
        val cached = createBitmap()
        assertFalse(service.isCachedValueValid(
            cached = cached,
            isSampled = true,
            request = request,
            size = PixelSize(200, 200),
            scale = Scale.FILL
        ))
        assertFalse(service.isCachedValueValid(
            cached = cached,
            isSampled = true,
            request = request,
            size = PixelSize(150, 50),
            scale = Scale.FILL
        ))
        assertTrue(service.isCachedValueValid(
            cached = cached,
            isSampled = true,
            request = request,
            size = PixelSize(100, 100),
            scale = Scale.FILL
        ))
        assertTrue(service.isCachedValueValid(
            cached = cached,
            isSampled = true,
            request = request,
            size = PixelSize(50, 100),
            scale = Scale.FILL
        ))
        assertTrue(service.isCachedValueValid(
            cached = cached,
            isSampled = true,
            request = request,
            size = PixelSize(50, 50),
            scale = Scale.FILL
        ))
        assertTrue(service.isCachedValueValid(
            cached = createBitmap(width = 400, height = 200),
            isSampled = true,
            request = request,
            size = PixelSize(400, 200),
            scale = Scale.FILL
        ))
    }

    @Test
    fun `isCachedValueValid - fit`() {
        val request = createRequest(context) {
            size(100, 100)
            precision(Precision.INEXACT)
        }
        val cached = createBitmap()
        assertFalse(service.isCachedValueValid(
            cached = cached,
            isSampled = true,
            request = request,
            size = PixelSize(200, 200),
            scale = Scale.FIT
        ))
        assertTrue(service.isCachedValueValid(
            cached = cached,
            isSampled = true,
            request = request,
            size = PixelSize(150, 50),
            scale = Scale.FIT
        ))
        assertTrue(service.isCachedValueValid(
            cached = cached,
            isSampled = true,
            request = request,
            size = PixelSize(100, 100),
            scale = Scale.FIT
        ))
        assertTrue(service.isCachedValueValid(
            cached = cached,
            isSampled = true,
            request = request,
            size = PixelSize(50, 100),
            scale = Scale.FIT
        ))
        assertTrue(service.isCachedValueValid(
            cached = cached,
            isSampled = true,
            request = request,
            size = PixelSize(50, 50),
            scale = Scale.FIT
        ))
        assertFalse(service.isCachedValueValid(
            cached = createBitmap(width = 200, height = 400),
            isSampled = true,
            request = request,
            size = PixelSize(400, 800),
            scale = Scale.FIT
        ))
    }

    @Test
    fun `isCachedValueValid - small not sampled cached drawable is valid`() {
        val request = createRequest(context) {
            precision(Precision.INEXACT)
        }
        val cached = createBitmap()
        val isValid = service.isCachedValueValid(
            cached = cached,
            isSampled = false,
            request = request,
            size = PixelSize(200, 200),
            scale = Scale.FILL
        )
        assertTrue(isValid)
    }

    @Test
    fun `isCachedValueValid - allowHardware=false prevents using cached hardware bitmap`() {
        val request = createRequest(context) {
            allowHardware(false)
        }

        fun isBitmapConfigValid(config: Bitmap.Config): Boolean {
            val cached = createBitmap(config = config)
            return service.isCachedValueValid(
                cached = cached,
                isSampled = true,
                request = request,
                size = PixelSize(100, 100),
                scale = Scale.FILL
            )
        }

        assertFalse(isBitmapConfigValid(Bitmap.Config.HARDWARE))
        assertTrue(isBitmapConfigValid(Bitmap.Config.RGBA_F16))
        assertTrue(isBitmapConfigValid(Bitmap.Config.ARGB_8888))
        assertTrue(isBitmapConfigValid(Bitmap.Config.RGB_565))
    }

    @Test
    fun `isCachedValueValid - exact precision`() {
        val request = createRequest(context) {
            precision(Precision.EXACT)
        }
        assertFalse(service.isCachedValueValid(
            cached = createBitmap(width = 100, height = 100),
            isSampled = true,
            request = request,
            size = PixelSize(50, 50),
            scale = Scale.FILL
        ))
        assertFalse(service.isCachedValueValid(
            cached = createBitmap(width = 100, height = 100),
            isSampled = true,
            request = request,
            size = PixelSize(50, 50),
            scale = Scale.FIT
        ))
        assertTrue(service.isCachedValueValid(
            cached = createBitmap(width = 100, height = 100),
            isSampled = true,
            request = request,
            size = PixelSize(100, 50),
            scale = Scale.FILL
        ))
        assertFalse(service.isCachedValueValid(
            cached = createBitmap(width = 100, height = 100),
            isSampled = true,
            request = request,
            size = PixelSize(100, 50),
            scale = Scale.FIT
        ))
        assertTrue(service.isCachedValueValid(
            cached = createBitmap(width = 100, height = 100),
            isSampled = true,
            request = request,
            size = PixelSize(100, 100),
            scale = Scale.FILL
        ))
        assertTrue(service.isCachedValueValid(
            cached = createBitmap(width = 100, height = 100),
            isSampled = true,
            request = request,
            size = PixelSize(100, 100),
            scale = Scale.FIT
        ))
        assertTrue(service.isCachedValueValid(
            cached = createBitmap(width = 400, height = 200),
            isSampled = true,
            request = request,
            size = PixelSize(400, 200),
            scale = Scale.FILL
        ))
        assertFalse(service.isCachedValueValid(
            cached = createBitmap(width = 200, height = 400),
            isSampled = true,
            request = request,
            size = PixelSize(400, 800),
            scale = Scale.FIT
        ))
    }

    @Test
    fun `isCachedValueValid - one pixel off`() {
        assertTrue(service.isCachedValueValid(
            cached = createBitmap(width = 244, height = 600),
            isSampled = true,
            request = createRequest(context) {
                precision(Precision.EXACT)
            },
            size = PixelSize(245, 600),
            scale = Scale.FIT
        ))
        assertTrue(service.isCachedValueValid(
            cached = createBitmap(width = 244, height = 600),
            isSampled = true,
            request = createRequest(context) {
                precision(Precision.INEXACT)
            },
            size = PixelSize(245, 600),
            scale = Scale.FIT
        ))
        assertFalse(service.isCachedValueValid(
            cached = createBitmap(width = 243, height = 599),
            isSampled = true,
            request = createRequest(context) {
                precision(Precision.EXACT)
            },
            size = PixelSize(245, 600),
            scale = Scale.FIT
        ))
        assertFalse(service.isCachedValueValid(
            cached = createBitmap(width = 243, height = 599),
            isSampled = true,
            request = createRequest(context) {
                precision(Precision.INEXACT)
            },
            size = PixelSize(245, 600),
            scale = Scale.FIT
        ))
    }

    @Test
    fun `isCachedValueValid - transformation that reduces size of output bitmap`() {
        val transformations = listOf(CircleCropTransformation())
        val cachedSize = PixelSize(1000, 500) // The size of the previous request.
        val key = Key("key", transformations, cachedSize)
        val value = object : Value {
            override val bitmap = createBitmap(width = 200, height = 200) // The small cached bitmap.
            override val isSampled = true
        }
        val request = createRequest(context)

        assertTrue(service.isCachedValueValid(
            key,
            value,
            request.newBuilder().precision(Precision.INEXACT).build(),
            PixelSize(650, 400),
            Scale.FIT
        ))

        assertTrue(service.isCachedValueValid(
            key,
            value,
            request.newBuilder().precision(Precision.EXACT).build(),
            PixelSize(1000, 500),
            Scale.FIT
        ))

        assertFalse(service.isCachedValueValid(
            key,
            value,
            request.newBuilder().precision(Precision.INEXACT).build(),
            PixelSize(1500, 1000),
            Scale.FIT
        ))

        assertFalse(service.isCachedValueValid(
            key,
            value,
            request.newBuilder().precision(Precision.EXACT).build(),
            PixelSize(800, 500),
            Scale.FIT
        ))
    }

    private fun MemoryCacheService.isCachedValueValid(
        cached: Bitmap,
        isSampled: Boolean,
        request: ImageRequest,
        size: Size,
        scale: Scale
    ): Boolean {
        val key = Key("key")
        val value = object : Value {
            override val bitmap = cached
            override val isSampled = isSampled
        }
        return isCachedValueValid(key, value, request, size, scale)
    }

    /** Convenience function to avoid having to specify the [SizeResolver] explicitly. */
    private fun MemoryCacheService.isCachedValueValid(
        cacheKey: Key?,
        cacheValue: Value,
        request: ImageRequest,
        size: Size,
        scale: Scale
    ): Boolean = isCachedValueValid(cacheKey, cacheValue, request, SizeResolver(size), size, scale)
}
