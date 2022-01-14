package coil.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.VectorDrawable
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.applyCanvas
import androidx.core.graphics.component1
import androidx.core.graphics.component2
import androidx.core.graphics.component3
import androidx.core.graphics.component4
import androidx.test.core.app.ApplicationProvider
import coil.base.R
import coil.size
import coil.size.Scale
import coil.size.Size
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class DrawableUtilsTest {

    @Test
    fun `vector with hardware config is converted correctly`() {
        val size = Size(200, 200)
        val input = object : VectorDrawable() {
            override fun getIntrinsicWidth() = 100
            override fun getIntrinsicHeight() = 100
        }
        val output = DrawableUtils.convertToBitmap(
            drawable = input,
            config = Bitmap.Config.HARDWARE,
            size = size,
            scale = Scale.FIT,
            allowInexactSize = true
        )

        assertEquals(Bitmap.Config.ARGB_8888, output.config)
        assertEquals(size, output.size)
    }

    /** Regression test: https://github.com/coil-kt/coil/issues/1081 */
    @Test
    fun `rectangular vector is converted correctly`() {
        val size = Size(200, 200)
        val context: Context = ApplicationProvider.getApplicationContext()
        val vector = AppCompatResources.getDrawable(context, R.drawable.ic_100tb)!!

        val expected = createBitmap(200, 74).applyCanvas {
            val (oldLeft, oldTop, oldRight, oldBottom) = vector.bounds
            vector.setBounds(0, 0, width, height)
            vector.draw(this)
            vector.setBounds(oldLeft, oldTop, oldRight, oldBottom)
        }
        val actual = DrawableUtils.convertToBitmap(
            drawable = vector,
            config = Bitmap.Config.HARDWARE,
            size = size,
            scale = Scale.FIT,
            allowInexactSize = true
        )

        actual.assertIsSimilarTo(expected)
    }

    @Test
    fun `unimplemented intrinsic size does not crash`() {
        val size = Size(200, 200)
        val input = object : VectorDrawable() {
            override fun getIntrinsicWidth() = -1
            override fun getIntrinsicHeight() = -1
        }
        val output = DrawableUtils.convertToBitmap(
            drawable = input,
            size = size,
            config = Bitmap.Config.HARDWARE,
            scale = Scale.FIT,
            allowInexactSize = true
        )

        assertEquals(Bitmap.Config.ARGB_8888, output.config)
        assertEquals(size, output.size)
    }

    @Test
    fun `aspect ratio is preserved`() {
        val input = object : VectorDrawable() {
            override fun getIntrinsicWidth() = 125
            override fun getIntrinsicHeight() = 250
        }
        val output = DrawableUtils.convertToBitmap(
            drawable = input,
            size = Size(200, 200),
            config = Bitmap.Config.ARGB_8888,
            scale = Scale.FIT,
            allowInexactSize = true
        )

        assertEquals(Bitmap.Config.ARGB_8888, output.config)
        assertEquals(Size(100, 200), output.size)
    }
}
