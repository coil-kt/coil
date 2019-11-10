package coil.decode

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.VectorDrawable
import androidx.test.core.app.ApplicationProvider
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

    private val context: Context = ApplicationProvider.getApplicationContext()

    private lateinit var service: DrawableDecoderService

    @Before
    fun before() {
        service = DrawableDecoderService(context, RealBitmapPool(0))
    }

    @Test
    fun `vector with hardware config is converted correctly`() {
        val input = VectorDrawable()
        val output = service.convertIfNecessary(
            drawable = input,
            size = PixelSize(200, 200),
            config = Bitmap.Config.HARDWARE
        )

        assertTrue(output is BitmapDrawable)
        assertEquals(Bitmap.Config.ARGB_8888, output.bitmap.config)
        assertTrue(output.bitmap.run { width == 200 && height == 200 })
    }

    @Test
    fun `aspect ratio is preserved`() {
        val input = object : ColorDrawable() {
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

    @Test
    fun `color is not converted`() {
        val input = ColorDrawable(Color.BLACK)
        val output = service.convertIfNecessary(
            drawable = input,
            size = PixelSize(200, 200),
            config = Bitmap.Config.ARGB_8888
        )

        assertEquals(input, output)
    }
}
