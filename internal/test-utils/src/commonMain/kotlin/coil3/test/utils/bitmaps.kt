package coil3.test.utils

import coil3.Bitmap
import coil3.BitmapImage
import coil3.Image
import coil3.size.Size
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.Dispatchers

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
expect val Bitmap.width: Int

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
expect val Bitmap.height: Int

val Bitmap.size: Size
    get() = Size(width, height)

val Image.bitmap: Bitmap
    get() = (this as BitmapImage).bitmap

/**
 * Compares two [Bitmap]s by ensuring that they are the same size and that
 * the cross correlation of their ARGB channels is >= [threshold].
 */
suspend fun Bitmap.isSimilarTo(
    expected: Bitmap,
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
suspend fun Bitmap.assertIsSimilarTo(
    expected: Bitmap,
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

expect suspend fun Bitmap.computeSimilarity(
    other: Bitmap,
    context: CoroutineContext = Dispatchers.Default,
): Double
