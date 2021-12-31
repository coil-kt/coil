package coil.decode

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build.VERSION.SDK_INT
import androidx.exifinterface.media.ExifInterface
import coil.ImageLoader
import coil.decode.Exif.Data.Companion.toExifData
import coil.decode.Exif.applyExifTransformations
import coil.fetch.SourceResult
import coil.request.Options
import coil.size.isOriginal
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
import kotlin.math.roundToInt

/** The base [Decoder] that uses [BitmapFactory] to decode a given [ImageSource]. */
class BitmapFactoryDecoder @JvmOverloads constructor(
    private val source: ImageSource,
    private val options: Options,
    private val parallelismLock: Semaphore = Semaphore(Int.MAX_VALUE)
) : Decoder {

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
        val exifData = if (Exif.shouldReadExifData(outMimeType)) {
            val inputStream = safeBufferedSource.peek().inputStream()
            val exifInterface = ExifInterface(ExifInterfaceInputStream(inputStream))
            safeSource.exception?.let { throw it }
            exifInterface.toExifData()
        } else {
            Exif.Data.DEFAULT
        }

        // srcWidth and srcHeight are the dimensions of the image after
        // EXIF transformations (but before sampling).
        val srcWidth = if (exifData.isSwapped) outHeight else outWidth
        val srcHeight = if (exifData.isSwapped) outWidth else outHeight

        inPreferredConfig = computeConfig(options, exifData)
        inPremultiplied = options.premultipliedAlpha

        if (SDK_INT >= 26 && options.colorSpace != null) {
            inPreferredColorSpace = options.colorSpace
        }

        // Always create immutable bitmaps as they have performance benefits.
        inMutable = false

        configureScale(srcWidth, srcHeight)

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
        val bitmap = applyExifTransformations(outBitmap, inPreferredConfig, exifData)

        return DecodeResult(
            drawable = bitmap.toDrawable(options.context),
            isSampled = inSampleSize > 1 || inScaled
        )
    }

    /** Compute and return [BitmapFactory.Options.inPreferredConfig]. */
    private fun BitmapFactory.Options.computeConfig(
        options: Options,
        exifData: Exif.Data
    ): Bitmap.Config {
        var config = options.config

        // Disable hardware bitmaps if we need to perform EXIF transformations.
        if (exifData.isFlipped || exifData.rotationDegrees > 0) {
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

    /**
     * Configure scaling of the output bitmap by considering density, sample size and the given
     * scaling algorithm.
     */
    private fun BitmapFactory.Options.configureScale(
        srcWidth: Int,
        srcHeight: Int
    ) {
        when {
            options.size.isOriginal && source.metadata is ResourceMetadata -> {
                inScaled = true
                // Read the resource density if available
                inDensity = (source.metadata as ResourceMetadata).density
                inTargetDensity = options.context.resources.displayMetrics.densityDpi
                // Clear outWidth and outHeight so that BitmapFactory will handle scaling itself.
                outWidth = 0
                outHeight = 0
            }
            outWidth > 0 && outHeight > 0 -> {
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

                // Only upscale the image if the options require an exact size.
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
            }
            else -> {
                // This occurs if there was an error decoding the image's size.
                inSampleSize = 1
                inScaled = false
            }
        }
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

    internal companion object {
        internal const val MIME_TYPE_JPEG = "image/jpeg"
        internal const val MIME_TYPE_WEBP = "image/webp"
        internal const val MIME_TYPE_HEIC = "image/heic"
        internal const val MIME_TYPE_HEIF = "image/heif"
        internal const val DEFAULT_MAX_PARALLELISM = 4
    }
}
