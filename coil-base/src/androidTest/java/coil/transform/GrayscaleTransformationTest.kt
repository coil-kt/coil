package coil.transform

import android.content.Context
import android.graphics.Bitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.test.core.app.ApplicationProvider
import coil.bitmappool.BitmapPool
import coil.bitmappool.FakeBitmapPool
import coil.decode.BitmapFactoryDecoder
import coil.size.PixelSize
import coil.util.createOptions
import coil.util.getPixels
import kotlinx.coroutines.runBlocking
import okio.buffer
import okio.source
import org.junit.Before
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GrayscaleTransformationTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private lateinit var pool: BitmapPool
    private lateinit var decoder: BitmapFactoryDecoder
    private lateinit var grayscaleTransformation: GrayscaleTransformation

    @Before
    fun before() {
        pool = FakeBitmapPool()
        decoder = BitmapFactoryDecoder(context)
        grayscaleTransformation = GrayscaleTransformation()
    }

    @Test
    fun withRGBBitmap_assertThatTransformToNewGrayscaleBitmap() {

        val coloredBitmap = getColoredBitmap()

        val grayscaleBitmap = runBlocking {
            grayscaleTransformation.transform(pool, coloredBitmap)
        }

        val (_, red, green, blue) = grayscaleBitmap.getPixels()

        assertTrue(red.contentEquals(green) && green.contentEquals(blue))
        assertFalse(grayscaleBitmap.sameAs(coloredBitmap))
    }

    private fun getColoredBitmap(): Bitmap {
        val source = context.assets.open("normal.jpg").source().buffer()
        val (drawable, _) = runBlocking {
            decoder.decode(
                pool = pool,
                source = source,
                size = PixelSize(100, 100),
                options = createOptions()
            )
        }
        return drawable.toBitmap()
    }
}
