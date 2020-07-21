package coil.bitmap

import android.graphics.Bitmap
import coil.util.createBitmap
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class SizeStrategyTest {

    private lateinit var strategy: SizeStrategy

    @Before
    fun before() {
        strategy = SizeStrategy()
    }

    @Test
    fun `equal size bitmap is reused`() {
        val bitmap = createBitmap()
        strategy.put(bitmap)

        assertEquals(bitmap, strategy.get(100, 100, Bitmap.Config.ARGB_8888))
    }

    @Test
    fun `too small bitmap is not reused`() {
        val bitmap = createBitmap(width = 20, height = 20)
        strategy.put(bitmap)

        assertEquals(null, strategy.get(100, 100, Bitmap.Config.ARGB_8888))
    }

    @Test
    fun `large enough bitmap with different config is reused`() {
        val bitmap = createBitmap(width = 250, height = 250, config = Bitmap.Config.RGB_565)
        strategy.put(bitmap)

        assertEquals(bitmap, strategy.get(100, 100, Bitmap.Config.ARGB_8888))
    }

    @Test
    fun `only puts are factored into ceilingKey`() {
        strategy.put(createBitmap(100, 100, Bitmap.Config.ARGB_8888))
        strategy.get(200, 100, Bitmap.Config.ARGB_8888)
        val expected = createBitmap(300, 100, Bitmap.Config.ARGB_8888)
        strategy.put(expected)

        assertEquals(expected, strategy.get(200, 100, Bitmap.Config.ARGB_8888))
    }
}
