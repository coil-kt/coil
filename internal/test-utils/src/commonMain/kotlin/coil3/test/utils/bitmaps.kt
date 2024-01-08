package coil3.test.utils

import coil3.Image
import coil3.size.Size

expect interface CoilBitmap {
    val width: Int
    val height: Int

    suspend fun CoilBitmap.computeSimilarity(
        other: CoilBitmap,
    ): Double
}

expect fun Image.asCoilBitmap(): CoilBitmap

val CoilBitmap.size: Size
    get() = Size(width, height)

/**
 * Compares two [CoilBitmap]s by ensuring that they are the same size and that
 * the cross correlation of their ARGB channels is >= [threshold].
 */
suspend fun CoilBitmap.isSimilarTo(
    expected: CoilBitmap,
    threshold: Double = 0.98,
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
suspend fun CoilBitmap.assertIsSimilarTo(
    expected: CoilBitmap,
    threshold: Double = 0.98,
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
