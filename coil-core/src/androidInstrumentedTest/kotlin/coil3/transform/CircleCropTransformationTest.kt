package coil3.transform

import coil3.size.Size
import coil3.test.assertIsSimilarTo
import coil3.test.context
import coil3.test.decodeBitmapAsset
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
