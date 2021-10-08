package coil.transform

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import coil.size.OriginalSize
import coil.util.assertIsSimilarTo
import coil.util.decodeBitmapAsset
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test

class CircleCropTransformationTest {

    private lateinit var context: Context
    private lateinit var transformation: CircleCropTransformation

    @Before
    fun before() {
        context = ApplicationProvider.getApplicationContext()
        transformation = CircleCropTransformation()
    }

    @Test
    fun basic() {
        val input = context.decodeBitmapAsset("normal_small.jpg")
        val expected = context.decodeBitmapAsset("normal_small_circle.png")

        val actual = runBlocking {
            transformation.transform(input, OriginalSize)
        }

        actual.assertIsSimilarTo(expected)
    }
}
