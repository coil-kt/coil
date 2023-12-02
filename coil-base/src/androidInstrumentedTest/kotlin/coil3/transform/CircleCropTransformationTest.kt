package coil3.transform

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import coil3.size.Size
import coil3.util.assertIsSimilarTo
import coil3.util.decodeBitmapAsset
import kotlinx.coroutines.test.runTest
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
    fun basic() = runTest {
        val input = context.decodeBitmapAsset("normal_small.jpg")
        val expected = context.decodeBitmapAsset("normal_small_circle.png")

        val actual = transformation.transform(input, Size.ORIGINAL)

        actual.assertIsSimilarTo(expected)
    }
}
