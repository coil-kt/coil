package coil3.transform

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import coil3.base.test.R
import coil3.size.Dimension
import coil3.size.Size
import coil3.util.assertIsSimilarTo
import coil3.util.decodeBitmapAsset
import coil3.util.size
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class RoundedCornerTransformationTest {

    private lateinit var context: Context
    private lateinit var transformation: RoundedCornersTransformation

    @Before
    fun before() {
        context = ApplicationProvider.getApplicationContext()
        transformation = RoundedCornersTransformation(20f)
    }

    @Test
    fun defined_size() = runTest {
        val input = context.decodeBitmapAsset("normal_small.jpg")
        val actual = transformation.transform(input, Size(100, 100))

        assertEquals(Size(100, 100), actual.size)
        actual.assertIsSimilarTo(R.drawable.rounded_corners_1)
    }

    @Test
    fun undefined_width() = runTest {
        val input = context.decodeBitmapAsset("normal_small.jpg")
        val actual = transformation.transform(input, Size(Dimension.Undefined, 100))

        assertEquals(Size(80, 100), actual.size)
        actual.assertIsSimilarTo(R.drawable.rounded_corners_2)
    }

    @Test
    fun undefined_height() = runTest {
        val input = context.decodeBitmapAsset("normal_small.jpg")
        val actual = transformation.transform(input, Size(100, Dimension.Undefined))

        assertEquals(Size(100, 125), actual.size)
        actual.assertIsSimilarTo(R.drawable.rounded_corners_3)
    }

    @Test
    fun undefined_size() = runTest {
        val input = context.decodeBitmapAsset("normal_small.jpg")
        val actual = transformation.transform(input, Size.ORIGINAL)

        assertEquals(Size(103, 129), actual.size)
        actual.assertIsSimilarTo(R.drawable.rounded_corners_4)
    }

    /** Regression test: https://github.com/coil-kt/coil/issues/1426 */
    @Test
    fun imageIsSmallerThanTarget() = runTest {
        val expectedSize = Size(200, 200)
        val input = context.decodeBitmapAsset("normal_small.jpg")
        val actual = transformation.transform(input, expectedSize)

        assertEquals(expectedSize, actual.size)
        actual.assertIsSimilarTo(R.drawable.rounded_corners_5)
    }

    @Test
    fun imageIsLargerThanTarget() = runTest {
        val expectedSize = Size(200, 200)
        val input = context.decodeBitmapAsset("normal.jpg")
        val actual = transformation.transform(input, expectedSize)

        assertEquals(expectedSize, actual.size)
        actual.assertIsSimilarTo(R.drawable.rounded_corners_6)
    }
}
