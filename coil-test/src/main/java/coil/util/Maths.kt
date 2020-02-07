package coil.util

import androidx.annotation.FloatRange
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Returns the cross correlation between two arrays.
 *
 * https://en.wikipedia.org/wiki/Cross-correlation
 */
@FloatRange(from = -1.0, to = 1.0)
fun crossCorrelation(x: IntArray, y: IntArray): Double {
    require(x.count() == y.count()) { "Input arrays must be of equal size." }

    val count = x.count()
    val xAvg = x.average()
    val yAvg = y.average()
    val xVar = x.variance()
    val yVar = y.variance()
    val squaredVariance = sqrt(xVar * yVar)
    return if (squaredVariance == 0.0) {
        1.0
    } else {
        (0 until count).sumByDouble { (x[it] - xAvg) * (y[it] - yAvg) } / count / squaredVariance
    }
}

/**
 * Returns an average value of elements in the array.
 */
fun IntArray.variance(): Double {
    if (isEmpty()) return Double.NaN
    val average = average()
    return sumByDouble { (it - average).pow(2) } / count()
}

fun Double.roundToInt(decimals: Int): Double {
    val multiplier = 10.0.pow(decimals)
    return (this * multiplier).roundToInt() / multiplier
}
