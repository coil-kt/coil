package coil.transform

import android.content.Context
import android.graphics.BitmapFactory
import androidx.test.core.app.ApplicationProvider
import coil.bitmappool.BitmapPool
import coil.size.OriginalSize
import coil.util.getPixels
import coil.util.isSimilarTo
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import kotlin.test.assertTrue

class GrayscaleTransformationTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private lateinit var pool: BitmapPool
    private lateinit var transformation: GrayscaleTransformation

    @Before
    fun before() {
        pool = BitmapPool(0)
        transformation = GrayscaleTransformation()
    }

    @Test
    fun basic() {
        val input = BitmapFactory.decodeStream(context.assets.open("normal.jpg"))
        val expected = BitmapFactory.decodeStream(context.assets.open("normal_grayscale.jpg"))

        val actual = runBlocking {
            transformation.transform(pool, input, OriginalSize)
        }

        val (_, red, green, blue) = actual.getPixels()

        assertTrue(red.contentEquals(green) && green.contentEquals(blue))
        assertTrue(actual.isSimilarTo(expected))
    }
}
