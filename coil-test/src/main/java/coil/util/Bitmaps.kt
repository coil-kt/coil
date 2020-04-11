package coil.util

import android.graphics.Bitmap
import androidx.annotation.FloatRange
import androidx.core.graphics.alpha
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red

/**
 * Returns an [Array] of 4 [IntArray]s, the alpha, red, green, and blue pixel values.
 */
fun Bitmap.getPixels(): Array<IntArray> {
    val size = width * height
    val pixels = IntArray(size)
    getPixels(pixels, 0, width, 0, 0, width, height)

    val alpha = IntArray(size)
    val red = IntArray(size)
    val green = IntArray(size)
    val blue = IntArray(size)

    pixels.forEachIndexed { index, pixel ->
        alpha[index] = pixel.alpha
        red[index] = pixel.red
        green[index] = pixel.green
        blue[index] = pixel.blue
    }

    return arrayOf(alpha, red, green, blue)
}

/**
 * Compares two [Bitmap]s by ensuring that they are the same size and that
 * the cross correlation of their ARGB channels is >= [threshold].
 */
fun Bitmap.isSimilarTo(other: Bitmap, @FloatRange(from = -1.0, to = 1.0) threshold: Double = 0.99): Boolean {
    require(threshold in -1.0..1.0) { "Invalid threshold: $threshold" }

    if (width != other.width || height != other.height) {
        return false
    }

    val (xAlpha, xRed, xGreen, xBlue) = getPixels()
    val (yAlpha, yRed, yGreen, yBlue) = other.getPixels()

    return crossCorrelation(xAlpha, yAlpha) >= threshold &&
        crossCorrelation(xRed, yRed) >= threshold &&
        crossCorrelation(xGreen, yGreen) >= threshold &&
        crossCorrelation(xBlue, yBlue) >= threshold
}
