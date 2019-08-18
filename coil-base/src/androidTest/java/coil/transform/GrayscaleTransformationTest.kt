package coil.transform

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.test.core.app.ApplicationProvider
import coil.bitmappool.BitmapPool
import coil.bitmappool.FakeBitmapPool
import coil.util.getPixels
import coil.util.sameAs
import kotlinx.coroutines.runBlocking
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
    private lateinit var grayscaleTransformation: GrayscaleTransformation

    @Before
    fun before() {
        pool = FakeBitmapPool()
        grayscaleTransformation = GrayscaleTransformation()
        normalBitmap = BitmapFactory.decodeStream(context.assets.open("normal.jpg"))
        normalGrayscaleBitmap = BitmapFactory.decodeStream(context.assets.open("normal_grayscale.jpg"))
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
}
