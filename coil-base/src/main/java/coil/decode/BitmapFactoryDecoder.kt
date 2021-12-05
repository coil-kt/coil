package coil.decode

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.os.Build.VERSION.SDK_INT
import androidx.core.graphics.applyCanvas
import androidx.core.graphics.createBitmap
import androidx.exifinterface.media.ExifInterface
import coil.ImageLoader
import coil.fetch.SourceResult
import coil.request.Options
import coil.size.pxOrElse
import coil.util.toDrawable
import coil.util.toSoftware
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import okio.Buffer
import okio.ForwardingSource
import okio.Source
import okio.buffer
import java.io.InputStream
import kotlin.math.roundToInt

/** The base [Decoder] that uses [BitmapFactory] to decode a given [ImageSource]. */
class BitmapFactoryDecoder @JvmOverloads constructor(
    private val source: ImageSource,
    private val options: Options,
    private val parallelismLock: Semaphore = Semaphore(Int.MAX_VALUE)
) : Decoder {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    override suspend fun decode() = parallelismLock.withPermit {
        runInterruptible { BitmapFactory.Options().decode() }
    }

    private fun BitmapFactory.Options.decode(): DecodeResult {
        val safeSource = ExceptionCatchingSource(source.source())
        val safeBufferedSource = safeSource.buffer()

        // Read the image's dimensions.
        inJustDecodeBounds = true
        BitmapFactory.decodeStream(safeBufferedSource.peek().inputStream(), null, this)
        safeSource.exception?.let { throw it }
        inJustDecodeBounds = false

        // Read the image's EXIF data.
        val isFlipped: Boolean
        val rotationDegrees: Int
        if (shouldReadExifData(outMimeType)) {
            val inputStream = safeBufferedSource.peek().inputStream()
            val exifInterface = ExifInterface(ExifInterfaceInputStream(inputStream))
            safeSource.exception?.let { throw it }
            isFlipped = exifInterface.isFlipped
            rotationDegrees = exifInterface.rotationDegrees
        } else {
            isFlipped = false
            rotationDegrees = 0
        }

        // srcWidth and srcHeight are the dimensions of the image after
        // EXIF transformations (but before sampling).
        val isSwapped = rotationDegrees == 90 || rotationDegrees == 270
        val srcWidth = if (isSwapped) outHeight else outWidth
        val srcHeight = if (isSwapped) outWidth else outHeight

        inPreferredConfig = computeConfig(options, isFlipped, rotationDegrees)
        inPremultiplied = options.premultipliedAlpha

        if (SDK_INT >= 26 && options.colorSpace != null) {
            inPreferredColorSpace = options.colorSpace
        }

        // Always create immutable bitmaps as they have performance benefits.
        inMutable = false

        if (outWidth > 0 && outHeight > 0) {
            val (width, height) = options.size
            val dstWidth = width.pxOrElse { srcWidth }
            val dstHeight = height.pxOrElse { srcHeight }
            inSampleSize = DecodeUtils.calculateInSampleSize(
                srcWidth = srcWidth,
                srcHeight = srcHeight,
                dstWidth = dstWidth,
                dstHeight = dstHeight,
                scale = options.scale
            )

            // Calculate the image's density scaling multiple.
            var scale = DecodeUtils.computeSizeMultiplier(
                srcWidth = srcWidth / inSampleSize.toDouble(),
                srcHeight = srcHeight / inSampleSize.toDouble(),
                dstWidth = dstWidth.toDouble(),
                dstHeight = dstHeight.toDouble(),
                scale = options.scale
            )

            // Only upscale the image if the request requests an exact size.
            if (options.allowInexactSize) {
                scale = scale.coerceAtMost(1.0)
            }

            inScaled = scale != 1.0
            if (inScaled) {
                if (scale > 1) {
                    // Upscale
                    inDensity = (Int.MAX_VALUE / scale).roundToInt()
                    inTargetDensity = Int.MAX_VALUE
                } else {
                    // Downscale
                    inDensity = Int.MAX_VALUE
                    inTargetDensity = (Int.MAX_VALUE * scale).roundToInt()
                }
            }
        } else {
            // This occurs if there was an error decoding the image's size.
            inSampleSize = 1
            inScaled = false
        }

        // Decode the bitmap.
        val outBitmap: Bitmap? = safeBufferedSource.use {
            BitmapFactory.decodeStream(it.inputStream(), null, this)
        }
        safeSource.exception?.let { throw it }
        checkNotNull(outBitmap) {
            "BitmapFactory returned a null bitmap. Often this means BitmapFactory could not " +
                "decode the image data read from the input source (e.g. network, disk, or " +
                "memory) as it's not encoded as a valid image format."
        }

        // Fix the incorrect density created by overloading inDensity/inTargetDensity.
        outBitmap.density = options.context.resources.displayMetrics.densityDpi

        // Apply any EXIF transformations.
        val bitmap = applyExifTransformations(outBitmap, inPreferredConfig, isFlipped, rotationDegrees)

        return DecodeResult(
            drawable = bitmap.toDrawable(options.context),
            isSampled = inSampleSize > 1 || inScaled
        )
    }

    /** Return 'true' if we should read the image's EXIF data. */
    private fun shouldReadExifData(mimeType: String?): Boolean {
        return mimeType != null && mimeType in SUPPORTED_EXIF_MIME_TYPES
    }

    /** Compute and return [BitmapFactory.Options.inPreferredConfig]. */
    private fun BitmapFactory.Options.computeConfig(
        options: Options,
        isFlipped: Boolean,
        rotationDegrees: Int
    ): Bitmap.Config {
        var config = options.config

        // Disable hardware bitmaps if we need to perform EXIF transformations.
        if (isFlipped || rotationDegrees > 0) {
            config = config.toSoftware()
        }

        // Decode the image as RGB_565 as an optimization if allowed.
        if (options.allowRgb565 && config == Bitmap.Config.ARGB_8888 && outMimeType == MIME_TYPE_JPEG) {
            config = Bitmap.Config.RGB_565
        }

        // High color depth images must be decoded as either RGBA_F16 or HARDWARE.
        if (SDK_INT >= 26 && outConfig == Bitmap.Config.RGBA_F16 && config != Bitmap.Config.HARDWARE) {
            config = Bitmap.Config.RGBA_F16
        }

        return config
    }

    /** This method assumes [config] is not [Bitmap.Config.HARDWARE]. */
    private fun applyExifTransformations(
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

    class Factory @JvmOverloads constructor(
        maxParallelism: Int = DEFAULT_MAX_PARALLELISM
    ) : Decoder.Factory {

        private val parallelismLock = Semaphore(maxParallelism)

        override fun create(result: SourceResult, options: Options, imageLoader: ImageLoader): Decoder {
            return BitmapFactoryDecoder(result.source, options, parallelismLock)
        }

        override fun equals(other: Any?) = other is Factory

        override fun hashCode() = javaClass.hashCode()
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

    /** Wrap [delegate] so that it works with [ExifInterface]. */
    private class ExifInterfaceInputStream(private val delegate: InputStream) : InputStream() {

        // Ensure that this value is always larger than the size of the image
        // so ExifInterface won't stop reading the stream prematurely.
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
    }

    internal companion object {
        private const val MIME_TYPE_JPEG = "image/jpeg"
        private const val MIME_TYPE_WEBP = "image/webp"
        private const val MIME_TYPE_HEIC = "image/heic"
        private const val MIME_TYPE_HEIF = "image/heif"
        private const val GIGABYTE_IN_BYTES = 1024 * 1024 * 1024
        internal const val DEFAULT_MAX_PARALLELISM = 4

        // NOTE: We don't support PNG EXIF data as it's very rarely used and requires buffering
        // the entire file into memory. All of the supported formats short circuit when the EXIF
        // chunk is found (often near the top of the file).
        private val SUPPORTED_EXIF_MIME_TYPES =
            arrayOf(MIME_TYPE_JPEG, MIME_TYPE_WEBP, MIME_TYPE_HEIC, MIME_TYPE_HEIF)
    }
}
