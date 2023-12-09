package coil3.test.utils

import kotlin.test.Test
import kotlin.test.assertEquals

class MathsTest {

    @Test
    fun emptyVariance() {
        assertEquals(Double.NaN, intArrayOf().variance())
    }

    @Test
    fun basicVariance() {
        assertEquals(2.67, intArrayOf(3, 4, 4, 5, 6, 8).variance().round(2))
        assertEquals(11.0, intArrayOf(1, 2, 4, 5, 7, 11).variance())
        assertEquals(21704.0, intArrayOf(600, 470, 170, 430, 300).variance())
    }

    @Test
    fun emptyCrossCorrelation() {
        val x = intArrayOf()
        val y = intArrayOf()
        assertEquals(Double.NaN, crossCorrelation(x, y))
    }

    @Test
    fun orthogonalCrossCorrelation() {
        val x = intArrayOf(1, 2, 3)
        val y = intArrayOf(3, 2, 1)
        assertEquals(-1.0, crossCorrelation(x, y))
    }

    @Test
    fun equalCrossCorrelation() {
        val x = intArrayOf(1, 2, 3)
        val y = intArrayOf(1, 2, 3)
        assertEquals(1.0, crossCorrelation(x, y))
    }

    @Test
    fun parallelCrossCorrelation() {
        val x = intArrayOf(1, 2, 3)
        val y = intArrayOf(2, 4, 6)
        assertEquals(1.0, crossCorrelation(x, y))
    }

    @Test
    fun complexCrossCorrelation() {
        val x = intArrayOf(123, 241, 211, 183, 145)
        val y = intArrayOf(81, 48, 178, 64, 255)
        assertEquals(-0.311775, crossCorrelation(x, y).round(6))
    }
}
