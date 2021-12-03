package coil.transform

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import coil.size.Size
import coil.util.assertIsSimilarTo
import coil.util.decodeBitmapAsset
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
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
