package coil3.decode

import android.graphics.ImageDecoder
import android.system.ErrnoException
import android.system.Os
import android.system.OsConstants.SEEK_SET
import androidx.annotation.RequiresApi
import androidx.core.graphics.decodeBitmap
import androidx.core.util.component1
import androidx.core.util.component2
import coil3.ImageLoader
import coil3.asImage
import coil3.decode.BitmapFactoryDecoder.Companion.DEFAULT_MAX_PARALLELISM
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import coil3.request.allowRgb565
import coil3.request.bitmapConfig
import coil3.request.colorSpace
import coil3.request.maxBitmapSize
import coil3.request.premultipliedAlpha
import coil3.size.Precision
import coil3.toAndroidUri
import coil3.util.component1
import coil3.util.component2
import coil3.util.isHardware
import kotlin.math.roundToInt
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import okio.FileSystem

@RequiresApi(29)
class StaticImageDecoder(
    private val source: ImageDecoder.Source,
    private val closeable: AutoCloseable,
    private val options: Options,
    private val parallelismLock: Semaphore,
) : Decoder {

    override suspend fun decode() = parallelismLock.withPermit {
        closeable.use {
            var isSampled = false
            val bitmap = source.decodeBitmap { info, _ ->
                // Configure the output image's size.
                val (srcWidth, srcHeight) = info.size
                val (dstWidth, dstHeight) = DecodeUtils.computeDstSize(
                    srcWidth = srcWidth,
                    srcHeight = srcHeight,
                    targetSize = options.size,
                    scale = options.scale,
                    maxSize = options.maxBitmapSize,
                )
                if (srcWidth > 0 && srcHeight > 0 &&
                    (srcWidth != dstWidth || srcHeight != dstHeight)
                ) {
                    val multiplier = DecodeUtils.computeSizeMultiplier(
                        srcWidth = srcWidth,
                        srcHeight = srcHeight,
                        dstWidth = dstWidth,
                        dstHeight = dstHeight,
                        scale = options.scale,
                    )

                    // Set the target size if the image is larger than the requested dimensions
                    // or the request requires exact dimensions.
                    isSampled = multiplier < 1
                    if (isSampled || options.precision == Precision.EXACT) {
                        val targetWidth = (multiplier * srcWidth).roundToInt()
                        val targetHeight = (multiplier * srcHeight).roundToInt()
                        setTargetSize(targetWidth, targetHeight)
                    }
                }

                // Configure any other attributes.
                configureImageDecoderProperties()
            }
            return@withPermit DecodeResult(
                image = bitmap.asImage(),
                isSampled = isSampled,
            )
        }
    }

    private fun ImageDecoder.configureImageDecoderProperties() {
        allocator = if (options.bitmapConfig.isHardware) {
            ImageDecoder.ALLOCATOR_HARDWARE
        } else {
            ImageDecoder.ALLOCATOR_SOFTWARE
        }
        memorySizePolicy = if (options.allowRgb565) {
            ImageDecoder.MEMORY_POLICY_LOW_RAM
        } else {
            ImageDecoder.MEMORY_POLICY_DEFAULT
        }
        if (options.colorSpace != null) {
            setTargetColorSpace(options.colorSpace)
        }
        isUnpremultipliedRequired = !options.premultipliedAlpha
    }

    class Factory(
        private val parallelismLock: Semaphore = Semaphore(DEFAULT_MAX_PARALLELISM),
    ) : Decoder.Factory {

        override fun create(
            result: SourceFetchResult,
            options: Options,
            imageLoader: ImageLoader,
        ): Decoder? {
            val source = result.source.imageDecoderSourceOrNull(options) ?: return null
            return StaticImageDecoder(source, result.source, options, parallelismLock)
        }

        private fun ImageSource.imageDecoderSourceOrNull(options: Options): ImageDecoder.Source? {
            if (fileSystem === FileSystem.SYSTEM) {
                val file = fileOrNull()
                if (file != null) {
                    return ImageDecoder.createSource(file.toFile())
                }
            }

            val metadata = metadata
            return when {
                metadata is AssetMetadata -> {
                    ImageDecoder.createSource(options.context.assets, metadata.filePath)
                }
                metadata is ContentMetadata -> try {
                    // Ensure the file descriptor supports lseek.
                    // https://github.com/coil-kt/coil/issues/2434
                    val asset = metadata.assetFileDescriptor
                    Os.lseek(asset.fileDescriptor, asset.startOffset, SEEK_SET)
                    ImageDecoder.createSource { asset }
                } catch (_: ErrnoException) {
                    ImageDecoder.createSource(options.context.contentResolver, metadata.uri.toAndroidUri())
                }
                metadata is ResourceMetadata && metadata.packageName == options.context.packageName -> {
                    ImageDecoder.createSource(options.context.resources, metadata.resId)
                }
                metadata is ByteBufferMetadata -> {
                    ImageDecoder.createSource(metadata.byteBuffer)
                }
                else -> null
            }
        }
    }
}
