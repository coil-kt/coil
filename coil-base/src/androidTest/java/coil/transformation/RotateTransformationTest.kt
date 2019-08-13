package coil.transformation

import android.graphics.Bitmap
import coil.bitmappool.BitmapPool
import coil.bitmappool.FakeBitmapPool
import coil.transform.RotateTransformation
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class RotateTransformationTest {

    private lateinit var pool: BitmapPool
    private lateinit var outputBitmap: Bitmap

    @Before
    fun before() {
        pool = FakeBitmapPool()
    }

    @Test
    fun with90RotationAngleShouldCreateBitmapChangingWidthAndHeight() {

        val width = 100
        val height = 200
        val expectedWidth = 200
        val expectedHeight = 100

        val inputBitmap = pool.get(width, height, Bitmap.Config.ARGB_8888)

        val rotateTransformation = RotateTransformation(90f)
        runBlocking {
            outputBitmap = rotateTransformation.transform(pool, inputBitmap)
        }
        assertEquals(expectedWidth, outputBitmap.width)
        assertEquals(expectedHeight, outputBitmap.height)
    }
}
