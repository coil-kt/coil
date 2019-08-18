package coil.transform

import android.content.Context
import android.graphics.BitmapFactory
import androidx.test.core.app.ApplicationProvider
import coil.bitmappool.BitmapPool
import coil.bitmappool.FakeBitmapPool
import coil.util.getPixels
import coil.util.isSimilarTo
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import kotlin.test.assertTrue

class GrayscaleTransformationTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private lateinit var pool: BitmapPool
    private lateinit var grayscaleTransformation: GrayscaleTransformation

    @Before
    fun before() {
        pool = FakeBitmapPool()
        grayscaleTransformation = GrayscaleTransformation()
    }

    @Test
    fun withRGBBitmap_assertThatTransformToNewGrayscaleBitmap() {
        val normalBitmap = BitmapFactory.decodeStream(context.assets.open("normal.jpg"))
        val normalGrayscaleBitmap = BitmapFactory.decodeStream(context.assets.open("normal_grayscale.jpg"))

        val grayscaleBitmap = runBlocking {
            grayscaleTransformation.transform(pool, normalBitmap)
        }

        val (_, red, green, blue) = grayscaleBitmap.getPixels()

        assertTrue(red.contentEquals(green) && green.contentEquals(blue))
        assertTrue(grayscaleBitmap.isSimilarTo(normalGrayscaleBitmap))
    }
}
