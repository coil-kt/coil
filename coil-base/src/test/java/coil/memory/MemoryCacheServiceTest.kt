package coil.memory

import android.content.Context
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import coil.DefaultRequestOptions
import coil.annotation.ExperimentalCoilApi
import coil.memory.MemoryCache.Key
import coil.memory.MemoryCache.Value
import coil.request.Request
import coil.size.PixelSize
import coil.size.Precision
import coil.size.Scale
import coil.size.Size
import coil.size.SizeResolver
import coil.transform.CircleCropTransformation
import coil.util.createBitmap
import coil.util.createGetRequest
import coil.util.createLoadRequest
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
        service = MemoryCacheService(requestService, defaults, null)
    }

    @Test
    fun `isCachedDrawableValid - fill`() {
        val request = createGetRequest {
            size(100, 100)
            precision(Precision.INEXACT)
        }
        val cached = createBitmap()
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
            cached = createBitmap(width = 400, height = 200),
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
        val cached = createBitmap()
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
            cached = createBitmap(width = 200, height = 400),
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
        val cached = createBitmap()
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
            val cached = createBitmap(config = config)
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
                cached = createBitmap(config = cachedConfig),
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
            val cached = createBitmap(config = config)
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
            cached = createBitmap(width = 100, height = 100),
            isSampled = true,
            request = request,
            size = PixelSize(50, 50),
            scale = Scale.FILL
        ))
        assertFalse(service.isCachedDrawableValid(
            cached = createBitmap(width = 100, height = 100),
            isSampled = true,
            request = request,
            size = PixelSize(50, 50),
            scale = Scale.FIT
        ))
        assertTrue(service.isCachedDrawableValid(
            cached = createBitmap(width = 100, height = 100),
            isSampled = true,
            request = request,
            size = PixelSize(100, 50),
            scale = Scale.FILL
        ))
        assertFalse(service.isCachedDrawableValid(
            cached = createBitmap(width = 100, height = 100),
            isSampled = true,
            request = request,
            size = PixelSize(100, 50),
            scale = Scale.FIT
        ))
        assertTrue(service.isCachedDrawableValid(
            cached = createBitmap(width = 100, height = 100),
            isSampled = true,
            request = request,
            size = PixelSize(100, 100),
            scale = Scale.FILL
        ))
        assertTrue(service.isCachedDrawableValid(
            cached = createBitmap(width = 100, height = 100),
            isSampled = true,
            request = request,
            size = PixelSize(100, 100),
            scale = Scale.FIT
        ))
        assertTrue(service.isCachedDrawableValid(
            cached = createBitmap(width = 400, height = 200),
            isSampled = true,
            request = request,
            size = PixelSize(400, 200),
            scale = Scale.FILL
        ))
        assertFalse(service.isCachedDrawableValid(
            cached = createBitmap(width = 200, height = 400),
            isSampled = true,
            request = request,
            size = PixelSize(400, 800),
            scale = Scale.FIT
        ))
    }

    @Test
    fun `isCachedDrawableValid - transformation that reduces size of output bitmap`() {
        val transformations = listOf(CircleCropTransformation())
        val cachedSize = PixelSize(1000, 500) // The size of the previous request.
        val key = Key("key", transformations, cachedSize)
        val value = object : Value {
            override val bitmap = createBitmap(width = 200, height = 200) // The small cached bitmap.
            override val isSampled = true
        }
        val request = createLoadRequest(context)

        assertTrue(service.isCachedDrawableValid(
            key,
            value,
            request.newBuilder().precision(Precision.INEXACT).build(),
            PixelSize(650, 400),
            Scale.FIT
        ))

        assertTrue(service.isCachedDrawableValid(
            key,
            value,
            request.newBuilder().precision(Precision.EXACT).build(),
            PixelSize(1000, 500),
            Scale.FIT
        ))

        assertFalse(service.isCachedDrawableValid(
            key,
            value,
            request.newBuilder().precision(Precision.INEXACT).build(),
            PixelSize(1500, 1000),
            Scale.FIT
        ))

        assertFalse(service.isCachedDrawableValid(
            key,
            value,
            request.newBuilder().precision(Precision.EXACT).build(),
            PixelSize(800, 500),
            Scale.FIT
        ))
    }

    private fun MemoryCacheService.isCachedDrawableValid(
        cached: Bitmap,
        isSampled: Boolean,
        request: Request,
        size: Size,
        scale: Scale
    ): Boolean {
        val key = Key("key")
        val value = object : Value {
            override val bitmap = cached
            override val isSampled = isSampled
        }
        return isCachedDrawableValid(key, value, request, size, scale)
    }

    /** Convenience function to avoid having to specify the [SizeResolver] explicitly. */
    private fun MemoryCacheService.isCachedDrawableValid(
        cacheKey: Key?,
        cacheValue: Value,
        request: Request,
        size: Size,
        scale: Scale
    ): Boolean = isCachedDrawableValid(cacheKey, cacheValue, request, SizeResolver(size), size, scale)
}
