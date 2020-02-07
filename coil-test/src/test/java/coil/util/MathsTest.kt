package coil.util

import org.junit.Test
import kotlin.test.assertEquals

class MathsTest {

    @Test
    fun `empty variance`() {
        assertEquals(Double.NaN, intArrayOf().variance())
    }

    @Test
    fun `basic variance`() {
        assertEquals(2.67, intArrayOf(3, 4, 4, 5, 6, 8).variance().roundToInt(2))
        assertEquals(11.0, intArrayOf(1, 2, 4, 5, 7, 11).variance())
        assertEquals(21704.0, intArrayOf(600, 470, 170, 430, 300).variance())
    }

    @Test
    fun `empty cross correlation`() {
        val x = intArrayOf()
        val y = intArrayOf()
        assertEquals(Double.NaN, crossCorrelation(x, y))
    }

    @Test
    fun `orthogonal cross correlation`() {
        val x = intArrayOf(1, 2, 3)
        val y = intArrayOf(3, 2, 1)
        assertEquals(-1.0, crossCorrelation(x, y))
    }

    @Test
    fun `equal cross correlation`() {
        val x = intArrayOf(1, 2, 3)
        val y = intArrayOf(1, 2, 3)
        assertEquals(1.0, crossCorrelation(x, y))
    }

    @Test
    fun `parallel cross correlation`() {
        val x = intArrayOf(1, 2, 3)
        val y = intArrayOf(2, 4, 6)
        assertEquals(1.0, crossCorrelation(x, y))
    }

    @Test
    fun `complex cross correlation`() {
        val x = intArrayOf(123, 241, 211, 183, 145)
        val y = intArrayOf(81, 48, 178, 64, 255)
        assertEquals(-0.311775, crossCorrelation(x, y).roundToInt(6))
    }
}
