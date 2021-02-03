package coil.transform

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.test.core.app.ApplicationProvider
import coil.size.PixelSize
import coil.util.RoundedCornerTransformation
import coil.util.decodeBitmapAsset
import coil.util.isSimilarTo
import org.junit.Before
import org.junit.Test
import kotlin.test.assertTrue

class AnimatedRoundedCornerTransformationTest {

    private lateinit var context: Context
    private lateinit var transformation: RoundedCornerTransformation

    @Before
    fun before() {
        context = ApplicationProvider.getApplicationContext()
        transformation = RoundedCornerTransformation()
    }

    @Test
    fun basic() {
        val input = context.decodeBitmapAsset("normal_small.jpg")
        val expected = context.decodeBitmapAsset("normal_small_rounded_corners.png")
        val actual = Bitmap.createBitmap(input.width, input.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(actual)
        canvas.drawBitmap(input, 0f, 0f, null)
        transformation.transform(canvas, PixelSize(input.width, input.height))
        assertTrue(actual.isSimilarTo(expected))
    }
}
