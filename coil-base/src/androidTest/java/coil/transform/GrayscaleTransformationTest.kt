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
import coil.util.sameAs
import kotlinx.coroutines.runBlocking
import okio.buffer
import okio.source
import org.junit.Before
import org.junit.Test
import kotlin.test.assertTrue

class GrayscaleTransformationTest {

    companion object {
        const val THRESHOLD = 0.99
    }

    private val context: Context = ApplicationProvider.getApplicationContext()

    private lateinit var normalBitmap: Bitmap
    private lateinit var normalGrayscaleBitmap: Bitmap
    private lateinit var pool: BitmapPool
    private lateinit var decoder: BitmapFactoryDecoder
    private lateinit var grayscaleTransformation: GrayscaleTransformation

    @Before
    fun before() {
        pool = FakeBitmapPool()
        decoder = BitmapFactoryDecoder(context)
        grayscaleTransformation = GrayscaleTransformation()
        normalBitmap = getBitmapFromAssets("normal.jpg")
        normalGrayscaleBitmap = getBitmapFromAssets("normal_grayscale.jpg")
    }

    @Test
    fun withRGBBitmap_assertThatTransformToNewGrayscaleBitmap() {

        val grayscaleBitmap = runBlocking {
            grayscaleTransformation.transform(pool, normalBitmap)
        }

        val (_, red, green, blue) = grayscaleBitmap.getPixels()

        assertTrue(red.contentEquals(green) && green.contentEquals(blue))
        assertTrue(grayscaleBitmap.sameAs(normalGrayscaleBitmap, THRESHOLD))
    }

    private fun getBitmapFromAssets(
        fileName: String
    ): Bitmap {
        val source = context.assets.open(fileName).source().buffer()
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
