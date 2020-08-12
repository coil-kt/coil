package coil.decode

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.os.Build.VERSION.SDK_INT
import androidx.core.graphics.applyCanvas
import androidx.exifinterface.media.ExifInterface
import coil.annotation.InternalCoilApi
import coil.bitmap.BitmapPool
import coil.size.PixelSize
import coil.size.Size
import coil.util.toDrawable
import coil.util.toSoftware
import okio.Buffer
import okio.BufferedSource
import okio.ForwardingSource
import okio.Source
import okio.buffer
import java.io.InputStream
import kotlin.math.ceil
import kotlin.math.roundToInt

/** The base [Decoder] that uses [BitmapFactory] to decode a given [BufferedSource]. */
internal class BitmapFactoryDecoder(private val context: Context) : Decoder {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    override fun handles(source: BufferedSource, mimeType: String?) = true

    @OptIn(InternalCoilApi::class)
    override suspend fun decode(
        pool: BitmapPool,
        source: BufferedSource,
        size: Size,
        options: Options
    ) = withInterruptibleSource(source) { interruptibleSource ->
        decodeInterruptible(pool, interruptibleSource, size, options)
    }

    private fun decodeInterruptible(
        pool: BitmapPool,
        source: Source,
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
        val isFlipped: Boolean
        val rotationDegrees: Int
        if (shouldReadExifData(outMimeType)) {
            val exifInterface = ExifInterface(ExifInterfaceInputStream(safeBufferedSource.peek().inputStream()))
            isFlipped = exifInterface.isFlipped
            rotationDegrees = exifInterface.rotationDegrees
        } else {
            isFlipped = false
            rotationDegrees = 0
        }

        // srcWidth and srcHeight are the dimensions of the image after EXIF transformations (but before sampling).
        val isSwapped = rotationDegrees == 90 || rotationDegrees == 270
        val srcWidth = if (isSwapped) outHeight else outWidth
        val srcHeight = if (isSwapped) outWidth else outHeight

        inPreferredConfig = computeConfig(options, isFlipped, rotationDegrees)

        if (SDK_INT >= 26 && options.colorSpace != null) {
            inPreferredColorSpace = options.colorSpace
        }

        // Create immutable bitmaps on API 24 and above.
        inMutable = SDK_INT < 24
        inScaled = false

        when {
            outWidth <= 0 || outHeight <= 0 -> {
                // This occurs if there was an error decoding the image's size.
                inSampleSize = 1
                inScaled = false
                inBitmap = null
            }
            size !is PixelSize -> {
                // This occurs if size is OriginalSize.
                inSampleSize = 1
                inScaled = false

                if (inMutable) {
                    inBitmap = pool.getDirty(outWidth, outHeight, inPreferredConfig)
                }
            }
            else -> {
                val (width, height) = size
                inSampleSize = DecodeUtils.calculateInSampleSize(srcWidth, srcHeight, width, height, options.scale)

                // Calculate the image's density scaling multiple.
                val rawScale = DecodeUtils.computeSizeMultiplier(
                    srcWidth = srcWidth / inSampleSize.toDouble(),
                    srcHeight = srcHeight / inSampleSize.toDouble(),
                    dstWidth = width.toDouble(),
                    dstHeight = height.toDouble(),
                    scale = options.scale
                )

                // Avoid loading the image larger than its original dimensions if allowed.
                val scale = if (options.allowInexactSize) rawScale.coerceAtMost(1.0) else rawScale

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

                if (inMutable) {
                    inBitmap = when {
                        // If we're not scaling the image, use the image's source dimensions.
                        inSampleSize == 1 && !inScaled -> {
                            pool.getDirty(outWidth, outHeight, inPreferredConfig)
                        }
                        // We can only re-use bitmaps that don't match the image's source dimensions on API 19 and above.
                        SDK_INT >= 19 -> {
                            // Request a slightly larger bitmap than necessary as the output bitmap's dimensions
                            // may not match the requested dimensions exactly. This is due to intricacies in Android's
                            // downsampling algorithm across different API levels.
                            val sampledOutWidth = outWidth / inSampleSize.toDouble()
                            val sampledOutHeight = outHeight / inSampleSize.toDouble()
                            pool.getDirty(
                                width = ceil(scale * sampledOutWidth + 0.5).toInt(),
                                height = ceil(scale * sampledOutHeight + 0.5).toInt(),
                                config = inPreferredConfig
                            )
                        }
                        // Else, let BitmapFactory allocate the bitmap internally.
                        else -> null
                    }
                }
            }
        }

        // Keep a reference to the input bitmap so it can be returned to
        // the pool if the decode doesn't complete successfully.
        val inBitmap: Bitmap? = inBitmap

        // Decode the bitmap.
        var outBitmap: Bitmap? = null
        try {
            outBitmap = safeBufferedSource.use {
                // outMimeType is null for unsupported formats as well as on older devices with incomplete WebP support.
                if (SDK_INT < 19 && outMimeType == null) {
                    val bytes = it.readByteArray()
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, this)
                } else {
                    BitmapFactory.decodeStream(it.inputStream(), null, this)
                }
            }
            safeSource.exception?.let { throw it }
        } catch (throwable: Throwable) {
            inBitmap?.let(pool::put)
            if (outBitmap !== inBitmap) {
                outBitmap?.let(pool::put)
            }
            throw throwable
        }

        // Apply any EXIF transformations.
        checkNotNull(outBitmap) {
            "BitmapFactory returned a null Bitmap. Often this means BitmapFactory could not decode the image data " +
                "read from the input source (e.g. network or disk) as it's not encoded as a valid image format."
        }
        val bitmap = applyExifTransformations(pool, outBitmap, inPreferredConfig, isFlipped, rotationDegrees)
        bitmap.density = Bitmap.DENSITY_NONE

        DecodeResult(
            drawable = bitmap.toDrawable(context),
            isSampled = inSampleSize > 1 || inScaled
        )
    }

    /** Return true if we should read the image's EXIF data. */
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
        // TODO: Peek the source to figure out its format (and if it has alpha) instead of relying on the MIME type.
        if (options.allowRgb565 && config == Bitmap.Config.ARGB_8888 && outMimeType == MIME_TYPE_JPEG) {
            config = Bitmap.Config.RGB_565
        }

        // High color depth images must be decoded as either RGBA_F16 or HARDWARE.
        if (SDK_INT >= 26 && outConfig == Bitmap.Config.RGBA_F16 && config != Bitmap.Config.HARDWARE) {
            config = Bitmap.Config.RGBA_F16
        }

        return config
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

    /** Wrap [delegate] so that it works with [ExifInterface]. */
    private class ExifInterfaceInputStream(private val delegate: InputStream) : InputStream() {

        override fun read() = delegate.read()

        override fun read(b: ByteArray) = delegate.read(b)

        override fun read(b: ByteArray, off: Int, len: Int) = delegate.read(b, off, len)

        override fun skip(n: Long) = delegate.skip(n)

        // Ensure that this value is always larger than the size of the image
        // so ExifInterface won't stop reading the stream prematurely.
        override fun available() = 1024 * 1024 * 1024

        override fun close() = delegate.close()

        override fun mark(readlimit: Int) = delegate.mark(readlimit)

        override fun reset() = delegate.reset()

        override fun markSupported() = delegate.markSupported()
    }

    companion object {
        private const val MIME_TYPE_JPEG = "image/jpeg"
        private const val MIME_TYPE_WEBP = "image/webp"
        private const val MIME_TYPE_HEIC = "image/heic"
        private const val MIME_TYPE_HEIF = "image/heif"

        // NOTE: We don't support PNG EXIF data as it's very rarely used and requires buffering
        // the entire file into memory. All of the supported formats short circuit when the EXIF
        // chunk is found (often near the top of the file).
        private val SUPPORTED_EXIF_MIME_TYPES = arrayOf(MIME_TYPE_JPEG, MIME_TYPE_WEBP, MIME_TYPE_HEIC, MIME_TYPE_HEIF)
    }
}
