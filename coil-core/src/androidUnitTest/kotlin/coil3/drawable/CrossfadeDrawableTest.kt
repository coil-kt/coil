package coil3.drawable

import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import androidx.core.graphics.createBitmap
import coil3.size.Scale
import coil3.test.utils.RobolectricTest
import coil3.test.utils.context
import coil3.transition.CrossfadeDrawable
import coil3.util.toDrawable
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

class CrossfadeDrawableTest : RobolectricTest() {

    @Test
    fun `SAME aspect ratio - FILL`() {
        val start = TestBitmapDrawable(100, 100)
        val end = TestBitmapDrawable(25, 25)
        val drawable = CrossfadeDrawable(start, end, Scale.FILL)

        assertEquals(100, drawable.intrinsicWidth)
        assertEquals(100, drawable.intrinsicHeight)

        val bounds = Rect(0, 0, 100, 100)

        drawable.updateBounds(start, bounds)
        assertEquals(Rect(0, 0, 100, 100), start.bounds)

        drawable.updateBounds(end, bounds)
        assertEquals(Rect(0, 0, 100, 100), end.bounds)
    }

    @Test
    fun `SAME aspect ratio - FIT`() {
        val start = TestBitmapDrawable(100, 100)
        val end = TestBitmapDrawable(25, 25)
        val drawable = CrossfadeDrawable(start, end, Scale.FIT)

        assertEquals(100, drawable.intrinsicWidth)
        assertEquals(100, drawable.intrinsicHeight)

        val bounds = Rect(0, 0, 100, 100)

        drawable.updateBounds(start, bounds)
        assertEquals(Rect(0, 0, 100, 100), start.bounds)

        drawable.updateBounds(end, bounds)
        assertEquals(Rect(0, 0, 100, 100), end.bounds)
    }

    @Test
    fun `DIFFERENT aspect ratio - FILL`() {
        val start = TestBitmapDrawable(40, 100)
        val end = TestBitmapDrawable(25, 20)
        val drawable = CrossfadeDrawable(start, end, Scale.FILL)

        assertEquals(40, drawable.intrinsicWidth)
        assertEquals(100, drawable.intrinsicHeight)

        val bounds = Rect(0, 0, 40, 100)

        drawable.updateBounds(start, bounds)
        assertEquals(Rect(0, 0, 40, 100), start.bounds)

        drawable.updateBounds(end, bounds)
        assertEquals(Rect(-42, 0, 82, 100), end.bounds)
    }

    @Test
    fun `DIFFERENT aspect ratio - FIT`() {
        val start = TestBitmapDrawable(200, 65)
        val end = TestBitmapDrawable(80, 210)
        val drawable = CrossfadeDrawable(start, end, Scale.FIT)

        assertEquals(200, drawable.intrinsicWidth)
        assertEquals(210, drawable.intrinsicHeight)

        val bounds = Rect(0, 0, 200, 210)

        drawable.updateBounds(start, bounds)
        assertEquals(Rect(0, 73, 200, 137), start.bounds)

        drawable.updateBounds(end, bounds)
        assertEquals(Rect(60, 0, 140, 210), end.bounds)
    }

    @Test
    fun `NULL drawable - FILL`() {
        val end = TestBitmapDrawable(25, 20)
        val drawable = CrossfadeDrawable(null, end, Scale.FILL)

        assertEquals(25, drawable.intrinsicWidth)
        assertEquals(20, drawable.intrinsicHeight)

        val bounds = Rect(0, 0, 40, 100)

        drawable.updateBounds(end, bounds)
        assertEquals(Rect(-42, 0, 82, 100), end.bounds)
    }

    @Test
    fun `NULL start drawable - FIT`() {
        val end = TestBitmapDrawable(80, 210)
        val drawable = CrossfadeDrawable(null, end, Scale.FIT)

        assertEquals(80, drawable.intrinsicWidth)
        assertEquals(210, drawable.intrinsicHeight)

        val bounds = Rect(0, 0, 200, 210)

        drawable.updateBounds(end, bounds)
        assertEquals(Rect(60, 0, 140, 210), end.bounds)
    }

    @Test
    fun `non bitmap drawable - FILL`() {
        val start = TestBitmapDrawable(240, 290)
        val end = TestDrawable(400, 300)
        val drawable = CrossfadeDrawable(start, end, Scale.FILL)

        assertEquals(400, drawable.intrinsicWidth)
        assertEquals(300, drawable.intrinsicHeight)

        val bounds = Rect(0, 0, 40, 100)

        drawable.updateBounds(start, bounds)
        assertEquals(Rect(-21, 0, 61, 100), start.bounds)

        drawable.updateBounds(end, bounds)
        assertEquals(Rect(-47, 0, 87, 100), end.bounds)
    }

    @Test
    fun `non bitmap drawable - FIT`() {
        val start = TestDrawable(200, 120)
        val end = TestDrawable(500, 175)
        val drawable = CrossfadeDrawable(start, end, Scale.FIT)

        assertEquals(500, drawable.intrinsicWidth)
        assertEquals(175, drawable.intrinsicHeight)

        val bounds = Rect(0, 0, 200, 210)

        drawable.updateBounds(start, bounds)
        assertEquals(Rect(0, 45, 200, 165), start.bounds)

        drawable.updateBounds(end, bounds)
        assertEquals(Rect(0, 70, 200, 140), end.bounds)
    }

    @Test
    fun `start and end drawables are mutated`() {
        val startDrawable = TestBitmapDrawable(100, 100)
        val endDrawable = TestBitmapDrawable(100, 100)
        assertFalse(startDrawable.mutated)
        assertFalse(endDrawable.mutated)

        CrossfadeDrawable(startDrawable, endDrawable)
        assertTrue(startDrawable.mutated)
        assertTrue(endDrawable.mutated)
    }

    @Test
    fun `preferExactIntrinsicSize=false`() {
        val drawable1 = CrossfadeDrawable(
            start = ColorDrawable(),
            end = createBitmap(100, 100).toDrawable(context),
            preferExactIntrinsicSize = false
        )

        assertEquals(-1, drawable1.intrinsicWidth)
        assertEquals(-1, drawable1.intrinsicHeight)

        val drawable2 = CrossfadeDrawable(
            start = null,
            end = createBitmap(100, 100).toDrawable(context),
            preferExactIntrinsicSize = false
        )

        assertEquals(100, drawable2.intrinsicWidth)
        assertEquals(100, drawable2.intrinsicHeight)
    }

    @Test
    fun `preferExactIntrinsicSize=true`() {
        val drawable1 = CrossfadeDrawable(
            start = ColorDrawable(),
            end = createBitmap(100, 100).toDrawable(context),
            preferExactIntrinsicSize = true
        )

        assertEquals(100, drawable1.intrinsicWidth)
        assertEquals(100, drawable1.intrinsicHeight)

        val drawable2 = CrossfadeDrawable(
            start = null,
            end = createBitmap(100, 100).toDrawable(context),
            preferExactIntrinsicSize = true
        )

        assertEquals(100, drawable2.intrinsicWidth)
        assertEquals(100, drawable2.intrinsicHeight)
    }

    private class TestDrawable(
        private val width: Int,
        private val height: Int
    ) : ColorDrawable() {
        var mutated = false
            private set

        override fun getIntrinsicWidth() = width
        override fun getIntrinsicHeight() = height
        override fun mutate() = super.mutate().also { mutated = true }
    }

    private inner class TestBitmapDrawable(
        private val width: Int,
        private val height: Int
    ) : BitmapDrawable(context.resources, createBitmap(width, height)) {
        var mutated = false
            private set

        override fun getIntrinsicWidth() = width
        override fun getIntrinsicHeight() = height
        override fun mutate() = super.mutate().also { mutated = true }
    }
}
