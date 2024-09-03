package coil3.decode

import android.graphics.ImageDecoder
import android.os.Build.VERSION.SDK_INT
import android.system.ErrnoException
import android.system.Os
import android.system.OsConstants.SEEK_SET
import androidx.annotation.RequiresApi
import androidx.core.graphics.decodeBitmap
import androidx.core.util.component1
import androidx.core.util.component2
import coil3.ImageLoader
import coil3.annotation.InternalCoilApi
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
            val source = result.source.toImageDecoderSource(options, animated = false) ?: return null
            return StaticImageDecoder(source, result.source, options, parallelismLock)
        }
    }
}

@InternalCoilApi
@RequiresApi(28)
fun ImageSource.toImageDecoderSource(options: Options, animated: Boolean): ImageDecoder.Source? {
    if (fileSystem === FileSystem.SYSTEM) {
        val file = fileOrNull()
        if (file != null) {
            return ImageDecoder.createSource(file.toFile())
        }
    }

    val metadata = metadata
    when {
        metadata is AssetMetadata -> {
            return ImageDecoder.createSource(options.context.assets, metadata.filePath)
        }
        metadata is ContentMetadata -> {
            if (SDK_INT >= 29) {
                try {
                    // Ensure the file descriptor supports lseek.
                    // https://github.com/coil-kt/coil/issues/2434
                    val asset = metadata.assetFileDescriptor
                    Os.lseek(asset.fileDescriptor, asset.startOffset, SEEK_SET)
                    return ImageDecoder.createSource { asset }
                } catch (_: ErrnoException) {}
            }
        }
        metadata is ResourceMetadata && metadata.packageName == options.context.packageName -> {
            return ImageDecoder.createSource(options.context.resources, metadata.resId)
        }
        metadata is ByteBufferMetadata && (SDK_INT >= 30 || !animated || metadata.byteBuffer.isDirect) -> {
            return ImageDecoder.createSource(metadata.byteBuffer)
        }
    }

    return null
}
