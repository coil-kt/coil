package coil3.decode

import android.graphics.ImageDecoder
import androidx.annotation.RequiresApi
import androidx.core.graphics.decodeBitmap
import androidx.core.util.component1
import androidx.core.util.component2
import coil3.ImageLoader
import coil3.annotation.ExperimentalCoilApi
import coil3.asCoilImage
import coil3.decode.BitmapFactoryDecoder.Companion.DEFAULT_MAX_PARALLELISM
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import coil3.request.allowRgb565
import coil3.request.bitmapConfig
import coil3.request.colorSpace
import coil3.request.premultipliedAlpha
import coil3.toAndroidUri
import coil3.util.heightPx
import coil3.util.isHardware
import coil3.util.widthPx
import kotlin.math.roundToInt
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

@ExperimentalCoilApi
@RequiresApi(28)
class StaticImageDecoderDecoder(
    private val source: ImageDecoder.Source,
    private val toClose: ImageSource,
    private val options: Options,
    private val parallelismLock: Semaphore = Semaphore(Int.MAX_VALUE),
) : Decoder {

    override suspend fun decode() = parallelismLock.withPermit {
        var isSampled = false
        val image = run {
            var imageDecoder: ImageDecoder? = null
            try {
                source.decodeBitmap { info, _ ->
                    // Capture the image decoder to manually close it later.
                    imageDecoder = this

                    // Configure the output image's size.
                    val (srcWidth, srcHeight) = info.size
                    val dstWidth = options.size.widthPx(options.scale) { srcWidth }
                    val dstHeight = options.size.heightPx(options.scale) { srcHeight }
                    if (srcWidth > 0 && srcHeight > 0 &&
                        (srcWidth != dstWidth || srcHeight != dstHeight)) {
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
                        if (isSampled || !options.allowInexactSize) {
                            val targetWidth = (multiplier * srcWidth).roundToInt()
                            val targetHeight = (multiplier * srcHeight).roundToInt()
                            setTargetSize(targetWidth, targetHeight)
                        }
                    }

                    // Configure any other attributes.
                    configureImageDecoderProperties()
                }
            } finally {
                imageDecoder?.close()
                toClose.close()
            }
        }
        DecodeResult(
            image = image.asCoilImage(),
            isSampled = isSampled,
        )
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

    class Factory @JvmOverloads constructor(
        maxParallelism: Int = DEFAULT_MAX_PARALLELISM,
    ) : Decoder.Factory {
        private val parallelismLock = Semaphore(maxParallelism)

        override fun create(
            result: SourceFetchResult,
            options: Options,
            imageLoader: ImageLoader,
        ): Decoder? {
            val source = result.source.imageDecoderSourceOrNull(options) ?: return null
            return StaticImageDecoderDecoder(source, result.source, options, parallelismLock)
        }
    }
}

@RequiresApi(28)
private fun ImageSource.imageDecoderSourceOrNull(options: Options): ImageDecoder.Source? {
    val file = fileOrNull()
    if (file != null) {
        return ImageDecoder.createSource(file.toFile())
    }

    val metadata = metadata
    if (metadata is AssetMetadata) {
        return ImageDecoder.createSource(options.context.assets, metadata.filePath)
    }
    if (metadata is ContentMetadata) {
        return ImageDecoder.createSource(options.context.contentResolver, metadata.uri.toAndroidUri())
    }
    if (metadata is ResourceMetadata && metadata.packageName == options.context.packageName) {
        return ImageDecoder.createSource(options.context.resources, metadata.resId)
    }

    return null
}
