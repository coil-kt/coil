package coil3.test.utils

import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Returns the cross correlation between two arrays.
 *
 * https://en.wikipedia.org/wiki/Cross-correlation
 */
fun crossCorrelation(x: IntArray, y: IntArray): Double {
    require(x.count() == y.count()) { "Input arrays must be of equal size." }

    val xVar = x.variance()
    val yVar = y.variance()
    val squaredVariance = sqrt(xVar * yVar)

    val xAvg = x.average()
    val yAvg = y.average()
    val count = x.count()

    var sum = 0.0
    for (index in 0 until count) {
        sum += (x[index] - xAvg) * (y[index] - yAvg)
    }
    return sum / count / squaredVariance
}

/**
 * Returns the cross correlation between two arrays.
 *
 * https://en.wikipedia.org/wiki/Cross-correlation
 */
fun crossCorrelation(x: ByteArray, y: ByteArray): Double {
    require(x.count() == y.count()) { "Input arrays must be of equal size." }

    val xVar = x.variance()
    val yVar = y.variance()
    val squaredVariance = sqrt(xVar * yVar)

    val xAvg = x.average()
    val yAvg = y.average()
    val count = x.count()

    var sum = 0.0
    for (index in 0 until count) {
        sum += (x[index] - xAvg) * (y[index] - yAvg)
    }
    return sum / count / squaredVariance
}

/**
 * Returns an average value of elements in the array.
 */
fun IntArray.variance(): Double {
    if (isEmpty()) return Double.NaN
    val average = average()
    return sumOf { (it - average).pow(2) } / count()
}

/**
 * Returns an average value of elements in the array.
 */
fun ByteArray.variance(): Double {
    if (isEmpty()) return Double.NaN
    val average = average()
    return sumOf { (it - average).pow(2) } / count()
}

/**
 * Round the given value to the nearest [Double] with [precision] number of decimal places.
 */
fun Double.round(precision: Int): Double {
    val multiplier = 10.0.pow(precision)
    return (this * multiplier).roundToInt() / multiplier
}
