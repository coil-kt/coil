package coil.drawable

import android.content.Context
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import androidx.test.core.app.ApplicationProvider
import coil.size.Scale
import coil.util.createBitmap
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class CrossfadeDrawableTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `FILL drawable in bounds SAME aspect ratio`() {
        val position = CrossfadeDrawable.Position()
        val start = TestDrawable(100, 100)
        val end = TestDrawable(25, 25)
        val drawable = CrossfadeDrawable(start, end, Scale.FILL)

        assertEquals(100, drawable.intrinsicWidth)
        assertEquals(100, drawable.intrinsicHeight)

        val bounds = Rect(0, 0, 100, 100)

        drawable.updateBounds(start, position, bounds)
        assertEquals(Rect(0, 0, 100, 100), start.bounds)

        drawable.updateBounds(end, position, bounds)
        assertEquals(Rect(0, 0, 100, 100), end.bounds)
    }

    @Test
    fun `FIT drawable in bounds SAME aspect ratio`() {
        val position = CrossfadeDrawable.Position()
        val start = TestDrawable(100, 100)
        val end = TestDrawable(25, 25)
        val drawable = CrossfadeDrawable(start, end, Scale.FIT)

        assertEquals(100, drawable.intrinsicWidth)
        assertEquals(100, drawable.intrinsicHeight)

        val bounds = Rect(0, 0, 100, 100)

        drawable.updateBounds(start, position, bounds)
        assertEquals(Rect(0, 0, 100, 100), start.bounds)

        drawable.updateBounds(end, position, bounds)
        assertEquals(Rect(0, 0, 100, 100), end.bounds)
    }

    @Test
    fun `FILL drawable in bounds DIFFERENT aspect ratio`() {
        val position = CrossfadeDrawable.Position()
        val start = TestDrawable(40, 100)
        val end = TestDrawable(25, 20)
        val drawable = CrossfadeDrawable(start, end, Scale.FILL)

        assertEquals(40, drawable.intrinsicWidth)
        assertEquals(100, drawable.intrinsicHeight)

        val bounds = Rect(0, 0, 40, 100)

        drawable.updateBounds(start, position, bounds)
        assertEquals(Rect(0, 0, 40, 100), start.bounds)

        drawable.updateBounds(end, position, bounds)
        assertEquals(Rect(-42, 0, 82, 100), end.bounds)
    }

    @Test
    fun `FIT drawable in bounds DIFFERENT aspect ratio`() {
        val position = CrossfadeDrawable.Position()
        val start = TestDrawable(200, 65)
        val end = TestDrawable(80, 210)
        val drawable = CrossfadeDrawable(start, end, Scale.FIT)

        assertEquals(200, drawable.intrinsicWidth)
        assertEquals(210, drawable.intrinsicHeight)

        val bounds = Rect(0, 0, 200, 210)

        drawable.updateBounds(start, position, bounds)
        assertEquals(Rect(0, 73, 200, 137), start.bounds)

        drawable.updateBounds(end, position, bounds)
        assertEquals(Rect(60, 0, 140, 210), end.bounds)
    }

    private inner class TestDrawable(
        private val width: Int,
        private val height: Int
    ) : BitmapDrawable(context.resources, createBitmap()) {
        override fun getIntrinsicWidth() = width
        override fun getIntrinsicHeight() = height
    }
}
