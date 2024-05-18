package coil3.test.utils

import coil3.Bitmap
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
actual val Bitmap.width: Int get() = width

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
actual val Bitmap.height: Int get() = height

/**
 * Returns a [ByteArray] of pixel values.
 */
fun Bitmap.getPixels(): ByteArray = readPixels() ?: byteArrayOf()

actual suspend fun Bitmap.computeSimilarity(
    other: Bitmap,
    context: CoroutineContext,
): Double = withContext(context) {
    val pixels1 = async { getPixels() }
    val pixels2 = async { other.getPixels() }

    val channel1 = pixels1.await()
    val channel2 = pixels2.await()

    val crossCorrelation = crossCorrelation(channel1, channel2)
    if (crossCorrelation.isFinite()) {
        return@withContext crossCorrelation
    }

    // Fall back to ensuring that each value in the array is at most one off from each other.
    for (i in channel1.indices) {
        if (channel1[i] !in (channel2[i] - 1)..(channel2[i] + 1)) {
            return@withContext 0.0
        }
    }
    return@withContext 1.0
}
