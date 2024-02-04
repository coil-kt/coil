package coil3.test.utils

import coil3.Image
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Bitmap

actual interface CoilBitmap {
    actual val width: Int
    actual val height: Int

    val bitmap: Bitmap

    actual suspend fun CoilBitmap.computeSimilarity(other: CoilBitmap): Double
}

actual fun Image.asCoilBitmap(): CoilBitmap = this.toBitmap().toCoilBitmap()

class CoilBitmapImpl(
    override val bitmap: Bitmap
) : CoilBitmap {
    override val width: Int = bitmap.width

    override val height: Int = bitmap.height

    override suspend fun CoilBitmap.computeSimilarity(
        other: CoilBitmap
    ): Double =
        bitmap.computeSimilarity(other.bitmap)
}

fun Bitmap.toCoilBitmap(): CoilBitmap = CoilBitmapImpl(this)

/**
 * Returns a [ByteArray] of pixel values.
 */
fun Bitmap.getPixels(): ByteArray =
    readPixels() ?: byteArrayOf()

suspend fun Bitmap.computeSimilarity(
    other: Bitmap
): Double = withContext(Dispatchers.Default) {
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
