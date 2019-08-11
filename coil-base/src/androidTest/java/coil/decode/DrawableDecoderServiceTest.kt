package coil.decode

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import androidx.test.core.app.ApplicationProvider
import coil.bitmappool.RealBitmapPool
import coil.resource.test.R
import coil.size.PixelSize
import coil.util.getDrawableCompat
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DrawableDecoderServiceTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private lateinit var service: DrawableDecoderService

    @Before
    fun before() {
        service = DrawableDecoderService(context, RealBitmapPool(0))
    }

    /**
     * NOTE: This test will fail on pre-Lollipop since we don't import AppCompatResources in the base library.
     */
    @Test
    fun vectorIsConvertedCorrectly() {
        val output = service.convertIfNecessary(
            drawable = context.getDrawableCompat(R.drawable.ic_android),
            size = PixelSize(200, 200),
            config = Bitmap.Config.ARGB_8888
        )

        assertTrue(output is BitmapDrawable)
        assertTrue(output.bitmap.run { width == 200 && height == 200 })
    }

    @Test
    fun colorIsNotConverted() {
        val input = ColorDrawable(Color.BLACK)
        val output = service.convertIfNecessary(
            drawable = input,
            size = PixelSize(200, 200),
            config = Bitmap.Config.ARGB_8888
        )

        assertEquals(input, output)
    }
}
