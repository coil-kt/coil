package coil.decode

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.KITKAT
import android.os.Build.VERSION_CODES.O
import androidx.core.graphics.applyCanvas
import androidx.exifinterface.media.ExifInterface
import coil.bitmappool.BitmapPool
import coil.size.PixelSize
import coil.size.Scale
import coil.size.Size
import coil.util.normalize
import coil.util.toDrawable
import okio.Buffer
import okio.BufferedSource
import okio.ForwardingSource
import okio.Source
import okio.buffer
import java.io.InputStream
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * The base [Decoder] that uses [BitmapFactory] to decode a given [BufferedSource].
 */
internal class BitmapFactoryDecoder(
    private val context: Context
) : Decoder {

    companion object {
        private const val MIME_TYPE_JPEG = "image/jpeg"
    }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    override fun handles(source: BufferedSource, mimeType: String?) = true

    override suspend fun decode(
        pool: BitmapPool,
        source: BufferedSource,
        size: Size,
        options: Options
    ): DecodeResult = BitmapFactory.Options().run {
        val safeSource = ExceptionCatchingSource(source)
        val safeBufferedSource = safeSource.buffer()

        // Read the image's dimensions.
        inJustDecodeBounds = true
        BitmapFactory.decodeStream(safeBufferedSource.peek().inputStream(), null, this)
        safeSource.exception?.let { throw it }
        inJustDecodeBounds = false

        // Read the image's EXIF data.
        val exifInterface = ExifInterface(AvailableInputStream(safeBufferedSource.peek().inputStream()))
        val isFlipped = exifInterface.isFlipped
        val rotationDegrees = exifInterface.rotationDegrees
        val isRotated = rotationDegrees > 0
        val isSwapped = rotationDegrees == 90 || rotationDegrees == 270

        // srcWidth and srcHeight are the dimensions of the image after EXIF transformations (but before sampling).
        val srcWidth = if (isSwapped) outHeight else outWidth
        val srcHeight = if (isSwapped) outWidth else outHeight

        // Disable hardware Bitmaps if we need to perform EXIF transformations.
        val safeConfig = if (isFlipped || isRotated) options.config.normalize() else options.config
        inPreferredConfig = if (allowRgb565(options.allowRgb565, safeConfig, outMimeType)) Bitmap.Config.RGB_565 else safeConfig

        if (SDK_INT >= O && options.colorSpace != null) {
            inPreferredColorSpace = options.colorSpace
        }

        inMutable = SDK_INT < O || inPreferredConfig != Bitmap.Config.HARDWARE
        inScaled = false

        when {
            outWidth <= 0 || outHeight <= 0 -> {
                // This occurs if there was an error decoding the image's size.
                inSampleSize = 1
                inBitmap = null
            }
            size !is PixelSize -> {
                // This occurs if size is OriginalSize.
                inSampleSize = 1

                if (inMutable) {
                    inBitmap = pool.getDirtyOrNull(outWidth, outHeight, inPreferredConfig)
                }
            }
            SDK_INT >= KITKAT -> {
                val (width, height) = size
                inSampleSize = DecodeUtils.calculateInSampleSize(srcWidth, srcHeight, width, height, options.scale)

                // Calculate the image's density scaling multiple.
                val sampledSrcWidth = srcWidth / inSampleSize.toDouble()
                val sampledSrcHeight = srcHeight / inSampleSize.toDouble()
                val widthPercent = min(1.0, width / sampledSrcWidth)
                val heightPercent = min(1.0, height / sampledSrcHeight)
                val scale = when (options.scale) {
                    Scale.FILL -> max(widthPercent, heightPercent)
                    Scale.FIT -> min(widthPercent, heightPercent)
                }

                inScaled = scale != 1.0
                if (inScaled) {
                    inDensity = Int.MAX_VALUE
                    inTargetDensity = (scale * Int.MAX_VALUE).roundToInt()
                }

                if (inMutable) {
                    // Allocate a slightly larger Bitmap than necessary as the output Bitmap's dimensions may not match the
                    // requested dimensions exactly. This is due to intricacies in Android's downsampling algorithm.
                    val sampledOutWidth = outWidth / inSampleSize.toDouble()
                    val sampledOutHeight = outHeight / inSampleSize.toDouble()
                    inBitmap = pool.getDirtyOrNull(
                        width = ceil(scale * sampledOutWidth + 0.5).toInt(),
                        height = ceil(scale * sampledOutHeight + 0.5).toInt(),
                        config = inPreferredConfig
                    )
                }
            }
            else -> {
                // We can only re-use Bitmaps that exactly match the size of the image.
                if (inMutable) {
                    inBitmap = pool.getDirtyOrNull(outWidth, outHeight, inPreferredConfig)
                }

                // Sample size must be 1 if we are re-using a Bitmap.
                inSampleSize = if (inBitmap != null) {
                    1
                } else {
                    DecodeUtils.calculateInSampleSize(srcWidth, srcHeight, size.width, size.height, options.scale)
                }
            }
        }

        // Decode the Bitmap.
        val rawBitmap: Bitmap? = safeBufferedSource.use {
            BitmapFactory.decodeStream(it.inputStream(), null, this)
        }
        safeSource.exception?.let { exception ->
            rawBitmap?.let(pool::put)
            throw exception
        }

        // Apply any EXIF transformations.
        checkNotNull(rawBitmap) { "BitmapFactory returned a null Bitmap." }
        val bitmap = applyExifTransformations(pool, rawBitmap, inPreferredConfig, isFlipped, rotationDegrees)
        bitmap.density = Bitmap.DENSITY_NONE

        DecodeResult(
            drawable = bitmap.toDrawable(context),
            isSampled = inSampleSize > 1 || inScaled
        )
    }

    /** TODO: Peek the source to figure out its data type (and if it has alpha) instead of relying on the MIME type. */
    private fun allowRgb565(
        allowRgb565: Boolean,
        config: Bitmap.Config,
        mimeType: String?
    ): Boolean {
        return allowRgb565 && (SDK_INT < O || config == Bitmap.Config.ARGB_8888) && mimeType == MIME_TYPE_JPEG
    }

    /** NOTE: This method assumes [config] is not [Bitmap.Config.HARDWARE] if the image has to be transformed. */
    private fun applyExifTransformations(
        pool: BitmapPool,
        inBitmap: Bitmap,
        config: Bitmap.Config,
        isFlipped: Boolean,
        rotationDegrees: Int
    ): Bitmap {
        // Short circuit if there are no transformations to apply.
        val isRotated = rotationDegrees > 0
        if (!isFlipped && !isRotated) {
            return inBitmap
        }

        val matrix = Matrix()
        val centerX = inBitmap.width / 2f
        val centerY = inBitmap.height / 2f
        if (isFlipped) {
            matrix.postScale(-1f, 1f, centerX, centerY)
        }
        if (isRotated) {
            matrix.postRotate(rotationDegrees.toFloat(), centerX, centerY)
        }

        val rect = RectF(0f, 0f, inBitmap.width.toFloat(), inBitmap.height.toFloat())
        matrix.mapRect(rect)
        if (rect.left != 0f || rect.top != 0f) {
            matrix.postTranslate(-rect.left, -rect.top)
        }

        val outBitmap = if (rotationDegrees == 90 || rotationDegrees == 270) {
            pool.get(inBitmap.height, inBitmap.width, config)
        } else {
            pool.get(inBitmap.width, inBitmap.height, config)
        }

        outBitmap.applyCanvas {
            drawBitmap(inBitmap, matrix, paint)
        }
        pool.put(inBitmap)
        return outBitmap
    }

    /** Prevent [BitmapFactory.decodeStream] from swallowing [Exception]s. */
    private class ExceptionCatchingSource(delegate: Source) : ForwardingSource(delegate) {

        var exception: Exception? = null
            private set

        override fun read(sink: Buffer, byteCount: Long): Long {
            try {
                return super.read(sink, byteCount)
            } catch (e: Exception) {
                exception = e
                throw e
            }
        }
    }

    /** Wrap [delegate] so that it always returns [Int.MAX_VALUE] for [available]. */
    private class AvailableInputStream(private val delegate: InputStream) : InputStream() {

        override fun read() = delegate.read()

        override fun read(b: ByteArray) = delegate.read(b)

        override fun read(b: ByteArray, off: Int, len: Int) = delegate.read(b, off, len)

        override fun skip(n: Long) = delegate.skip(n)

        override fun available() = Int.MAX_VALUE

        override fun close() = delegate.close()

        override fun mark(readlimit: Int) = delegate.mark(readlimit)

        override fun reset() = delegate.reset()

        override fun markSupported() = delegate.markSupported()
    }
}
