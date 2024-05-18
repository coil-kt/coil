package coil3.test.utils

import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.alpha
import androidx.core.graphics.blue
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.green
import androidx.core.graphics.red
import androidx.test.platform.app.InstrumentationRegistry
import coil3.Bitmap
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

actual val Bitmap.width: Int get() = width

actual val Bitmap.height: Int get() = height

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

actual suspend fun Bitmap.computeSimilarity(
    other: Bitmap,
    context: CoroutineContext,
): Double = withContext(context) {
    val pixels1 = async { getPixels() }
    val pixels2 = async { other.getPixels() }

    suspend fun computeThresholdAsync(index: Int) = async {
        val channel1 = pixels1.await()[index]
        val channel2 = pixels2.await()[index]

        val crossCorrelation = crossCorrelation(channel1, channel2)
        if (crossCorrelation.isFinite()) {
            return@async crossCorrelation
        }

        // Fall back to ensuring that each value in the array is at most one off from each other.
        for (i in channel1.indices) {
            if (channel1[i] !in (channel2[i] - 1)..(channel2[i] + 1)) {
                return@async 0.0
            }
        }
        return@async 1.0
    }

    val alphaThreshold = computeThresholdAsync(0)
    val redThreshold = computeThresholdAsync(1)
    val greenThreshold = computeThresholdAsync(2)
    val blueThreshold = computeThresholdAsync(3)

    minOf(
        alphaThreshold.await(),
        redThreshold.await(),
        greenThreshold.await(),
        blueThreshold.await(),
    )
}

/**
 * Asserts that [this] and [expected] are the same size and that
 * the cross correlation of their ARGB channels is >= [threshold].
 */
fun Bitmap.assertIsSimilarTo(
    @DrawableRes expected: Int,
    threshold: Double = 0.98,
) = runBlocking {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    assertIsSimilarTo(AppCompatResources.getDrawable(context, expected)!!.toBitmap(), threshold)
}
