package coil.transform

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import coil.bitmap.BitmapPool
import coil.size.OriginalSize
import coil.util.decodeBitmapAsset
import coil.util.isSimilarTo
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import kotlin.test.assertTrue

class CircleCropTransformationTest {

    private lateinit var context: Context
    private lateinit var pool: BitmapPool
    private lateinit var transformation: CircleCropTransformation

    @Before
    fun before() {
        context = ApplicationProvider.getApplicationContext()
        pool = BitmapPool(0)
        transformation = CircleCropTransformation()
    }

    @Test
    fun basic() {
        val input = context.decodeBitmapAsset("normal_small.jpg")
        val expected = context.decodeBitmapAsset("normal_small_circle.png")

        val actual = runBlocking {
            transformation.transform(pool, input, OriginalSize)
        }

        assertTrue(actual.isSimilarTo(expected))
    }
}
