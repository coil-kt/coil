package coil.bitmappool

import android.content.ComponentCallbacks2.TRIM_MEMORY_BACKGROUND
import android.content.ComponentCallbacks2.TRIM_MEMORY_COMPLETE
import android.content.ComponentCallbacks2.TRIM_MEMORY_MODERATE
import android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL
import android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW
import android.content.ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN
import android.graphics.Bitmap
import coil.util.DEFAULT_BITMAP_SIZE
import coil.util.createBitmap
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalStdlibApi::class)
class RealBitmapPoolTest {

    companion object {
        private const val MAX_BITMAPS = 10
        private const val MAX_SIZE = MAX_BITMAPS * DEFAULT_BITMAP_SIZE
        private val ALLOWED_CONFIGS = setOf(Bitmap.Config.ARGB_8888)
    }

    private lateinit var strategy: FakeReuseStrategy
    private lateinit var pool: RealBitmapPool

    @Before
    fun before() {
        strategy = FakeReuseStrategy()
        pool = RealBitmapPool(MAX_SIZE, ALLOWED_CONFIGS, strategy)
    }

    @Test
    fun `bitmap is reused`() {
        val bitmap = createBitmap()
        pool.put(bitmap)

        assertEquals(bitmap, pool.get(100, 100, Bitmap.Config.ARGB_8888))
    }

    @Test
    fun `immutable bitmaps are not added`() {
        val bitmap = createBitmap(isMutable = false)
        pool.put(bitmap)

        assertTrue(strategy.bitmaps.isEmpty())
    }

    @Test
    fun `size is limited`() {
        pool.fill(MAX_BITMAPS + 2)

        assertEquals(2, strategy.numRemoves)
    }

    @Test
    fun `clear memory removes all bitmaps`() {
        pool.fill(MAX_BITMAPS)
        pool.clearMemory()

        assertEquals(MAX_BITMAPS, strategy.numRemoves)
    }

    @Test
    fun `evicted bitmaps are recycled`() {
        pool.fill(MAX_BITMAPS)
        val bitmaps = strategy.bitmaps.toList()
        pool.clearMemory()

        bitmaps.forEach { assertTrue(it.isRecycled) }
    }

    @Test
    fun `trim memory running low or critical removes half`() {
        listOf(TRIM_MEMORY_RUNNING_LOW, TRIM_MEMORY_RUNNING_CRITICAL).forEach { trimLevel ->
            testTrimMemory(trimLevel, MAX_BITMAPS / 2)
        }
    }

    @Test
    fun `trim memory running low reduces current size by half`() {
        pool.fill(MAX_BITMAPS)

        pool.trimMemory(TRIM_MEMORY_RUNNING_LOW)
        assertEquals(MAX_BITMAPS / 2, strategy.numRemoves)

        pool.trimMemory(TRIM_MEMORY_RUNNING_LOW)
        assertEquals(MAX_BITMAPS - (MAX_BITMAPS / 4), strategy.numRemoves)
    }

    @Test
    fun `trim memory background or greater removes all bitmaps`() {
        listOf(TRIM_MEMORY_BACKGROUND, TRIM_MEMORY_MODERATE, TRIM_MEMORY_COMPLETE).forEach { trimLevel ->
            testTrimMemory(trimLevel, MAX_BITMAPS)
        }
    }

    @Test
    fun `trim memory ui hidden removes no bitmaps`() {
        testTrimMemory(TRIM_MEMORY_UI_HIDDEN, 0)
    }

    private fun testTrimMemory(trimLevel: Int, numRemoves: Int) {
        val strategy = FakeReuseStrategy()
        val pool = RealBitmapPool(MAX_SIZE, ALLOWED_CONFIGS, strategy)
        pool.fill(MAX_BITMAPS)
        pool.trimMemory(trimLevel)

        assertEquals(numRemoves, strategy.numRemoves, "Failed level=$trimLevel")
    }

    @Test
    fun `bitmap larger than pool is ignored and recycled`() {
        pool = RealBitmapPool(100, ALLOWED_CONFIGS, strategy)
        val bitmap = createBitmap()
        pool.put(bitmap)

        assertEquals(0, strategy.numRemoves)
        assertEquals(0, strategy.numPuts)
        assertTrue(bitmap.isRecycled)
    }

    @Test
    fun `bitmaps with disallowed configs are ignored and recycled`() {
        pool = RealBitmapPool(MAX_SIZE, setOf(Bitmap.Config.ARGB_8888), strategy)

        val bitmap = createBitmap(config = Bitmap.Config.RGB_565)
        pool.put(bitmap)

        assertEquals(0, strategy.numPuts)
        assertTrue(bitmap.isRecycled)
    }

    @Test
    fun `bitmaps with null config are not allowed`() {
        pool = RealBitmapPool(MAX_SIZE, strategy = strategy)

        val bitmap = createBitmap()
        bitmap.config = null

        pool.put(bitmap)

        assertEquals(0, strategy.numPuts)
    }

    private fun RealBitmapPool.fill(fillCount: Int) {
        repeat(fillCount) { put(createBitmap()) }
    }
}
