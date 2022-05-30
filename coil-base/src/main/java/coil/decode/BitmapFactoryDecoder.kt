package coil.decode

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build.VERSION.SDK_INT
import coil.ImageLoader
import coil.fetch.SourceResult
import coil.request.Options
import coil.size.isOriginal
import coil.util.MIME_TYPE_JPEG
import coil.util.heightPx
import coil.util.toDrawable
import coil.util.toSoftware
import coil.util.widthPx
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import okio.Buffer
import okio.ForwardingSource
import okio.Source
import okio.buffer
import kotlin.math.roundToInt

/** The base [Decoder] that uses [BitmapFactory] to decode a given [ImageSource]. */
class BitmapFactoryDecoder(
    private val source: ImageSource,
    private val options: Options,
    private val parallelismLock: Semaphore = Semaphore(Int.MAX_VALUE),
    private val exifOrientationPolicy: ExifOrientationPolicy = ExifOrientationPolicy.RESPECT_PERFORMANCE
) : Decoder {

    @Deprecated(message = "Kept for binary compatibility.", level = DeprecationLevel.HIDDEN)
    constructor(
        source: ImageSource,
        options: Options
    ) : this(source, options)

    @Deprecated(message = "Kept for binary compatibility.", level = DeprecationLevel.HIDDEN)
    constructor(
        source: ImageSource,
        options: Options,
        parallelismLock: Semaphore = Semaphore(Int.MAX_VALUE)
    ) : this(source, options, parallelismLock)

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

        // Get the image's EXIF data.
        val exifData = ExifUtils.getExifData(outMimeType, safeBufferedSource, exifOrientationPolicy)
        safeSource.exception?.let { throw it }

        // Always create immutable bitmaps as they have better performance.
        inMutable = false

        if (SDK_INT >= 26 && options.colorSpace != null) {
            inPreferredColorSpace = options.colorSpace
        }
        inPremultiplied = options.premultipliedAlpha

        configureConfig(exifData)
        configureScale(exifData)

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

        // Reverse the EXIF transformations to get the original image.
        val bitmap = ExifUtils.reverseTransformations(outBitmap, exifData)

        return DecodeResult(
            drawable = bitmap.toDrawable(options.context),
            isSampled = inSampleSize > 1 || inScaled
        )
    }

    /** Compute and set [BitmapFactory.Options.inPreferredConfig]. */
    private fun BitmapFactory.Options.configureConfig(exifData: ExifData) {
        var config = options.config

        // Disable hardware bitmaps if we need to perform EXIF transformations.
        if (exifData.isFlipped || exifData.isRotated) {
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

        inPreferredConfig = config
    }

    /** Compute and set the scaling properties for [BitmapFactory.Options]. */
    private fun BitmapFactory.Options.configureScale(exifData: ExifData) {
        // Requests that request original size from a resource source need to be decoded with
        // respect to their intrinsic density.
        val metadata = source.metadata
        if (metadata is ResourceMetadata && options.size.isOriginal) {
            inSampleSize = 1
            inScaled = true
            inDensity = metadata.density
            inTargetDensity = options.context.resources.displayMetrics.densityDpi
            return
        }

        // This occurs if there was an error decoding the image's size.
        if (outWidth <= 0 || outHeight <= 0) {
            inSampleSize = 1
            inScaled = false
            return
        }

        // srcWidth and srcHeight are the original dimensions of the image after
        // EXIF transformations (but before sampling).
        val srcWidth = if (exifData.isSwapped) outHeight else outWidth
        val srcHeight = if (exifData.isSwapped) outWidth else outHeight

        val dstWidth = options.size.widthPx(options.scale) { srcWidth }
        val dstHeight = options.size.heightPx(options.scale) { srcHeight }

        // Calculate the image's sample size.
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

    class Factory(
        maxParallelism: Int = DEFAULT_MAX_PARALLELISM,
        private val exifOrientationPolicy: ExifOrientationPolicy = ExifOrientationPolicy.RESPECT_PERFORMANCE
    ) : Decoder.Factory {

        @Suppress("NEWER_VERSION_IN_SINCE_KOTLIN")
        @SinceKotlin("999.9") // Only public in Java.
        constructor() : this()

        @Deprecated(message = "Kept for binary compatibility.", level = DeprecationLevel.HIDDEN)
        constructor(maxParallelism: Int = DEFAULT_MAX_PARALLELISM) : this(maxParallelism)

        private val parallelismLock = Semaphore(maxParallelism)

        override fun create(result: SourceResult, options: Options, imageLoader: ImageLoader): Decoder {
            return BitmapFactoryDecoder(result.source, options, parallelismLock, exifOrientationPolicy)
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
        internal const val DEFAULT_MAX_PARALLELISM = 4
    }
}
