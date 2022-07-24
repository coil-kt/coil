package coil.transform

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import coil.size.Dimension
import coil.size.Size
import coil.util.decodeBitmapAsset
import coil.util.size
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
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
    }

    @Test
    fun undefined_width() = runTest {
        val input = context.decodeBitmapAsset("normal_small.jpg")
        val actual = transformation.transform(input, Size(Dimension.Undefined, 100))

        assertEquals(Size(80, 100), actual.size)
    }

    @Test
    fun undefined_height() = runTest {
        val input = context.decodeBitmapAsset("normal_small.jpg")
        val actual = transformation.transform(input, Size(100, Dimension.Undefined))

        assertEquals(Size(100, 125), actual.size)
    }

    @Test
    fun undefined_size() = runTest {
        val input = context.decodeBitmapAsset("normal_small.jpg")
        val actual = transformation.transform(input, Size.ORIGINAL)

        assertEquals(Size(103, 129), actual.size)
    }
}
