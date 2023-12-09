package coil3.transform

import coil3.size.Size
import coil3.test.utils.assertIsSimilarTo
import coil3.test.utils.context
import coil3.test.utils.decodeBitmapAsset
import kotlinx.coroutines.test.runTest
import org.junit.Test

class CircleCropTransformationTest {

    private val transformation = CircleCropTransformation()

    @Test
    fun basic() = runTest {
        val input = context.decodeBitmapAsset("normal_small.jpg")
        val expected = context.decodeBitmapAsset("normal_small_circle.png")

        val actual = transformation.transform(input, Size.ORIGINAL)

        actual.assertIsSimilarTo(expected)
    }
}
