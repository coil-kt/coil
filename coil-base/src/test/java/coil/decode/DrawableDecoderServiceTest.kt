package coil.decode

import android.graphics.Bitmap
import android.graphics.drawable.VectorDrawable
import coil.bitmap.BitmapPool
import coil.size.PixelSize
import coil.size.Scale
import coil.util.size
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class DrawableDecoderServiceTest {

    private lateinit var service: DrawableDecoderService

    @Before
    fun before() {
        service = DrawableDecoderService(BitmapPool(0))
    }

    @Test
    fun `vector with hardware config is converted correctly`() {
        val size = PixelSize(200, 200)
        val input = object : VectorDrawable() {
            override fun getIntrinsicWidth() = 100
            override fun getIntrinsicHeight() = 100
        }
        val output = service.convert(
            drawable = input,
            config = Bitmap.Config.HARDWARE,
            size = size,
            scale = Scale.FIT,
            allowInexactSize = true
        )

        assertEquals(Bitmap.Config.ARGB_8888, output.config)
        assertEquals(size, output.size)
    }

    @Test
    fun `unimplemented intrinsic size does not crash`() {
        val size = PixelSize(200, 200)
        val input = object : VectorDrawable() {
            override fun getIntrinsicWidth() = -1
            override fun getIntrinsicHeight() = -1
        }
        val output = service.convert(
            drawable = input,
            size = size,
            config = Bitmap.Config.HARDWARE,
            scale = Scale.FIT,
            allowInexactSize = true
        )

        assertEquals(Bitmap.Config.ARGB_8888, output.config)
        assertEquals(size, output.size)
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
            config = Bitmap.Config.ARGB_8888,
            scale = Scale.FIT,
            allowInexactSize = true
        )

        assertEquals(Bitmap.Config.ARGB_8888, output.config)
        assertEquals(PixelSize(100, 200), output.size)
    }
}
