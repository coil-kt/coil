package coil.transform

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.test.core.app.ApplicationProvider
import coil.bitmap.BitmapPool
import coil.size.OriginalSize
import coil.util.decodeBitmapAsset
import coil.util.getPixels
import coil.util.isSimilarTo
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import kotlin.test.assertTrue

class GrayscaleTransformationTest {

    private lateinit var context: Context
    private lateinit var pool: BitmapPool
    private lateinit var transformation: GrayscaleTransformation

    @Before
    fun before() {
        context = ApplicationProvider.getApplicationContext()
        pool = BitmapPool(0)
        transformation = GrayscaleTransformation()
    }

    @Test
    fun basic() {
        val options = BitmapFactory.Options().apply {
            inPreferredConfig = Bitmap.Config.ARGB_8888
            inSampleSize = 2 // Downsample the image to avoid running out of memory on pre-API 21.
        }
        val input = context.decodeBitmapAsset("normal.jpg", options)
        val expected = context.decodeBitmapAsset("normal_grayscale.jpg", options)

        val actual = runBlocking {
            transformation.transform(pool, input, OriginalSize)
        }

        val (_, red, green, blue) = actual.getPixels()

        assertTrue(red.contentEquals(green) && green.contentEquals(blue))
        assertTrue(actual.isSimilarTo(expected))
    }
}
