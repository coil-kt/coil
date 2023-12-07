package coil3.decode

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import androidx.core.graphics.applyCanvas
import androidx.core.graphics.createBitmap
import androidx.exifinterface.media.ExifInterface
import coil3.util.MIME_TYPE_HEIC
import coil3.util.MIME_TYPE_HEIF
import coil3.util.MIME_TYPE_JPEG
import coil3.util.MIME_TYPE_WEBP
import coil3.util.safeConfig
import java.io.InputStream
import okio.BufferedSource

/** Utility methods for interacting with Exchangeable Image File Format data. */
internal object ExifUtils {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    /**
     * Return the image's EXIF data.
     */
    fun getExifData(
        mimeType: String?,
        source: BufferedSource,
        policy: ExifOrientationPolicy
    ): ExifData {
        if (policy.supports(mimeType)) {
            val exifInterface = ExifInterface(ExifInterfaceInputStream(source.peek().inputStream()))
            return ExifData(exifInterface.isFlipped, exifInterface.rotationDegrees)
        } else {
            return ExifData.NONE
        }
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
            drawBitmap(inBitmap, matrix, paint)
        }
        inBitmap.recycle()
        return outBitmap
    }
}

/** Properties read from an image's EXIF header. */
internal class ExifData(
    val isFlipped: Boolean,
    val rotationDegrees: Int,
) {

    companion object {
        @JvmField val NONE = ExifData(false, 0)
    }
}

internal val ExifData.isSwapped get() = rotationDegrees == 90 || rotationDegrees == 270

internal val ExifData.isRotated get() = rotationDegrees > 0

/** The MIME types that are supported by [ExifOrientationPolicy.RESPECT_PERFORMANCE]. */
private val RESPECT_PERFORMANCE_MIME_TYPES = setOf(
    MIME_TYPE_JPEG, MIME_TYPE_WEBP, MIME_TYPE_HEIC, MIME_TYPE_HEIF
)

internal fun ExifOrientationPolicy.supports(mimeType: String?) = when (this) {
    ExifOrientationPolicy.RESPECT_PERFORMANCE ->
        mimeType != null && mimeType in RESPECT_PERFORMANCE_MIME_TYPES
    ExifOrientationPolicy.IGNORE -> false
    ExifOrientationPolicy.RESPECT_ALL -> true
}

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
