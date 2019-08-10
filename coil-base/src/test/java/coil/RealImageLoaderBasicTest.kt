package coil

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import androidx.test.core.app.ApplicationProvider
import coil.size.PixelSize
import coil.util.createBitmap
import coil.util.createGetRequest
import coil.util.toDrawable
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Basic tests for [RealImageLoader] that don't touch Android's graphics pipeline ([BitmapFactory], [ImageDecoder], etc.).
 */
@RunWith(RobolectricTestRunner::class)
class RealImageLoaderBasicTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private lateinit var imageLoader: RealImageLoader

    @Before
    fun before() {
        imageLoader = ImageLoader(context) as RealImageLoader
    }

    @After
    fun after() {
        imageLoader.shutdown()
    }

    @Test
    fun `large enough cached drawable is valid`() {
        val request = createGetRequest()
        val cached = createBitmap().toDrawable(context)
        val isValid = imageLoader.isCachedDrawableValid(cached, true, PixelSize(100, 100), request)
        assertTrue(isValid)
    }

    @Test
    fun `too small cached drawable is invalid`() {
        val request = createGetRequest()
        val cached = createBitmap().toDrawable(context)
        val isValid = imageLoader.isCachedDrawableValid(cached, true, PixelSize(200, 200), request)
        assertFalse(isValid)
    }

    @Test
    fun `too small not sampled cached drawable is valid`() {
        val request = createGetRequest()
        val cached = createBitmap().toDrawable(context)
        val isValid = imageLoader.isCachedDrawableValid(cached, false, PixelSize(200, 200), request)
        assertTrue(isValid)
    }

    @Test
    fun `only requests with higher quality are valid`() {
        val request = createGetRequest()

        fun isBitmapConfigValid(config: Bitmap.Config): Boolean {
            val cached = createBitmap(config = config).toDrawable(context)
            return imageLoader.isCachedDrawableValid(cached, true, PixelSize(100, 100), request)
        }

        assertTrue(isBitmapConfigValid(Bitmap.Config.RGBA_F16))
        assertTrue(isBitmapConfigValid(Bitmap.Config.HARDWARE))
        assertTrue(isBitmapConfigValid(Bitmap.Config.ARGB_8888))
        assertFalse(isBitmapConfigValid(Bitmap.Config.RGB_565))
        assertFalse(isBitmapConfigValid(Bitmap.Config.ARGB_4444))
    }

    @Test
    fun `allowRgb565=true allows using RGB_565 bitmaps with ARGB_8888 bitmaps`() {
        val request = createGetRequest {
            allowRgb565(true)
        }
        val cached = createBitmap(config = Bitmap.Config.HARDWARE).toDrawable(context)
        val isValid = imageLoader.isCachedDrawableValid(cached, true, PixelSize(100, 100), request)
        assertTrue(isValid)
    }

    @Test
    fun `allowHardware=false prevents using cached hardware bitmap`() {
        val request = createGetRequest {
            allowHardware(false)
        }

        fun isBitmapConfigValid(config: Bitmap.Config): Boolean {
            val cached = createBitmap(config = config).toDrawable(context)
            return imageLoader.isCachedDrawableValid(cached, true, PixelSize(100, 100), request)
        }

        assertFalse(isBitmapConfigValid(Bitmap.Config.HARDWARE))
        assertTrue(isBitmapConfigValid(Bitmap.Config.ARGB_8888))
    }
}
