package coil3.compose

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.FilterQuality
import coil3.asImage
import coil3.test.utils.RobolectricTest
import coil3.test.utils.context
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ImagePainterTest : RobolectricTest() {

    @Test
    fun drawableImageAsPainter_appliesFilterQuality() {
        val drawable = TestDrawable()

        drawable.asImage().asPainter(context, FilterQuality.None)

        assertFalse(drawable.filterBitmap)
    }

    @Test
    fun drawableImageAsPainter_enablesFilterQuality() {
        val drawable = TestDrawable(initialFilterBitmap = false)

        drawable.asImage().asPainter(context, FilterQuality.Low)

        assertTrue(drawable.filterBitmap)
    }

    private class TestDrawable(
        initialFilterBitmap: Boolean = true,
    ) : Drawable() {
        var filterBitmap = initialFilterBitmap

        override fun draw(canvas: Canvas) = Unit

        override fun setAlpha(alpha: Int) = Unit

        override fun setColorFilter(colorFilter: ColorFilter?) = Unit

        override fun setFilterBitmap(filter: Boolean) {
            filterBitmap = filter
        }

        override fun mutate(): Drawable {
            return this
        }

        @Deprecated("Deprecated in Java")
        override fun getOpacity() = PixelFormat.TRANSLUCENT
    }
}
