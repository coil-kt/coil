package coil.decode

import android.graphics.Bitmap
import android.graphics.drawable.VectorDrawable
import coil.bitmappool.RealBitmapPool
import coil.size.PixelSize
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class DrawableDecoderServiceTest {

    private lateinit var service: DrawableDecoderService

    @Before
    fun before() {
        service = DrawableDecoderService(RealBitmapPool(0))
    }

    @Test
    fun `vector with hardware config is converted correctly`() {
        val input = object : VectorDrawable() {
            override fun getIntrinsicWidth() = 100
            override fun getIntrinsicHeight() = 100
        }
        val output = service.convert(
            drawable = input,
            size = PixelSize(200, 200),
            config = Bitmap.Config.HARDWARE
        )

        assertEquals(Bitmap.Config.ARGB_8888, output.config)
        assertTrue(output.run { width == 200 && height == 200 })
    }

    @Test
    fun `unimplemented intrinsic size does not crash`() {
        val input = object : VectorDrawable() {
            override fun getIntrinsicWidth() = -1
            override fun getIntrinsicHeight() = -1
        }
        val output = service.convert(
            drawable = input,
            size = PixelSize(200, 200),
            config = Bitmap.Config.HARDWARE
        )

        assertEquals(Bitmap.Config.ARGB_8888, output.config)
        assertTrue(output.run { width == 200 && height == 200 })
    }

    @Test
    fun `aspect ratio is preserved`() {
        val input = object : VectorDrawable() {
            override fun getIntrinsicWidth() = 125
            override fun getIntrinsicHeight() = 250
        }
        val output = service.convert(
            drawable = input,
            size = PixelSize(200, 200),
            config = Bitmap.Config.ARGB_8888
        )

        assertEquals(Bitmap.Config.ARGB_8888, output.config)
        assertTrue(output.width == 100 && output.height == 200)
    }
}
