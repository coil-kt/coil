package coil.decode

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import androidx.core.graphics.applyCanvas
import androidx.core.graphics.createBitmap
import androidx.exifinterface.media.ExifInterface
import coil.util.MIME_TYPE_HEIC
import coil.util.MIME_TYPE_HEIF
import coil.util.MIME_TYPE_JPEG
import coil.util.MIME_TYPE_WEBP
import coil.util.safeConfig
import okio.BufferedSource
import java.io.InputStream

/** Utility methods for interacting with Exchangeable Image File Format data. */
internal object ExifUtils {

    /**
     * We don't support PNG EXIF data as it's very rarely used and requires buffering the entire
     * file into memory. All of the supported formats short circuit when the EXIF chunk is found
     * (often near the top of the file).
     */
    private val SUPPORTED_MIME_TYPES = arrayOf(
        MIME_TYPE_JPEG, MIME_TYPE_WEBP, MIME_TYPE_HEIC, MIME_TYPE_HEIF
    )

    private val PAINT = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    /**
     * Read the image's EXIF data.
     */
    fun readData(mimeType: String?, source: BufferedSource): ExifData {
        if (mimeType == null || mimeType !in SUPPORTED_MIME_TYPES) {
            return ExifData.NONE
        }

        val exifInterface = ExifInterface(ExifInterfaceInputStream(source.peek().inputStream()))
        return ExifData(exifInterface.isFlipped, exifInterface.rotationDegrees)
    }

    /**
     * Reverse the EXIF transformations applied to [inBitmap] and return the output image.
     */
    fun reverseTransformations(inBitmap: Bitmap, exifData: ExifData): Bitmap {
        // Short circuit if there are no transformations to apply.
        if (!exifData.isFlipped && !exifData.isRotated) {
            return inBitmap
        }

        val matrix = Matrix()
        val centerX = inBitmap.width / 2f
        val centerY = inBitmap.height / 2f
        if (exifData.isFlipped) {
            matrix.postScale(-1f, 1f, centerX, centerY)
        }
        if (exifData.isRotated) {
            matrix.postRotate(exifData.rotationDegrees.toFloat(), centerX, centerY)
        }

        val rect = RectF(0f, 0f, inBitmap.width.toFloat(), inBitmap.height.toFloat())
        matrix.mapRect(rect)
        if (rect.left != 0f || rect.top != 0f) {
            matrix.postTranslate(-rect.left, -rect.top)
        }

        val outBitmap = if (exifData.isSwapped) {
            createBitmap(inBitmap.height, inBitmap.width, inBitmap.safeConfig)
        } else {
            createBitmap(inBitmap.width, inBitmap.height, inBitmap.safeConfig)
        }

        outBitmap.applyCanvas {
            drawBitmap(inBitmap, matrix, PAINT)
        }
        inBitmap.recycle()
        return outBitmap
    }
}

/** Properties read from an image's EXIF header. */
internal class ExifData(
    val isFlipped: Boolean,
    val rotationDegrees: Int
) {

    companion object {
        @JvmField val NONE = ExifData(false, 0)
    }
}

internal val ExifData.isSwapped get() = rotationDegrees == 90 || rotationDegrees == 270

internal val ExifData.isRotated get() = rotationDegrees > 0

/** Wrap [delegate] so that it works with [ExifInterface]. */
private class ExifInterfaceInputStream(private val delegate: InputStream) : InputStream() {

    /**
     * Ensure that this value is always larger than the size of the image
     * so ExifInterface won't stop reading the stream prematurely.
     */
    private var availableBytes = 1024 * 1024 * 1024 // 1GB

    override fun read() = interceptBytesRead(delegate.read())

    override fun read(b: ByteArray) = interceptBytesRead(delegate.read(b))

    override fun read(b: ByteArray, off: Int, len: Int) =
        interceptBytesRead(delegate.read(b, off, len))

    override fun skip(n: Long) = delegate.skip(n)

    override fun available() = availableBytes

    override fun close() = delegate.close()

    private fun interceptBytesRead(bytesRead: Int): Int {
        if (bytesRead == -1) availableBytes = 0
        return bytesRead
    }
}
