package coil3.test.utils

import coil3.size.Size
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.jetbrains.skia.Bitmap

val Bitmap.size: Size
    get() = Size(width, height)

/**
 * Returns an [Array] of 4 [IntArray]s, the alpha, red, green, and blue pixel values.
 */
fun Bitmap.getPixels(): ByteArray =
    readPixels() ?: byteArrayOf()

fun Bitmap.computeSimilarity(
    other: Bitmap,
): Double = runBlocking(Dispatchers.Default) {
    val pixels1 = async { getPixels() }
    val pixels2 = async { other.getPixels() }

    val channel1 = pixels1.await()
    val channel2 = pixels2.await()

    val crossCorrelation = crossCorrelation(channel1, channel2)
    if (crossCorrelation.isFinite()) {
        return@runBlocking crossCorrelation
    }

    // Fall back to ensuring that each value in the array is at most one off from each other.
    for (i in channel1.indices) {
        if (channel1[i] !in (channel2[i] - 1)..(channel2[i] + 1)) {
            return@runBlocking 0.0
        }
    }
    return@runBlocking 1.0
}

/**
 * Compares two [Bitmap]s by ensuring that they are the same size and that
 * the cross correlation of their ARGB channels is >= [threshold].
 */
fun Bitmap.isSimilarTo(
    expected: Bitmap,
    threshold: Double = 0.99
): Boolean {
    require(threshold in -1.0..1.0) { "Invalid threshold: $threshold" }
    require(width == expected.width && height == expected.height) {
        "The actual image ($width, $height) is not the same size as the " +
            "expected image (${expected.width}, ${expected.height})."
    }

    return computeSimilarity(expected) >= threshold
}

/**
 * Asserts that [this] and [expected] are the same size and that
 * the cross correlation of their ARGB channels is >= [threshold].
 */
fun Bitmap.assertIsSimilarTo(
    expected: Bitmap,
    threshold: Double = 0.987
) {
    require(threshold in -1.0..1.0) { "Invalid threshold: $threshold" }
    require(width == expected.width && height == expected.height) {
        "The actual image ($width, $height) is not the same size as the " +
            "expected image (${expected.width}, ${expected.height})."
    }

    val similarity = computeSimilarity(expected)
    check(similarity >= threshold) {
        "The images are not visually similar. Expected: $threshold; Actual: $similarity."
    }
}
