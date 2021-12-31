package coil.decode

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import androidx.core.graphics.applyCanvas
import androidx.core.graphics.createBitmap
import androidx.exifinterface.media.ExifInterface
import java.io.InputStream

internal object Exif {

    /**
     * NOTE: We don't support PNG EXIF data as it's very rarely used and requires buffering
     * the entire file into memory. All of the supported formats short circuit when the EXIF
     * chunk is found (often near the top of the file).
     */
    private val SUPPORTED_EXIF_MIME_TYPES = arrayOf(
        BitmapFactoryDecoder.MIME_TYPE_JPEG,
        BitmapFactoryDecoder.MIME_TYPE_WEBP,
        BitmapFactoryDecoder.MIME_TYPE_HEIC,
        BitmapFactoryDecoder.MIME_TYPE_HEIF
    )

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    /**
     * Return 'true' if we should read the image's EXIF data.
     */
    internal fun shouldReadExifData(mimeType: String?): Boolean {
        return mimeType != null && mimeType in SUPPORTED_EXIF_MIME_TYPES
    }

    /**
     * This method assumes [config] is not [Bitmap.Config.HARDWARE].
     */
    internal fun applyExifTransformations(
        inBitmap: Bitmap,
        config: Bitmap.Config,
        exifData: Data
    ): Bitmap {
        // Short circuit if there are no transformations to apply.
        val isRotated = exifData.rotationDegrees > 0
        if (!exifData.isFlipped && !isRotated) {
            return inBitmap
        }

        val matrix = Matrix()
        val centerX = inBitmap.width / 2f
        val centerY = inBitmap.height / 2f
        if (exifData.isFlipped) {
            matrix.postScale(-1f, 1f, centerX, centerY)
        }
        if (isRotated) {
            matrix.postRotate(exifData.rotationDegrees.toFloat(), centerX, centerY)
        }

        val rect = RectF(0f, 0f, inBitmap.width.toFloat(), inBitmap.height.toFloat())
        matrix.mapRect(rect)
        if (rect.left != 0f || rect.top != 0f) {
            matrix.postTranslate(-rect.left, -rect.top)
        }

        val outBitmap = if (exifData.isSwapped) {
            createBitmap(inBitmap.height, inBitmap.width, config)
        } else {
            createBitmap(inBitmap.width, inBitmap.height, config)
        }

        outBitmap.applyCanvas {
            drawBitmap(inBitmap, matrix, paint)
        }
        inBitmap.recycle()
        return outBitmap
    }

    internal data class Data(
        val isFlipped: Boolean,
        val rotationDegrees: Int
    ) {
        val isSwapped = rotationDegrees == 90 || rotationDegrees == 270

        internal companion object {
            val DEFAULT = Data(
                isFlipped = false,
                rotationDegrees = 0
            )

            internal fun ExifInterface.toExifData() = Data(
                isFlipped = isFlipped,
                rotationDegrees = rotationDegrees
            )
        }
    }
}

/**
 * Wrap [delegate] so that it works with [ExifInterface].
 */
internal class ExifInterfaceInputStream(private val delegate: InputStream) : InputStream() {

    /**
     * Ensure that this value is always larger than the size of the image
     * so ExifInterface won't stop reading the stream prematurely.
     */
    private var availableBytes = GIGABYTE_IN_BYTES

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

    private companion object {
        const val GIGABYTE_IN_BYTES = 1024 * 1024 * 1024
    }
}
